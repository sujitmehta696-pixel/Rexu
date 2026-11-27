package com.aurumtrade.shared.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val ADMIN_EMAIL = "sujitmehta696@gmail.com"
const val APP_CONFIG_DOC_ID = "global"
const val ADMIN_SECRET_FAMGATEWAY_DOC_ID = "famgateway"

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class AssetCategory(val label: String) {
  STOCK("NSE Stocks"),
  INDEX("Indices"),
  GOLD("Gold & Silver"),
  COMMODITY("Commodities"),
  MUTUAL_FUND("Mutual Funds"),
  OPTION("F&O Options");

  companion object {
    fun safeFrom(raw: String): AssetCategory =
      entries.firstOrNull { it.name == raw } ?: STOCK
  }
}

enum class TerminalThemeMode(val label: String, val isProOnly: Boolean) {
  DARK_OLED("Dark OLED", false),
  LIGHT("Daylight", false),
  MIDNIGHT_GOLD("Midnight Gold", true),
  NEON_TERMINAL("Neon Terminal", true);

  companion object {
    fun safeFrom(raw: String): TerminalThemeMode =
      entries.firstOrNull { it.name == raw } ?: DARK_OLED
  }
}

enum class ChartTimeframe(val label: String, val intervalMillis: Long, val isProOnly: Boolean) {
  M1("1m", 60_000L, true),
  M5("5m", 300_000L, true),
  M15("15m", 900_000L, true),
  H1("1H", 3_600_000L, true),
  D1("1D", 86_400_000L, false),
  W1("1W", 604_800_000L, true)
}

enum class ChartType(val label: String, val isProOnly: Boolean) {
  AREA("Line / Area", false),
  CANDLESTICK("Candles", true),
  HOLLOW("Hollow", true),
  HEIKIN_ASHI("Heikin-Ashi", true)
}

enum class OrderProductType(val label: String, val maxLeverage: Long, val isProOnly: Boolean) {
  CNC("CNC Delivery", 1L, false),
  MIS("MIS Intraday 5x", 5L, true)
}

enum class BrokerOrderKind(val label: String, val isProOnly: Boolean) {
  MARKET("Market", false),
  LIMIT("Limit", true),
  SL("Stop-Loss (SL)", true),
  SL_M("SL-Market", true),
  GTT("GTT Trigger", true)
}

enum class JournalTag(val label: String) {
  NONE("Untagged"),
  FOLLOWED_PLAN("Followed Plan"),
  BREAKOUT("Breakout Setup"),
  FOMO("FOMO"),
  OVERTRADING("Overtrading"),
  DISCIPLINED("Disciplined Exit");

