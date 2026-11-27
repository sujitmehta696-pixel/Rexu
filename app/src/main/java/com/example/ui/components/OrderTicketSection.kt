package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.aurumtrade.shared.market.SimulatedMarketEngine
import com.aurumtrade.shared.model.BrokerOrderKind
import com.aurumtrade.shared.model.JournalTag
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.OrderBasket
import com.aurumtrade.shared.model.OrderProductType
import com.aurumtrade.shared.model.Position
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.util.formatInr
import java.util.Locale
import kotlin.math.max

@Composable
fun OrderTicketSection(
  quote: LiveQuote,
  allQuotes: Map<String, LiveQuote>,
  openPos: Position?,
  baskets: List<OrderBasket>,
  isPro: Boolean,
  onPlaceOrder: (
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
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current

  var isBuySide by remember { mutableStateOf(true) }
  var productType by remember { mutableStateOf(OrderProductType.CNC) }
  var orderKind by remember { mutableStateOf(BrokerOrderKind.MARKET) }
  var qtyText by remember(quote.asset.symbol) { mutableStateOf("1") }
  var limitPriceText by remember(quote.asset.symbol) {
    mutableStateOf(String.format(Locale.US, "%.2f", quote.currentPrice))
  }
  var triggerPriceText by remember(quote.asset.symbol) {
    mutableStateOf(String.format(Locale.US, "%.2f", quote.currentPrice))
  }
  var leverage by remember { mutableLongStateOf(1L) }
  var useBracket by remember { mutableStateOf(false) }
  var stopLossText by remember(quote.asset.symbol) { mutableStateOf("") }
  var takeProfitText by remember(quote.asset.symbol) { mutableStateOf("") }
  var trailingDistText by remember(quote.asset.symbol) { mutableStateOf("") }
  var selectedTag by remember { mutableStateOf(JournalTag.FOLLOWED_PLAN) }

  var showDepth by remember { mutableStateOf(false) }
  var showBasketDialog by remember { mutableStateOf(false) }

  val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val execPrice = if (orderKind == BrokerOrderKind.MARKET) {
    quote.currentPrice
  } else {
    limitPriceText.toDoubleOrNull() ?: quote.currentPrice
  }
  val effectiveLev = if (isPro && productType == OrderProductType.MIS) leverage else 1L
  val notional = qty * execPrice
  val marginReq = notional / effectiveLev.toDouble()
  val isShortOpening = !isBuySide && (openPos == null || openPos.isShort)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.cardBorder, RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header + Holding Pill
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Broker Order Ticket", style = MaterialTheme.typography.titleLarge)
      if (openPos != null) {
        val sideLabel = if (openPos.isShort) "SHORT" else "LONG"
        Text(
          text = "$sideLabel ${String.format(Locale.US, "%.2f", openPos.quantity)}",
          style = TabularMonoStyle.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (openPos.isShort) trading.bearishCrimson else trading.bullishEmerald
          )
        )
      }
    }

    // Buy / Sell / Short-Sell Tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = { isBuySide = true },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isBuySide) trading.bullishEmerald else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (isBuySide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("buy_side_tab")
      ) {
        Text(if (openPos?.isShort == true) "BUY / COVER" else "BUY", fontWeight = FontWeight.Bold)
      }
      Button(
        onClick = {
          if (!isPro && (openPos == null || openPos.isShort)) {
            onLockedFeatureClick(ProFeature.ADVANCED_ORDERS)
          } else {
            isBuySide = false
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (!isBuySide) trading.bearishCrimson else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (!isBuySide) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("sell_side_tab")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(if (openPos != null && !openPos.isShort) "SELL" else "SHORT SELL", fontWeight = FontWeight.Bold)
          if (!isPro && (openPos == null || openPos.isShort)) {
            Spacer(Modifier.width(4.dp))
            ProLockBadge(label = "PRO")
          }
        }
      }
    }

    // Product Type: CNC (Free) vs MIS Intraday 5x (PRO)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OrderProductType.entries.forEach { pt ->
        FilterChip(
          selected = productType == pt,
          onClick = {
            if (pt.isProOnly && !isPro) {
              onLockedFeatureClick(ProFeature.ADVANCED_ORDERS)
            } else {
              productType = pt
              leverage = if (pt == OrderProductType.CNC) 1L else max(2L, leverage)
            }
          },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(pt.label)
              if (pt.isProOnly && !isPro) {
                Spacer(Modifier.width(6.dp))
                ProLockBadge()
              }
            }
          }
        )
      }
    }

    // Order Type: Market (Free), Limit / SL / SL-M / GTT (PRO)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      BrokerOrderKind.entries.forEach { kind ->
        FilterChip(
          selected = orderKind == kind,
          onClick = {
            if (kind.isProOnly && !isPro) {
              onLockedFeatureClick(ProFeature.ADVANCED_ORDERS)
            } else {
              orderKind = kind
            }
          },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(kind.label)
              if (kind.isProOnly && !isPro) {
                Spacer(Modifier.width(5.dp))
                ProLockBadge()
              }
            }
          }
        )
      }
    }

    // Quantity + Preset Chips
    OutlinedTextField(
      value = qtyText,
      onValueChange = { qtyText = it },
      label = { Text("Quantity (${quote.asset.unitLabel})") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("order_quantity_input")
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("1", "5", "10", "25", "50").forEach { preset ->
        FilterChip(
          selected = qtyText == preset,
          onClick = { qtyText = preset },
          label = { Text(preset) }
        )
      }
    }

    // Limit / Trigger Price Inputs for Pending / GTT Orders
    if (orderKind != BrokerOrderKind.MARKET && isPro) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (orderKind == BrokerOrderKind.LIMIT || orderKind == BrokerOrderKind.GTT || orderKind == BrokerOrderKind.SL) {
          OutlinedTextField(
            value = limitPriceText,
            onValueChange = { limitPriceText = it },
            label = { Text(if (orderKind == BrokerOrderKind.GTT) "GTT Target ₹" else "Limit Price ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        if (orderKind == BrokerOrderKind.SL || orderKind == BrokerOrderKind.SL_M) {
          OutlinedTextField(
            value = triggerPriceText,
            onValueChange = { triggerPriceText = it },
            label = { Text("Trigger Price ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Leverage Multiplier when MIS is active
    if (productType == OrderProductType.MIS && isPro) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("MIS Leverage", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(2L, 3L, 5L).forEach { lev ->
            FilterChip(
              selected = leverage == lev,
              onClick = { leverage = lev },
              label = { Text("${lev}x") }
            )
          }
        }
      }
    }

    // Bracket Orders + Trailing Stop-Loss (always visible, PRO-locked for Free)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          if (!isPro) onLockedFeatureClick(ProFeature.ADVANCED_ORDERS) else useBracket = !useBracket
        },
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Bracket + Trailing SL", style = MaterialTheme.typography.titleMedium)
        if (!isPro) {
          Spacer(Modifier.width(6.dp))
          ProLockBadge()
        }
      }
      Switch(
        checked = useBracket && isPro,
        onCheckedChange = { checked ->
          if (isPro) useBracket = checked else onLockedFeatureClick(ProFeature.ADVANCED_ORDERS)
        }
      )
    }

    if (useBracket && isPro) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = stopLossText,
          onValueChange = { stopLossText = it },
          label = { Text("SL ₹") },
          placeholder = { Text(String.format(Locale.US, "%.1f", execPrice * 0.97)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = takeProfitText,
          onValueChange = { takeProfitText = it },
          label = { Text("Target ₹") },
          placeholder = { Text(String.format(Locale.US, "%.1f", execPrice * 1.05)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = trailingDistText,
          onValueChange = { trailingDistText = it },
          label = { Text("Trail ₹") },
          placeholder = { Text("15") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(0.9f)
        )
      }
    }

    // Trade Psychology Tag Selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Journal Setup Tag", style = MaterialTheme.typography.labelLarge)
        if (!isPro) {
          Spacer(Modifier.width(6.dp))
          ProLockBadge()
        }
      }
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        JournalTag.entries.filter { it != JournalTag.NONE }.forEach { tag ->
          FilterChip(
            selected = isPro && selectedTag == tag,
            onClick = {
              if (!isPro) onLockedFeatureClick(ProFeature.MARKET_DEPTH_BASKETS)
              else selectedTag = tag
            },
            label = { Text(tag.label, fontSize = 11.sp) }
          )
        }
      }
    }

    // Margin Summary
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = if (isBuySide || isShortOpening) "Margin Required (${effectiveLev}x)" else "Estimated Proceeds",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = formatInr(if (isBuySide || isShortOpening) marginReq else notional),
        style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
      )
    }

    Button(
      onClick = {
        val sl = if (useBracket && isPro) stopLossText.toDoubleOrNull() ?: 0.0 else 0.0
        val tp = if (useBracket && isPro) takeProfitText.toDoubleOrNull() ?: 0.0 else 0.0
        val trail = if (useBracket && isPro) trailingDistText.toDoubleOrNull() ?: 0.0 else 0.0
        onPlaceOrder(
          quote.asset,
          if (isBuySide) "BUY" else "SELL",
          orderKind,
          productType,
          qty,
          limitPriceText.toDoubleOrNull() ?: quote.currentPrice,
          triggerPriceText.toDoubleOrNull() ?: quote.currentPrice,
          effectiveLev,
          sl,
          tp,
          trail,
          selectedTag
        )
      },
      enabled = qty > 0.0,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isBuySide) trading.bullishEmerald else trading.bearishCrimson
      ),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .testTag("submit_order_button")
    ) {
      val actionText = when {
        orderKind != BrokerOrderKind.MARKET -> "Place ${orderKind.label} ${if (isBuySide) "BUY" else "SELL"}"
        isBuySide && openPos?.isShort == true -> "Cover Short (${quote.asset.symbol})"
        isBuySide -> "Buy ${quote.asset.symbol} (${effectiveLev}x)"
        openPos != null && !openPos.isShort -> "Sell ${quote.asset.symbol}"
        else -> "Short Sell ${quote.asset.symbol} (${effectiveLev}x)"
      }
      Text(
        text = actionText,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    HorizontalDivider(color = trading.cardBorder)

    // 5-Level Market Depth Ladder + 1-Tap Basket Orders Trigger
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = {
          if (!isPro) onLockedFeatureClick(ProFeature.MARKET_DEPTH_BASKETS)
          else showDepth = !showDepth
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("market_depth_toggle")
      ) {
        Icon(
          imageVector = if (showDepth) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = "Market Depth",
          modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text("5-Lvl Depth", fontSize = 12.sp)
        if (!isPro) {
          Spacer(Modifier.width(4.dp))
          ProLockBadge(label = "PRO")
        }
      }

      OutlinedButton(
        onClick = {
          if (!isPro) onLockedFeatureClick(ProFeature.MARKET_DEPTH_BASKETS)
          else showBasketDialog = true
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("basket_orders_button")
      ) {
        Icon(Icons.Rounded.Layers, contentDescription = "Basket Orders", modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Baskets (${baskets.size})", fontSize = 12.sp)
        if (!isPro) {
          Spacer(Modifier.width(4.dp))
          ProLockBadge(label = "PRO")
        }
      }
    }

    if (showDepth && isPro) {
      MarketDepthLadder(quote = quote)
    }
  }

  if (showBasketDialog && isPro) {
    BasketOrdersDialog(
      currentSymbol = quote.asset.symbol,
      allQuotes = allQuotes,
      baskets = baskets,
      onSaveBasket = onSaveBasket,
      onExecuteBasket = onExecuteBasket,
      onDeleteBasket = onDeleteBasket,
      onDismiss = { showBasketDialog = false }
    )
  }
}

@Composable
private fun MarketDepthLadder(quote: LiveQuote) {
  val trading = LocalAurumTradingColors.current
  val depth = remember(quote.currentPrice, quote.asset.symbol) {
    SimulatedMarketEngine.generateMarketDepth(quote)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("BID QTY", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bullishEmerald))
      Text("BID", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bullishEmerald))
      Text("ASK", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bearishCrimson))
      Text("ASK QTY", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bearishCrimson))
    }
    for (i in 0 until 5) {
      val b = depth.bids[i]
      val a = depth.asks[i]
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${b.quantity}", style = TabularMonoStyle.copy(fontSize = 11.sp))
        Text(formatInr(b.price), style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.bullishEmerald, fontWeight = FontWeight.Bold))
        Text(formatInr(a.price), style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.bearishCrimson, fontWeight = FontWeight.Bold))
        Text("${a.quantity}", style = TabularMonoStyle.copy(fontSize = 11.sp))
      }
    }
    HorizontalDivider(color = trading.cardBorder)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Total Bid: ${depth.totalBidQty}", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bullishEmerald))
      Text("Total Ask: ${depth.totalAskQty}", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bearishCrimson))
    }
  }
}

