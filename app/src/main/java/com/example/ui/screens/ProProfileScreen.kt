package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.TerminalThemeMode
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.model.isValidHttpUrl
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProCrownBadge
import com.aurumtrade.shared.ui.ProLockBadge
import com.aurumtrade.shared.util.formatInr
import com.example.ui.CheckoutState
import com.example.ui.RewardedAdButton
import com.example.ui.components.FreeVsProComparisonSection
import com.example.ui.components.ProFeature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProProfileScreen(
  profile: UserProfile?,
  plansState: UiState<List<SubscriptionPlan>>,
  appConfig: AppConfig,
  checkoutState: CheckoutState,
  onSelectTheme: (TerminalThemeMode) -> Unit,
  onSubscribeClick: (SubscriptionPlan) -> Unit,
  onWatchAdClick: () -> Unit,
  onLockedFeatureClick: (ProFeature) -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val trading = LocalAurumTradingColors.current
  val isPro = profile?.hasValidProAccess == true
  val activeTheme = TerminalThemeMode.safeFrom(profile?.preferredTheme ?: TerminalThemeMode.DARK_OLED.name)
  val showReferralCard = appConfig.enableReferralCard && isValidHttpUrl(appConfig.referralUrl)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Profile Card
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, if (isPro) trading.aurumGold else trading.cardBorder, RoundedCornerShape(18.dp))
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(trading.aurumGoldSoft),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = (profile?.displayName?.firstOrNull() ?: 'A').uppercase(),
          style = MaterialTheme.typography.titleLarge.copy(color = trading.aurumGold)
        )
      }
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(profile?.displayName ?: "Trader", style = MaterialTheme.typography.titleLarge)
          if (isPro) {
            Spacer(Modifier.width(8.dp))
            ProCrownBadge()
          }
        }
        Text(
          text = profile?.email ?: "",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isPro && (profile?.planExpiresAtMillis ?: 0L) > 0L) {
          val exp = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(profile!!.planExpiresAtMillis))
          Text(
            text = "${profile.planName} • Valid until $exp",
            style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.bullishEmerald)
          )
        } else {
          Text(
            text = "Free Tier • ₹1L Paper Cash • 5 Watchlist Cap",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = trading.aurumGold)
          )
        }
      }
    }

    // Dynamic Partner / Referral Card (hidden completely unless enabled with valid http/https URL)
    if (showReferralCard) {
      PartnerReferralCard(
        title = appConfig.referralCardTitle.ifBlank { "Featured Partner Offer" },
        subtitle = appConfig.referralCardSubtitle,
        buttonLabel = appConfig.referralButtonLabel.ifBlank { "Explore Partner" },
        onOpenClick = {
          val cleanUrl = appConfig.referralUrl.trim()
          if (isValidHttpUrl(cleanUrl)) {
            runCatching {
              val uri = Uri.parse(cleanUrl)
              val scheme = uri.scheme?.lowercase(Locale.ROOT)
              if (scheme == "http" || scheme == "https") {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
              }
            }.onFailure {
              Toast.makeText(context, "Unable to open partner link.", Toast.LENGTH_SHORT).show()
            }
          }
        }
      )
    }

    // Theme Switcher (Dark OLED, Daylight, Midnight Gold [PRO], Neon Terminal [PRO])
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text("Terminal Theme", style = MaterialTheme.typography.titleMedium)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TerminalThemeMode.entries.forEach { mode ->
          val locked = mode.isProOnly && !isPro
          FilterChip(
            selected = activeTheme == mode,
            onClick = {
              if (locked) onLockedFeatureClick(ProFeature.AUTO_SIP_ALERTS)
              else onSelectTheme(mode)
            },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mode.label)
                if (locked) {
                  Spacer(Modifier.width(5.dp))
                  ProLockBadge()
                }
              }
            }
          )
        }
      }
    }

    // Completely hide ads when Pro subscription is active
    if (!isPro) {
      RewardedAdButton(
        appConfig = appConfig,
        onClick = onWatchAdClick,
        modifier = Modifier.fillMaxWidth()
      )
    }

    // Interactive Free vs Pro Feature Comparison Banner & Matrix
    FreeVsProComparisonSection(
      isPro = isPro,
      onInspectFeature = onLockedFeatureClick
    )

    // FamGateway Auto-UPI Pro Plans (fetched live from Firestore)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = trading.aurumGold)
        Spacer(Modifier.width(8.dp))
        Text("Aurum Pro Auto-UPI Plans", style = MaterialTheme.typography.titleLarge)
      }
      Text(
        text = "Instant In-App UPI checkout via FamGateway v2.0. Auto-verifies in 3.5s — no UTR needed.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      when (plansState) {
        is UiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        is UiState.Error -> {
          Text(plansState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
          val plans = plansState.data
          if (plans.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
            ) {
              Text(
                text = "No active subscription plans published yet. Create plans in the Aurum Admin APK to enable Pro checkout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            plans.forEach { plan ->
              SubscriptionPlanCard(
                plan = plan,
                isLoading = checkoutState is CheckoutState.CreatingOrder,
                onSubscribe = { onSubscribeClick(plan) }
              )
            }
          }
        }
      }
    }

    OutlinedButton(
      onClick = onSignOut,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 50.dp)
        .testTag("sign_out_button")
    ) {
      Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text("Sign Out")
    }
  }
}

@Composable
private fun PartnerReferralCard(
  title: String,
  subtitle: String,
  buttonLabel: String,
  onOpenClick: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val gradientBg = Brush.linearGradient(
    colors = listOf(
      trading.aurumGoldSoft,
      MaterialTheme.colorScheme.surface,
      trading.bullishEmerald.copy(alpha = 0.14f)
    )
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(gradientBg)
      .border(1.dp, trading.aurumGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
      .padding(16.dp)
      .testTag("partner_referral_card"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(trading.aurumGold.copy(alpha = 0.2f))
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = trading.aurumGold,
            modifier = Modifier.size(12.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = "PARTNER",
            style = TabularMonoStyle.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = trading.aurumGold
            )
          )
        }
      }
    }

    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )

    if (subtitle.isNotBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Button(
      onClick = onOpenClick,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = trading.bullishEmerald),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("partner_referral_cta")
    ) {
      Text(buttonLabel, fontWeight = FontWeight.Bold)
      Spacer(Modifier.width(6.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = "Open Partner Link",
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
private fun SubscriptionPlanCard(
  plan: SubscriptionPlan,
  isLoading: Boolean,
  onSubscribe: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val decimals = if (plan.priceInr % 1.0 == 0.0) 0 else 2
  val formattedPrice = formatInr(plan.priceInr, decimals = decimals)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.aurumGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(plan.name, style = MaterialTheme.typography.titleLarge)
          if (plan.discountBadge.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(trading.aurumGoldSoft)
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = plan.discountBadge,
                style = TabularMonoStyle.copy(fontSize = 10.sp, color = trading.aurumGold, fontWeight = FontWeight.Bold)
              )
            }
          }
        }
        if (plan.tagline.isNotBlank()) {
          Text(plan.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formattedPrice,
          style = TabularMonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = trading.aurumGold)
        )
        Text(
          text = "${plan.validityDays} Days",
          style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    Button(
      onClick = onSubscribe,
      enabled = !isLoading,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("subscribe_plan_${plan.id}")
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        Text("Pay $formattedPrice via Auto-UPI", fontWeight = FontWeight.Bold)
      }
    }
  }
}