  companion object {
    fun safeFrom(raw: String): JournalTag =
      entries.firstOrNull { it.name == raw } ?: NONE
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String = "",
  val virtualCash: Double = 100000.0,
  val initialCash: Double = 100000.0,
  val totalRealizedPnl: Double = 0.0,
  val totalTrades: Long = 0L,
  val winningTrades: Long = 0L,
  val portfolioResetsUsed: Long = 0L,
  val preferredTheme: String = TerminalThemeMode.DARK_OLED.name,
  val watchlist: List<String> = listOf("RELIANCE", "TCS", "NIFTY50", "GOLD_24K", "INFY"),
  val subscriptionActive: Boolean = false,
  val adFree: Boolean = false,
  val planId: String = "",
  val planName: String = "",
  val planStartedAtMillis: Long = 0L,
  val planExpiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  // Strictly verified against Firestore subscriptionActive && unexpired planExpiresAtMillis
  val hasValidProAccess: Boolean
    get() = subscriptionActive && planExpiresAtMillis > System.currentTimeMillis()
}

@Keep
@IgnoreExtraProperties
data class Position(
  @DocumentId val id: String = "",
  val userId: String = "",
  val symbol: String = "",
  val name: String = "",
  val category: String = AssetCategory.STOCK.name,
  val sector: String = "General",
  val positionSide: String = "LONG",
  val quantity: Double = 0.0,
  val avgPrice: Double = 0.0,
  val leverage: Long = 1L,
  val marginUsed: Double = 0.0,
  val stopLossPrice: Double = 0.0,
  val takeProfitPrice: Double = 0.0,
  val trailingSlDistance: Double = 0.0,
  val highWaterPrice: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val isShort: Boolean get() = positionSide == "SHORT"
}

@Keep
@IgnoreExtraProperties
data class TradeRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val symbol: String = "",
  val name: String = "",
  val category: String = AssetCategory.STOCK.name,
  val sector: String = "General",
  val side: String = "BUY",
  val orderType: String = "MARKET",
  val quantity: Double = 0.0,
  val executionPrice: Double = 0.0,
  val totalValue: Double = 0.0,
  val leverage: Long = 1L,
  val realizedPnl: Double = 0.0,
  val stopLossPrice: Double = 0.0,
  val takeProfitPrice: Double = 0.0,
  val journalTag: String = JournalTag.NONE.name,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PendingOrder(
  @DocumentId val id: String = "",
  val userId: String = "",
  val symbol: String = "",
  val name: String = "",
  val category: String = AssetCategory.STOCK.name,
  val sector: String = "General",
  val side: String = "BUY",
  val orderKind: String = BrokerOrderKind.LIMIT.name,
  val productType: String = OrderProductType.CNC.name,
  val quantity: Double = 0.0,
  val limitPrice: Double = 0.0,
  val triggerPrice: Double = 0.0,
  val leverage: Long = 1L,
  val stopLossPrice: Double = 0.0,
  val takeProfitPrice: Double = 0.0,
  val trailingSlDistance: Double = 0.0,
  val status: String = "OPEN",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomWatchlist(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val symbols: List<String> = emptyList(),
  val sortOrder: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderBasket(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val items: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SipPlan(
  @DocumentId val id: String = "",
  val userId: String = "",
  val symbol: String = "",
  val assetName: String = "",
  val amountInr: Double = 5000.0,
  val frequency: String = "MONTHLY",
  val active: Boolean = true,
  val totalInvested: Double = 0.0,
  val installmentsCompleted: Long = 0L,
  val lastExecutedAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PriceAlert(
  @DocumentId val id: String = "",
  val userId: String = "",
  val symbol: String = "",
  val assetName: String = "",
  val targetPrice: Double = 0.0,
  val direction: String = "ABOVE",
  val triggered: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
  @DocumentId val id: String = "",
  val name: String = "",
  val priceInr: Double = 199.0,
  val validityDays: Long = 30L,
  val discountBadge: String = "",
  val tagline: String = "",
  val active: Boolean = true,
  val sortOrder: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
  @DocumentId val id: String = "",
  val adMobEnabled: Boolean = true,
  val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
  val rewardedAdUnitId: String = "ca-app-pub-3940256099942544/5224354917",
  val startingVirtualCash: Double = 100000.0,
  val rewardedAdBonus: Double = 25000.0,
  val backendBaseUrl: String = "",
  val enableReferralCard: Boolean = false,
  val referralCardTitle: String = "",
  val referralCardSubtitle: String = "",
  val referralButtonLabel: String = "",
  val referralUrl: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

fun isValidHttpUrl(rawUrl: String): Boolean {
  val trimmed = rawUrl.trim()
  if (trimmed.isEmpty() || trimmed.length > 500 || trimmed.any { it.isWhitespace() }) {
    return false
  }
  val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return false
  val scheme = uri.scheme?.lowercase(java.util.Locale.ROOT) ?: return false
  val host = uri.host ?: return false
  return (scheme == "https" || scheme == "http") && host.isNotBlank()
}

@Keep
@IgnoreExtraProperties
data class MarketAsset(
  @DocumentId val id: String = "",
  val symbol: String = "",
  val name: String = "",
  val category: String = AssetCategory.STOCK.name,
  val sector: String = "General",
  val basePrice: Double = 100.0,
  val volatility: Double = 0.015,
  val unitLabel: String = "1 Share",
  val active: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val netWorth: Double = 100000.0,
  val returnPct: Double = 0.0,
  val proMember: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminGatewaySecret(
  @DocumentId val id: String = "",
  val apiKey: String = "",
  val webhookSecret: String = "",
  val backendUrl: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentOrder(
  @DocumentId val id: String = "",
  val orderId: String = "",
  val uid: String = "",
  val planId: String = "",
  val planName: String = "",
  val amountInr: Double = 0.0,
  val validityDays: Long = 30L,
  val status: String = "PENDING",
  val checkoutUrl: String = "",
  val utr: String = "",
  val createdAtMillis: Long = 0L,
  val verifiedAtMillis: Long = 0L
)
