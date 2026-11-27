package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.model.JournalTag
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.PendingOrder
import com.aurumtrade.shared.model.Position
import com.aurumtrade.shared.model.SipPlan
import com.aurumtrade.shared.model.TradeRecord
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.CompoundingChart
import com.aurumtrade.shared.ui.DailyPnlCalendarHeatmap
import com.aurumtrade.shared.ui.EquityCurveChart
import com.aurumtrade.shared.ui.ProCrownBadge
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.ui.SectorDonutChart
import com.aurumtrade.shared.util.formatCompactInr
import com.aurumtrade.shared.util.formatInr
import com.aurumtrade.shared.util.formatPct
import com.example.ui.components.ProFeature
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

@Composable
fun PortfolioSipScreen(
  profile: UserProfile?,
  positions: List<Position>,
  pendingOrders: List<PendingOrder>,
  trades: List<TradeRecord>,
  sipPlans: List<SipPlan>,
  quotes: Map<String, LiveQuote>,
  onClosePosition: (Position) -> Unit,
  onCancelPendingOrder: (String) -> Unit,
  onResetPortfolio: () -> Unit,
  onRefillProCash: () -> Unit,
  onCreateSip: (MarketAsset, Double, String) -> Unit,
  onRunSipNow: (SipPlan) -> Unit,
  onDeleteSip: (String) -> Unit,
  onUpdateTradeTag: (String, JournalTag) -> Unit,
  onExportCsvUri: () -> Result<Uri>,
  onExportPdfUri: () -> Result<Uri>,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val context = LocalContext.current
  val isPro = profile?.hasValidProAccess == true
  var activeSubTab by remember { mutableIntStateOf(0) } // 0=Holdings & Orders, 1=Auto-SIP, 2=Pro Analytics

  val equityValue = positions.sumOf { pos ->
    val live = quotes[pos.symbol]?.currentPrice ?: pos.avgPrice
    val unrealized = if (pos.isShort) (pos.avgPrice - live) * pos.quantity else (live - pos.avgPrice) * pos.quantity
    pos.marginUsed + unrealized
  }
  val unrealizedPnl = positions.sumOf { pos ->
    val live = quotes[pos.symbol]?.currentPrice ?: pos.avgPrice
    if (pos.isShort) (pos.avgPrice - live) * pos.quantity else (live - pos.avgPrice) * pos.quantity
  }
  val netWorth = (profile?.virtualCash ?: 0.0) + equityValue
  val initial = (profile?.initialCash ?: 100000.0).coerceAtLeast(1.0)
  val totalReturnPct = ((netWorth - initial) / initial) * 100.0

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Net Worth Hero Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Portfolio Net Worth", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = {
            if (!isPro && (profile?.portfolioResetsUsed ?: 0L) >= 1L) {
              onLockedFeatureClick(ProFeature.AUTO_SIP_ALERTS)
            } else {
              onResetPortfolio()
            }
          },
          modifier = Modifier.testTag("reset_portfolio_button")
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (isPro) "Reset (∞)" else "Reset (${1 - (profile?.portfolioResetsUsed ?: 0L).coerceAtMost(1L)})")
        }
      }

      Text(formatInr(netWorth), style = TabularMonoStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = "Cash: ${formatInr(profile?.virtualCash ?: 0.0)}",
          style = TabularMonoStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
          text = "Unrealized: ${formatInr(unrealizedPnl, showSign = true)} (${formatPct(totalReturnPct)})",
          style = TabularMonoStyle.copy(
            fontSize = 12.sp,
            color = if (unrealizedPnl >= 0) trading.bullishEmerald else trading.bearishCrimson,
            fontWeight = FontWeight.SemiBold
          )
        )
      }

      OutlinedButton(
        onClick = {
          if (isPro) onRefillProCash() else onLockedFeatureClick(ProFeature.AUTO_SIP_ALERTS)
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("pro_refill_cash_button")
      ) {
        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = trading.aurumGold, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("+₹1,00,000 Unlimited Capital Refill", color = trading.aurumGold)
        if (!isPro) {
          Spacer(Modifier.width(6.dp))
          ProLockBadge()
        }
      }
    }

    // Sub-tabs
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = activeSubTab == 0,
        onClick = { activeSubTab = 0 },
        label = { Text("Holdings (${positions.size})") }
      )
      FilterChip(
        selected = activeSubTab == 1,
        onClick = { activeSubTab = 1 },
        label = { Text("Auto-SIP") }
      )
      FilterChip(
        selected = activeSubTab == 2,
        onClick = { activeSubTab = 2 },
        label = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Analytics")
            if (!isPro) {
              Spacer(Modifier.width(4.dp))
              ProLockBadge()
            }
          }
        }
      )
    }

    when (activeSubTab) {
      0 -> HoldingsAndOrdersTab(
        positions = positions,
        pendingOrders = pendingOrders,
        quotes = quotes,
        isPro = isPro,
        onClosePosition = onClosePosition,
        onCancelPendingOrder = onCancelPendingOrder,
        onLockedFeatureClick = onLockedFeatureClick
      )

      1 -> AutoSipSimulatorSection(
        isPro = isPro,
        sipPlans = sipPlans,
        quotes = quotes,
        onCreateSip = onCreateSip,
        onRunSipNow = onRunSipNow,
        onDeleteSip = onDeleteSip,
        onLockedFeatureClick = onLockedFeatureClick
      )

      2 -> ProAnalyticsSection(
        isPro = isPro,
        netWorth = netWorth,
        initialCapital = initial,
        positions = positions,
        trades = trades,
        quotes = quotes,
        onUpdateTradeTag = onUpdateTradeTag,
        onExportCsv = {
          if (!isPro) {
            onLockedFeatureClick(ProFeature.ANALYTICS_JOURNAL)
          } else {
            onExportCsvUri().onSuccess { uri ->
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(intent, "Export CSV Report"))
            }.onFailure {
              Toast.makeText(context, "CSV export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
          }
        },
        onExportPdf = {
          if (!isPro) {
            onLockedFeatureClick(ProFeature.ANALYTICS_JOURNAL)
          } else {
            onExportPdfUri().onSuccess { uri ->
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(intent, "Export PDF Statement"))
            }.onFailure {
              Toast.makeText(context, "PDF export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
          }
        },
        onLockedFeatureClick = onLockedFeatureClick
      )
    }
  }
}