@Composable
private fun BasketOrdersDialog(
  currentSymbol: String,
  allQuotes: Map<String, LiveQuote>,
  baskets: List<OrderBasket>,
  onSaveBasket: (String, List<String>) -> Unit,
  onExecuteBasket: (OrderBasket) -> Unit,
  onDeleteBasket: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  var basketName by remember { mutableStateOf("Nifty Leaders Basket") }
  var selectedSymbols by remember {
    mutableStateOf(
      listOf(currentSymbol, "RELIANCE", "TCS", "HDFCBANK").distinct().take(3)
    )
  }
  var legQty by remember { mutableStateOf("2") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("1-Tap Basket Orders", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = basketName,
          onValueChange = { basketName = it },
          label = { Text("New Basket Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = legQty,
          onValueChange = { legQty = it },
          label = { Text("Qty per Leg") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Text("Select 1–5 Stocks:", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          allQuotes.keys.take(10).forEach { sym ->
            val picked = sym in selectedSymbols
            FilterChip(
              selected = picked,
              onClick = {
                selectedSymbols = if (picked && selectedSymbols.size > 1) {
                  selectedSymbols - sym
                } else if (!picked && selectedSymbols.size < 5) {
                  selectedSymbols + sym
                } else selectedSymbols
              },
              label = { Text(sym, fontSize = 11.sp) }
            )
          }
        }
        Button(
          onClick = {
            val q = legQty.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val encoded = selectedSymbols.map { "$it|BUY|$q|1" }
            onSaveBasket(basketName, encoded)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Save ${selectedSymbols.size}-Stock Basket")
        }

        if (baskets.isNotEmpty()) {
          HorizontalDivider()
          Text("Saved Baskets", style = MaterialTheme.typography.labelLarge)
          baskets.take(4).forEach { basket ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(basket.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${basket.items.size} orders", style = TabularMonoStyle.copy(fontSize = 10.sp))
              }
              Row {
                IconButton(onClick = {
                  onExecuteBasket(basket)
                  onDismiss()
                }) {
                  Icon(Icons.Rounded.PlayArrow, contentDescription = "Execute Basket", tint = trading.bullishEmerald)
                }
                IconButton(onClick = { onDeleteBasket(basket.id) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Basket", tint = trading.bearishCrimson)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      OutlinedButton(onClick = onDismiss) { Text("Close") }
    }
  )
}
