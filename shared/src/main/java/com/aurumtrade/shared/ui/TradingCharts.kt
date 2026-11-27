package com.aurumtrade.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.Candle
import com.aurumtrade.shared.market.SimulatedMarketEngine
import com.aurumtrade.shared.model.ChartType
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.util.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProCrownBadge(
  modifier: Modifier = Modifier,
  label: String = "PRO"
) {
  val colors = LocalAurumTradingColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(colors.aurumGoldSoft)
      .border(1.dp, colors.aurumGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
      .padding(horizontal = 7.dp, vertical = 3.dp)
  ) {
    Icon(
      imageVector = Icons.Rounded.WorkspacePremium,
      contentDescription = "Pro Member",
      tint = colors.aurumGold,
      modifier = Modifier.size(13.dp)
    )
    Spacer(Modifier.width(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = colors.aurumGold,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
    )
  }
}

@Composable
fun ProLockBadge(
  modifier: Modifier = Modifier,
  label: String = "PRO"
) {
  val colors = LocalAurumTradingColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .shadow(
        elevation = 4.dp,
        shape = RoundedCornerShape(7.dp),
        ambientColor = colors.aurumGold.copy(alpha = 0.6f),
        spotColor = colors.aurumGold.copy(alpha = 0.6f)
      )
      .clip(RoundedCornerShape(7.dp))
      .background(colors.aurumGold.copy(alpha = 0.18f))
      .border(1.dp, colors.aurumGold.copy(alpha = 0.75f), RoundedCornerShape(7.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Icon(
      imageVector = Icons.Rounded.Lock,
      contentDescription = "Pro Locked",
      tint = colors.aurumGold,
      modifier = Modifier.size(11.dp)
    )
    Spacer(Modifier.width(3.dp))
    Text(
      text = label,
      style = TabularMonoStyle.copy(
        fontSize = 9.5.sp,
        color = colors.aurumGold,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    )
  }
}

@Composable
fun AreaPriceChart(
  candles: List<Candle>,
  previousClose: Double,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  if (candles.isEmpty()) return

  val visible = remember(candles) { candles.takeLast(60) }
  val closes = remember(visible) { visible.map { it.close } }
  val isUp = closes.last() >= previousClose
  val lineColor = if (isUp) trading.bullishEmerald else trading.bearishCrimson

  var selectedIdx by remember(visible.size) { mutableIntStateOf(-1) }
  val hudCandle = if (selectedIdx in visible.indices) visible[selectedIdx] else visible.last()
  val fmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

  Column(modifier = modifier) {
    // Touch-and-drag Crosshair OHLCV Inspector HUD
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "O ${formatInr(hudCandle.open, decimals = 1)} H ${formatInr(hudCandle.high, decimals = 1)} L ${formatInr(hudCandle.low, decimals = 1)} C ${formatInr(hudCandle.close, decimals = 1)}",
        style = TabularMonoStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
      )
      Text(
        text = if (selectedIdx in visible.indices) fmt.format(Date(hudCandle.timestampMillis)) else "V ${hudCandle.volume}",
        style = TabularMonoStyle.copy(color = lineColor, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
      )
    }

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
        .pointerInput(visible.size) {
          detectTapGestures { offset ->
            val idx = ((offset.x / size.width) * (closes.size - 1)).roundToInt().coerceIn(0, closes.lastIndex)
            selectedIdx = if (selectedIdx == idx) -1 else idx
          }
        }
        .pointerInput(visible.size) {
          detectDragGestures(
            onDragEnd = { selectedIdx = -1 },
            onDragCancel = { selectedIdx = -1 }
          ) { change, _ ->
            val idx = ((change.position.x / size.width) * (closes.size - 1)).roundToInt().coerceIn(0, closes.lastIndex)
            selectedIdx = idx
          }
        }
    ) {
      val minVal = min(closes.min(), previousClose)
      val maxVal = max(closes.max(), previousClose)
      val range = max(0.01, maxVal - minVal)
      val padY = size.height * 0.08f
      val drawH = size.height - padY * 2f

      for (i in 0..3) {
        val y = padY + (drawH / 3f) * i
        drawLine(trading.chartGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
      }

      val refY = padY + drawH * (1f - ((previousClose - minVal) / range).toFloat())
      drawLine(
        color = Color.Gray.copy(alpha = 0.45f),
        start = Offset(0f, refY),
        end = Offset(size.width, refY),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
      )

      val strokePath = Path()
      val fillPath = Path()
      val stepX = if (closes.size > 1) size.width / (closes.size - 1) else size.width

      closes.forEachIndexed { i, price ->
        val x = i * stepX
        val y = padY + drawH * (1f - ((price - minVal) / range).toFloat())
        if (i == 0) {
          strokePath.moveTo(x, y)
          fillPath.moveTo(x, size.height)
          fillPath.lineTo(x, y)
        } else {
          strokePath.lineTo(x, y)
          fillPath.lineTo(x, y)
        }
      }
      fillPath.lineTo((closes.size - 1) * stepX, size.height)
      fillPath.close()

      drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
          colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
          startY = 0f,
          endY = size.height
        )
      )
      drawPath(
        path = strokePath,
        color = lineColor,
        style = Stroke(width = 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      )

      if (selectedIdx in closes.indices) {
        val x = selectedIdx * stepX
        val y = padY + drawH * (1f - ((closes[selectedIdx] - minVal) / range).toFloat())
        drawLine(
          color = trading.aurumGold.copy(alpha = 0.65f),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1.5f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
        drawLine(
          color = trading.aurumGold.copy(alpha = 0.45f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.2f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
        drawCircle(color = trading.aurumGold, radius = 8f, center = Offset(x, y))
      }
    }
  }
}

@Composable
fun ProBrokerChart(
  candles: List<Candle>,
  chartType: ChartType,
  ema9: List<Double>,
  ema20: List<Double>,
  ema50: List<Double>,
  sma200: List<Double>,
  bbUpper: List<Double>,
  bbLower: List<Double>,
  vwap: List<Double>,
  rsi14: List<Double>,
  macdLine: List<Double>,
  macdSignal: List<Double>,
  macdHist: List<Double>,
  showEma9: Boolean,
  showEma20: Boolean,
  showEma50: Boolean,
  showSma200: Boolean,
  showBollinger: Boolean,
  showVwap: Boolean,
  showRsi: Boolean,
  showMacd: Boolean,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  if (candles.isEmpty()) return

  val displayCandles = remember(candles, chartType) {
    val raw = candles.takeLast(min(50, candles.size))
    if (chartType == ChartType.HEIKIN_ASHI) SimulatedMarketEngine.toHeikinAshi(raw) else raw
  }
  val visibleCount = displayCandles.size
  val vEma9 = remember(ema9, visibleCount) { ema9.takeLast(visibleCount) }
  val vEma20 = remember(ema20, visibleCount) { ema20.takeLast(visibleCount) }
  val vEma50 = remember(ema50, visibleCount) { ema50.takeLast(visibleCount) }
  val vSma200 = remember(sma200, visibleCount) { sma200.takeLast(visibleCount) }
  val vBbU = remember(bbUpper, visibleCount) { bbUpper.takeLast(visibleCount) }
  val vBbL = remember(bbLower, visibleCount) { bbLower.takeLast(visibleCount) }
  val vVwap = remember(vwap, visibleCount) { vwap.takeLast(visibleCount) }
  val vRsi = remember(rsi14, visibleCount) { rsi14.takeLast(visibleCount) }
  val vMacd = remember(macdLine, visibleCount) { macdLine.takeLast(visibleCount) }
  val vSig = remember(macdSignal, visibleCount) { macdSignal.takeLast(visibleCount) }
  val vHist = remember(macdHist, visibleCount) { macdHist.takeLast(visibleCount) }

  var selectedIdx by remember(visibleCount) { mutableIntStateOf(-1) }
  val hudCandle = if (selectedIdx in displayCandles.indices) displayCandles[selectedIdx] else displayCandles.last()
  val hudRsi = if (selectedIdx in vRsi.indices) vRsi[selectedIdx] else vRsi.lastOrNull() ?: 50.0
  val hudMacd = if (selectedIdx in vMacd.indices) vMacd[selectedIdx] else vMacd.lastOrNull() ?: 0.0
  val timeFmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

  Column(modifier = modifier) {
    // OHLCV Crosshair Inspector HUD
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "O ${formatInr(hudCandle.open, decimals = 1)}  H ${formatInr(hudCandle.high, decimals = 1)}  L ${formatInr(hudCandle.low, decimals = 1)}  C ${formatInr(hudCandle.close, decimals = 1)}",
          style = TabularMonoStyle.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp)
        )
        Text(
          text = "Vol ${hudCandle.volume}",
          style = TabularMonoStyle.copy(color = trading.aurumGold, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (selectedIdx in displayCandles.indices) timeFmt.format(Date(hudCandle.timestampMillis)) else "${chartType.label} • Drag chart to inspect",
          style = TabularMonoStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (showRsi) {
            Text(
              text = "RSI ${String.format(Locale.US, "%.1f", hudRsi)}",
              style = TabularMonoStyle.copy(
                color = when {
                  hudRsi >= 70.0 -> trading.bearishCrimson
                  hudRsi <= 30.0 -> trading.bullishEmerald
                  else -> trading.aurumGold
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            )
          }
          if (showMacd) {
            Text(
              text = "MACD ${String.format(Locale.US, "%.1f", hudMacd)}",
              style = TabularMonoStyle.copy(color = trading.ema50Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }

    Spacer(Modifier.height(6.dp))

    // Main Candle + Overlay Indicators Canvas
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(230.dp)
        .pointerInput(visibleCount) {
          detectTapGestures { offset ->
            val idx = ((offset.x / size.width) * visibleCount).toInt().coerceIn(0, visibleCount - 1)
            selectedIdx = if (selectedIdx == idx) -1 else idx
          }
        }
        .pointerInput(visibleCount) {
          detectDragGestures(
            onDragEnd = { selectedIdx = -1 },
            onDragCancel = { selectedIdx = -1 }
          ) { change, _ ->
            val idx = ((change.position.x / size.width) * visibleCount).toInt().coerceIn(0, visibleCount - 1)
            selectedIdx = idx
          }
        }
    ) {
      var minVal = displayCandles.minOf { it.low }
      var maxVal = displayCandles.maxOf { it.high }
      if (showBollinger && vBbU.isNotEmpty() && vBbL.isNotEmpty()) {
        minVal = min(minVal, vBbL.min())
        maxVal = max(maxVal, vBbU.max())
      }
      if (showSma200 && vSma200.isNotEmpty()) {
        minVal = min(minVal, vSma200.min())
        maxVal = max(maxVal, vSma200.max())
      }
      val range = max(0.01, maxVal - minVal)
      val padY = size.height * 0.06f
      val drawH = size.height - padY * 2f
      fun priceToY(p: Double): Float = padY + drawH * (1f - ((p - minVal) / range).toFloat())

      for (i in 0..4) {
        val y = padY + (drawH / 4f) * i
        drawLine(trading.chartGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
      }

      val slotW = size.width / visibleCount
      val bodyW = max(3f, slotW * 0.62f)

      fun drawOverlayLine(values: List<Double>, color: Color, strokeWidth: Float = 2.4f, dashed: Boolean = false) {
        if (values.size != visibleCount) return
        val path = Path()
        values.forEachIndexed { i, v ->
          val x = i * slotW + slotW / 2f
          val y = priceToY(v)
          if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
          path = path,
          color = color,
          style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null
          )
        )
      }

      // Bollinger Bands envelope
      if (showBollinger && vBbU.size == visibleCount && vBbL.size == visibleCount) {
        val bandFill = Path()
        vBbU.forEachIndexed { i, v ->
          val x = i * slotW + slotW / 2f
          val y = priceToY(v)
          if (i == 0) bandFill.moveTo(x, y) else bandFill.lineTo(x, y)
        }
        for (i in visibleCount - 1 downTo 0) {
          bandFill.lineTo(i * slotW + slotW / 2f, priceToY(vBbL[i]))
        }
        bandFill.close()
        drawPath(bandFill, color = trading.ema50Cyan.copy(alpha = 0.09f))
        drawOverlayLine(vBbU, trading.ema50Cyan.copy(alpha = 0.65f), 1.8f, dashed = true)
        drawOverlayLine(vBbL, trading.ema50Cyan.copy(alpha = 0.65f), 1.8f, dashed = true)
      }

      // Candles (Standard, Hollow, or Heikin-Ashi)
      displayCandles.forEachIndexed { idx, c ->
        val centerX = idx * slotW + slotW / 2f
        val bullish = c.close >= c.open
        val color = if (bullish) trading.bullishEmerald else trading.bearishCrimson
        val highY = priceToY(c.high)
        val lowY = priceToY(c.low)
        val openY = priceToY(c.open)
        val closeY = priceToY(c.close)

        drawLine(color = color, start = Offset(centerX, highY), end = Offset(centerX, lowY), strokeWidth = 2.0f)

        val topY = min(openY, closeY)
        val bodyH = max(2.5f, abs(closeY - openY))
        if (chartType == ChartType.HOLLOW && bullish) {
          drawRect(
            color = color,
            topLeft = Offset(centerX - bodyW / 2f, topY),
            size = Size(bodyW, bodyH),
            style = Stroke(width = 2.2f)
          )
        } else {
          drawRect(
            color = color,
            topLeft = Offset(centerX - bodyW / 2f, topY),
            size = Size(bodyW, bodyH)
          )
        }
      }

      if (showEma9) drawOverlayLine(vEma9, trading.bullishEmerald.copy(alpha = 0.85f), 2.2f)
      if (showEma20) drawOverlayLine(vEma20, trading.ema20Amber, 2.5f)
      if (showEma50) drawOverlayLine(vEma50, trading.ema50Cyan, 2.5f)
      if (showSma200) drawOverlayLine(vSma200, trading.sma200Violet, 2.8f)
      if (showVwap) drawOverlayLine(vVwap, trading.vwapPink, 2.4f, dashed = true)

      if (selectedIdx in displayCandles.indices) {
        val x = selectedIdx * slotW + slotW / 2f
        val y = priceToY(hudCandle.close)
        drawLine(
          color = trading.aurumGold.copy(alpha = 0.7f),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1.6f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
        drawLine(
          color = trading.aurumGold.copy(alpha = 0.5f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.3f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
      }
    }

    // Active indicator legend
    if (showEma9 || showEma20 || showEma50 || showSma200 || showBollinger || showVwap) {
      Row(
        modifier = Modifier.padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (showEma9) LegendDot(color = trading.bullishEmerald, label = "EMA9")
        if (showEma20) LegendDot(color = trading.ema20Amber, label = "EMA20")
        if (showEma50) LegendDot(color = trading.ema50Cyan, label = "EMA50")
        if (showSma200) LegendDot(color = trading.sma200Violet, label = "SMA200")
        if (showBollinger) LegendDot(color = trading.ema50Cyan.copy(alpha = 0.7f), label = "BB")
        if (showVwap) LegendDot(color = trading.vwapPink, label = "VWAP")
      }
    }

    // RSI (14) Sub-Chart
    if (showRsi && vRsi.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      Text("RSI (14)", style = TabularMonoStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(58.dp)
      ) {
        val y70 = size.height * (1f - 70f / 100f)
        val y30 = size.height * (1f - 30f / 100f)
        drawLine(trading.bearishCrimson.copy(alpha = 0.45f), Offset(0f, y70), Offset(size.width, y70), 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
        drawLine(trading.bullishEmerald.copy(alpha = 0.45f), Offset(0f, y30), Offset(size.width, y30), 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
        val slotW = size.width / vRsi.size
        val rsiPath = Path()
        vRsi.forEachIndexed { idx, r ->
          val x = idx * slotW + slotW / 2f
          val y = size.height * (1f - (r.toFloat().coerceIn(0f, 100f) / 100f))
          if (idx == 0) rsiPath.moveTo(x, y) else rsiPath.lineTo(x, y)
        }
        drawPath(rsiPath, color = trading.aurumGold, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
      }
    }

    // MACD (12, 26, 9) Sub-Chart
    if (showMacd && vMacd.size == visibleCount && vSig.size == visibleCount && vHist.size == visibleCount) {
      Spacer(Modifier.height(8.dp))
      Text("MACD (12, 26, 9)", style = TabularMonoStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(66.dp)
      ) {
        val maxAbs = max(
          0.1,
          max(vMacd.maxOf { abs(it) }, max(vSig.maxOf { abs(it) }, vHist.maxOf { abs(it) }))
        )
        val midY = size.height / 2f
        fun valToY(v: Double): Float = midY - ((v / maxAbs).toFloat() * (size.height * 0.42f))
        drawLine(trading.chartGrid, Offset(0f, midY), Offset(size.width, midY), 1.2f)

        val slotW = size.width / visibleCount
        val barW = max(2f, slotW * 0.52f)
        vHist.forEachIndexed { idx, h ->
          val cx = idx * slotW + slotW / 2f
          val y = valToY(h)
          val top = min(midY, y)
          val bh = max(1.5f, abs(y - midY))
          drawRect(
            color = if (h >= 0) trading.bullishEmerald.copy(alpha = 0.65f) else trading.bearishCrimson.copy(alpha = 0.65f),
            topLeft = Offset(cx - barW / 2f, top),
            size = Size(barW, bh)
          )
        }

        val macdP = Path()
        val sigP = Path()
        vMacd.forEachIndexed { idx, m ->
          val cx = idx * slotW + slotW / 2f
          if (idx == 0) {
            macdP.moveTo(cx, valToY(m))
            sigP.moveTo(cx, valToY(vSig[idx]))
          } else {
            macdP.lineTo(cx, valToY(m))
            sigP.lineTo(cx, valToY(vSig[idx]))
          }
        }
        drawPath(macdP, color = trading.ema50Cyan, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
        drawPath(sigP, color = trading.ema20Amber, style = Stroke(width = 2.0f, cap = StrokeCap.Round))
      }
    }
  }
}

@Composable
fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(color)
    )
    Spacer(Modifier.width(5.dp))
    Text(
      text = label,
      style = TabularMonoStyle.copy(
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
  }
}

@Composable
fun CompoundingChart(
  monthlyAmount: Double,
  annualReturnPct: Double,
  years: Int = 10,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val points = remember(monthlyAmount, annualReturnPct, years) {
    val r = (annualReturnPct / 100.0) / 12.0
    (1..years).map { yr ->
      val months = yr * 12
      val invested = monthlyAmount * months
      val futureValue = if (r > 0) {
        monthlyAmount * ((Math.pow(1.0 + r, months.toDouble()) - 1.0) / r) * (1.0 + r)
      } else invested
      Triple(yr, invested, futureValue)
    }
  }

  Column(modifier = modifier) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp)
    ) {
      val maxVal = max(1.0, points.maxOf { it.third })
      val padY = size.height * 0.08f
      val drawH = size.height - padY * 2f
      val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width

      val wealthPath = Path()
      val wealthFill = Path()
      val investedPath = Path()

      points.forEachIndexed { i, (_, inv, fv) ->
        val x = i * stepX
        val yFv = padY + drawH * (1f - (fv / maxVal).toFloat())
        val yInv = padY + drawH * (1f - (inv / maxVal).toFloat())
        if (i == 0) {
          wealthPath.moveTo(x, yFv)
          wealthFill.moveTo(x, size.height)
          wealthFill.lineTo(x, yFv)
          investedPath.moveTo(x, yInv)
        } else {
          wealthPath.lineTo(x, yFv)
          wealthFill.lineTo(x, yFv)
          investedPath.lineTo(x, yInv)
        }
      }
      wealthFill.lineTo((points.size - 1) * stepX, size.height)
      wealthFill.close()

      drawPath(
        wealthFill,
        brush = Brush.verticalGradient(
          colors = listOf(trading.aurumGold.copy(alpha = 0.30f), Color.Transparent)
        )
      )
      drawPath(
        investedPath,
        color = trading.ema50Cyan,
        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
      )
      drawPath(
        wealthPath,
        color = trading.aurumGold,
        style = Stroke(width = 4f, cap = StrokeCap.Round)
      )
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      LegendDot(color = trading.aurumGold, label = "Projected Value")
      LegendDot(color = trading.ema50Cyan, label = "Invested Capital")
    }
  }
}

@Composable
fun SectorDonutChart(
  sectorAllocations: List<Pair<String, Double>>,
  modifier: Modifier = Modifier
) {
  val palette = listOf(
    Color(0xFF00E59B),
    Color(0xFFF5C451),
    Color(0xFF38BDF8),
    Color(0xFFFF4D6A),
    Color(0xFFA78BFA),
    Color(0xFFFB923C)
  )
  val total = sectorAllocations.sumOf { it.second }.coerceAtLeast(1.0)

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Box(
      modifier = Modifier.size(124.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeW = 24f
        var startAngle = -90f
        sectorAllocations.forEachIndexed { idx, (_, value) ->
          val sweep = ((value / total) * 360.0).toFloat()
          drawArc(
            color = palette[idx % palette.size],
            startAngle = startAngle,
            sweepAngle = max(2f, sweep - 2f),
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
          )
          startAngle += sweep
        }
      }
      Text(
        text = "${sectorAllocations.size}\nSectors",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
      sectorAllocations.take(6).forEachIndexed { idx, (sector, value) ->
        val pct = (value / total) * 100.0
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          LegendDot(color = palette[idx % palette.size], label = sector)
          Text(
            text = String.format(Locale.US, "%.1f%%", pct),
            style = TabularMonoStyle.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }
      }
    }
  }
}
