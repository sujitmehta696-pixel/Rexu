package com.example.ui

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
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.util.formatCompactInr
import kotlinx.coroutines.delay

@Composable
fun AdMobBannerSlot(
  appConfig: AppConfig,
  isPro: Boolean,
  onUpgradeClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (isPro || !appConfig.adMobEnabled) return
  val trading = LocalAurumTradingColors.current

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
      .border(1.dp, trading.cardBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onUpgradeClick)
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("admob_banner_slot"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(trading.aurumGoldSoft)
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = "AD",
            style = TabularMonoStyle.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = trading.aurumGold
            )
          )
        }
        Spacer(Modifier.width(6.dp))
        Text(
          text = "AdMob Banner Hook Ready",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Text(
        text = "Unit: ${appConfig.bannerAdUnitId.takeLast(14)}",
        style = TabularMonoStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Rounded.WorkspacePremium,
        contentDescription = null,
        tint = trading.aurumGold,
        modifier = Modifier.size(16.dp)
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = "Go Ad-Free",
        style = MaterialTheme.typography.labelLarge.copy(color = trading.aurumGold)
      )
    }
  }
}

@Composable
fun RewardedAdButton(
  appConfig: AppConfig,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!appConfig.adMobEnabled) return
  val trading = LocalAurumTradingColors.current
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .heightIn(min = 48.dp)
      .testTag("rewarded_ad_button")
  ) {
    Icon(
      imageVector = Icons.Rounded.PlayCircle,
      contentDescription = "Watch Ad for virtual cash",
      tint = trading.aurumGold,
      modifier = Modifier.size(18.dp)
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = "Watch Ad → +${formatCompactInr(appConfig.rewardedAdBonus)}",
      style = MaterialTheme.typography.labelLarge.copy(color = trading.aurumGold)
    )
  }
}

@Composable
fun RewardedAdDialog(
  appConfig: AppConfig,
  onRewardEarned: () -> Unit,
  onDismiss: () -> Unit
) {
  var secondsLeft by remember { mutableIntStateOf(5) }
  val trading = LocalAurumTradingColors.current

  LaunchedEffect(Unit) {
    while (secondsLeft > 0) {
      delay(1000L)
      secondsLeft--
    }
  }

  AlertDialog(
    onDismissRequest = { if (secondsLeft == 0) onDismiss() },
    title = {
      Text(
        text = "Rewarded Ad Hook",
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "AdMob Rewarded Unit: ${appConfig.rewardedAdUnitId}",
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
          text = if (secondsLeft > 0) {
            "Claiming +${formatCompactInr(appConfig.rewardedAdBonus)} virtual cash in ${secondsLeft}s..."
          } else {
            "Reward unlocked! Tap Claim to add +${formatCompactInr(appConfig.rewardedAdBonus)} to your paper wallet."
          },
          style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
          progress = { (5 - secondsLeft) / 5f },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = trading.aurumGold
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onRewardEarned,
        enabled = secondsLeft == 0,
        colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold)
      ) {
        Text(if (secondsLeft == 0) "Claim Cash" else "Wait ${secondsLeft}s")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
