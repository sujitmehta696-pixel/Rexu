package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.market.LiveQuote
import com.aurumtrade.shared.market.TechnicalSignalMatch
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.AssetCategory
import com.aurumtrade.shared.model.CustomWatchlist
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProCrownBadge
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.ui.SectorHeatmapGrid
import com.aurumtrade.shared.util.formatInr
import com.aurumtrade.shared.util.formatPct
import com.example.ui.AdMobBannerSlot
import com.example.ui.components.OptionChainPanel
import com.example.ui.components.ProFeature

private enum class MarketSubTab(val label: String) {
  QUOTES("Quotes"),
  SCREENER("Screener"),
  HEATMAP("Heatmap"),
  OPTIONS("F&O Chain")
}

private enum class MarketFilter(val label: String) {
  ALL("All"),
  WATCHLIST("Watchlist"),
  STOCK("NSE Stocks"),
  INDEX("Indices"),
  GOLD("Gold"),
  COMMODITY("Commodities"),
  MUTUAL_FUND("Mutual Funds")
}

private enum class ScreenerMode(val label: String) {
  GAINERS("Top Gainers"),
  LOSERS("Top Losers"),
  ACTIVE("Most Active"),
  SIGNALS("Signal Scanner")
}

@Composable
fun MarketScreen(
  profile: UserProfile?,
  quotes: Map<String, LiveQuote>,
  signals: List<TechnicalSignalMatch>,
  customWatchlists: List<CustomWatchlist>,
  appConfig: AppConfig,
  onSelectAsset: (String) -> Unit,
  onToggleWatchlist: (String) -> Unit,
  onSaveCustomWatchlist: (watchlistId: String?, name: String, symbols: List<String>, sortOrder: Long) -> Unit,
  onPlaceOptionOrder: (symbol: String, name: String, ltp: Double, lots: Long, lotSize: Int, side: String) -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit,
  onNavigateToPro: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val isPro = profile?.hasValidProAccess == true
  val primaryWatchlist = profile?.watchlist.orEmpty()

  var activeSubTab by remember { mutableStateOf(MarketSubTab.QUOTES) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(MarketFilter.ALL) }
  var activeCustomWatchlistIdx by remember { mutableIntStateOf(-1) } // -1 = primary watchlist
  var screenerMode by remember { mutableStateOf(ScreenerMode.GAINERS) }
  var showWatchlistDialog by remember { mutableStateOf(false) }

  val activeWatchlistSymbols = remember(activeCustomWatchlistIdx, primaryWatchlist, customWatchlists) {
    if (activeCustomWatchlistIdx < 0 || activeCustomWatchlistIdx >= customWatchlists.size) {
      primaryWatchlist
    } else {
      customWatchlists[activeCustomWatchlistIdx].symbols
    }
  }

  val filteredQuotes = remember(quotes, searchQuery, selectedFilter, activeWatchlistSymbols) {
    quotes.values.filter { q ->
      val matchesSearch = searchQuery.isBlank() ||
        q.asset.symbol.contains(searchQuery, ignoreCase = true) ||
        q.asset.name.contains(searchQuery, ignoreCase = true) ||
        q.asset.sector.contains(searchQuery, ignoreCase = true)
      val matchesCat = when (selectedFilter) {
        MarketFilter.ALL -> true
        MarketFilter.WATCHLIST -> q.asset.symbol in activeWatchlistSymbols
        MarketFilter.STOCK -> q.asset.category == AssetCategory.STOCK.name
        MarketFilter.INDEX -> q.asset.category == AssetCategory.INDEX.name
        MarketFilter.GOLD -> q.asset.category == AssetCategory.GOLD.name
        MarketFilter.COMMODITY -> q.asset.category == AssetCategory.COMMODITY.name
        MarketFilter.MUTUAL_FUND -> q.asset.category == AssetCategory.MUTUAL_FUND.name
      }
      matchesSearch && matchesCat
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Market Terminal", style = MaterialTheme.typography.headlineMedium)
          if (isPro) {
            Spacer(Modifier.width(8.dp))
            ProCrownBadge()
          }
        }
        Text(
          text = "Cash: ${formatInr(profile?.virtualCash ?: 0.0)} • Watchlists: ${1 + customWatchlists.size}/${if (isPro) "5" else "1 (5 cap)"}",
          style = TabularMonoStyle.copy(fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    // Live Index & Commodity Ribbon
    val ribbonSymbols = listOf("NIFTY50", "BANKNIFTY", "GOLD_24K", "CRUDEOIL")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      ribbonSymbols.mapNotNull { quotes[it] }.forEach { q ->
        val up = q.change >= 0
        val accent = if (up) trading.bullishEmerald else trading.bearishCrimson
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onSelectAsset(q.asset.symbol) }
            .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
          Text(q.asset.symbol, style = MaterialTheme.typography.labelSmall, color = trading.aurumGold)
          Text(formatInr(q.currentPrice), style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
          Text(formatPct(q.changePct), style = TabularMonoStyle.copy(fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold))
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    // Market Sub-Tabs: Quotes | Screener | Heatmap | F&O Chain
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MarketSubTab.entries.forEach { tab ->
        val showProTag = !isPro && (tab == MarketSubTab.OPTIONS || tab == MarketSubTab.SCREENER)
        FilterChip(
          selected = activeSubTab == tab,
          onClick = { activeSubTab = tab },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(tab.label)
              if (showProTag) {
                Spacer(Modifier.width(5.dp))
                ProLockBadge()
              }
            }
          },
          modifier = Modifier.testTag("market_subtab_${tab.name.lowercase()}")
        )
      }
    }

    when (activeSubTab) {
      MarketSubTab.QUOTES -> {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search Stocks, Gold, Crude Oil, Nifty, MF") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("market_search_input")
        )

        // Category Filters
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MarketFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = { Text(filter.label) }
            )
          }
        }

        // Custom Watchlists Bar (visible to all; Watchlists 2-5 PRO-locked for Free)
        if (selectedFilter == MarketFilter.WATCHLIST || customWatchlists.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            FilterChip(
              selected = activeCustomWatchlistIdx == -1,
              onClick = {
                activeCustomWatchlistIdx = -1
                selectedFilter = MarketFilter.WATCHLIST
              },
              label = { Text("Main (${primaryWatchlist.size})") }
            )
            customWatchlists.forEachIndexed { idx, wl ->
              FilterChip(
                selected = activeCustomWatchlistIdx == idx,
                onClick = {
                  activeCustomWatchlistIdx = idx
                  selectedFilter = MarketFilter.WATCHLIST
                },
                label = { Text("${wl.name} (${wl.symbols.size})") }
              )
            }
            FilterChip(
              selected = false,
              onClick = {
                if (!isPro) onLockedFeatureClick(ProFeature.SCREENER_SCANNER)
                else showWatchlistDialog = true
              },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Add, contentDescription = "Add Watchlist", modifier = Modifier.size(14.dp))
                  Spacer(Modifier.width(3.dp))
                  Text("New List")
                  if (!isPro) {
                    Spacer(Modifier.width(4.dp))
                    ProLockBadge()
                  }
                }
              }
            )
          }
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredQuotes, key = { it.asset.symbol }) { quote ->
            MarketAssetRow(
              quote = quote,
              inWatchlist = quote.asset.symbol in primaryWatchlist,
              onClick = { onSelectAsset(quote.asset.symbol) },
              onToggleWatchlist = { onToggleWatchlist(quote.asset.symbol) }
            )
          }
        }
      }

      MarketSubTab.SCREENER -> {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ScreenerMode.entries.forEach { mode ->
            FilterChip(
              selected = screenerMode == mode,
              onClick = { screenerMode = mode },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(mode.label)
                  if (!isPro && mode == ScreenerMode.SIGNALS) {
                    Spacer(Modifier.width(5.dp))
                    ProLockBadge()
                  }
                }
              }
            )
          }
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (screenerMode == ScreenerMode.SIGNALS) {
            if (!isPro) {
              item {
                ScreenerUnlockBanner(
                  title = "Technical Signal Scanner (PRO)",
                  subtitle = "Showing live signal matches in preview mode. Unlock Pro for instant one-tap chart jumps & scanner alerts.",
                  onUnlock = { onLockedFeatureClick(ProFeature.SCREENER_SCANNER) }
                )
              }
            }
            items(signals, key = { "${it.symbol}_${it.signalType}" }) { sig ->
              SignalScannerRow(
                signal = sig,
                isPro = isPro,
                onClick = {
                  if (isPro) onSelectAsset(sig.symbol)
                  else onLockedFeatureClick(ProFeature.SCREENER_SCANNER)
                }
              )
            }
          } else {
            val sorted = when (screenerMode) {
              ScreenerMode.GAINERS -> quotes.values.sortedByDescending { it.changePct }
              ScreenerMode.LOSERS -> quotes.values.sortedBy { it.changePct }
              ScreenerMode.ACTIVE -> quotes.values.sortedByDescending { it.totalVolume }
              ScreenerMode.SIGNALS -> emptyList()
            }
            val visibleRows = if (isPro) sorted else sorted.take(3)
            items(visibleRows, key = { it.asset.symbol }) { quote ->
              MarketAssetRow(
                quote = quote,
                inWatchlist = quote.asset.symbol in primaryWatchlist,
                onClick = { onSelectAsset(quote.asset.symbol) },
                onToggleWatchlist = { onToggleWatchlist(quote.asset.symbol) }
              )
            }
            if (!isPro && sorted.size > 3) {
              item {
                ScreenerUnlockBanner(
                  title = "Unlock ${sorted.size - 3} More Screener Assets",
                  subtitle = "Free users see Top 3 movers. Upgrade to Pro for full market ranking & 5 custom watchlists.",
                  onUnlock = { onLockedFeatureClick(ProFeature.SCREENER_SCANNER) }
                )
              }
            }
          }
        }
      }

      MarketSubTab.HEATMAP -> {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            Text("6-Sector & Multi-Asset Heatmap", style = MaterialTheme.typography.titleLarge)
            Text(
              text = "Tap any sector tile to jump straight to its highest-momentum asset.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          item {
            SectorHeatmapGrid(
              quotes = quotes,
              onSectorStockClick = onSelectAsset
            )
          }
        }
      }

      MarketSubTab.OPTIONS -> {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(16.dp)
        ) {
          item {
            OptionChainPanel(
              quotes = quotes,
              isPro = isPro,
              onPlaceOptionOrder = onPlaceOptionOrder,
              onLockedFeatureClick = onLockedFeatureClick
            )
          }
        }
      }
    }

    AdMobBannerSlot(
      appConfig = appConfig,
      isPro = isPro,
      onUpgradeClick = onNavigateToPro,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
  }

  if (showWatchlistDialog && isPro) {
    var listName by remember { mutableStateOf("Swing Picks") }
    var pickedSymbols by remember { mutableStateOf(listOf("RELIANCE", "TATAMOTORS", "SUNPHARMA")) }
    AlertDialog(
      onDismissRequest = { showWatchlistDialog = false },
      title = { Text("Create Pro Watchlist") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = listName,
            onValueChange = { listName = it },
            label = { Text("Watchlist Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quotes.keys.forEach { sym ->
              val selected = sym in pickedSymbols
              FilterChip(
                selected = selected,
                onClick = {
                  pickedSymbols = if (selected) pickedSymbols - sym else pickedSymbols + sym
                },
                label = { Text(sym, fontSize = 11.sp) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          onSaveCustomWatchlist(null, listName, pickedSymbols, (customWatchlists.size + 1).toLong())
          showWatchlistDialog = false
        }) { Text("Save") }
      },
      dismissButton = {
        OutlinedButton(onClick = { showWatchlistDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun SignalScannerRow(
  signal: TechnicalSignalMatch,
  isPro: Boolean,
  onClick: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val accent = if (signal.changePct >= 0) trading.bullishEmerald else trading.bearishCrimson
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = trading.aurumGold, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(signal.symbol, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(trading.aurumGoldSoft)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(signal.signalType.label, style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.aurumGold, fontWeight = FontWeight.Bold))
        }
      }
      Text(signal.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(formatInr(signal.currentPrice), style = TabularMonoStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
      Text(formatPct(signal.changePct), style = TabularMonoStyle.copy(fontSize = 11.sp, color = accent))
      if (!isPro) {
        Spacer(Modifier.height(4.dp))
        ProLockBadge()
      }
    }
  }
}

@Composable
private fun ScreenerUnlockBanner(
  title: String,
  subtitle: String,
  onUnlock: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(trading.aurumGoldSoft)
      .border(1.dp, trading.aurumGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
      .clickable(onClick = onUnlock)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = trading.aurumGold))
      ProLockBadge()
    }
    Text(subtitle, style = MaterialTheme.typography.bodySmall)
    Button(
      onClick = onUnlock,
      colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.heightIn(min = 44.dp)
    ) {
      Text("Preview & Unlock Pro", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun MarketAssetRow(
  quote: LiveQuote,
  inWatchlist: Boolean,
  onClick: () -> Unit,
  onToggleWatchlist: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val isUp = quote.change >= 0.0
  val accent = if (isUp) trading.bullishEmerald else trading.bearishCrimson
  val badgeBg = if (isUp) trading.bullishSoft else trading.bearishSoft

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("asset_row_${quote.asset.symbol}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = quote.asset.symbol,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = quote.asset.sector,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }
      Spacer(Modifier.height(2.dp))
      Text(
        text = "${quote.asset.name} • ${quote.asset.unitLabel}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = formatInr(quote.currentPrice),
        style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
      )
      Spacer(Modifier.height(2.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(badgeBg)
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Icon(
          imageVector = if (isUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
          text = formatPct(quote.changePct),
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold)
        )
      }
    }

    IconButton(
      onClick = onToggleWatchlist,
      modifier = Modifier.testTag("watchlist_toggle_${quote.asset.symbol}")
    ) {
      Icon(
        imageVector = if (inWatchlist) Icons.Rounded.Star else Icons.Rounded.StarOutline,
        contentDescription = "Toggle Watchlist",
        tint = if (inWatchlist) trading.aurumGold else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
