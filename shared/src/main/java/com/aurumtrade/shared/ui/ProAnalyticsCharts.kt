package com.aurumtrade.shared.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.model.TradeRecord
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.util.formatCompactInr
import com.aurumtrade.shared.util.formatInr
import com.aurumtrade.shared.util.formatPct
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class DailyPnlTile(
  val dateKey: String,
  val displayDate: String,
  val realizedPnl: Double,
  val tradeCount: Int
)

@Composable
fun DailyPnlCalendarHeatmap(
  trades: List<TradeRecord>,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val keyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val labelFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

  val tiles = remember(trades) {
    val grouped = trades.filter { it.createdAt != null }
      .groupBy { keyFmt.format(it.createdAt!!.toDate()) }

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -27)
    (0 until 28).map {
      val date = cal.time
      val k = keyFmt.format(date)
      val dayTrades = grouped[k].orEmpty()
      val pnl = dayTrades.filter { t -> t.side == "SELL" || t.realizedPnl != 0.0 }.sumOf { t -> t.realizedPnl }
      cal.add(Calendar.DAY_OF_YEAR, 1)
      DailyPnlTile(
        dateKey = k,
        displayDate = labelFmt.format(date),
        realizedPnl = pnl,
        tradeCount = dayTrades.size
      )
    }
  }

  var selectedTile by remember(tiles) { mutableStateOf(tiles.lastOrNull { it.tradeCount > 0 } ?: tiles.lastOrNull()) }
  val maxAbs = max(100.0, tiles.maxOfOrNull { abs(it.realizedPnl) } ?: 100.0)

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Daily P&L Calendar (28D)", style = MaterialTheme.typography.titleMedium)
      selectedTile?.let { sel ->
        Text(
          text = "${sel.displayDate}: ${formatInr(sel.realizedPnl, showSign = true)} (${sel.tradeCount})",
          style = TabularMonoStyle.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
              sel.realizedPnl > 0 -> trading.bullishEmerald
              sel.realizedPnl < 0 -> trading.bearishCrimson
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        )
      }
    }

    // 4 weeks x 7 days Zerodha-style tile grid
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      tiles.chunked(7).forEach { week ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          week.forEach { tile ->
            val intensity = (abs(tile.realizedPnl) / maxAbs).toFloat().coerceIn(0.25f, 1f)
            val bgColor = when {
              tile.tradeCount == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
              tile.realizedPnl > 0.0 -> trading.bullishEmerald.copy(alpha = intensity * 0.85f)
              tile.realizedPnl < 0.0 -> trading.bearishCrimson.copy(alpha = intensity * 0.85f)
              else -> trading.aurumGold.copy(alpha = 0.35f)
            }
            val isSelected = selectedTile?.dateKey == tile.dateKey
            Box(
              modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .border(
                  width = if (isSelected) 1.5.dp else 0.5.dp,
                  color = if (isSelected) trading.aurumGold else trading.cardBorder,
                  shape = RoundedCornerShape(6.dp)
                )
                .clickable { selectedTile = tile },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = tile.dateKey.takeLast(2),
                style = TabularMonoStyle.copy(
                  fontSize = 9.5.sp,
                  color = if (tile.tradeCount > 0 && tile.realizedPnl != 0.0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EquityCurveChart(
  initialCapital: Double,
  trades: List<TradeRecord>,
  currentNetWorth: Double,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val curve = remember(initialCapital, trades, currentNetWorth) {
    val pts = mutableListOf(initialCapital)
    var running = initialCapital
    trades.reversed().forEach { t ->
      if (t.realizedPnl != 0.0) {
        running += t.realizedPnl
        pts.add(max(0.0, running))
      }
    }
    pts.add(max(0.0, currentNetWorth))
    pts
  }

  val isGain = curve.last() >= initialCapital
  val strokeColor = if (isGain) trading.bullishEmerald else trading.bearishCrimson

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("Account Equity Curve", style = MaterialTheme.typography.titleMedium)
      Text(
        text = formatCompactInr(curve.last()),
        style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = strokeColor)
      )
    }

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
    ) {
      val minV = curve.min()
      val maxV = curve.max()
      val range = max(1.0, maxV - minV)
      val padY = size.height * 0.10f
      val drawH = size.height - padY * 2f
      val stepX = if (curve.size > 1) size.width / (curve.size - 1) else size.width

      val strokeP = Path()
      val fillP = Path()
      curve.forEachIndexed { idx, v ->
        val x = idx * stepX
        val y = padY + drawH * (1f - ((v - minV) / range).toFloat())
        if (idx == 0) {
          strokeP.moveTo(x, y)
          fillP.moveTo(x, size.height)
          fillP.lineTo(x, y)
        } else {
          strokeP.lineTo(x, y)
          fillP.lineTo(x, y)
        }
      }
      fillP.lineTo((curve.size - 1) * stepX, size.height)
      fillP.close()

      drawPath(
        fillP,
        brush = Brush.verticalGradient(listOf(strokeColor.copy(alpha = 0.28f), Color.Transparent))
      )
      drawPath(
        strokeP,
        color = strokeColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      )
    }
  }
}

data class SectorHeatItem(
  val sector: String,
  val avgChangePct: Double,
  val assetCount: Int,
  val topSymbol: String
)

@Composable
fun SectorHeatmapGrid(
  quotes: Map<String, LiveQuote>,
  onSectorStockClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val targetSectors = listOf("Banking", "IT", "Auto", "Pharma", "Energy", "FMCG", "Commodities", "Index")

  val sectors = remember(quotes) {
    quotes.values
      .groupBy { it.asset.sector }
      .map { (sector, list) ->
        val avg = list.map { it.changePct }.average()
        val leader = list.maxByOrNull { abs(it.changePct) }?.asset?.symbol ?: list.first().asset.symbol
        SectorHeatItem(sector = sector, avgChangePct = avg, assetCount = list.size, topSymbol = leader)
      }
      .sortedBy { item ->
        val idx = targetSectors.indexOf(item.sector)
        if (idx >= 0) idx else 99
      }
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    sectors.chunked(2).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rowItems.forEach { item ->
          val up = item.avgChangePct >= 0.0
          val intensity = (abs(item.avgChangePct) / 2.5).toFloat().coerceIn(0.18f, 0.72f)
          val accent = if (up) trading.bullishEmerald else trading.bearishCrimson
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(accent.copy(alpha = intensity * 0.35f))
              .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
              .clickable { onSectorStockClick(item.topSymbol) }
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = item.sector,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = formatPct(item.avgChangePct),
                style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
              )
            }
            Text(
              text = "${item.assetCount} assets • Mover: ${item.topSymbol}",
              style = TabularMonoStyle.copy(fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
        if (rowItems.size == 1) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}
