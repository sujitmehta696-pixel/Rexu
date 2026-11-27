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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.market.OptionStrikeRow
import com.aurumtrade.shared.market.SimulatedMarketEngine
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.util.formatInr
import kotlin.math.max

private data class SelectedOptionContract(
  val symbol: String,
  val label: String,
  val ltp: Double,
  val iv: Double,
  val delta: Double,
  val theta: Double,
  val lotSize: Int
)

@Composable
fun OptionChainPanel(
  quotes: Map<String, LiveQuote>,
  isPro: Boolean,
  onPlaceOptionOrder: (symbol: String, name: String, ltp: Double, lots: Long, lotSize: Int, side: String) -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  var selectedUnderlying by remember { mutableStateOf("NIFTY50") }
  var expiryLabel by remember { mutableStateOf("Weekly") }
  var selectedContract by remember { mutableStateOf<SelectedOptionContract?>(null) }

  val spotQuote = quotes[selectedUnderlying]
  val spotPrice = spotQuote?.currentPrice ?: if (selectedUnderlying == "BANKNIFTY") 52180.0 else 24368.0
  val lotSize = if (selectedUnderlying == "BANKNIFTY") 15 else 25

  val chain = remember(spotPrice, selectedUnderlying, expiryLabel) {
    SimulatedMarketEngine.generateOptionChain(spotPrice, selectedUnderlying, expiryLabel)
  }
  val maxOi = remember(chain) {
    max(1, chain.maxOfOrNull { max(it.callOi, it.putOi) } ?: 100)
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Header & Underlying Selector
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("F&O Option Chain", style = MaterialTheme.typography.titleLarge)
        if (!isPro) {
          Spacer(Modifier.width(8.dp))
          ProLockBadge()
        }
      }
      Text(
        text = "Spot ${formatInr(spotPrice)}",
        style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = trading.aurumGold)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("NIFTY50", "BANKNIFTY").forEach { sym ->
          FilterChip(
            selected = selectedUnderlying == sym,
            onClick = { selectedUnderlying = sym },
            label = { Text(sym) }
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Weekly", "Monthly").forEach { exp ->
          FilterChip(
            selected = expiryLabel == exp,
            onClick = { expiryLabel = exp },
            label = { Text(exp) }
          )
        }
      }
    }

    // Table Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("CALL LTP / IV", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bullishEmerald), modifier = Modifier.weight(1.2f))
      Text("STRIKE", style = TabularMonoStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
      Text("PUT LTP / IV", style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.bearishCrimson), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
    }

    // Strike Rows
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
    ) {
      chain.forEachIndexed { idx, row ->
        OptionStrikeRowItem(
          row = row,
          maxOi = maxOi,
          onSelectCall = {
            if (!isPro) {
              onLockedFeatureClick(ProFeature.OPTION_CHAIN)
            } else {
              selectedContract = SelectedOptionContract(
                symbol = row.callSymbol,
                label = "$selectedUnderlying ${row.strike} CE ($expiryLabel)",
                ltp = row.callLtp,
                iv = row.callIv,
                delta = row.callDelta,
                theta = row.callTheta,
                lotSize = lotSize
              )
            }
          },
          onSelectPut = {
            if (!isPro) {
              onLockedFeatureClick(ProFeature.OPTION_CHAIN)
            } else {
              selectedContract = SelectedOptionContract(
                symbol = row.putSymbol,
                label = "$selectedUnderlying ${row.strike} PE ($expiryLabel)",
                ltp = row.putLtp,
                iv = row.putIv,
                delta = row.putDelta,
                theta = row.putTheta,
                lotSize = lotSize
              )
            }
          }
        )
        if (idx < chain.lastIndex) {
          HorizontalDivider(color = trading.cardBorder.copy(alpha = 0.5f))
        }
      }
    }
  }

  selectedContract?.let { contract ->
    var lots by remember(contract.symbol) { mutableLongStateOf(1L) }
    var isBuy by remember(contract.symbol) { mutableStateOf(true) }
    AlertDialog(
      onDismissRequest = { selectedContract = null },
      title = { Text(contract.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "LTP ${formatInr(contract.ltp)} • IV ${contract.iv}% • Δ ${contract.delta} • Θ ${contract.theta}",
            style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.aurumGold)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isBuy, onClick = { isBuy = true }, label = { Text("BUY CE/PE") })
            FilterChip(selected = !isBuy, onClick = { isBuy = false }, label = { Text("SELL / WRITE") })
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1L, 2L, 5L, 10L).forEach { l ->
              FilterChip(selected = lots == l, onClick = { lots = l }, label = { Text("${l} Lot") })
            }
          }
          Text(
            text = "Est. Value (${lots * contract.lotSize} qty): ${formatInr(lots * contract.lotSize * contract.ltp)}",
            style = TabularMonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onPlaceOptionOrder(
              contract.symbol,
              contract.label,
              contract.ltp,
              lots,
              contract.lotSize,
              if (isBuy) "BUY" else "SELL"
            )
            selectedContract = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isBuy) trading.bullishEmerald else trading.bearishCrimson
          ),
          modifier = Modifier.testTag("confirm_option_order")
        ) {
          Text(if (isBuy) "Buy Option" else "Sell Option")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { selectedContract = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun OptionStrikeRowItem(
  row: OptionStrikeRow,
  maxOi: Int,
  onSelectCall: () -> Unit,
  onSelectPut: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val rowBg = if (row.isAtm) trading.aurumGoldSoft.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(rowBg)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // CALL side
    Column(
      modifier = Modifier
        .weight(1.2f)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onSelectCall)
        .padding(4.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = formatInr(row.callLtp),
          style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = trading.bullishEmerald)
        )
        Text(
          text = "OI ${row.callOi}L",
          style = TabularMonoStyle.copy(fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
      Spacer(Modifier.height(3.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth((row.callOi.toFloat() / maxOi.toFloat()).coerceIn(0.1f, 1f))
          .height(3.dp)
          .background(trading.bullishEmerald.copy(alpha = 0.65f))
      )
      Text(
        text = "IV ${row.callIv}% • Δ ${row.callDelta}",
        style = TabularMonoStyle.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }

    // Center Strike
    Column(
      modifier = Modifier.weight(0.9f),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "${row.strike}",
        style = TabularMonoStyle.copy(
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (row.isAtm) trading.aurumGold else MaterialTheme.colorScheme.onSurface
        )
      )
      if (row.isAtm) {
        Text(
          text = "ATM",
          style = TabularMonoStyle.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = trading.aurumGold)
        )
      }
    }

    // PUT side
    Column(
      modifier = Modifier
        .weight(1.2f)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onSelectPut)
        .padding(4.dp),
      horizontalAlignment = Alignment.End
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = "OI ${row.putOi}L",
          style = TabularMonoStyle.copy(fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
          text = formatInr(row.putLtp),
          style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = trading.bearishCrimson)
        )
      }
      Spacer(Modifier.height(3.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth((row.putOi.toFloat() / maxOi.toFloat()).coerceIn(0.1f, 1f))
          .height(3.dp)
          .background(trading.bearishCrimson.copy(alpha = 0.65f))
      )
      Text(
        text = "IV ${row.putIv}% • Δ ${row.putDelta}",
        style = TabularMonoStyle.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }
  }
}
