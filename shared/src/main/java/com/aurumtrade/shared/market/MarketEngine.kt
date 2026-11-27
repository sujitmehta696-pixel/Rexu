package com.aurumtrade.shared.market

import com.aurumtrade.shared.model.AssetCategory
import com.aurumtrade.shared.model.ChartTimeframe
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

data class Candle(
  val timestampMillis: Long,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Long
)

data class DepthLevel(
  val price: Double,
  val quantity: Int
)

data class MarketDepth(
  val bids: List<DepthLevel>,
  val asks: List<DepthLevel>,
  val totalBidQty: Int,
  val totalAskQty: Int
)

data class OptionStrikeRow(
  val strike: Int,
  val isAtm: Boolean,
  val callSymbol: String,
  val putSymbol: String,
  val callLtp: Double,
  val putLtp: Double,
  val callOi: Int,
  val putOi: Int,
  val callIv: Double,
  val putIv: Double,
  val callDelta: Double,
  val putDelta: Double,
  val callTheta: Double,
  val putTheta: Double
)

enum class SignalType(val label: String) {
  RSI_OVERSOLD("RSI Oversold"),
  GOLDEN_CROSS("Golden Cross"),
  HIGH_BREAKOUT("52W High Breakout"),
  VWAP_RECLAIM("VWAP Bullish")
}

data class TechnicalSignalMatch(
  val symbol: String,
  val name: String,
  val currentPrice: Double,
  val changePct: Double,
  val signalType: SignalType,
  val detail: String
)

data class LiveQuote(
  val asset: MarketAsset,
  val currentPrice: Double,
  val previousClose: Double,
  val dayHigh: Double,
  val dayLow: Double,
  val week52High: Double,
  val totalVolume: Long,
  val candlesByTimeframe: Map<ChartTimeframe, List<Candle>>,
  val ema9ByTimeframe: Map<ChartTimeframe, List<Double>>,
  val ema20ByTimeframe: Map<ChartTimeframe, List<Double>>,
  val ema50ByTimeframe: Map<ChartTimeframe, List<Double>>,
  val sma200ByTimeframe: Map<ChartTimeframe, List<Double>>,
  val bbUpperByTimeframe: Map<ChartTimeframe, List<Double>>,
  val bbLowerByTimeframe: Map<ChartTimeframe, List<Double>>,
  val vwapByTimeframe: Map<ChartTimeframe, List<Double>>,
  val rsi14ByTimeframe: Map<ChartTimeframe, List<Double>>,
  val macdLineByTimeframe: Map<ChartTimeframe, List<Double>>,
  val macdSignalByTimeframe: Map<ChartTimeframe, List<Double>>,
  val macdHistByTimeframe: Map<ChartTimeframe, List<Double>>
) {
  val change: Double get() = currentPrice - previousClose
  val changePct: Double get() = if (previousClose > 0.0) ((currentPrice - previousClose) / previousClose) * 100.0 else 0.0
}