@Composable
private fun HoldingsAndOrdersTab(
  positions: List<Position>,
  pendingOrders: List<PendingOrder>,
  quotes: Map<String, LiveQuote>,
  isPro: Boolean,
  onClosePosition: (Position) -> Unit,
  onCancelPendingOrder: (String) -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val openOrders = pendingOrders.filter { it.status == "OPEN" }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (positions.isEmpty()) {
      EmptySectionCard(
        title = "No Open Positions",
        subtitle = "Head to Market or Trade to place NSE Stock, Gold, Commodity, Mutual Fund, or F&O paper trades."
      )
    } else {
      positions.forEach { pos ->
        val live = quotes[pos.symbol]?.currentPrice ?: pos.avgPrice
        val pnl = if (pos.isShort) (pos.avgPrice - live) * pos.quantity else (live - pos.avgPrice) * pos.quantity
        val pnlPct = if (pos.marginUsed > 0) (pnl / pos.marginUsed) * 100.0 else 0.0
        val pnlColor = if (pnl >= 0) trading.bullishEmerald else trading.bearishCrimson

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(pos.symbol, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (pos.isShort) "SHORT" else "LONG",
                style = TabularMonoStyle.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (pos.isShort) trading.bearishCrimson else trading.bullishEmerald
                )
              )
              if (pos.leverage > 1L) {
                Spacer(Modifier.width(6.dp))
                Text("${pos.leverage}x", style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.aurumGold))
              }
            }
            Text("${formatInr(pnl, showSign = true)} (${formatPct(pnlPct)})", style = TabularMonoStyle.copy(fontSize = 13.sp, color = pnlColor, fontWeight = FontWeight.Bold))
          }
          Text(
            text = "Qty ${String.format(Locale.US, "%.2f", pos.quantity)} • Avg ${formatInr(pos.avgPrice)} • LTP ${formatInr(live)}",
            style = TabularMonoStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
          if (pos.stopLossPrice > 0.0 || pos.takeProfitPrice > 0.0 || pos.trailingSlDistance > 0.0) {
            val trailStr = if (pos.trailingSlDistance > 0) " • Trail ${formatInr(pos.trailingSlDistance)}" else ""
            Text(
              text = "SL ${if (pos.stopLossPrice > 0) formatInr(pos.stopLossPrice) else "—"}  |  TP ${if (pos.takeProfitPrice > 0) formatInr(pos.takeProfitPrice) else "—"}$trailStr",
              style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.aurumGold)
            )
          }
          Button(
            onClick = { onClosePosition(pos) },
            colors = ButtonDefaults.buttonColors(containerColor = if (pos.isShort) trading.bullishEmerald else trading.bearishCrimson),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
          ) {
            Text(if (pos.isShort) "Cover Short @ ${formatInr(live)}" else "Square Off @ ${formatInr(live)}")
          }
        }
      }
    }

    // Pending Orders / GTT Queue (always visible)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Pending Limit / SL / GTT Orders (${openOrders.size})", style = MaterialTheme.typography.titleMedium)
      if (!isPro) ProLockBadge()
    }
    if (openOrders.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
          .clickable { if (!isPro) onLockedFeatureClick(ProFeature.ADVANCED_ORDERS) }
          .padding(14.dp)
      ) {
        Text(
          text = if (isPro) "No open pending orders. Place Limit, SL, SL-M or GTT orders from the Trade tab." else "Pro Limit, SL, SL-M & GTT auto-trigger queue — tap to preview.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      openOrders.forEach { order ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, trading.cardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${order.orderKind} ${order.side} ${order.symbol} (${order.quantity})", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "Limit ${formatInr(order.limitPrice)} • Trigger ${formatInr(order.triggerPrice)} • ${order.productType}",
              style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
          IconButton(onClick = { onCancelPendingOrder(order.id) }) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel Order", tint = trading.bearishCrimson)
          }
        }
      }
    }
  }
}

