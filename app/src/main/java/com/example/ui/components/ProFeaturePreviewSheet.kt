package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProLockBadge

enum class ProFeature(
  val title: String,
  val subtitle: String,
  val highlights: List<String>
) {
  PRO_CHARTS(
    title = "Pro Multi-Timeframe Candlesticks & Indicators",
    subtitle = "Analyze NSE Stocks, Commodities & Indices like an institutional desk.",
    highlights = listOf(
      "Candlestick, Hollow Candles & Heikin-Ashi chart styles",
      "1m, 5m, 15m, 1H, 1D & 1W live timeframes with Crosshair OHLCV inspector",
      "Toggleable EMA 9/20/50, SMA 200, Bollinger Bands, VWAP, RSI(14) & MACD"
    )
  ),
  ADVANCED_ORDERS(
    title = "Limit, SL, SL-M, GTT & Trailing Brackets",
    subtitle = "Automate entries and risk management while the live price engine ticks.",
    highlights = listOf(
      "Place pending Limit, Stop-Loss (SL/SL-M) & Good-Till-Triggered (GTT) orders",
      "Bracket Orders with Target + Stop-Loss + Auto-Ratcheting Trailing Stop-Loss",
      "5x MIS Intraday margin leverage & Short Selling (sell high, cover lower)"
    )
  ),
  MARKET_DEPTH_BASKETS(
    title = "5-Level Order Book & 1-Tap Basket Orders",
    subtitle = "Inspect bid/ask liquidity and deploy multi-stock strategies in one tap.",
    highlights = listOf(
      "Live 5-level Bid/Ask depth ladder with total buy vs sell pressure ratio",
      "Save custom 3–5 stock Basket Orders for instant 1-tap execution",
      "Tag executions with Trade Psychology setup tags for discipline tracking"
    )
  ),
  OPTION_CHAIN(
    title = "NIFTY 50 & BANKNIFTY Paper F&O Terminal",
    subtitle = "Practice index options trading with live simulated Greeks and Open Interest.",
    highlights = listOf(
      "Full CE / PE strike ladder for NIFTY 50 & BANKNIFTY (Weekly & Monthly)",
      "Visual Open Interest bars, Implied Volatility %, Delta & Theta Greeks",
      "Paper Buy/Sell Calls & Puts tracked live in your Portfolio"
    )
  ),
  SCREENER_SCANNER(
    title = "Full Market Screener & Technical Scanner",
    subtitle = "Spot momentum breakouts, oversold reversals & sector leaders instantly.",
    highlights = listOf(
      "Unlocks complete Top Gainers, Top Losers & Most Active asset lists",
      "Real-time Technical Signal Scanner: RSI Oversold, Golden Cross, 52W Breakout, VWAP",
      "Up to 5 Custom Watchlists with unlimited tracked symbols"
    )
  ),
  ANALYTICS_JOURNAL(
    title = "Pro Analytics, P&L Heatmap & PDF/CSV Export",
    subtitle = "Audit your trading edge with hedge-fund style performance metrics.",
    highlights = listOf(
      "Zerodha-style 28-Day Daily P&L Calendar Heatmap & Account Equity Curve",
      "Win Rate %, Profit Factor, Max Drawdown %, Avg Risk-Reward & Sector Donut",
      "Editable Trade Psychology Journal + 1-tap CSV & PDF statement export"
    )
  ),
  AUTO_SIP_ALERTS(
    title = "Auto-SIP Mandates, Capital Refill & Alerts",
    subtitle = "Build long-term wealth simulations and never miss a key price level.",
    highlights = listOf(
      "Automated recurring Index, Digital Gold & Mutual Fund SIP mandates",
      "Unlimited Price Target Alerts, Portfolio Resets & Instant +₹1L Capital Refills",
      "100% Ad-Free terminal + Gold PRO Crown + Midnight Gold & Neon Terminal themes"
    )
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProFeaturePreviewBottomSheet(
  feature: ProFeature,
  onDismiss: () -> Unit,
  onUnlockPro: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        ProLockBadge(label = "AURUM PRO")
        Text(
          text = "Instant Auto-UPI Unlock",
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.bullishEmerald, fontWeight = FontWeight.SemiBold)
        )
      }

      Text(
        text = feature.title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = feature.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(trading.aurumGoldSoft.copy(alpha = 0.45f))
          .border(1.dp, trading.aurumGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        feature.highlights.forEach { point ->
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = trading.bullishEmerald,
              modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = point, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      Button(
        onClick = {
          onDismiss()
          onUnlockPro()
        },
        colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
          .testTag("sheet_unlock_pro_button")
      ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Unlock with Pro Subscription", fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
      ) {
        Text("Maybe Later")
      }
    }
  }
}

private data class ComparisonRowData(
  val label: String,
  val freeTier: String,
  val proTier: String,
  val linkedFeature: ProFeature? = null
)

@Composable
fun FreeVsProComparisonSection(
  isPro: Boolean,
  onInspectFeature: (ProFeature) -> Unit,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val rows = listOf(
    ComparisonRowData("Charts & Styles", "Area (1D)", "Candles, Hollow, HA (1m–1W)", ProFeature.PRO_CHARTS),
    ComparisonRowData("Indicators", "Price + High/Low", "EMA, SMA200, BB, VWAP, RSI, MACD", ProFeature.PRO_CHARTS),
    ComparisonRowData("Order Types", "CNC Market", "Limit, SL, SL-M, GTT, Bracket + TSL", ProFeature.ADVANCED_ORDERS),
    ComparisonRowData("Leverage & Short", "1x Long Only", "5x MIS + Short Selling", ProFeature.ADVANCED_ORDERS),
    ComparisonRowData("Depth & Baskets", "Preview Only", "5-Level Book + 1-Tap Baskets", ProFeature.MARKET_DEPTH_BASKETS),
    ComparisonRowData("F&O Option Chain", "View Strikes", "Full NIFTY & BANKNIFTY Paper F&O", ProFeature.OPTION_CHAIN),
    ComparisonRowData("Screener & Lists", "Top 3 + 1 List (5)", "Full Scanner + 5 Watchlists (∞)", ProFeature.SCREENER_SCANNER),
    ComparisonRowData("Analytics & Export", "SIP 10Y Calc", "P&L Heatmap, Equity Curve, CSV/PDF", ProFeature.ANALYTICS_JOURNAL),
    ComparisonRowData("Ads & Themes", "Banner + Rewarded Ad", "100% Ad-Free + Midnight Gold & Neon", ProFeature.AUTO_SIP_ALERTS)
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.aurumGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    val bannerBrush = Brush.linearGradient(
      colors = listOf(trading.aurumGoldSoft, trading.bullishSoft)
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(bannerBrush)
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = trading.aurumGold, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            text = if (isPro) "PRO TERMINAL ACTIVE" else "FREE vs PRO TERMINAL",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = trading.aurumGold)
          )
        }
        Text(
          text = if (isPro) "All institutional tools, F&O, and analytics unlocked." else "Tap any Pro row to preview what unlocks instantly.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("Feature", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.1f))
      Text("Free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.95f))
      Text("Pro", style = MaterialTheme.typography.labelSmall.copy(color = trading.aurumGold, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.25f))
    }
    HorizontalDivider(color = trading.cardBorder)

    rows.forEachIndexed { idx, row ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable(enabled = row.linkedFeature != null) {
            row.linkedFeature?.let(onInspectFeature)
          }
          .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = row.label,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          modifier = Modifier.weight(1.1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.95f)) {
          if (row.freeTier == "Preview Only") {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = trading.bearishCrimson, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
          }
          Text(
            text = row.freeTier,
            style = TabularMonoStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.25f)) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = trading.bullishEmerald, modifier = Modifier.size(13.dp))
          Spacer(Modifier.width(4.dp))
          Text(
            text = row.proTier,
            style = TabularMonoStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = trading.aurumGold)
          )
        }
      }
      if (idx < rows.lastIndex) {
        HorizontalDivider(color = trading.cardBorder.copy(alpha = 0.5f))
      }
    }
  }
}