object DefaultMarketCatalog {
  val defaultAssets: List<MarketAsset> = listOf(
    MarketAsset(id = "RELIANCE", symbol = "RELIANCE", name = "Reliance Industries", category = AssetCategory.STOCK.name, sector = "Energy", basePrice = 2948.50, volatility = 0.012, unitLabel = "1 Share"),
    MarketAsset(id = "TCS", symbol = "TCS", name = "Tata Consultancy", category = AssetCategory.STOCK.name, sector = "IT", basePrice = 4082.25, volatility = 0.010, unitLabel = "1 Share"),
    MarketAsset(id = "HDFCBANK", symbol = "HDFCBANK", name = "HDFC Bank Ltd", category = AssetCategory.STOCK.name, sector = "Banking", basePrice = 1712.40, volatility = 0.011, unitLabel = "1 Share"),
    MarketAsset(id = "INFY", symbol = "INFY", name = "Infosys Ltd", category = AssetCategory.STOCK.name, sector = "IT", basePrice = 1864.80, volatility = 0.013, unitLabel = "1 Share"),
    MarketAsset(id = "ICICIBANK", symbol = "ICICIBANK", name = "ICICI Bank Ltd", category = AssetCategory.STOCK.name, sector = "Banking", basePrice = 1286.10, volatility = 0.012, unitLabel = "1 Share"),
    MarketAsset(id = "TATAMOTORS", symbol = "TATAMOTORS", name = "Tata Motors Ltd", category = AssetCategory.STOCK.name, sector = "Auto", basePrice = 934.60, volatility = 0.019, unitLabel = "1 Share"),
    MarketAsset(id = "MARUTI", symbol = "MARUTI", name = "Maruti Suzuki India", category = AssetCategory.STOCK.name, sector = "Auto", basePrice = 12480.00, volatility = 0.013, unitLabel = "1 Share"),
    MarketAsset(id = "SBIN", symbol = "SBIN", name = "State Bank of India", category = AssetCategory.STOCK.name, sector = "Banking", basePrice = 824.30, volatility = 0.015, unitLabel = "1 Share"),
    MarketAsset(id = "SUNPHARMA", symbol = "SUNPHARMA", name = "Sun Pharmaceutical", category = AssetCategory.STOCK.name, sector = "Pharma", basePrice = 1842.75, volatility = 0.013, unitLabel = "1 Share"),
    MarketAsset(id = "DRREDDY", symbol = "DRREDDY", name = "Dr. Reddy's Labs", category = AssetCategory.STOCK.name, sector = "Pharma", basePrice = 1295.40, volatility = 0.012, unitLabel = "1 Share"),
    MarketAsset(id = "ONGC", symbol = "ONGC", name = "Oil & Natural Gas Corp", category = AssetCategory.STOCK.name, sector = "Energy", basePrice = 278.60, volatility = 0.016, unitLabel = "1 Share"),
    MarketAsset(id = "BHARTIARTL", symbol = "BHARTIARTL", name = "Bharti Airtel Ltd", category = AssetCategory.STOCK.name, sector = "Telecom", basePrice = 1618.90, volatility = 0.013, unitLabel = "1 Share"),
    MarketAsset(id = "BAJFINANCE", symbol = "BAJFINANCE", name = "Bajaj Finance Ltd", category = AssetCategory.STOCK.name, sector = "Banking", basePrice = 7142.00, volatility = 0.017, unitLabel = "1 Share"),
    MarketAsset(id = "ITC", symbol = "ITC", name = "ITC Limited", category = AssetCategory.STOCK.name, sector = "FMCG", basePrice = 492.45, volatility = 0.008, unitLabel = "1 Share"),
    MarketAsset(id = "HINDUNILVR", symbol = "HINDUNILVR", name = "Hindustan Unilever", category = AssetCategory.STOCK.name, sector = "FMCG", basePrice = 2514.20, volatility = 0.009, unitLabel = "1 Share"),
    MarketAsset(id = "NIFTY50", symbol = "NIFTY50", name = "NIFTY 50 Index", category = AssetCategory.INDEX.name, sector = "Index", basePrice = 24368.40, volatility = 0.007, unitLabel = "1 Lot"),
    MarketAsset(id = "BANKNIFTY", symbol = "BANKNIFTY", name = "NIFTY Bank Index", category = AssetCategory.INDEX.name, sector = "Index", basePrice = 52180.75, volatility = 0.009, unitLabel = "1 Lot"),
    MarketAsset(id = "GOLD_24K", symbol = "GOLD_24K", name = "24K Digital Gold (1g)", category = AssetCategory.GOLD.name, sector = "Commodities", basePrice = 7785.00, volatility = 0.006, unitLabel = "1 Gram"),
    MarketAsset(id = "SILVER_999", symbol = "SILVER_999", name = "999 Pure Silver (10g)", category = AssetCategory.COMMODITY.name, sector = "Commodities", basePrice = 942.50, volatility = 0.009, unitLabel = "10 Grams"),
    MarketAsset(id = "CRUDEOIL", symbol = "CRUDEOIL", name = "MCX Crude Oil Future", category = AssetCategory.COMMODITY.name, sector = "Commodities", basePrice = 6142.00, volatility = 0.018, unitLabel = "1 Barrel"),
    MarketAsset(id = "NIFTY_INDEX_MF", symbol = "NIFTY_INDEX_MF", name = "Nifty 50 Direct Growth MF", category = AssetCategory.MUTUAL_FUND.name, sector = "Index Fund", basePrice = 214.80, volatility = 0.004, unitLabel = "1 Unit"),
    MarketAsset(id = "GOLD_ETF_MF", symbol = "GOLD_ETF_MF", name = "Aurum Sovereign Gold SIP", category = AssetCategory.MUTUAL_FUND.name, sector = "Gold Fund", basePrice = 68.45, volatility = 0.003, unitLabel = "1 Unit")
  )