@Composable
private fun AutoSipSimulatorSection(
  isPro: Boolean,
  sipPlans: List<SipPlan>,
  quotes: Map<String, LiveQuote>,
  onCreateSip: (MarketAsset, Double, String) -> Unit,
  onRunSipNow: (SipPlan) -> Unit,
  onDeleteSip: (String) -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  var monthlyAmount by remember { mutableFloatStateOf(5000f) }
  var expectedCagr by remember { mutableFloatStateOf(14f) }
  var selectedSipSymbol by remember { mutableStateOf("NIFTY_INDEX_MF") }
  var sipAmountText by remember { mutableStateOf("5000") }

  val r = (expectedCagr.toDouble() / 100.0) / 12.0
  val months = 120
  val totalInvested10Y = monthlyAmount.toDouble() * months
  val futureValue10Y = if (r > 0) {
    monthlyAmount.toDouble() * (((1.0 + r).pow(months.toDouble()) - 1.0) / r) * (1.0 + r)
  } else totalInvested10Y

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("10-Year SIP Wealth Simulator", style = MaterialTheme.typography.titleLarge)
        if (isPro) ProCrownBadge() else ProLockBadge()
      }
      Text(
        text = "Monthly SIP: ${formatInr(monthlyAmount.toDouble(), decimals = 0)} • CAGR ${expectedCagr.toInt()}%",
        style = TabularMonoStyle.copy(fontSize = 13.sp, color = trading.aurumGold)
      )
      Slider(value = monthlyAmount, onValueChange = { monthlyAmount = it }, valueRange = 1000f..50000f)
      Slider(value = expectedCagr, onValueChange = { expectedCagr = it }, valueRange = 6f..24f)
      CompoundingChart(monthlyAmount = monthlyAmount.toDouble(), annualReturnPct = expectedCagr.toDouble(), years = 10)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text("Invested (10Y)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(formatCompactInr(totalInvested10Y), style = TabularMonoStyle.copy(fontWeight = FontWeight.Bold))
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Projected Wealth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(formatCompactInr(futureValue10Y), style = TabularMonoStyle.copy(color = trading.aurumGold, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
      }
    }

    if (!isPro) {
      ProPreviewCard(
        title = "Auto-SIP Mandates (PRO)",
        subtitle = "Create automated Index, Digital Gold & Mutual Fund SIP mandates with 1-tap execution.",
        onClick = { onLockedFeatureClick(ProFeature.AUTO_SIP_ALERTS) }
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Create Auto-SIP Mandate", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("NIFTY_INDEX_MF", "GOLD_ETF_MF", "GOLD_24K", "NIFTY50").forEach { sym ->
            FilterChip(selected = selectedSipSymbol == sym, onClick = { selectedSipSymbol = sym }, label = { Text(sym.replace("_MF", "")) })
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = sipAmountText,
            onValueChange = { sipAmountText = it },
            label = { Text("SIP ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Button(
            onClick = {
              val asset = quotes[selectedSipSymbol]?.asset ?: return@Button
              val amt = sipAmountText.toDoubleOrNull()?.coerceAtLeast(100.0) ?: 5000.0
              onCreateSip(asset, amt, "MONTHLY")
            },
            modifier = Modifier.heightIn(min = 52.dp)
          ) { Text("Start SIP") }
        }
      }
      sipPlans.forEach { sip ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(sip.assetName, style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${formatInr(sip.amountInr)} / ${sip.frequency} • ${sip.installmentsCompleted} runs (${formatInr(sip.totalInvested)})",
              style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onRunSipNow(sip) }) { Text("Execute") }
            OutlinedButton(onClick = { onDeleteSip(sip.id) }) { Text("Del") }
          }
        }
      }
    }
  }
}

