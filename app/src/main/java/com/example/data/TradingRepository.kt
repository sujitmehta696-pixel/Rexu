package com.example.data

import android.content.Context
import com.aurumtrade.shared.market.DefaultMarketCatalog
import com.aurumtrade.shared.model.APP_CONFIG_DOC_ID
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.BrokerOrderKind
import com.aurumtrade.shared.model.CustomWatchlist
import com.aurumtrade.shared.model.JournalTag
import com.aurumtrade.shared.model.LeaderboardEntry
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.OrderBasket
import com.aurumtrade.shared.model.PendingOrder
import com.aurumtrade.shared.model.Position
import com.aurumtrade.shared.model.PriceAlert
import com.aurumtrade.shared.model.SipPlan
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.TerminalThemeMode
import com.aurumtrade.shared.model.TradeRecord
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.util.OperationType
import com.aurumtrade.shared.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

class TradingRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(com.example.R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun <T> Flow<T>.handleStreamErrors(op: OperationType, path: String, uid: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(exceptionToLog, op, path)
      }
      throw error
    }

  suspend fun ensureUserProfile(user: FirebaseUser, startingCash: Double): Result<Unit> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val displayName = (user.displayName ?: user.email?.substringBefore("@") ?: "Trader")
          .take(80).ifBlank { "Trader" }
        val email = (user.email ?: "trader@aurumtrade.app").take(160)
        val photoUrl = user.photoUrl?.toString()?.take(500) ?: ""
        val payload = mapOf(
          "userId" to uid,
          "displayName" to displayName,
          "email" to email,
          "photoUrl" to photoUrl,
          "virtualCash" to startingCash,
          "initialCash" to startingCash,
          "totalRealizedPnl" to 0.0,
          "totalTrades" to 0L,
          "winningTrades" to 0L,
          "portfolioResetsUsed" to 0L,
          "preferredTheme" to TerminalThemeMode.DARK_OLED.name,
          "watchlist" to listOf("RELIANCE", "TCS", "NIFTY50", "GOLD_24K", "INFY"),
          "subscriptionActive" to false,
          "adFree" to false,
          "planId" to "",
          "planName" to "",
          "planStartedAtMillis" to 0L,
          "planExpiresAtMillis" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.GET, path, uid)
    )
  }

  fun observePositions(uid: String): Flow<List<Position>> = flow {
    val path = "users/$uid/positions"
    emitAll(
      db.collection("users").document(uid).collection("positions")
        .snapshots()
        .map { it.toObjects(Position::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeTrades(uid: String): Flow<List<TradeRecord>> = flow {
    val path = "users/$uid/trades"
    emitAll(
      db.collection("users").document(uid).collection("trades")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(180)
        .snapshots()
        .map { it.toObjects(TradeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observePendingOrders(uid: String): Flow<List<PendingOrder>> = flow {
    val path = "users/$uid/pending_orders"
    emitAll(
      db.collection("users").document(uid).collection("pending_orders")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(100)
        .snapshots()
        .map { it.toObjects(PendingOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeCustomWatchlists(uid: String): Flow<List<CustomWatchlist>> = flow {
    val path = "users/$uid/watchlists"
    emitAll(
      db.collection("users").document(uid).collection("watchlists")
        .orderBy("sortOrder", Query.Direction.ASCENDING)
        .snapshots()
        .map { it.toObjects(CustomWatchlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeBaskets(uid: String): Flow<List<OrderBasket>> = flow {
    val path = "users/$uid/baskets"
    emitAll(
      db.collection("users").document(uid).collection("baskets")
        .snapshots()
        .map { it.toObjects(OrderBasket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeSipPlans(uid: String): Flow<List<SipPlan>> = flow {
    val path = "users/$uid/sip_plans"
    emitAll(
      db.collection("users").document(uid).collection("sip_plans")
        .snapshots()
        .map { it.toObjects(SipPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observePriceAlerts(uid: String): Flow<List<PriceAlert>> = flow {
    val path = "users/$uid/price_alerts"
    emitAll(
      db.collection("users").document(uid).collection("price_alerts")
        .snapshots()
        .map { it.toObjects(PriceAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeSubscriptionPlans(uid: String): Flow<List<SubscriptionPlan>> = flow {
    val path = "subscription_plans"
    emitAll(
      db.collection(path)
        .orderBy("sortOrder", Query.Direction.ASCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.active }
        }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeAppConfig(uid: String): Flow<AppConfig> = flow {
    val path = "app_config/$APP_CONFIG_DOC_ID"
    emitAll(
      db.collection("app_config").document(APP_CONFIG_DOC_ID)
        .snapshots()
        .map { snap ->
          snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: AppConfig()
        }
        .handleStreamErrors(OperationType.GET, path, uid)
    )
  }

  fun observeMarketAssets(uid: String): Flow<List<MarketAsset>> = flow {
    val path = "market_assets"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          val remote = snap.toObjects(MarketAsset::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (remote.isEmpty()) DefaultMarketCatalog.defaultAssets else remote.filter { it.active }
        }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeLeaderboard(uid: String): Flow<List<LeaderboardEntry>> = flow {
    val path = "leaderboard_entries"
    emitAll(
      db.collection(path)
        .orderBy("returnPct", Query.Direction.DESCENDING)
        .limit(50)
        .snapshots()
        .map { it.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  suspend fun executeBuyOrder(
    asset: MarketAsset,
    quantity: Double,
    price: Double,
    leverage: Long,
    stopLossPrice: Double,
    takeProfitPrice: Double,
    trailingSlDistance: Double = 0.0,
    orderType: String = "MARKET",
    journalTag: String = JournalTag.NONE.name,
    notes: String = ""
  ): Result<Double> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val posRef = userRef.collection("positions").document(asset.symbol)
    val tradeRef = userRef.collection("trades").document()
    return try {
      require(quantity > 0.0 && price > 0.0) { "Invalid order quantity or price" }
      require(leverage in 1L..5L) { "Leverage must be between 1x and 5x" }
      require(trailingSlDistance >= 0.0) { "Trailing SL distance cannot be negative" }

      val realizedOrZero = db.runTransaction { tx ->
        val userSnap = tx.get(userRef)
        val user = userSnap.toObject(UserProfile::class.java)
          ?: throw IllegalStateException("Profile missing")
        val requiresPro = leverage > 1L || stopLossPrice > 0.0 || takeProfitPrice > 0.0 ||
          trailingSlDistance > 0.0 || asset.category == "OPTION"
        if (requiresPro && !user.hasValidProAccess) {
          throw IllegalStateException("Pro subscription required for this order configuration.")
        }

        val posSnap = tx.get(posRef)
        val existingPos = posSnap.toObject(Position::class.java)

        // If covering an existing SHORT position
        if (existingPos != null && existingPos.isShort && existingPos.quantity > 0.0) {
          if (!user.hasValidProAccess) {
            throw IllegalStateException("Pro subscription required to cover short positions.")
          }
          val coverQty = min(quantity, existingPos.quantity)
          val fraction = coverQty / existingPos.quantity
          val releasedMargin = existingPos.marginUsed * fraction
          val pnl = (existingPos.avgPrice - price) * coverQty
          val cashReturn = max(0.0, releasedMargin + pnl)
          val newCash = (user.virtualCash + cashReturn).coerceAtMost(1_000_000_000.0)
          val newWinning = if (pnl > 0.0) user.winningTrades + 1L else user.winningTrades

          tx.update(
            userRef,
            mapOf(
              "virtualCash" to newCash,
              "totalRealizedPnl" to user.totalRealizedPnl + pnl,
              "totalTrades" to user.totalTrades + 1L,
              "winningTrades" to newWinning,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )

          val remainingQty = existingPos.quantity - coverQty
          if (remainingQty <= 0.0001) {
            tx.delete(posRef)
          } else {
            tx.update(
              posRef,
              mapOf(
                "quantity" to remainingQty,
                "marginUsed" to max(0.0, existingPos.marginUsed - releasedMargin),
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
          }

          val finalOrderType = if (orderType == "MARKET") "SHORT_COVER" else orderType
          tx.set(
            tradeRef,
            mapOf(
              "userId" to uid,
              "symbol" to asset.symbol,
              "name" to asset.name,
              "category" to asset.category,
              "sector" to asset.sector,
              "side" to "BUY",
              "orderType" to finalOrderType,
              "quantity" to coverQty,
              "executionPrice" to price,
              "totalValue" to coverQty * price,
              "leverage" to existingPos.leverage,
              "realizedPnl" to pnl,
              "stopLossPrice" to existingPos.stopLossPrice,
              "takeProfitPrice" to existingPos.takeProfitPrice,
              "journalTag" to journalTag.take(40),
              "notes" to notes.take(300),
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
          return@runTransaction pnl
        }

        // Standard LONG buy or averaging into LONG position
        val totalNotional = quantity * price
        val marginRequired = totalNotional / leverage.toDouble()
        if (user.virtualCash < marginRequired) {
          throw IllegalStateException("Insufficient virtual cash margin.")
        }

        val newCash = (user.virtualCash - marginRequired).coerceAtLeast(0.0)
        tx.update(
          userRef,
          mapOf(
            "virtualCash" to newCash,
            "totalTrades" to user.totalTrades + 1L,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        val effectiveSl = when {
          stopLossPrice > 0.0 -> stopLossPrice
          trailingSlDistance > 0.0 -> max(0.05, price - trailingSlDistance)
          else -> existingPos?.stopLossPrice ?: 0.0
        }

        if (existingPos != null && existingPos.quantity > 0.0) {
          val newQty = existingPos.quantity + quantity
          val weightedAvg = ((existingPos.quantity * existingPos.avgPrice) + (quantity * price)) / newQty
          val newMargin = existingPos.marginUsed + marginRequired
          tx.update(
            posRef,
            mapOf(
              "name" to asset.name,
              "category" to asset.category,
              "sector" to asset.sector,
              "positionSide" to "LONG",
              "quantity" to newQty,
              "avgPrice" to weightedAvg,
              "leverage" to max(existingPos.leverage, leverage),
              "marginUsed" to newMargin,
              "stopLossPrice" to effectiveSl,
              "takeProfitPrice" to if (takeProfitPrice > 0.0) takeProfitPrice else existingPos.takeProfitPrice,
              "trailingSlDistance" to if (trailingSlDistance > 0.0) trailingSlDistance else existingPos.trailingSlDistance,
              "highWaterPrice" to max(existingPos.highWaterPrice, price),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          tx.set(
            posRef,
            mapOf(
              "userId" to uid,
              "symbol" to asset.symbol,
              "name" to asset.name,
              "category" to asset.category,
              "sector" to asset.sector,
              "positionSide" to "LONG",
              "quantity" to quantity,
              "avgPrice" to price,
              "leverage" to leverage,
              "marginUsed" to marginRequired,
              "stopLossPrice" to effectiveSl,
              "takeProfitPrice" to takeProfitPrice,
              "trailingSlDistance" to trailingSlDistance,
              "highWaterPrice" to price,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        tx.set(
          tradeRef,
          mapOf(
            "userId" to uid,
            "symbol" to asset.symbol,
            "name" to asset.name,
            "category" to asset.category,
            "sector" to asset.sector,
            "side" to "BUY",
            "orderType" to orderType,
            "quantity" to quantity,
            "executionPrice" to price,
            "totalValue" to totalNotional,
            "leverage" to leverage,
            "realizedPnl" to 0.0,
            "stopLossPrice" to effectiveSl,
            "takeProfitPrice" to takeProfitPrice,
            "journalTag" to journalTag.take(40),
            "notes" to notes.take(300),
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        0.0
      }.await()
      Result.success(realizedOrZero)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$uid/positions/${asset.symbol}")
      Result.failure(e)
    }
  }

  suspend fun executeSellOrder(
    asset: MarketAsset,
    quantity: Double,
    price: Double,
    leverage: Long = 1L,
    stopLossPrice: Double = 0.0,
    takeProfitPrice: Double = 0.0,
    trailingSlDistance: Double = 0.0,
    orderType: String = "MARKET",
    journalTag: String = JournalTag.NONE.name,
    notes: String = ""
  ): Result<Double> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val posRef = userRef.collection("positions").document(asset.symbol)
    val tradeRef = userRef.collection("trades").document()
    return try {
      require(quantity > 0.0 && price > 0.0) { "Invalid sell quantity or price" }
      require(leverage in 1L..5L) { "Leverage must be between 1x and 5x" }
      val realizedPnl = db.runTransaction { tx ->
        val userSnap = tx.get(userRef)
        val user = userSnap.toObject(UserProfile::class.java)
          ?: throw IllegalStateException("Profile missing")
        val posSnap = tx.get(posRef)
        val pos = posSnap.toObject(Position::class.java)

        // Closing an existing LONG position
        if (pos != null && !pos.isShort && pos.quantity > 0.0) {
          val sellQty = quantity.coerceAtMost(pos.quantity)
          val fraction = sellQty / pos.quantity
          val releasedMargin = pos.marginUsed * fraction
          val pnl = (price - pos.avgPrice) * sellQty
          val cashReturn = max(0.0, releasedMargin + pnl)

          val newCash = (user.virtualCash + cashReturn).coerceAtMost(1_000_000_000.0)
          val newWinning = if (pnl > 0.0) user.winningTrades + 1L else user.winningTrades

          tx.update(
            userRef,
            mapOf(
              "virtualCash" to newCash,
              "totalRealizedPnl" to user.totalRealizedPnl + pnl,
              "totalTrades" to user.totalTrades + 1L,
              "winningTrades" to newWinning,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )

          val remainingQty = pos.quantity - sellQty
          if (remainingQty <= 0.0001) {
            tx.delete(posRef)
          } else {
            tx.update(
              posRef,
              mapOf(
                "quantity" to remainingQty,
                "marginUsed" to max(0.0, pos.marginUsed - releasedMargin),
                "updatedAt" to FieldValue.serverTimestamp()
              )
            )
          }

          tx.set(
            tradeRef,
            mapOf(
              "userId" to uid,
              "symbol" to pos.symbol,
              "name" to pos.name,
              "category" to pos.category,
              "sector" to pos.sector,
              "side" to "SELL",
              "orderType" to orderType,
              "quantity" to sellQty,
              "executionPrice" to price,
              "totalValue" to sellQty * price,
              "leverage" to pos.leverage,
              "realizedPnl" to pnl,
              "stopLossPrice" to pos.stopLossPrice,
              "takeProfitPrice" to pos.takeProfitPrice,
              "journalTag" to journalTag.take(40),
              "notes" to notes.take(300),
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
          return@runTransaction pnl
        }

        // Opening or adding to a PRO SHORT position
        if (!user.hasValidProAccess) {
          throw IllegalStateException("No open LONG position in ${asset.symbol}. Short Selling requires Pro subscription.")
        }
        val totalNotional = quantity * price
        val marginRequired = totalNotional / leverage.toDouble()
        if (user.virtualCash < marginRequired) {
          throw IllegalStateException("Insufficient virtual cash margin for Short position.")
        }

        val effectiveSl = when {
          stopLossPrice > 0.0 -> stopLossPrice
          trailingSlDistance > 0.0 -> price + trailingSlDistance
          else -> pos?.stopLossPrice ?: 0.0
        }

        val newCash = (user.virtualCash - marginRequired).coerceAtLeast(0.0)
        tx.update(
          userRef,
          mapOf(
            "virtualCash" to newCash,
            "totalTrades" to user.totalTrades + 1L,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        if (pos != null && pos.isShort && pos.quantity > 0.0) {
          val newQty = pos.quantity + quantity
          val weightedAvg = ((pos.quantity * pos.avgPrice) + (quantity * price)) / newQty
          val prevLowWater = if (pos.highWaterPrice > 0.0) min(pos.highWaterPrice, price) else price
          tx.update(
            posRef,
            mapOf(
              "name" to asset.name,
              "category" to asset.category,
              "sector" to asset.sector,
              "positionSide" to "SHORT",
              "quantity" to newQty,
              "avgPrice" to weightedAvg,
              "leverage" to max(pos.leverage, leverage),
              "marginUsed" to pos.marginUsed + marginRequired,
              "stopLossPrice" to effectiveSl,
              "takeProfitPrice" to if (takeProfitPrice > 0.0) takeProfitPrice else pos.takeProfitPrice,
              "trailingSlDistance" to if (trailingSlDistance > 0.0) trailingSlDistance else pos.trailingSlDistance,
              "highWaterPrice" to prevLowWater,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          tx.set(
            posRef,
            mapOf(
              "userId" to uid,
              "symbol" to asset.symbol,
              "name" to asset.name,
              "category" to asset.category,
              "sector" to asset.sector,
              "positionSide" to "SHORT",
              "quantity" to quantity,
              "avgPrice" to price,
              "leverage" to leverage,
              "marginUsed" to marginRequired,
              "stopLossPrice" to effectiveSl,
              "takeProfitPrice" to takeProfitPrice,
              "trailingSlDistance" to trailingSlDistance,
              "highWaterPrice" to price,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        val finalOrderType = if (orderType == "MARKET") "SHORT_SELL" else orderType
        tx.set(
          tradeRef,
          mapOf(
            "userId" to uid,
            "symbol" to asset.symbol,
            "name" to asset.name,
            "category" to asset.category,
            "sector" to asset.sector,
            "side" to "SELL",
            "orderType" to finalOrderType,
            "quantity" to quantity,
            "executionPrice" to price,
            "totalValue" to totalNotional,
            "leverage" to leverage,
            "realizedPnl" to 0.0,
            "stopLossPrice" to effectiveSl,
            "takeProfitPrice" to takeProfitPrice,
            "journalTag" to journalTag.take(40),
            "notes" to notes.take(300),
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        0.0
      }.await()
      Result.success(realizedPnl)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$uid/positions/${asset.symbol}")
      Result.failure(e)
    }
  }

  suspend fun updatePositionTrailingSl(
    symbol: String,
    newStopLoss: Double,
    newWaterPrice: Double
  ): Result<Unit> {
    val uid = requireUserId()
    val posRef = db.collection("users").document(uid).collection("positions").document(symbol)
    return try {
      posRef.update(
        mapOf(
          "stopLossPrice" to newStopLoss,
          "highWaterPrice" to newWaterPrice,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, posRef.path)
      Result.failure(e)
    }
  }

  suspend fun placePendingOrder(
    asset: MarketAsset,
    side: String,
    orderKind: BrokerOrderKind,
    productType: String,
    quantity: Double,
    limitPrice: Double,
    triggerPrice: Double,
    leverage: Long,
    stopLossPrice: Double,
    takeProfitPrice: Double,
    trailingSlDistance: Double
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val orderRef = userRef.collection("pending_orders").document()
    return try {
      require(quantity > 0.0) { "Invalid order quantity" }
      val userSnap = userRef.get().await()
      val user = userSnap.toObject(UserProfile::class.java)
        ?: throw IllegalStateException("Profile missing")
      if (!user.hasValidProAccess) {
        return Result.failure(IllegalStateException("Pro subscription required for Limit, SL, SL-M & GTT pending orders."))
      }

      orderRef.set(
        mapOf(
          "userId" to uid,
          "symbol" to asset.symbol,
          "name" to asset.name,
          "category" to asset.category,
          "sector" to asset.sector,
          "side" to side,
          "orderKind" to orderKind.name,
          "productType" to productType,
          "quantity" to quantity,
          "limitPrice" to limitPrice,
          "triggerPrice" to triggerPrice,
          "leverage" to leverage.coerceIn(1L, 5L),
          "stopLossPrice" to stopLossPrice,
          "takeProfitPrice" to takeProfitPrice,
          "trailingSlDistance" to trailingSlDistance,
          "status" to "OPEN",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun updatePendingOrderStatus(orderId: String, status: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("pending_orders").document(orderId)
    return try {
      docRef.update(
        mapOf(
          "status" to status,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateTradeJournalTag(tradeId: String, tag: JournalTag): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("trades").document(tradeId)
    return try {
      docRef.update(mapOf("journalTag" to tag.name)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveCustomWatchlist(
    watchlistId: String?,
    name: String,
    symbols: List<String>,
    sortOrder: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("watchlists")
    return try {
      val cleanName = name.trim().take(40).ifBlank { "Watchlist" }
      val cleanSymbols = symbols.distinct().take(50)
      if (watchlistId.isNullOrBlank()) {
        val docRef = colRef.document()
        docRef.set(
          mapOf(
            "userId" to uid,
            "name" to cleanName,
            "symbols" to cleanSymbols,
            "sortOrder" to sortOrder.coerceIn(0L, 20L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        colRef.document(watchlistId).update(
          mapOf(
            "name" to cleanName,
            "symbols" to cleanSymbols,
            "sortOrder" to sortOrder.coerceIn(0L, 20L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCustomWatchlist(watchlistId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("watchlists").document(watchlistId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveBasket(name: String, items: List<String>): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("baskets").document()
    return try {
      require(items.isNotEmpty() && items.size <= 10) { "Basket must contain 1 to 10 legs." }
      docRef.set(
        mapOf(
          "userId" to uid,
          "name" to name.trim().take(60).ifBlank { "Pro Basket" },
          "items" to items,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteBasket(basketId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("baskets").document(basketId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleWatchlist(symbol: String, currentWatchlist: List<String>, isPro: Boolean): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid"
    return try {
      val updated = if (symbol in currentWatchlist) {
        currentWatchlist - symbol
      } else {
        if (!isPro && currentWatchlist.size >= 5) {
          return Result.failure(IllegalStateException("Free watchlist limit is 5 assets. Upgrade to Pro for unlimited watchlist."))
        }
        (currentWatchlist + symbol).take(50)
      }
      db.collection("users").document(uid).update(
        mapOf("watchlist" to updated, "updatedAt" to FieldValue.serverTimestamp())
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateTheme(mode: TerminalThemeMode, isPro: Boolean): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid"
    return try {
      if (mode.isProOnly && !isPro) {
        return Result.failure(IllegalStateException("${mode.label} terminal theme requires Pro subscription."))
      }
      db.collection("users").document(uid).update(
        mapOf("preferredTheme" to mode.name, "updatedAt" to FieldValue.serverTimestamp())
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun grantRewardedAdCash(bonusAmount: Double): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid"
    return try {
      db.collection("users").document(uid).update(
        mapOf(
          "virtualCash" to FieldValue.increment(bonusAmount),
          "initialCash" to FieldValue.increment(bonusAmount),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun refillProVirtualCash(amount: Double, isPro: Boolean): Result<Unit> {
    if (!isPro) {
      return Result.failure(IllegalStateException("Unlimited Capital Refill requires Pro subscription."))
    }
    return grantRewardedAdCash(amount)
  }

  suspend fun resetPortfolio(startingCash: Double, isPro: Boolean): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    return try {
      val userSnap = userRef.get().await()
      val user = userSnap.toObject(UserProfile::class.java)
        ?: throw IllegalStateException("Profile missing")
      if (!isPro && user.portfolioResetsUsed >= 1L) {
        return Result.failure(IllegalStateException("Free tier allows 1 reset. Upgrade to Pro for unlimited portfolio resets."))
      }

      val positions = userRef.collection("positions").get().await()
      val batch = db.batch()
      positions.documents.forEach { batch.delete(it.reference) }
      batch.update(
        userRef,
        mapOf(
          "virtualCash" to startingCash,
          "initialCash" to startingCash,
          "totalRealizedPnl" to 0.0,
          "portfolioResetsUsed" to user.portfolioResetsUsed + 1L,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$uid")
      Result.failure(e)
    }
  }

  suspend fun createSipPlan(asset: MarketAsset, amountInr: Double, frequency: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("sip_plans").document()
    return try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "symbol" to asset.symbol,
          "assetName" to asset.name,
          "amountInr" to amountInr,
          "frequency" to frequency,
          "active" to true,
          "totalInvested" to 0.0,
          "installmentsCompleted" to 0L,
          "lastExecutedAtMillis" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun executeSipInstallment(sip: SipPlan, asset: MarketAsset, currentPrice: Double): Result<Unit> {
    val uid = requireUserId()
    return try {
      val qty = sip.amountInr / max(0.01, currentPrice)
      executeBuyOrder(
        asset = asset,
        quantity = qty,
        price = currentPrice,
        leverage = 1L,
        stopLossPrice = 0.0,
        takeProfitPrice = 0.0,
        orderType = "SIP_AUTO",
        notes = "Auto-SIP installment #${sip.installmentsCompleted + 1}"
      ).getOrThrow()

      db.collection("users").document(uid).collection("sip_plans").document(sip.id).update(
        mapOf(
          "totalInvested" to sip.totalInvested + sip.amountInr,
          "installmentsCompleted" to sip.installmentsCompleted + 1L,
          "lastExecutedAtMillis" to System.currentTimeMillis(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/sip_plans/${sip.id}")
      Result.failure(e)
    }
  }

  suspend fun deleteSipPlan(sipId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("sip_plans").document(sipId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createPriceAlert(asset: MarketAsset, targetPrice: Double, direction: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("price_alerts").document()
    return try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "symbol" to asset.symbol,
          "assetName" to asset.name,
          "targetPrice" to targetPrice,
          "direction" to direction,
          "triggered" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markAlertTriggered(alertId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("price_alerts").document(alertId)
    return try {
      docRef.update(mapOf("triggered" to true)).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePriceAlert(alertId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("price_alerts").document(alertId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun syncLeaderboard(
    displayName: String,
    netWorth: Double,
    returnPct: Double,
    proMember: Boolean
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("leaderboard_entries").document(uid)
    return try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "displayName" to displayName.take(80).ifBlank { "Trader" },
          "netWorth" to netWorth.coerceIn(0.0, 2_000_000_000.0),
          "returnPct" to returnPct.coerceIn(-100.0, 100_000.0),
          "proMember" to proMember,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