  val defaultSubscriptionPlans: List<SubscriptionPlan> = listOf(
    SubscriptionPlan(
      id = "pro_monthly",
      name = "Pro Monthly",
      priceInr = 199.0,
      validityDays = 30L,
      discountBadge = "POPULAR",
      tagline = "Candlesticks, Indicators, F&O Chain, 5x MIS & Ad-Free",
      active = true,
      sortOrder = 1L
    ),
    SubscriptionPlan(
      id = "pro_quarterly",
      name = "Pro Quarterly",
      priceInr = 499.0,
      validityDays = 90L,
      discountBadge = "SAVE 16%",
      tagline = "Full Pro Terminal + Auto-SIP + P&L Heatmap + Midnight Gold",
      active = true,
      sortOrder = 2L
    ),
    SubscriptionPlan(
      id = "pro_annual",
      name = "Pro Annual",
      priceInr = 1499.0,
      validityDays = 365L,
      discountBadge = "BEST VALUE 37% OFF",
      tagline = "12 Months Institutional Pro Terminal, F&O & Analytics",
      active = true,
      sortOrder = 3L
    )
  )
}

class SimulatedMarketEngine {
  private val _quotes = MutableStateFlow<Map<String, LiveQuote>>(emptyMap())
  val quotes: StateFlow<Map<String, LiveQuote>> = _quotes.asStateFlow()

  private var tickCounter = 0L

  init {
    setAssets(DefaultMarketCatalog.defaultAssets)
  }

  fun setAssets(assets: List<MarketAsset>) {
    val activeAssets = assets.filter { it.active }
    val targetList = if (activeAssets.isNotEmpty()) activeAssets else DefaultMarketCatalog.defaultAssets

    val currentMap = _quotes.value.toMutableMap()
    val updatedMap = linkedMapOf<String, LiveQuote>()
    val now = System.currentTimeMillis()

    targetList.forEach { asset ->
      val existing = currentMap[asset.symbol]
      if (existing != null) {
        updatedMap[asset.symbol] = existing.copy(asset = asset)
      } else {
        updatedMap[asset.symbol] = buildInitialQuote(asset, now)
      }
    }
    _quotes.value = updatedMap
  }

