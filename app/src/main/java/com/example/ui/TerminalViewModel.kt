package com.example.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.market.SimulatedMarketEngine
import com.aurumtrade.shared.market.TechnicalSignalMatch
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.AssetCategory
import com.aurumtrade.shared.model.BrokerOrderKind
import com.aurumtrade.shared.model.CustomWatchlist
import com.aurumtrade.shared.model.JournalTag
import com.aurumtrade.shared.model.LeaderboardEntry
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.OrderBasket
import com.aurumtrade.shared.model.OrderProductType
import com.aurumtrade.shared.model.PendingOrder
import com.aurumtrade.shared.model.Position
import com.aurumtrade.shared.model.PriceAlert
import com.aurumtrade.shared.model.SipPlan
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.TerminalThemeMode
import com.aurumtrade.shared.model.TradeRecord
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.util.formatInr
import com.example.data.FamGatewayClient
import com.example.data.TradingRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

sealed interface CheckoutState {
  data object Idle : CheckoutState
  data object CreatingOrder : CheckoutState
  data class WebViewOpen(
    val orderId: String,
    val checkoutUrl: String,
    val planName: String,
    val amountInr: Double
  ) : CheckoutState
  data class ProActivated(
    val planName: String,
    val expiresAtMillis: Long
  ) : CheckoutState
}

