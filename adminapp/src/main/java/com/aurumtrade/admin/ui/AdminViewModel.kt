package com.aurumtrade.admin.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurumtrade.admin.data.AdminRepository
import com.aurumtrade.shared.model.AdminGatewaySecret
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
  private val repository: AdminRepository,
  private val adminUid: String
) : ViewModel() {

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val events: SharedFlow<String> = _events.asSharedFlow()

  private fun <T> Flow<T>.toUiStateFlow(tag: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Admin stream error [$tag]", root)
        emit(UiState.Error(root.message ?: "Failed to load $tag"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
      )

  val plansState: StateFlow<UiState<List<SubscriptionPlan>>> =
    repository.observePlans(adminUid).toUiStateFlow("plans")

  val gatewaySecretState: StateFlow<UiState<AdminGatewaySecret>> =
    repository.observeGatewaySecret(adminUid).toUiStateFlow("gateway_secret")

  val appConfigState: StateFlow<UiState<AppConfig>> =
    repository.observeAppConfig(adminUid).toUiStateFlow("app_config")

  val assetsState: StateFlow<UiState<List<MarketAsset>>> =
    repository.observeAssets(adminUid).toUiStateFlow("market_assets")

  val usersState: StateFlow<UiState<List<UserProfile>>> =
    repository.observeUsers(adminUid).toUiStateFlow("users")

  fun savePlan(
    planId: String,
    name: String,
    priceInr: Double,
    validityDays: Long,
    discountBadge: String,
    tagline: String,
    active: Boolean,
    sortOrder: Long
  ) {
    viewModelScope.launch {
      repository.savePlan(planId, name, priceInr, validityDays, discountBadge, tagline, active, sortOrder)
        .onSuccess { _events.tryEmit("Saved plan: $name") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to save plan") }
    }
  }

  fun saveAndSyncPlans(plans: List<SubscriptionPlan>) {
    viewModelScope.launch {
      repository.saveAndSyncPlans(plans)
        .onSuccess { _events.tryEmit("Plans synced to Firestore (${plans.size} active/paused)") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to sync plans") }
    }
  }

  fun deletePlan(planId: String) {
    viewModelScope.launch {
      repository.deletePlan(planId)
        .onSuccess { _events.tryEmit("Plan deleted") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to delete plan") }
    }
  }

  fun seedDefaultPlans() {
    viewModelScope.launch {
      repository.seedDefaultPlans()
        .onSuccess { _events.tryEmit("Default Pro plans published") }
        .onFailure { _events.tryEmit(it.message ?: "Seed failed") }
    }
  }

  fun saveReferralSettings(
    enableReferralCard: Boolean,
    title: String,
    subtitle: String,
    buttonLabel: String,
    referralUrl: String
  ) {
    val currentConfig = (appConfigState.value as? UiState.Success)?.data ?: AppConfig()
    viewModelScope.launch {
      repository.saveReferralSettings(
        currentConfig = currentConfig,
        enableReferralCard = enableReferralCard,
        title = title,
        subtitle = subtitle,
        buttonLabel = buttonLabel,
        referralUrl = referralUrl
      ).onSuccess {
        _events.tryEmit("Saved Partner / Referral settings to /app_config/global")
      }.onFailure {
        _events.tryEmit(it.message ?: "Failed to save referral settings")
      }
    }
  }

  fun saveGatewayAndConfig(
    apiKey: String,
    webhookSecret: String,
    backendUrl: String,
    adMobEnabled: Boolean,
    bannerAdUnitId: String,
    rewardedAdUnitId: String,
    startingVirtualCash: Double,
    rewardedAdBonus: Double
  ) {
    val currentConfig = (appConfigState.value as? UiState.Success)?.data ?: AppConfig()
    viewModelScope.launch {
      repository.saveGatewayAndAppConfig(
        currentConfig = currentConfig,
        apiKey = apiKey,
        webhookSecret = webhookSecret,
        backendUrl = backendUrl,
        adMobEnabled = adMobEnabled,
        bannerAdUnitId = bannerAdUnitId,
        rewardedAdUnitId = rewardedAdUnitId,
        startingVirtualCash = startingVirtualCash,
        rewardedAdBonus = rewardedAdBonus
      ).onSuccess {
        _events.tryEmit("Saved /admin_secrets/famgateway & /app_config/global")
      }.onFailure {
        _events.tryEmit(it.message ?: "Failed to save gateway config")
      }
    }
  }

  fun testBackendHealth(url: String) {
    viewModelScope.launch {
      repository.testBackendHealth(url)
        .onSuccess { _events.tryEmit(it) }
        .onFailure { _events.tryEmit("Health check failed: ${it.message}") }
    }
  }

  fun saveAsset(asset: MarketAsset) {
    viewModelScope.launch {
      repository.saveAsset(asset)
        .onSuccess { _events.tryEmit("Saved asset ${asset.symbol}") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to save asset") }
    }
  }

  fun seedDefaultAssets() {
    viewModelScope.launch {
      repository.seedDefaultAssets()
        .onSuccess { _events.tryEmit("Default NSE/Gold/MF assets published") }
        .onFailure { _events.tryEmit(it.message ?: "Failed to seed assets") }
    }
  }

  private companion object {
    const val TAG = "AdminVM"
  }
}