  fun stepMarketTick() {
    tickCounter++
    val now = System.currentTimeMillis()
    val current = _quotes.value
    if (current.isEmpty()) return

    val nextMap = linkedMapOf<String, LiveQuote>()
    current.forEach { (symbol, quote) ->
      val asset = quote.asset
      val rng = Random(now xor symbol.hashCode().toLong() xor tickCounter)
      val tickSigma = asset.volatility * 0.16
      val drift = (asset.basePrice - quote.currentPrice) / asset.basePrice * 0.03
      val shock = (rng.nextDouble() - 0.5) * 2.0 * tickSigma
      val newPrice = roundPrice(max(1.0, quote.currentPrice * (1.0 + drift + shock)))
      val deltaVol = rng.nextLong(60, 750)

      val updatedCandles = quote.candlesByTimeframe.toMutableMap()
      val updatedEma9 = quote.ema9ByTimeframe.toMutableMap()
      val updatedEma20 = quote.ema20ByTimeframe.toMutableMap()
      val updatedEma50 = quote.ema50ByTimeframe.toMutableMap()
      val updatedSma200 = quote.sma200ByTimeframe.toMutableMap()
      val updatedBbUpper = quote.bbUpperByTimeframe.toMutableMap()
      val updatedBbLower = quote.bbLowerByTimeframe.toMutableMap()
      val updatedVwap = quote.vwapByTimeframe.toMutableMap()
      val updatedRsi = quote.rsi14ByTimeframe.toMutableMap()
      val updatedMacdLine = quote.macdLineByTimeframe.toMutableMap()
      val updatedMacdSig = quote.macdSignalByTimeframe.toMutableMap()
      val updatedMacdHist = quote.macdHistByTimeframe.toMutableMap()

      ChartTimeframe.entries.forEach { tf ->
        val list = updatedCandles[tf]?.toMutableList() ?: mutableListOf()
        if (list.isNotEmpty()) {
          val last = list.last()
          val elapsed = now - last.timestampMillis
          if (elapsed >= tf.intervalMillis || (tf == ChartTimeframe.M1 && tickCounter % 12L == 0L)) {
            list.add(
              Candle(
                timestampMillis = now,
                open = last.close,
                high = max(last.close, newPrice),
                low = min(last.close, newPrice),
                close = newPrice,
                volume = rng.nextLong(800, 12500)
              )
            )
            if (list.size > 110) list.removeAt(0)
          } else {
            list[list.lastIndex] = last.copy(
              high = max(last.high, newPrice),
              low = min(last.low, newPrice),
              close = newPrice,
              volume = last.volume + deltaVol
            )
          }
          val closes = list.map { it.close }
          updatedCandles[tf] = list
          updatedEma9[tf] = computeEma(closes, 9)
          updatedEma20[tf] = computeEma(closes, 20)
          updatedEma50[tf] = computeEma(closes, 50)
          updatedSma200[tf] = computeSma(closes, 200)
          val (bbU, bbL) = computeBollinger(closes, 20, 2.0)
          updatedBbUpper[tf] = bbU
          updatedBbLower[tf] = bbL
          updatedVwap[tf] = computeVwap(list)
          updatedRsi[tf] = computeRsi(closes, 14)
          val macd = computeMacd(closes)
          updatedMacdLine[tf] = macd.first
          updatedMacdSig[tf] = macd.second
          updatedMacdHist[tf] = macd.third
        }
      }

      nextMap[symbol] = quote.copy(
        currentPrice = newPrice,
        dayHigh = max(quote.dayHigh, newPrice),
        dayLow = min(quote.dayLow, newPrice),
        week52High = max(quote.week52High, newPrice),
        totalVolume = quote.totalVolume + deltaVol,
        candlesByTimeframe = updatedCandles,
        ema9ByTimeframe = updatedEma9,
        ema20ByTimeframe = updatedEma20,
        ema50ByTimeframe = updatedEma50,
        sma200ByTimeframe = updatedSma200,
        bbUpperByTimeframe = updatedBbUpper,
        bbLowerByTimeframe = updatedBbLower,
        vwapByTimeframe = updatedVwap,
        rsi14ByTimeframe = updatedRsi,
        macdLineByTimeframe = updatedMacdLine,
        macdSignalByTimeframe = updatedMacdSig,
        macdHistByTimeframe = updatedMacdHist
      )
    }
    _quotes.value = nextMap
  }