class TerminalViewModel(
  private val repository: TradingRepository,
  private val famGatewayClient: FamGatewayClient,
  private val currentUserId: String
) : ViewModel() {

  private val marketEngine = SimulatedMarketEngine()
  val quotes: StateFlow<Map<String, LiveQuote>> = marketEngine.quotes

  val signals: StateFlow<List<TechnicalSignalMatch>> = quotes
    .map { SimulatedMarketEngine.scanTechnicalSignals(it) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  private val _selectedSymbol = MutableStateFlow("RELIANCE")
  val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

  private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
  val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 24)
  val events: SharedFlow<String> = _events.asSharedFlow()

  private var pollingJob: Job? = null
  private val bracketExecutingSymbols = mutableSetOf<String>()
  private val trailingUpdatingSymbols = mutableSetOf<String>()
  private val pendingExecutingIds = mutableSetOf<String>()
  private val triggeringAlertIds = mutableSetOf<String>()

  private fun <T> Flow<T>.toUiStateFlow(tag: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Stream error [$tag]", root)
        emit(UiState.Error(root.message ?: "Failed to load $tag"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
      )

  val userState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

  val positionsState: StateFlow<UiState<List<Position>>> =
    repository.observePositions(currentUserId).toUiStateFlow("positions")

  val tradesState: StateFlow<UiState<List<TradeRecord>>> =
    repository.observeTrades(currentUserId).toUiStateFlow("trades")

  val pendingOrdersState: StateFlow<UiState<List<PendingOrder>>> =
    repository.observePendingOrders(currentUserId).toUiStateFlow("pending_orders")

  val customWatchlistsState: StateFlow<UiState<List<CustomWatchlist>>> =
    repository.observeCustomWatchlists(currentUserId).toUiStateFlow("watchlists")

  val basketsState: StateFlow<UiState<List<OrderBasket>>> =
    repository.observeBaskets(currentUserId).toUiStateFlow("baskets")

  val sipPlansState: StateFlow<UiState<List<SipPlan>>> =
    repository.observeSipPlans(currentUserId).toUiStateFlow("sip_plans")

  val alertsState: StateFlow<UiState<List<PriceAlert>>> =
    repository.observePriceAlerts(currentUserId).toUiStateFlow("price_alerts")

  val subscriptionPlansState: StateFlow<UiState<List<SubscriptionPlan>>> =
    repository.observeSubscriptionPlans(currentUserId).toUiStateFlow("subscription_plans")

  val appConfigState: StateFlow<AppConfig> =
    repository.observeAppConfig(currentUserId)
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(AppConfig())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppConfig()
      )

  val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
    repository.observeLeaderboard(currentUserId).toUiStateFlow("leaderboard")

  init {
    viewModelScope.launch {
      val currentUser = Firebase.auth.currentUser
      if (currentUser != null && currentUser.uid == currentUserId) {
        repository.ensureUserProfile(currentUser, appConfigState.value.startingVirtualCash)
      }
    }

    viewModelScope.launch {
      repository.observeMarketAssets(currentUserId)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
        }
        .collect { assets ->
          marketEngine.setAssets(assets)
        }
    }

    // Real-time simulated market ticks + bracket SL/TP/Trailing SL + pending Limit/SL/GTT + alerts
    viewModelScope.launch {
      while (isActive) {
        delay(1800L)
        marketEngine.stepMarketTick()
        evaluateAutomatedRules()
      }
    }

    // Automatically close WebView if webhook activates Pro via Firestore snapshot
    viewModelScope.launch {
      userState.collect { state ->
        val profile = (state as? UiState.Success)?.data
        val activeWebView = _checkoutState.value as? CheckoutState.WebViewOpen
        if (profile != null && profile.hasValidProAccess && activeWebView != null) {
          pollingJob?.cancel()
          _checkoutState.value = CheckoutState.ProActivated(
            planName = profile.planName.ifBlank { activeWebView.planName },
            expiresAtMillis = profile.planExpiresAtMillis
          )
        }
      }
    }
  }

  private fun evaluateAutomatedRules() {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    if (!profile.hasValidProAccess) return

    val currentQuotes = quotes.value
    val positions = (positionsState.value as? UiState.Success)?.data.orEmpty()
    val pendingOrders = (pendingOrdersState.value as? UiState.Success)?.data.orEmpty()
    val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()

    // 1. Evaluate Trailing Stop-Loss + Bracket SL/TP on open LONG and SHORT positions
    positions.forEach { pos ->
      val quote = currentQuotes[pos.symbol] ?: return@forEach
      val livePrice = quote.currentPrice

      if (pos.trailingSlDistance > 0.0) {
        if (!pos.isShort) {
          val water = max(pos.highWaterPrice, pos.avgPrice)
          if (livePrice > water * 1.001) {
            val newSl = max(pos.stopLossPrice, max(0.05, livePrice - pos.trailingSlDistance))
            if (newSl > pos.stopLossPrice + 0.05 && trailingUpdatingSymbols.add(pos.symbol)) {
              viewModelScope.launch {
                try {
                  repository.updatePositionTrailingSl(pos.symbol, newSl, livePrice)
                } finally {
                  trailingUpdatingSymbols.remove(pos.symbol)
                }
              }
            }
          }
        } else {
          val lowWater = if (pos.highWaterPrice > 0.0) min(pos.highWaterPrice, pos.avgPrice) else pos.avgPrice
          if (livePrice < lowWater * 0.999) {
            val candidateSl = livePrice + pos.trailingSlDistance
            val newSl = if (pos.stopLossPrice > 0.0) min(pos.stopLossPrice, candidateSl) else candidateSl
            if (pos.stopLossPrice == 0.0 || newSl < pos.stopLossPrice - 0.05) {
              if (trailingUpdatingSymbols.add(pos.symbol)) {
                viewModelScope.launch {
                  try {
                    repository.updatePositionTrailingSl(pos.symbol, newSl, livePrice)
                  } finally {
                    trailingUpdatingSymbols.remove(pos.symbol)
                  }
                }
              }
            }
          }
        }
      }

      val hitSl = if (!pos.isShort) {
        pos.stopLossPrice > 0.0 && livePrice <= pos.stopLossPrice
      } else {
        pos.stopLossPrice > 0.0 && livePrice >= pos.stopLossPrice
      }
      val hitTp = if (!pos.isShort) {
        pos.takeProfitPrice > 0.0 && livePrice >= pos.takeProfitPrice
      } else {
        pos.takeProfitPrice > 0.0 && livePrice <= pos.takeProfitPrice
      }

      if ((hitSl || hitTp) && bracketExecutingSymbols.add(pos.symbol)) {
        val orderType = when {
          hitTp -> "BRACKET_TP"
          pos.trailingSlDistance > 0.0 -> "BRACKET_TSL"
          else -> "BRACKET_SL"
        }
        val label = when {
          hitTp -> "Take-Profit"
          pos.trailingSlDistance > 0.0 -> "Trailing Stop-Loss"
          else -> "Stop-Loss"
        }
        viewModelScope.launch {
          try {
            val res = if (!pos.isShort) {
              repository.executeSellOrder(
                asset = quote.asset,
                quantity = pos.quantity,
                price = livePrice,
                orderType = orderType,
                notes = "Auto $label triggered at ${formatInr(livePrice)}"
              )
            } else {
              repository.executeBuyOrder(
                asset = quote.asset,
                quantity = pos.quantity,
                price = livePrice,
                leverage = pos.leverage,
                stopLossPrice = 0.0,
                takeProfitPrice = 0.0,
                orderType = orderType,
                notes = "Auto $label covered short at ${formatInr(livePrice)}"
              )
            }
            res.onSuccess { pnl ->
              _events.tryEmit("$label triggered for ${pos.symbol}: P&L ${formatInr(pnl, showSign = true)}")
              syncUserLeaderboard()
            }
          } finally {
            bracketExecutingSymbols.remove(pos.symbol)
          }
        }
      }
    }

    // 2. Evaluate Pending Limit / SL / SL-M / GTT orders
    pendingOrders.filter { it.status == "OPEN" }.forEach { order ->
      val quote = currentQuotes[order.symbol] ?: return@forEach
      val livePrice = quote.currentPrice
      val shouldExecute = when (order.orderKind) {
        BrokerOrderKind.LIMIT.name, BrokerOrderKind.GTT.name -> {
          if (order.side == "BUY") livePrice <= order.limitPrice && order.limitPrice > 0.0
          else livePrice >= order.limitPrice && order.limitPrice > 0.0
        }
        BrokerOrderKind.SL.name, BrokerOrderKind.SL_M.name -> {
          if (order.side == "BUY") livePrice >= order.triggerPrice && order.triggerPrice > 0.0
          else livePrice <= order.triggerPrice && order.triggerPrice > 0.0
        }
        else -> false
      }

      if (shouldExecute && pendingExecutingIds.add(order.id)) {
        viewModelScope.launch {
          try {
            val execRes = if (order.side == "BUY") {
              repository.executeBuyOrder(
                asset = quote.asset,
                quantity = order.quantity,
                price = livePrice,
                leverage = order.leverage,
                stopLossPrice = order.stopLossPrice,
                takeProfitPrice = order.takeProfitPrice,
                trailingSlDistance = order.trailingSlDistance,
                orderType = order.orderKind,
                notes = "Auto-triggered ${order.orderKind} order"
              )
            } else {
              repository.executeSellOrder(
                asset = quote.asset,
                quantity = order.quantity,
                price = livePrice,
                leverage = order.leverage,
                stopLossPrice = order.stopLossPrice,
                takeProfitPrice = order.takeProfitPrice,
                trailingSlDistance = order.trailingSlDistance,
                orderType = order.orderKind,
                notes = "Auto-triggered ${order.orderKind} order"
              )
            }
            execRes.onSuccess { pnl ->
              repository.updatePendingOrderStatus(order.id, "EXECUTED")
              val suffix = if (pnl != 0.0) " • P&L ${formatInr(pnl, showSign = true)}" else ""
              _events.tryEmit("${order.orderKind} ${order.side} filled: ${order.quantity} ${order.symbol} @ ${formatInr(livePrice)}$suffix")
              syncUserLeaderboard()
            }.onFailure { err ->
              _events.tryEmit("Pending order ${order.symbol} failed: ${err.message}")
            }
          } finally {
            pendingExecutingIds.remove(order.id)
          }
        }
      }
    }

    // 3. Evaluate Price Alerts
    alerts.filter { !it.triggered }.forEach { alert ->
      val livePrice = currentQuotes[alert.symbol]?.currentPrice ?: return@forEach
      val triggered = when (alert.direction) {
        "ABOVE" -> livePrice >= alert.targetPrice
        "BELOW" -> livePrice <= alert.targetPrice
        else -> false
      }
      if (triggered && triggeringAlertIds.add(alert.id)) {
        viewModelScope.launch {
          try {
            repository.markAlertTriggered(alert.id)
            _events.tryEmit("Price Alert: ${alert.symbol} hit ${formatInr(livePrice)}")
          } finally {
            triggeringAlertIds.remove(alert.id)
          }
        }
      }
    }
  }

  fun selectSymbol(symbol: String) {
    _selectedSymbol.value = symbol
  }

  fun placeBrokerOrder(
    asset: MarketAsset,
    side: String,
    orderKind: BrokerOrderKind,
    productType: OrderProductType,
    quantity: Double,
    limitPrice: Double,
    triggerPrice: Double,
    leverage: Long,
    stopLossPrice: Double,
    takeProfitPrice: Double,
    trailingSlDistance: Double,
    journalTag: JournalTag
  ) {
    val livePrice = quotes.value[asset.symbol]?.currentPrice ?: asset.basePrice
    val effectiveTag = if (userState.value.let { (it as? UiState.Success)?.data?.hasValidProAccess == true }) {
      journalTag.name
    } else {
      JournalTag.NONE.name
    }

    viewModelScope.launch {
      if (orderKind != BrokerOrderKind.MARKET) {
        val targetLimit = if (limitPrice > 0.0) limitPrice else livePrice
        val targetTrigger = if (triggerPrice > 0.0) triggerPrice else livePrice
        repository.placePendingOrder(
          asset = asset,
          side = side,
          orderKind = orderKind,
          productType = productType.name,
          quantity = quantity,
          limitPrice = targetLimit,
          triggerPrice = targetTrigger,
          leverage = leverage,
          stopLossPrice = stopLossPrice,
          takeProfitPrice = takeProfitPrice,
          trailingSlDistance = trailingSlDistance
        ).onSuccess {
          _events.tryEmit("${orderKind.label} $side order placed for $quantity ${asset.symbol}")
        }.onFailure { err ->
          _events.tryEmit(err.message ?: "Pending order failed")
        }
        return@launch
      }

      if (side == "BUY") {
        repository.executeBuyOrder(
          asset = asset,
          quantity = quantity,
          price = livePrice,
          leverage = leverage,
          stopLossPrice = stopLossPrice,
          takeProfitPrice = takeProfitPrice,
          trailingSlDistance = trailingSlDistance,
          orderType = "MARKET",
          journalTag = effectiveTag
        ).onSuccess { pnl ->
          val msg = if (pnl != 0.0) {
            "Covered Short $quantity ${asset.symbol} @ ${formatInr(livePrice)} • P&L ${formatInr(pnl, showSign = true)}"
          } else {
            "Bought $quantity ${asset.symbol} @ ${formatInr(livePrice)}"
          }
          _events.tryEmit(msg)
          syncUserLeaderboard()
        }.onFailure { err ->
          _events.tryEmit(err.message ?: "Buy order failed")
        }
      } else {
        repository.executeSellOrder(
          asset = asset,
          quantity = quantity,
          price = livePrice,
          leverage = leverage,
          stopLossPrice = stopLossPrice,
          takeProfitPrice = takeProfitPrice,
          trailingSlDistance = trailingSlDistance,
          orderType = "MARKET",
          journalTag = effectiveTag
        ).onSuccess { pnl ->
          val msg = if (pnl != 0.0) {
            "Sold $quantity ${asset.symbol} • P&L ${formatInr(pnl, showSign = true)}"
          } else {
            "Opened Short $quantity ${asset.symbol} @ ${formatInr(livePrice)}"
          }
          _events.tryEmit(msg)
          syncUserLeaderboard()
        }.onFailure { err ->
          _events.tryEmit(err.message ?: "Sell order failed")
        }
      }
    }
  }

  fun placeOptionOrder(
    optionSymbol: String,
    underlyingName: String,
    ltp: Double,
    lots: Long,
    lotSize: Int,
    side: String
  ) {
    val qty = (lots.coerceAtLeast(1L) * lotSize).toDouble()
    val optionAsset = MarketAsset(
      id = optionSymbol,
      symbol = optionSymbol,
      name = "$underlyingName Option",
      category = AssetCategory.OPTION.name,
      sector = "Derivatives",
      basePrice = ltp,
      volatility = 0.035,
      unitLabel = "1 Unit"
    )
    viewModelScope.launch {
      val res = if (side == "BUY") {
        repository.executeBuyOrder(
          asset = optionAsset,
          quantity = qty,
          price = ltp,
          leverage = 1L,
          stopLossPrice = 0.0,
          takeProfitPrice = 0.0,
          orderType = "OPTION_BUY",
          journalTag = JournalTag.FOLLOWED_PLAN.name,
          notes = "F&O Paper Option Buy ($lots lot)"
        )
      } else {
        repository.executeSellOrder(
          asset = optionAsset,
          quantity = qty,
          price = ltp,
          leverage = 1L,
          orderType = "OPTION_SELL",
          journalTag = JournalTag.FOLLOWED_PLAN.name,
          notes = "F&O Paper Option Sell ($lots lot)"
        )
      }
      res.onSuccess {
        _events.tryEmit("F&O $side executed: $optionSymbol ($qty qty @ ${formatInr(ltp)})")
        syncUserLeaderboard()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "Option order failed")
      }
    }
  }

  fun placeQuickSellPosition(pos: Position) {
    val quote = quotes.value[pos.symbol]
    val asset = quote?.asset ?: MarketAsset(
      id = pos.symbol,
      symbol = pos.symbol,
      name = pos.name,
      category = pos.category,
      sector = pos.sector,
      basePrice = pos.avgPrice
    )
    val livePrice = quote?.currentPrice ?: pos.avgPrice
    viewModelScope.launch {
      val res = if (pos.isShort) {
        repository.executeBuyOrder(
          asset = asset,
          quantity = pos.quantity,
          price = livePrice,
          leverage = pos.leverage,
          stopLossPrice = 0.0,
          takeProfitPrice = 0.0,
          orderType = "SHORT_COVER"
        )
      } else {
        repository.executeSellOrder(
          asset = asset,
          quantity = pos.quantity,
          price = livePrice,
          orderType = "MARKET"
        )
      }
      res.onSuccess { pnl ->
        _events.tryEmit("Exited ${pos.symbol} • P&L ${formatInr(pnl, showSign = true)}")
        syncUserLeaderboard()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "Square-off failed")
      }
    }
  }

  fun cancelPendingOrder(orderId: String) {
    viewModelScope.launch {
      repository.updatePendingOrderStatus(orderId, "CANCELLED")
        .onSuccess { _events.tryEmit("Pending order cancelled") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to cancel order") }
    }
  }

  fun saveBasket(name: String, items: List<String>) {
    viewModelScope.launch {
      repository.saveBasket(name, items)
        .onSuccess { _events.tryEmit("Saved basket \"$name\"") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to save basket") }
    }
  }

  fun executeBasket(basket: OrderBasket) {
    val currentQuotes = quotes.value
    viewModelScope.launch {
      var successCount = 0
      basket.items.forEach { encoded ->
        val parts = encoded.split("|")
        if (parts.size >= 4) {
          val symbol = parts[0]
          val side = parts[1]
          val qty = parts[2].toDoubleOrNull() ?: 1.0
          val lev = parts[3].toLongOrNull()?.coerceIn(1L, 5L) ?: 1L
          val quote = currentQuotes[symbol] ?: return@forEach
          val res = if (side == "BUY") {
            repository.executeBuyOrder(
              asset = quote.asset,
              quantity = qty,
              price = quote.currentPrice,
              leverage = lev,
              stopLossPrice = 0.0,
              takeProfitPrice = 0.0,
              orderType = "BASKET",
              notes = "Basket: ${basket.name}"
            )
          } else {
            repository.executeSellOrder(
              asset = quote.asset,
              quantity = qty,
              price = quote.currentPrice,
              leverage = lev,
              orderType = "BASKET",
              notes = "Basket: ${basket.name}"
            )
          }
          if (res.isSuccess) successCount++
        }
      }
      if (successCount > 0) {
        _events.tryEmit("Basket \"${basket.name}\" executed ($successCount/${basket.items.size} legs filled)")
        syncUserLeaderboard()
      } else {
        _events.tryEmit("Basket execution failed. Check margin or open positions.")
      }
    }
  }

  fun deleteBasket(basketId: String) {
    viewModelScope.launch {
      repository.deleteBasket(basketId)
    }
  }

  fun saveCustomWatchlist(watchlistId: String?, name: String, symbols: List<String>, sortOrder: Long) {
    val existing = (customWatchlistsState.value as? UiState.Success)?.data.orEmpty()
    if (watchlistId == null && existing.size >= 4) {
      _events.tryEmit("Maximum 5 total watchlists reached (1 primary + 4 custom).")
      return
    }
    viewModelScope.launch {
      repository.saveCustomWatchlist(watchlistId, name, symbols, sortOrder)
        .onSuccess { _events.tryEmit("Saved watchlist \"$name\"") }
        .onFailure { _events.tryEmit(it.message ?: "Watchlist save failed") }
    }
  }

  fun deleteCustomWatchlist(watchlistId: String) {
    viewModelScope.launch {
      repository.deleteCustomWatchlist(watchlistId)
        .onSuccess { _events.tryEmit("Watchlist deleted") }
    }
  }

  fun updateTradeJournalTag(tradeId: String, tag: JournalTag) {
    viewModelScope.launch {
      repository.updateTradeJournalTag(tradeId, tag)
        .onSuccess { _events.tryEmit("Tagged trade as ${tag.label}") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to update tag") }
    }
  }

  fun toggleWatchlist(symbol: String) {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      val res = repository.toggleWatchlist(symbol, profile.watchlist, profile.hasValidProAccess)
      res.onSuccess {
        val msg = if (symbol in profile.watchlist) "$symbol removed from watchlist" else "$symbol added to watchlist"
        _events.tryEmit(msg)
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "Watchlist update failed")
      }
    }
  }

  fun setTheme(mode: TerminalThemeMode) {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.updateTheme(mode, profile.hasValidProAccess)
        .onFailure { _events.tryEmit(it.message ?: "Theme change failed") }
    }
  }

  fun claimRewardedAdBonus() {
    val bonus = appConfigState.value.rewardedAdBonus
    viewModelScope.launch {
      repository.grantRewardedAdCash(bonus)
        .onSuccess {
          _events.tryEmit("Added +${formatInr(bonus)} virtual cash!")
          syncUserLeaderboard()
        }
        .onFailure { _events.tryEmit(it.message ?: "Failed to credit ad bonus") }
    }
  }

  fun refillProVirtualCash(amount: Double = 100_000.0) {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.refillProVirtualCash(amount, profile.hasValidProAccess)
        .onSuccess {
          _events.tryEmit("Pro Capital Refill: +${formatInr(amount)} added!")
          syncUserLeaderboard()
        }
        .onFailure { _events.tryEmit(it.message ?: "Capital refill failed") }
    }
  }

  fun resetPortfolio() {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    val starting = appConfigState.value.startingVirtualCash
    viewModelScope.launch {
      repository.resetPortfolio(starting, profile.hasValidProAccess)
        .onSuccess {
          _events.tryEmit("Portfolio reset to ${formatInr(starting)}")
          syncUserLeaderboard()
        }
        .onFailure { _events.tryEmit(it.message ?: "Reset failed") }
    }
  }

  fun createSipPlan(asset: MarketAsset, amountInr: Double, frequency: String) {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    if (!profile.hasValidProAccess) {
      _events.tryEmit("Pro subscription required for Auto-SIP Wealth Simulator")
      return
    }
    viewModelScope.launch {
      repository.createSipPlan(asset, amountInr, frequency)
        .onSuccess { _events.tryEmit("Auto-SIP created for ${asset.symbol}") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to create SIP") }
    }
  }

  fun runSipInstallmentNow(sip: SipPlan) {
    val quote = quotes.value[sip.symbol] ?: return
    viewModelScope.launch {
      repository.executeSipInstallment(sip, quote.asset, quote.currentPrice)
        .onSuccess {
          _events.tryEmit("SIP executed: ${formatInr(sip.amountInr)} in ${sip.symbol}")
          syncUserLeaderboard()
        }
        .onFailure { _events.tryEmit(it.message ?: "SIP execution failed") }
    }
  }

  fun deleteSipPlan(sipId: String) {
    viewModelScope.launch {
      repository.deleteSipPlan(sipId)
        .onFailure { _events.tryEmit(it.message ?: "Failed to delete SIP") }
    }
  }

  fun createPriceAlert(asset: MarketAsset, targetPrice: Double, direction: String) {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    if (!profile.hasValidProAccess) {
      _events.tryEmit("Pro subscription required for Price Target Alerts")
      return
    }
    viewModelScope.launch {
      repository.createPriceAlert(asset, targetPrice, direction)
        .onSuccess { _events.tryEmit("Price alert set for ${asset.symbol} @ ${formatInr(targetPrice)}") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to create alert") }
    }
  }

  fun deletePriceAlert(alertId: String) {
    viewModelScope.launch {
      repository.deletePriceAlert(alertId)
    }
  }

  fun syncUserLeaderboard() {
    val profile = (userState.value as? UiState.Success)?.data ?: return
    val positions = (positionsState.value as? UiState.Success)?.data.orEmpty()
    val currentQuotes = quotes.value
    val equityValue = positions.sumOf { pos ->
      val live = currentQuotes[pos.symbol]?.currentPrice ?: pos.avgPrice
      val unrealized = if (pos.isShort) (pos.avgPrice - live) * pos.quantity else (live - pos.avgPrice) * pos.quantity
      pos.marginUsed + unrealized
    }
    val netWorth = (profile.virtualCash + equityValue).coerceAtLeast(0.0)
    val base = profile.initialCash.coerceAtLeast(1.0)
    val returnPct = ((netWorth - base) / base) * 100.0

    viewModelScope.launch {
      repository.syncLeaderboard(
        displayName = profile.displayName,
        netWorth = netWorth,
        returnPct = returnPct,
        proMember = profile.hasValidProAccess
      )
    }
  }

  fun startFamGatewayCheckout(plan: SubscriptionPlan) {
    val backendUrl = appConfigState.value.backendBaseUrl
    if (backendUrl.isBlank()) {
      _events.tryEmit("Backend URL not configured yet. Ask Admin to set it in Aurum Admin -> Gateway.")
      return
    }

    viewModelScope.launch {
      _checkoutState.value = CheckoutState.CreatingOrder
      try {
        val user = Firebase.auth.currentUser
          ?: throw IllegalStateException("Please sign in again.")
        val idToken = user.getIdToken(false).await().token
          ?: throw IllegalStateException("Could not obtain Firebase ID token.")

        val order = famGatewayClient.createOrder(backendUrl, idToken, plan.id).getOrThrow()
        _checkoutState.value = CheckoutState.WebViewOpen(
          orderId = order.orderId,
          checkoutUrl = order.checkoutUrl,
          planName = order.planName,
          amountInr = order.amountInr
        )
        startStatusPolling(backendUrl, order.orderId, order.planName)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _checkoutState.value = CheckoutState.Idle
        _events.tryEmit(e.message ?: "Failed to start FamGateway checkout")
      }
    }
  }

  private fun startStatusPolling(backendUrl: String, orderId: String, planName: String) {
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
      while (isActive && _checkoutState.value is CheckoutState.WebViewOpen) {
        delay(3500L)
        val user = Firebase.auth.currentUser ?: break
        val token = runCatching { user.getIdToken(false).await().token }.getOrNull() ?: continue
        val statusRes = famGatewayClient.checkOrderStatus(backendUrl, token, orderId)
        statusRes.onSuccess { status ->
          if (status.verified || status.status.equals("PAID", ignoreCase = true)) {
            _checkoutState.value = CheckoutState.ProActivated(
              planName = status.planName.ifBlank { planName },
              expiresAtMillis = status.planExpiresAtMillis
            )
            return@launch
          }
        }
      }
    }
  }

  fun closeCheckout() {
    pollingJob?.cancel()
    _checkoutState.value = CheckoutState.Idle
  }

  fun exportTradesCsvUri(context: Context): Result<Uri> = runCatching {
    val trades = (tradesState.value as? UiState.Success)?.data.orEmpty()
    val file = File(context.cacheDir, "aurum_trade_report.csv")
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val csv = buildString {
      appendLine("Date,Symbol,Side,OrderType,Quantity,ExecutionPrice,TotalValue,Leverage,RealizedPnL,JournalTag,Notes")
      trades.forEach { t ->
        val dt = t.createdAt?.toDate()?.let { sdf.format(it) } ?: "Pending"
        val cleanNotes = t.notes.replace(",", ";").replace("\n", " ")
        appendLine("$dt,${t.symbol},${t.side},${t.orderType},${t.quantity},${t.executionPrice},${t.totalValue},${t.leverage},${t.realizedPnl},${t.journalTag},$cleanNotes")
      }
    }
    file.writeText(csv)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  fun exportTradesPdfUri(context: Context): Result<Uri> = runCatching {
    val profile = (userState.value as? UiState.Success)?.data
    val trades = (tradesState.value as? UiState.Success)?.data.orEmpty()
    val file = File(context.cacheDir, "aurum_pro_statement.pdf")
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
      color = Color.rgb(18, 22, 32)
      textSize = 18f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint().apply {
      color = Color.rgb(45, 52, 68)
      textSize = 11f
    }
    val monoPaint = Paint().apply {
      color = Color.rgb(30, 35, 45)
      textSize = 10f
      typeface = Typeface.MONOSPACE
    }

    var y = 46f
    canvas.drawText("AURUM TRADE — PRO TRADING STATEMENT", 36f, y, titlePaint)
    y += 22f
    canvas.drawText("Trader: ${profile?.displayName ?: "Trader"} (${profile?.email ?: ""})", 36f, y, bodyPaint)
    y += 16f
    canvas.drawText(
      "Cash: ${formatInr(profile?.virtualCash ?: 0.0)}  |  Realized P&L: ${formatInr(profile?.totalRealizedPnl ?: 0.0, showSign = true)}",
      36f,
      y,
      bodyPaint
    )
    y += 16f
    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US)
    canvas.drawText("Generated: ${sdf.format(Date())}  |  Total Trades: ${profile?.totalTrades ?: 0}", 36f, y, bodyPaint)
    y += 24f
    canvas.drawText("SYMBOL        SIDE   TYPE         QTY        PRICE         REALIZED P&L    TAG", 36f, y, monoPaint)
    y += 14f

    trades.take(36).forEach { t ->
      val line = String.format(
        Locale.US,
        "%-12s %-5s %-11s %7.2f %12s %14s  %s",
        t.symbol.take(12),
        t.side.take(5),
        t.orderType.take(11),
        t.quantity,
        formatInr(t.executionPrice).take(12),
        formatInr(t.realizedPnl, showSign = true).take(14),
        JournalTag.safeFrom(t.journalTag).label.take(16)
      )
      canvas.drawText(line, 36f, y, monoPaint)
      y += 15f
    }

    doc.finishPage(page)
    FileOutputStream(file).use { out -> doc.writeTo(out) }
    doc.close()
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  override fun onCleared() {
    super.onCleared()
    pollingJob?.cancel()
  }

  private companion object {
    const val TAG = "TerminalVM"
  }
}