@Composable
private fun ProAnalyticsSection(
  isPro: Boolean,
  netWorth: Double,
  initialCapital: Double,
  positions: List<Position>,
  trades: List<TradeRecord>,
  quotes: Map<String, LiveQuote>,
  onUpdateTradeTag: (String, JournalTag) -> Unit,
  onExportCsv: () -> Unit,
  onExportPdf: () -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val closedTrades = trades.filter { it.realizedPnl != 0.0 || it.side == "SELL" }
  val wins = closedTrades.filter { it.realizedPnl > 0.0 }
  val losses = closedTrades.filter { it.realizedPnl < 0.0 }
  val winRate = if (closedTrades.isNotEmpty()) (wins.size.toDouble() / closedTrades.size) * 100.0 else 0.0
  val grossProfit = wins.sumOf { it.realizedPnl }
  val grossLoss = abs(losses.sumOf { it.realizedPnl })
  val profitFactor = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0.0) 9.99 else 0.0
  val avgWin = if (wins.isNotEmpty()) grossProfit / wins.size else 0.0
  val avgLoss = if (losses.isNotEmpty()) grossLoss / losses.size else 0.0
  val avgRiskReward = if (avgLoss > 0.0) avgWin / avgLoss else if (avgWin > 0.0) 2.5 else 0.0

  var peak = 0.0
  var cumulative = 0.0
  var maxDrawdown = 0.0
  closedTrades.reversed().forEach { t ->
    cumulative += t.realizedPnl
    peak = max(peak, cumulative)
    maxDrawdown = max(maxDrawdown, peak - cumulative)
  }

  val sectorMap = positions.groupBy { it.sector }
    .mapValues { (_, list) ->
      list.sumOf { pos -> pos.quantity * (quotes[pos.symbol]?.currentPrice ?: pos.avgPrice) }
    }.toList().sortedByDescending { it.second }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (!isPro) {
      ProPreviewCard(
        title = "Institutional Analytics Preview",
        subtitle = "All analytics tools are visible below. Unlock Pro to export CSV/PDF reports & tag your psychology journal.",
        onClick = { onLockedFeatureClick(ProFeature.ANALYTICS_JOURNAL) }
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Pro Performance Desk", style = MaterialTheme.typography.titleLarge)
        if (!isPro) ProLockBadge()
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox("Win Rate", String.format(Locale.US, "%.1f%%", winRate), Modifier.weight(1f))
        MetricBox("Profit Factor", String.format(Locale.US, "%.2f", profitFactor), Modifier.weight(1f))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox("Max Drawdown", formatCompactInr(maxDrawdown), Modifier.weight(1f))
        MetricBox("Avg R:R Ratio", String.format(Locale.US, "1 : %.2f", avgRiskReward), Modifier.weight(1f))
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_csv_button")) {
          Icon(Icons.Rounded.Download, contentDescription = "Export CSV", modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("CSV")
          if (!isPro) { Spacer(Modifier.width(4.dp)); ProLockBadge() }
        }
        OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_pdf_button")) {
          Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("PDF")
          if (!isPro) { Spacer(Modifier.width(4.dp)); ProLockBadge() }
        }
      }

      EquityCurveChart(initialCapital = initialCapital, trades = trades, currentNetWorth = netWorth)
      DailyPnlCalendarHeatmap(trades = trades)

      if (sectorMap.isNotEmpty()) {
        Text("Sector Exposure", style = MaterialTheme.typography.titleMedium)
        SectorDonutChart(sectorAllocations = sectorMap)
      }
    }

    Text("Trade Psychology Journal (${trades.size})", style = MaterialTheme.typography.titleMedium)
    trades.take(25).forEach { t ->
      val currentTag = JournalTag.safeFrom(t.journalTag)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.cardBorder, RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${t.side} ${String.format(Locale.US, "%.2f", t.quantity)} ${t.symbol}", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${t.orderType} @ ${formatInr(t.executionPrice)} • ${t.leverage}x",
              style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
          if (t.realizedPnl != 0.0 || t.side == "SELL") {
            Text(
              text = formatInr(t.realizedPnl, showSign = true),
              style = TabularMonoStyle.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (t.realizedPnl >= 0) trading.bullishEmerald else trading.bearishCrimson
              )
            )
          }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          JournalTag.entries.filter { it != JournalTag.NONE }.forEach { tag ->
            FilterChip(
              selected = currentTag == tag,
              onClick = {
                if (isPro) onUpdateTradeTag(t.id, tag) else onLockedFeatureClick(ProFeature.ANALYTICS_JOURNAL)
              },
              label = { Text(tag.label, fontSize = 10.sp) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(10.dp)
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    Text(value, style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
  }
}

@Composable
private fun ProPreviewCard(title: String, subtitle: String, onClick: () -> Unit) {
  val trading = LocalAurumTradingColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(trading.aurumGoldSoft)
      .border(1.dp, trading.aurumGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(title, style = MaterialTheme.typography.titleMedium.copy(color = trading.aurumGold, fontWeight = FontWeight.Bold))
      ProLockBadge()
    }
    Text(subtitle, style = MaterialTheme.typography.bodySmall)
    Button(
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.heightIn(min = 44.dp)
    ) {
      Text("Unlock with Pro Subscription", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun EmptySectionCard(title: String, subtitle: String) {
  val trading = LocalAurumTradingColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