  private fun buildInitialQuote(asset: MarketAsset, now: Long): LiveQuote {
    val seed = asset.symbol.hashCode().toLong() * 31L + 7919L
    val rng = Random(seed)
    val candlesMap = mutableMapOf<ChartTimeframe, List<Candle>>()
    val ema9Map = mutableMapOf<ChartTimeframe, List<Double>>()
    val ema20Map = mutableMapOf<ChartTimeframe, List<Double>>()
    val ema50Map = mutableMapOf<ChartTimeframe, List<Double>>()
    val sma200Map = mutableMapOf<ChartTimeframe, List<Double>>()
    val bbUpperMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val bbLowerMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val vwapMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val rsiMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val macdLineMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val macdSigMap = mutableMapOf<ChartTimeframe, List<Double>>()
    val macdHistMap = mutableMapOf<ChartTimeframe, List<Double>>()

    var anchorClose = asset.basePrice
    ChartTimeframe.entries.forEach { tf ->
      val tfScale = when (tf) {
        ChartTimeframe.M1 -> 0.25
        ChartTimeframe.M5 -> 0.45
        ChartTimeframe.M15 -> 0.70
        ChartTimeframe.H1 -> 1.05
        ChartTimeframe.D1 -> 1.35
        ChartTimeframe.W1 -> 1.90
      }
      val count = 84
      val candles = ArrayList<Candle>(count)
      var price = asset.basePrice * (0.96 + rng.nextDouble() * 0.06)
      for (i in 0 until count) {
        val ts = now - (count - 1 - i) * tf.intervalMillis
        val open = roundPrice(price)
        val bodyMove = (rng.nextDouble() - 0.482) * asset.volatility * tfScale * open
        val close = roundPrice(max(1.0, open + bodyMove))
        val wickUp = abs(rng.nextDouble()) * asset.volatility * tfScale * 0.65 * open
        val wickDown = abs(rng.nextDouble()) * asset.volatility * tfScale * 0.65 * open
        val high = roundPrice(max(open, close) + wickUp)
        val low = roundPrice(max(1.0, min(open, close) - wickDown))
        candles.add(
          Candle(
            timestampMillis = ts,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = rng.nextLong(1500, 56000)
          )
        )
        price = close
      }
      if (tf == ChartTimeframe.M1) {
        anchorClose = candles.last().close
      } else {
        val last = candles.last()
        candles[candles.lastIndex] = last.copy(
          close = anchorClose,
          high = max(last.high, anchorClose),
          low = min(last.low, anchorClose)
        )
      }
      val closes = candles.map { it.close }
      candlesMap[tf] = candles
      ema9Map[tf] = computeEma(closes, 9)
      ema20Map[tf] = computeEma(closes, 20)
      ema50Map[tf] = computeEma(closes, 50)
      sma200Map[tf] = computeSma(closes, 200)
      val (bbU, bbL) = computeBollinger(closes, 20, 2.0)
      bbUpperMap[tf] = bbU
      bbLowerMap[tf] = bbL
      vwapMap[tf] = computeVwap(candles)
      rsiMap[tf] = computeRsi(closes, 14)
      val macd = computeMacd(closes)
      macdLineMap[tf] = macd.first
      macdSigMap[tf] = macd.second
      macdHistMap[tf] = macd.third
    }

    val m15 = candlesMap[ChartTimeframe.M15].orEmpty()
    val d1 = candlesMap[ChartTimeframe.D1].orEmpty()
    val prevClose = if (m15.size > 12) m15[m15.size - 12].close else asset.basePrice
    val dayHigh = m15.maxOfOrNull { it.high } ?: anchorClose
    val dayLow = m15.minOfOrNull { it.low } ?: anchorClose
    val week52High = (d1.dropLast(1).maxOfOrNull { it.high } ?: anchorClose) * 1.01
    val totalVol = m15.takeLast(24).sumOf { it.volume }

    return LiveQuote(
      asset = asset,
      currentPrice = anchorClose,
      previousClose = prevClose,
      dayHigh = dayHigh,
      dayLow = dayLow,
      week52High = max(week52High, dayHigh),
      totalVolume = totalVol,
      candlesByTimeframe = candlesMap,
      ema9ByTimeframe = ema9Map,
      ema20ByTimeframe = ema20Map,
      ema50ByTimeframe = ema50Map,
      sma200ByTimeframe = sma200Map,
      bbUpperByTimeframe = bbUpperMap,
      bbLowerByTimeframe = bbLowerMap,
      vwapByTimeframe = vwapMap,
      rsi14ByTimeframe = rsiMap,
      macdLineByTimeframe = macdLineMap,
      macdSignalByTimeframe = macdSigMap,
      macdHistByTimeframe = macdHistMap
    )
  }

