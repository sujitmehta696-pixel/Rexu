package com.aurumtrade.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.model.AdminGatewaySecret
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.AssetCategory
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.ui.ProCrownBadge
import com.aurumtrade.shared.util.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPlansScreen(
  plansState: UiState<List<SubscriptionPlan>>,
  onSavePlan: (String, String, Double, Long, String, String, Boolean, Long) -> Unit,
  onSyncAllPlans: (List<SubscriptionPlan>) -> Unit,
  onDeletePlan: (String) -> Unit,
  onSeedDefaults: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val remotePlans = (plansState as? UiState.Success)?.data.orEmpty()
  var localPlans by remember(remotePlans) { mutableStateOf(remotePlans) }

  var planId by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("Pro Monthly") }
  var priceText by remember { mutableStateOf("199") }
  var daysText by remember { mutableStateOf("30") }
  var badge by remember { mutableStateOf("BEST VALUE") }
  var tagline by remember { mutableStateOf("Candlesticks, 5x Leverage, Auto-SIP & Ad-Free") }
  var sortOrderText by remember { mutableStateOf("1") }
  var active by remember { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Subscription Plans", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = onSeedDefaults, modifier = Modifier.testTag("seed_plans_button")) {
          Text("Seed 3 Plans")
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.aurumGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = if (planId.isBlank()) "Subscription Plan Manager • Add Plan" else "Subscription Plan Manager • Edit ($planId)",
          style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Plan Name (e.g., Monthly, Quarterly, Yearly)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it },
            label = { Text("Validity Days") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = badge,
            onValueChange = { badge = it },
            label = { Text("Offer / Badge (e.g. 50% OFF)") },
            singleLine = true,
            modifier = Modifier.weight(1.6f)
          )
          OutlinedTextField(
            value = sortOrderText,
            onValueChange = { sortOrderText = it },
            label = { Text("Order") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(0.7f)
          )
        }
        OutlinedTextField(
          value = tagline,
          onValueChange = { tagline = it },
          label = { Text("Tagline") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Enabled for Purchase", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = active, onCheckedChange = { active = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              val price = priceText.toDoubleOrNull()?.coerceIn(1.0, 100_000.0) ?: 199.0
              val days = daysText.toLongOrNull()?.coerceIn(1L, 3650L) ?: 30L
              val order = sortOrderText.toLongOrNull()?.coerceIn(0L, 1000L) ?: 1L
              onSavePlan(planId, name, price, days, badge, tagline, active, order)
              planId = ""
            },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("save_plan_button")
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (planId.isBlank()) "Add Plan" else "Apply Edit")
          }
          if (planId.isNotBlank()) {
            OutlinedButton(
              onClick = { planId = "" },
              modifier = Modifier.heightIn(min = 48.dp)
            ) {
              Text("Cancel")
            }
          }
        }

        Button(
          onClick = {
            if (planId.isNotBlank() && name.isNotBlank()) {
              val price = priceText.toDoubleOrNull()?.coerceIn(1.0, 100_000.0) ?: 199.0
              val days = daysText.toLongOrNull()?.coerceIn(1L, 3650L) ?: 30L
              val order = sortOrderText.toLongOrNull()?.coerceIn(0L, 1000L) ?: 1L
              val updatedList = localPlans.map { existing ->
                if (existing.id == planId) {
                  existing.copy(
                    name = name,
                    priceInr = price,
                    validityDays = days,
                    discountBadge = badge,
                    tagline = tagline,
                    active = active,
                    sortOrder = order
                  )
                } else existing
              }
              onSyncAllPlans(updatedList)
              planId = ""
            } else if (localPlans.isNotEmpty()) {
              onSyncAllPlans(localPlans)
            } else if (name.isNotBlank()) {
              val price = priceText.toDoubleOrNull()?.coerceIn(1.0, 100_000.0) ?: 199.0
              val days = daysText.toLongOrNull()?.coerceIn(1L, 3650L) ?: 30L
              val order = sortOrderText.toLongOrNull()?.coerceIn(0L, 1000L) ?: 1L
              onSavePlan("", name, price, days, badge, tagline, active, order)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("save_sync_plans_button")
        ) {
          Text("Save & Sync Plans", fontWeight = FontWeight.Bold)
        }
      }
    }

    when (plansState) {
      is UiState.Loading -> item { CircularProgressIndicator() }
      is UiState.Error -> item { Text(plansState.message, color = MaterialTheme.colorScheme.error) }
      is UiState.Success -> {
        items(localPlans, key = { it.id }) { p ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surface)
              .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${p.name} • ${formatInr(p.priceInr, decimals = if (p.priceInr % 1.0 == 0.0) 0 else 2)} / ${p.validityDays}d",
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = "${if (p.active) "ENABLED" else "DISABLED"}${if (p.discountBadge.isNotBlank()) " • ${p.discountBadge}" else ""}",
                style = TabularMonoStyle.copy(
                  fontSize = 11.sp,
                  color = if (p.active) trading.bullishEmerald else trading.bearishCrimson
                )
              )
            }
            Switch(
              checked = p.active,
              onCheckedChange = { checked ->
                localPlans = localPlans.map { if (it.id == p.id) it.copy(active = checked) else it }
              }
            )
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
              onClick = {
                planId = p.id
                name = p.name
                priceText = if (p.priceInr % 1.0 == 0.0) p.priceInr.toInt().toString() else p.priceInr.toString()
                daysText = p.validityDays.toString()
                badge = p.discountBadge
                tagline = p.tagline
                sortOrderText = p.sortOrder.toString()
                active = p.active
              }
            ) {
              Text("Edit")
            }
            IconButton(onClick = { onDeletePlan(p.id) }) {
              Icon(Icons.Rounded.Delete, contentDescription = "Delete Plan", tint = trading.bearishCrimson)
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminGatewayConfigScreen(
  secretState: UiState<AdminGatewaySecret>,
  appConfigState: UiState<AppConfig>,
  onSaveAll: (String, String, String, Boolean, String, String, Double, Double) -> Unit,
  onSaveReferral: (Boolean, String, String, String, String) -> Unit,
  onTestHealth: (String) -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val currentSecret = (secretState as? UiState.Success)?.data ?: AdminGatewaySecret()
  val currentConfig = (appConfigState as? UiState.Success)?.data ?: AppConfig()

  var apiKey by remember(currentSecret.apiKey) { mutableStateOf(currentSecret.apiKey) }
  var webhookSecret by remember(currentSecret.webhookSecret) { mutableStateOf(currentSecret.webhookSecret) }
  var backendUrl by remember(currentSecret.backendUrl, currentConfig.backendBaseUrl) {
    mutableStateOf(currentSecret.backendUrl.ifBlank { currentConfig.backendBaseUrl })
  }
  var adMobEnabled by remember(currentConfig.adMobEnabled) { mutableStateOf(currentConfig.adMobEnabled) }
  var bannerAdUnitId by remember(currentConfig.bannerAdUnitId) { mutableStateOf(currentConfig.bannerAdUnitId) }
  var rewardedAdUnitId by remember(currentConfig.rewardedAdUnitId) { mutableStateOf(currentConfig.rewardedAdUnitId) }
  var startingCashText by remember(currentConfig.startingVirtualCash) {
    mutableStateOf(currentConfig.startingVirtualCash.toLong().toString())
  }
  var adBonusText by remember(currentConfig.rewardedAdBonus) {
    mutableStateOf(currentConfig.rewardedAdBonus.toLong().toString())
  }

  // Partner / Referral Settings state (/app_config/global)
  var enableReferralCard by remember(currentConfig.enableReferralCard) {
    mutableStateOf(currentConfig.enableReferralCard)
  }
  var referralTitle by remember(currentConfig.referralCardTitle) {
    mutableStateOf(currentConfig.referralCardTitle)
  }
  var referralSubtitle by remember(currentConfig.referralCardSubtitle) {
    mutableStateOf(currentConfig.referralCardSubtitle)
  }
  var referralButtonLabel by remember(currentConfig.referralButtonLabel) {
    mutableStateOf(currentConfig.referralButtonLabel)
  }
  var referralUrl by remember(currentConfig.referralUrl) {
    mutableStateOf(currentConfig.referralUrl)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text("Gateway, Referral & AdMob", style = MaterialTheme.typography.headlineMedium)

    // Partner / Referral Settings Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.bullishEmerald.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("Partner / Referral Settings (/app_config/global)", style = MaterialTheme.typography.titleMedium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Enable Referral Card", style = MaterialTheme.typography.bodyMedium)
        Switch(
          checked = enableReferralCard,
          onCheckedChange = { enableReferralCard = it },
          modifier = Modifier.testTag("enable_referral_switch")
        )
      }
      OutlinedTextField(
        value = referralTitle,
        onValueChange = { referralTitle = it },
        label = { Text("Card Title") },
        placeholder = { Text("Open Free Demat & Earn Bonus") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = referralSubtitle,
        onValueChange = { referralSubtitle = it },
        label = { Text("Subtitle") },
        placeholder = { Text("Trade live NSE markets with our verified broker partner") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = referralButtonLabel,
        onValueChange = { referralButtonLabel = it },
        label = { Text("Button Label") },
        placeholder = { Text("Open Partner Link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = referralUrl,
        onValueChange = { referralUrl = it },
        label = { Text("Referral URL (https://...)") },
        placeholder = { Text("https://partner.example.com/invite") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Button(
        onClick = {
          onSaveReferral(
            enableReferralCard,
            referralTitle,
            referralSubtitle,
            referralButtonLabel,
            referralUrl
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = trading.bullishEmerald),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("save_referral_settings_button")
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.aurumGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("FamGateway v2.0 Secrets (/admin_secrets/famgateway)", style = MaterialTheme.typography.titleMedium)
      OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        label = { Text("FamGateway Secret API Key") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = webhookSecret,
        onValueChange = { webhookSecret = it },
        label = { Text("Webhook HMAC-SHA256 Secret") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = backendUrl,
        onValueChange = { backendUrl = it },
        label = { Text("Render Backend Base URL") },
        placeholder = { Text("https://aurum-backend.onrender.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedButton(
        onClick = { onTestHealth(backendUrl) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
      ) {
        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Test Backend /health")
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text("AdMob & Virtual Economy (/app_config/global)", style = MaterialTheme.typography.titleMedium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Enable AdMob Hooks (Free Users)", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = adMobEnabled, onCheckedChange = { adMobEnabled = it })
      }
      OutlinedTextField(
        value = bannerAdUnitId,
        onValueChange = { bannerAdUnitId = it },
        label = { Text("AdMob Banner Unit ID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = rewardedAdUnitId,
        onValueChange = { rewardedAdUnitId = it },
        label = { Text("AdMob Rewarded Unit ID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = startingCashText,
          onValueChange = { startingCashText = it },
          label = { Text("Starting Cash ₹") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = adBonusText,
          onValueChange = { adBonusText = it },
          label = { Text("Ad Bonus ₹") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Button(
      onClick = {
        onSaveAll(
          apiKey,
          webhookSecret,
          backendUrl,
          adMobEnabled,
          bannerAdUnitId,
          rewardedAdUnitId,
          startingCashText.toDoubleOrNull() ?: 100000.0,
          adBonusText.toDoubleOrNull() ?: 25000.0
        )
      },
      colors = ButtonDefaults.buttonColors(containerColor = trading.aurumGold),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .testTag("save_gateway_config_button")
    ) {
      Text("Save Gateway & App Config", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun AdminAssetsScreen(
  assetsState: UiState<List<MarketAsset>>,
  onSaveAsset: (MarketAsset) -> Unit,
  onSeedDefaults: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  var symbol by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var sector by remember { mutableStateOf("IT") }
  var category by remember { mutableStateOf(AssetCategory.STOCK) }
  var priceText by remember { mutableStateOf("1500") }
  var unitLabel by remember { mutableStateOf("1 Share") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Market Assets", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = onSeedDefaults, modifier = Modifier.testTag("seed_assets_button")) {
          Text("Seed 16 Assets")
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Add / Update Stock, Gold or MF", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it.uppercase() },
            label = { Text("Symbol") },
            placeholder = { Text("WIPRO") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Base Price ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Asset Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AssetCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.label) }
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sector,
            onValueChange = { sector = it },
            label = { Text("Sector") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = unitLabel,
            onValueChange = { unitLabel = it },
            label = { Text("Unit") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Button(
          onClick = {
            if (symbol.isNotBlank() && name.isNotBlank()) {
              onSaveAsset(
                MarketAsset(
                  symbol = symbol.trim(),
                  name = name.trim(),
                  category = category.name,
                  sector = sector.trim().ifBlank { "General" },
                  basePrice = priceText.toDoubleOrNull() ?: 100.0,
                  volatility = 0.015,
                  unitLabel = unitLabel.trim().ifBlank { "1 Share" },
                  active = true
                )
              )
              symbol = ""
              name = ""
            }
          },
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
          Text("Publish Market Asset")
        }
      }
    }

    when (assetsState) {
      is UiState.Loading -> item { CircularProgressIndicator() }
      is UiState.Error -> item { Text(assetsState.message, color = MaterialTheme.colorScheme.error) }
      is UiState.Success -> {
        items(assetsState.data, key = { it.symbol }) { asset ->
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
            Column {
              Text("${asset.symbol} • ${asset.name}", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "${asset.category} • ${asset.sector} • ${asset.unitLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = formatInr(asset.basePrice),
              style = TabularMonoStyle.copy(fontWeight = FontWeight.Bold, color = trading.bullishEmerald)
            )
          }
        }
      }
    }
  }
}

@Composable
fun AdminUsersScreen(usersState: UiState<List<UserProfile>>) {
  val trading = LocalAurumTradingColors.current
  val fmt = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text("Users & Subscriptions", style = MaterialTheme.typography.headlineMedium)
    }

    when (usersState) {
      is UiState.Loading -> item { CircularProgressIndicator() }
      is UiState.Error -> item { Text(usersState.message, color = MaterialTheme.colorScheme.error) }
      is UiState.Success -> {
        val list = usersState.data
        if (list.isEmpty()) {
          item { Text("No registered traders yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
          items(list, key = { it.userId }) { u ->
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, if (u.hasValidProAccess) trading.aurumGold else trading.cardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                if (u.hasValidProAccess) {
                  ProCrownBadge(label = u.planName.ifBlank { "PRO" })
                } else {
                  Text("FREE", style = TabularMonoStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
              }
              Text(u.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Cash: ${formatInr(u.virtualCash)} • Trades: ${u.totalTrades}",
                  style = TabularMonoStyle.copy(fontSize = 11.sp)
                )
                if (u.planExpiresAtMillis > 0L) {
                  Text(
                    text = "Exp: ${fmt.format(Date(u.planExpiresAtMillis))}",
                    style = TabularMonoStyle.copy(fontSize = 11.sp, color = trading.aurumGold)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
