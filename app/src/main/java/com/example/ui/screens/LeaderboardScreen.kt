package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.model.LeaderboardEntry
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProCrownBadge
import com.aurumtrade.shared.util.formatCompactInr
import com.aurumtrade.shared.util.formatPct

@Composable
fun LeaderboardScreen(
  currentUserId: String,
  leaderboardState: UiState<List<LeaderboardEntry>>,
  onSyncRank: () -> Unit
) {
  val trading = LocalAurumTradingColors.current

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Trader Leaderboard", style = MaterialTheme.typography.headlineMedium)
        Text(
          text = "Ranked by portfolio return %",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      OutlinedButton(
        onClick = onSyncRank,
        modifier = Modifier.testTag("sync_leaderboard_button")
      ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Sync Rank")
      }
    }

    when (leaderboardState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Loading rankings...", style = MaterialTheme.typography.bodyMedium)
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(leaderboardState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        val entries = leaderboardState.data
        if (entries.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = trading.aurumGold,
                modifier = Modifier.size(48.dp)
              )
              Spacer(Modifier.height(10.dp))
              Text("No rankings published yet", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Tap Sync Rank to post your paper trading performance to the board.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            itemsIndexed(entries, key = { _, e -> e.userId }) { idx, entry ->
              val isMe = entry.userId == currentUserId
              val rank = idx + 1
              val accent = if (entry.returnPct >= 0) trading.bullishEmerald else trading.bearishCrimson

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    if (isMe) trading.aurumGoldSoft else MaterialTheme.colorScheme.surface
                  )
                  .border(
                    width = if (isMe) 1.5.dp else 1.dp,
                    color = if (isMe) trading.aurumGold else trading.cardBorder,
                    shape = RoundedCornerShape(16.dp)
                  )
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                      when (rank) {
                        1 -> trading.aurumGoldSoft
                        else -> MaterialTheme.colorScheme.surfaceVariant
                      }
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "#$rank",
                    style = TabularMonoStyle.copy(
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (rank == 1) trading.aurumGold else MaterialTheme.colorScheme.onSurface
                    )
                  )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                      style = MaterialTheme.typography.titleMedium
                    )
                    if (entry.proMember) {
                      Spacer(Modifier.width(8.dp))
                      ProCrownBadge(label = "PRO")
                    }
                  }
                  Text(
                    text = "Net Worth ${formatCompactInr(entry.netWorth)}",
                    style = TabularMonoStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  )
                }

                Text(
                  text = formatPct(entry.returnPct),
                  style = TabularMonoStyle.copy(fontSize = 15.sp, color = accent, fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }
      }
    }
  }
}