  private fun roundPrice(value: Double): Double = (value * 100.0).roundToLong() / 100.0

  companion object {
    fun toHeikinAshi(candles: List<Candle>): List<Candle> {
      if (candles.isEmpty()) return emptyList()
      val out = ArrayList<Candle>(candles.size)
      var prevHaOpen = (candles.first().open + candles.first().close) / 2.0
      var prevHaClose = (candles.first().open + candles.first().high + candles.first().low + candles.first().close) / 4.0
      candles.forEachIndexed { idx, c ->
        val haClose = (c.open + c.high + c.low + c.close) / 4.0
        val haOpen = if (idx == 0) prevHaOpen else (prevHaOpen + prevHaClose) / 2.0
        val haHigh = max(c.high, max(haOpen, haClose))
        val haLow = min(c.low, min(haOpen, haClose))
        out.add(
          Candle(
            timestampMillis = c.timestampMillis,
            open = haOpen,
            high = haHigh,
            low = haLow,
            close = haClose,
            volume = c.volume
          )
        )
        prevHaOpen = haOpen
        prevHaClose = haClose
      }
      return out
    }

    fun computeEma(values: List<Double>, period: Int): List<Double> {
      if (values.isEmpty()) return emptyList()
      val k = 2.0 / (period + 1.0)
      val out = ArrayList<Double>(values.size)
      var ema = values.first()
      out.add(ema)
      for (i in 1 until values.size) {
        ema = values[i] * k + ema * (1.0 - k)
        out.add(ema)
      }
      return out
    }

    fun computeSma(values: List<Double>, period: Int): List<Double> {
      if (values.isEmpty()) return emptyList()
      val out = ArrayList<Double>(values.size)
      var runningSum = 0.0
      for (i in values.indices) {
        runningSum += values[i]
        if (i >= period) {
          runningSum -= values[i - period]
        }
        val windowSize = min(i + 1, period)
        out.add(runningSum / windowSize.toDouble())
      }
      return out
    }

    fun computeBollinger(values: List<Double>, period: Int = 20, mult: Double = 2.0): Pair<List<Double>, List<Double>> {
      if (values.isEmpty()) return emptyList<Double>() to emptyList()
      val upper = ArrayList<Double>(values.size)
      val lower = ArrayList<Double>(values.size)
      for (i in values.indices) {
        val start = max(0, i - period + 1)
        val slice = values.subList(start, i + 1)
        val mean = slice.average()
        val variance = slice.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance)
        upper.add(mean + mult * std)
        lower.add(max(0.01, mean - mult * std))
      }
      return upper to lower
    }

    fun computeVwap(candles: List<Candle>): List<Double> {
      if (candles.isEmpty()) return emptyList()
      val out = ArrayList<Double>(candles.size)
      var cumPv = 0.0
      var cumVol = 0.0
      candles.forEach { c ->
        val typical = (c.high + c.low + c.close) / 3.0
        val vol = max(1L, c.volume).toDouble()
        cumPv += typical * vol
        cumVol += vol
        out.add(cumPv / cumVol)
      }
      return out
    }

    fun computeRsi(values: List<Double>, period: Int = 14): List<Double> {
      if (values.size < 2) return List(values.size) { 50.0 }
      val out = ArrayList<Double>(values.size)
      out.add(50.0)
      var avgGain = 0.0
      var avgLoss = 0.0
      for (i in 1 until values.size) {
        val diff = values[i] - values[i - 1]
        val gain = if (diff > 0) diff else 0.0
        val loss = if (diff < 0) -diff else 0.0
        if (i <= period) {
          avgGain += gain / period
          avgLoss += loss / period
        } else {
          avgGain = (avgGain * (period - 1) + gain) / period
          avgLoss = (avgLoss * (period - 1) + loss) / period
        }
        val rsi = when {
          avgLoss == 0.0 && avgGain == 0.0 -> 50.0
          avgLoss == 0.0 -> 100.0
          else -> 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }
        out.add(rsi)
      }
      return out
    }

