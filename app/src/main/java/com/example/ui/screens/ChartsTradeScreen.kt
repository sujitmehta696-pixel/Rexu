package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.BrokerOrderKind
import com.aurumtrade.shared.model.ChartTimeframe
import com.aurumtrade.shared.model.ChartType
import com.aurumtrade.shared.model.JournalTag
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.OrderBasket
import com.aurumtrade.shared.model.OrderProductType
import com.aurumtrade.shared.model.Position
import com.aurumtrade.shared.model.PriceAlert
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.AreaPriceChart
import com.aurumtrade.shared.ui.ProBrokerChart
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.util.formatInr
import com.aurumtrade.shared.util.formatPct
import com.example.ui.RewardedAdButton
import com.example.ui.components.OrderTicketSection
import com.example.ui.components.ProFeature

@Composable
fun ChartsTradeScreen(
  profile: UserProfile?,
  quotes: Map<String, LiveQuote>,
  selectedSymbol: String,
  positions: List<Position>,
  baskets: List<OrderBasket>,
  alerts: List<PriceAlert>,
  appConfig: AppConfig,
  onSelectSymbol: (String) -> Unit,
  onPlaceBrokerOrder: (
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
  ) -> Unit,
  onSaveBasket: (String, List<String>) -> Unit,
  onExecuteBasket: (OrderBasket) -> Unit,
  onDeleteBasket: (String) -> Unit,
  onCreateAlert: (MarketAsset, Double, String) -> Unit,
  onDeleteAlert: (String) -> Unit,
  onWatchAdClick: () -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val isPro = profile?.hasValidProAccess == true
  val quote = quotes[selectedSymbol] ?: quotes.values.firstOrNull() ?: return
  val openPos = positions.firstOrNull { it.symbol == quote.asset.symbol }

  var chartType by remember(isPro) { mutableStateOf(if (isPro) ChartType.CANDLESTICK else ChartType.AREA) }
  var timeframe by remember(isPro) { mutableStateOf(if (isPro) ChartTimeframe.M15 else ChartTimeframe.D1) }

  var showEma9 by remember { mutableStateOf(false) }
  var showEma20 by remember { mutableStateOf(true) }
  var showEma50 by remember { mutableStateOf(true) }
  var showSma200 by remember { mutableStateOf(false) }
  var showBollinger by remember { mutableStateOf(false) }
  var showVwap by remember { mutableStateOf(false) }
  var showRsi by remember { mutableStateOf(true) }
  var showMacd by remember { mutableStateOf(false) }

  var alertPriceText by remember(selectedSymbol) { mutableStateOf("") }
  var alertAbove by remember { mutableStateOf(true) }

  val isUp = quote.change >= 0.0
  val accent = if (isUp) trading.bullishEmerald else trading.bearishCrimson

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Quick Symbol Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      quotes.values.forEach { q ->
        FilterChip(
          selected = q.asset.symbol == quote.asset.symbol,
          onClick = { onSelectSymbol(q.asset.symbol) },
          label = { Text(q.asset.symbol) }
        )
      }
    }

    // Pro Charting Terminal Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(quote.asset.symbol, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
          Text(
            text = "${quote.asset.name} • ${quote.asset.sector}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(formatInr(quote.currentPrice), style = TabularMonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
          Text(
            text = "${formatInr(quote.change, showSign = true)} (${formatPct(quote.changePct)})",
            style = TabularMonoStyle.copy(fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold)
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = "H ${formatInr(quote.dayHigh)}   L ${formatInr(quote.dayLow)}",
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
          text = "Cash ${formatInr(profile?.virtualCash ?: 0.0)}",
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.aurumGold)
        )
      }

      // Chart Type Selector (Area Free; Candles, Hollow, Heikin-Ashi visible with PRO lock)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        ChartType.entries.forEach { type ->
          FilterChip(
            selected = chartType == type,
            onClick = {
              if (type.isProOnly && !isPro) onLockedFeatureClick(ProFeature.PRO_CHARTS)
              else chartType = type
            },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(type.label, fontSize = 12.sp)
                if (type.isProOnly && !isPro) {
                  Spacer(Modifier.width(4.dp))
                  ProLockBadge()
                }
              }
            }
          )
        }
      }

      // Timeframe Selector (1D Free; 1m, 5m, 15m, 1H, 1W visible with PRO lock)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        ChartTimeframe.entries.forEach { tf ->
          val active = timeframe == tf
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (active) trading.aurumGoldSoft else MaterialTheme.colorScheme.surfaceVariant)
              .clickable {
                if (tf.isProOnly && !isPro) onLockedFeatureClick(ProFeature.PRO_CHARTS)
                else timeframe = tf
              }
              .padding(horizontal = 9.dp, vertical = 6.dp)
          ) {
            Text(
              text = tf.label,
              style = TabularMonoStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) trading.aurumGold else MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            if (tf.isProOnly && !isPro) {
              Spacer(Modifier.width(4.dp))
              ProLockBadge(label = "PRO")
            }
          }
        }
      }

      // Technical Indicators Strip (always visible with PRO lock badges for Free)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        IndicatorToggleChip("EMA 9", showEma9, isPro) {
          if (isPro) showEma9 = !showEma9 else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("EMA 20", showEma20, isPro) {
          if (isPro) showEma20 = !showEma20 else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("EMA 50", showEma50, isPro) {
          if (isPro) showEma50 = !showEma50 else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("SMA 200", showSma200, isPro) {
          if (isPro) showSma200 = !showSma200 else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("Bollinger", showBollinger, isPro) {
          if (isPro) showBollinger = !showBollinger else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("VWAP", showVwap, isPro) {
          if (isPro) showVwap = !showVwap else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("RSI (14)", showRsi, isPro) {
          if (isPro) showRsi = !showRsi else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
        IndicatorToggleChip("MACD", showMacd, isPro) {
          if (isPro) showMacd = !showMacd else onLockedFeatureClick(ProFeature.PRO_CHARTS)
        }
      }

      // Main Canvas Chart + Touch OHLCV Crosshair
      val activeCandles = quote.candlesByTimeframe[timeframe].orEmpty()
      if (chartType != ChartType.AREA && isPro) {
        ProBrokerChart(
          candles = activeCandles,
          chartType = chartType,
          ema9 = quote.ema9ByTimeframe[timeframe].orEmpty(),
          ema20 = quote.ema20ByTimeframe[timeframe].orEmpty(),
          ema50 = quote.ema50ByTimeframe[timeframe].orEmpty(),
          sma200 = quote.sma200ByTimeframe[timeframe].orEmpty(),
          bbUpper = quote.bbUpperByTimeframe[timeframe].orEmpty(),
          bbLower = quote.bbLowerByTimeframe[timeframe].orEmpty(),
          vwap = quote.vwapByTimeframe[timeframe].orEmpty(),
          rsi14 = quote.rsi14ByTimeframe[timeframe].orEmpty(),
          macdLine = quote.macdLineByTimeframe[timeframe].orEmpty(),
          macdSignal = quote.macdSignalByTimeframe[timeframe].orEmpty(),
          macdHist = quote.macdHistByTimeframe[timeframe].orEmpty(),
          showEma9 = showEma9,
          showEma20 = showEma20,
          showEma50 = showEma50,
          showSma200 = showSma200,
          showBollinger = showBollinger,
          showVwap = showVwap,
          showRsi = showRsi,
          showMacd = showMacd
        )
      } else {
        AreaPriceChart(
          candles = quote.candlesByTimeframe[ChartTimeframe.D1].orEmpty(),
          previousClose = quote.previousClose
        )
      }
    }

    // Real Broker Order Ticket + 5-Level Depth + 1-Tap Baskets
    OrderTicketSection(
      quote = quote,
      allQuotes = quotes,
      openPos = openPos,
      baskets = baskets,
      isPro = isPro,
      onPlaceOrder = onPlaceBrokerOrder,
      onSaveBasket = onSaveBasket,
      onExecuteBasket = onExecuteBasket,
      onDeleteBasket = onDeleteBasket,
      onLockedFeatureClick = onLockedFeatureClick
    )

    // Hide Rewarded Ads completely when Pro is active
    if (!isPro) {
      RewardedAdButton(
        appConfig = appConfig,
        onClick = onWatchAdClick,
        modifier = Modifier.fillMaxWidth()
      )
    }

    // Price Target Alerts Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = "Price Target Alerts",
            tint = trading.aurumGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text("Price Target Alerts", style = MaterialTheme.typography.titleMedium)
        }
        if (!isPro) ProLockBadge()
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = alertPriceText,
          onValueChange = { alertPriceText = it },
          label = { Text("Target ₹") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
        FilterChip(
          selected = alertAbove,
          onClick = { alertAbove = !alertAbove },
          label = { Text(if (alertAbove) "Above" else "Below") }
        )
        OutlinedButton(
          onClick = {
            if (!isPro) {
              onLockedFeatureClick(ProFeature.AUTO_SIP_ALERTS)
            } else {
              val tp = alertPriceText.toDoubleOrNull() ?: return@OutlinedButton
              onCreateAlert(quote.asset, tp, if (alertAbove) "ABOVE" else "BELOW")
              alertPriceText = ""
            }
          },
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("set_price_alert_button")
        ) {
          Text(if (isPro) "Set" else "Unlock")
        }
      }

      alerts.filter { it.symbol == quote.asset.symbol }.forEach { a ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${a.direction} ${formatInr(a.targetPrice)} ${if (a.triggered) "• TRIGGERED" else ""}",
            style = TabularMonoStyle.copy(fontSize = 12.sp)
          )
          Text(
            text = "Remove",
            style = MaterialTheme.typography.labelLarge.copy(color = trading.bearishCrimson),
            modifier = Modifier.clickable { onDeleteAlert(a.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun IndicatorToggleChip(
  label: String,
  selected: Boolean,
  isPro: Boolean,
  onClick: () -> Unit
) {
  FilterChip(
    selected = isPro && selected,
    onClick = onClick,
    label = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp)
        if (!isPro) {
          Spacer(Modifier.width(4.dp))
          ProLockBadge()
        }
      }
    }
  )
}