    fun computeMacd(values: List<Double>): Triple<List<Double>, List<Double>, List<Double>> {
      if (values.isEmpty()) return Triple(emptyList(), emptyList(), emptyList())
      val ema12 = computeEma(values, 12)
      val ema26 = computeEma(values, 26)
      val macdLine = values.indices.map { ema12[it] - ema26[it] }
      val signalLine = computeEma(macdLine, 9)
      val hist = values.indices.map { macdLine[it] - signalLine[it] }
      return Triple(macdLine, signalLine, hist)
    }

    fun generateMarketDepth(quote: LiveQuote): MarketDepth {
      val price = quote.currentPrice
      val tickSize = when {
        price >= 10000.0 -> 1.0
        price >= 1000.0 -> 0.25
        else -> 0.05
      }
      val seed = quote.asset.symbol.hashCode().toLong() xor (price * 100.0).roundToLong()
      val rng = Random(seed)
      val bids = (1..5).map { level ->
        val p = ((price - level * tickSize) * 100.0).roundToLong() / 100.0
        DepthLevel(price = max(0.05, p), quantity = rng.nextInt(45, 1450))
      }
      val asks = (1..5).map { level ->
        val p = ((price + level * tickSize) * 100.0).roundToLong() / 100.0
        DepthLevel(price = p, quantity = rng.nextInt(45, 1450))
      }
      return MarketDepth(
        bids = bids,
        asks = asks,
        totalBidQty = bids.sumOf { it.quantity },
        totalAskQty = asks.sumOf { it.quantity }
      )
    }

    fun generateOptionChain(
      spotPrice: Double,
      underlyingSymbol: String,
      expiryLabel: String
    ): List<OptionStrikeRow> {
      val isBank = underlyingSymbol == "BANKNIFTY"
      val step = if (isBank) 100 else 50
      val atmStrike = ((spotPrice / step.toDouble()).roundToInt() * step)
      val dteFactor = if (expiryLabel.contains("Monthly", ignoreCase = true)) 1.65 else 1.0
      val baseIv = (if (isBank) 16.4 else 13.8) * (if (expiryLabel.contains("Monthly")) 1.08 else 1.0)
      val rng = Random(underlyingSymbol.hashCode().toLong() xor atmStrike.toLong() xor (spotPrice * 10).toLong())

      return (-4..4).map { offset ->
        val strike = atmStrike + offset * step
        val moneyness = (spotPrice - strike) / max(1.0, spotPrice)
        val absDist = abs(moneyness)
        val callIv = ((baseIv + absDist * 115.0 + rng.nextDouble(-0.4, 0.4)) * 10.0).roundToInt() / 10.0
        val putIv = ((baseIv + absDist * 125.0 + rng.nextDouble(-0.4, 0.4)) * 10.0).roundToInt() / 10.0

        val callIntrinsic = max(0.0, spotPrice - strike)
        val putIntrinsic = max(0.0, strike - spotPrice)
        val timeValue = spotPrice * 0.0052 * dteFactor * exp(-absDist * 28.0)

        val callLtp = max(2.5, ((callIntrinsic + timeValue) * 20.0).roundToInt() / 20.0)
        val putLtp = max(2.5, ((putIntrinsic + timeValue * 1.03) * 20.0).roundToInt() / 20.0)

        val rawCallDelta = 1.0 / (1.0 + exp(-moneyness * 42.0))
        val callDelta = (rawCallDelta.coerceIn(0.05, 0.95) * 100.0).roundToInt() / 100.0
        val putDelta = ((callDelta - 1.0) * 100.0).roundToInt() / 100.0

        val callTheta = -((timeValue * 0.085 / dteFactor) * 10.0).roundToInt() / 10.0
        val putTheta = -((timeValue * 0.088 / dteFactor) * 10.0).roundToInt() / 10.0

        val callOi = ((1.2 - abs(offset) * 0.14) * rng.nextDouble(45.0, 120.0)).roundToInt().coerceAtLeast(8)
        val putOi = ((1.2 - abs(offset) * 0.14) * rng.nextDouble(45.0, 120.0)).roundToInt().coerceAtLeast(8)
        val prefix = if (isBank) "BANKNIFTY" else "NIFTY"

        OptionStrikeRow(
          strike = strike,
          isAtm = strike == atmStrike,
          callSymbol = "${prefix}_${strike}CE",
          putSymbol = "${prefix}_${strike}PE",
          callLtp = callLtp,
          putLtp = putLtp,
          callOi = callOi,
          putOi = putOi,
          callIv = callIv,
          putIv = putIv,
          callDelta = callDelta,
          putDelta = putDelta,
          callTheta = callTheta,
          putTheta = putTheta
        )
      }
    }

    fun scanTechnicalSignals(quotes: Map<String, LiveQuote>): List<TechnicalSignalMatch> {
      val matches = mutableListOf<TechnicalSignalMatch>()
      quotes.values.forEach { q ->
        val rsi = q.rsi14ByTimeframe[ChartTimeframe.M15]?.lastOrNull() ?: 50.0
        val ema20 = q.ema20ByTimeframe[ChartTimeframe.M15]?.lastOrNull() ?: q.currentPrice
        val ema50 = q.ema50ByTimeframe[ChartTimeframe.M15]?.lastOrNull() ?: q.currentPrice
        val prevEma20 = q.ema20ByTimeframe[ChartTimeframe.M15]?.dropLast(1)?.lastOrNull() ?: ema20
        val prevEma50 = q.ema50ByTimeframe[ChartTimeframe.M15]?.dropLast(1)?.lastOrNull() ?: ema50
        val vwap = q.vwapByTimeframe[ChartTimeframe.M15]?.lastOrNull() ?: q.currentPrice

        if (rsi <= 38.0) {
          matches.add(
            TechnicalSignalMatch(
              symbol = q.asset.symbol,
              name = q.asset.name,
              currentPrice = q.currentPrice,
              changePct = q.changePct,
              signalType = SignalType.RSI_OVERSOLD,
              detail = "RSI(14) at ${String.format(java.util.Locale.US, "%.1f", rsi)} — Reversal Watch"
            )
          )
        }
        if ((ema20 > ema50 && prevEma20 <= prevEma50) || (ema20 > ema50 * 1.002 && q.changePct > 0.4)) {
          matches.add(
            TechnicalSignalMatch(
              symbol = q.asset.symbol,
              name = q.asset.name,
              currentPrice = q.currentPrice,
              changePct = q.changePct,
              signalType = SignalType.GOLDEN_CROSS,
              detail = "EMA20 crossed above EMA50 on 15m"
            )
          )
        }
        if (q.currentPrice >= q.week52High * 0.992) {
          matches.add(
            TechnicalSignalMatch(
              symbol = q.asset.symbol,
              name = q.asset.name,
              currentPrice = q.currentPrice,
              changePct = q.changePct,
              signalType = SignalType.HIGH_BREAKOUT,
              detail = "Trading within 0.8% of 52W High"
            )
          )
        }
        if (q.currentPrice > vwap && q.changePct > 0.25) {
          matches.add(
            TechnicalSignalMatch(
              symbol = q.asset.symbol,
              name = q.asset.name,
              currentPrice = q.currentPrice,
              changePct = q.changePct,
              signalType = SignalType.VWAP_RECLAIM,
              detail = "Price holding above session VWAP"
            )
          )
        }
      }
      return matches.distinctBy { "${it.symbol}_${it.signalType}" }
    }
  }
}
