package com.aurumtrade.admin.data

import com.aurumtrade.shared.market.DefaultMarketCatalog
import com.aurumtrade.shared.model.ADMIN_SECRET_FAMGATEWAY_DOC_ID
import com.aurumtrade.shared.model.APP_CONFIG_DOC_ID
import com.aurumtrade.shared.model.AdminGatewaySecret
import com.aurumtrade.shared.model.AppConfig
import com.aurumtrade.shared.model.MarketAsset
import com.aurumtrade.shared.model.SubscriptionPlan
import com.aurumtrade.shared.model.UserProfile
import com.aurumtrade.shared.model.isValidHttpUrl
import com.aurumtrade.shared.util.OperationType
import com.aurumtrade.shared.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AdminRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private fun <T> Flow<T>.handleStreamErrors(op: OperationType, path: String, uid: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val ex = firestoreError ?: (error as? Exception)
      if (ex != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(ex, op, path)
      }
      throw error
    }

  fun observePlans(uid: String): Flow<List<SubscriptionPlan>> = flow {
    val path = "subscription_plans"
    emitAll(
      db.collection(path)
        .orderBy("sortOrder", Query.Direction.ASCENDING)
        .snapshots()
        .map { it.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeGatewaySecret(uid: String): Flow<AdminGatewaySecret> = flow {
    val path = "admin_secrets/$ADMIN_SECRET_FAMGATEWAY_DOC_ID"
    emitAll(
      db.collection("admin_secrets").document(ADMIN_SECRET_FAMGATEWAY_DOC_ID)
        .snapshots()
        .map { it.toObject(AdminGatewaySecret::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AdminGatewaySecret() }
        .handleStreamErrors(OperationType.GET, path, uid)
    )
  }

  fun observeAppConfig(uid: String): Flow<AppConfig> = flow {
    val path = "app_config/$APP_CONFIG_DOC_ID"
    emitAll(
      db.collection("app_config").document(APP_CONFIG_DOC_ID)
        .snapshots()
        .map { it.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: AppConfig() }
        .handleStreamErrors(OperationType.GET, path, uid)
    )
  }

  fun observeAssets(uid: String): Flow<List<MarketAsset>> = flow {
    val path = "market_assets"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { it.toObjects(MarketAsset::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  fun observeUsers(uid: String): Flow<List<UserProfile>> = flow {
    val path = "users"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleStreamErrors(OperationType.LIST, path, uid)
    )
  }

  suspend fun savePlan(
    planId: String,
    name: String,
    priceInr: Double,
    validityDays: Long,
    discountBadge: String,
    tagline: String,
    active: Boolean,
    sortOrder: Long
  ): Result<Unit> {
    val docId = planId.ifBlank { "plan_${System.currentTimeMillis()}" }
    val docRef = db.collection("subscription_plans").document(docId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "name" to name.trim().take(60),
            "priceInr" to priceInr,
            "validityDays" to validityDays,
            "discountBadge" to discountBadge.trim().take(40),
            "tagline" to tagline.trim().take(120),
            "active" to active,
            "sortOrder" to sortOrder,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "name" to name.trim().take(60),
            "priceInr" to priceInr,
            "validityDays" to validityDays,
            "discountBadge" to discountBadge.trim().take(40),
            "tagline" to tagline.trim().take(120),
            "active" to active,
            "sortOrder" to sortOrder,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePlan(planId: String): Result<Unit> {
    val docRef = db.collection("subscription_plans").document(planId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun seedDefaultPlans(): Result<Unit> {
    return try {
      val batch = db.batch()
      DefaultMarketCatalog.defaultSubscriptionPlans.forEach { p ->
        val ref = db.collection("subscription_plans").document(p.id)
        val exists = ref.get().await().exists()
        if (exists) {
          batch.update(
            ref,
            mapOf(
              "name" to p.name,
              "priceInr" to p.priceInr,
              "validityDays" to p.validityDays,
              "discountBadge" to p.discountBadge,
              "tagline" to p.tagline,
              "active" to p.active,
              "sortOrder" to p.sortOrder,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          batch.set(
            ref,
            mapOf(
              "name" to p.name,
              "priceInr" to p.priceInr,
              "validityDays" to p.validityDays,
              "discountBadge" to p.discountBadge,
              "tagline" to p.tagline,
              "active" to p.active,
              "sortOrder" to p.sortOrder,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "subscription_plans")
      Result.failure(e)
    }
  }

  suspend fun saveAndSyncPlans(plans: List<SubscriptionPlan>): Result<Unit> {
    return try {
      val batch = db.batch()
      plans.forEachIndexed { index, plan ->
        val docId = plan.id.ifBlank { "plan_${System.currentTimeMillis()}_$index" }
        val ref = db.collection("subscription_plans").document(docId)
        val exists = ref.get().await().exists()
        val sortVal = if (plan.sortOrder > 0L) plan.sortOrder else (index + 1).toLong()
        if (exists) {
          batch.update(
            ref,
            mapOf(
              "name" to plan.name.trim().take(60),
              "priceInr" to plan.priceInr.coerceIn(1.0, 100_000.0),
              "validityDays" to plan.validityDays.coerceIn(1L, 3650L),
              "discountBadge" to plan.discountBadge.trim().take(40),
              "tagline" to plan.tagline.trim().take(120),
              "active" to plan.active,
              "sortOrder" to sortVal,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          batch.set(
            ref,
            mapOf(
              "name" to plan.name.trim().take(60),
              "priceInr" to plan.priceInr.coerceIn(1.0, 100_000.0),
              "validityDays" to plan.validityDays.coerceIn(1L, 3650L),
              "discountBadge" to plan.discountBadge.trim().take(40),
              "tagline" to plan.tagline.trim().take(120),
              "active" to plan.active,
              "sortOrder" to sortVal,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "subscription_plans")
      Result.failure(e)
    }
  }

  suspend fun saveReferralSettings(
    currentConfig: AppConfig,
    enableReferralCard: Boolean,
    title: String,
    subtitle: String,
    buttonLabel: String,
    referralUrl: String
  ): Result<Unit> {
    val cleanUrl = referralUrl.trim()
    if (cleanUrl.isNotEmpty() && !isValidHttpUrl(cleanUrl)) {
      return Result.failure(IllegalArgumentException("Referral URL must start with https:// or http:// (no spaces)."))
    }
    if (enableReferralCard && cleanUrl.isEmpty()) {
      return Result.failure(IllegalArgumentException("Enter a valid https:// Referral URL before enabling the Referral Card."))
    }
    val configRef = db.collection("app_config").document(APP_CONFIG_DOC_ID)
    return try {
      configRef.set(
        mapOf(
          "adMobEnabled" to currentConfig.adMobEnabled,
          "bannerAdUnitId" to currentConfig.bannerAdUnitId.trim().take(120),
          "rewardedAdUnitId" to currentConfig.rewardedAdUnitId.trim().take(120),
          "startingVirtualCash" to currentConfig.startingVirtualCash.coerceIn(1000.0, 100_000_000.0),
          "rewardedAdBonus" to currentConfig.rewardedAdBonus.coerceIn(0.0, 10_000_000.0),
          "backendBaseUrl" to currentConfig.backendBaseUrl.trim().trimEnd('/').take(300),
          "enableReferralCard" to enableReferralCard,
          "referralCardTitle" to title.trim().take(80),
          "referralCardSubtitle" to subtitle.trim().take(200),
          "referralButtonLabel" to buttonLabel.trim().take(40),
          "referralUrl" to cleanUrl.take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, configRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveGatewayAndAppConfig(
    currentConfig: AppConfig,
    apiKey: String,
    webhookSecret: String,
    backendUrl: String,
    adMobEnabled: Boolean,
    bannerAdUnitId: String,
    rewardedAdUnitId: String,
    startingVirtualCash: Double,
    rewardedAdBonus: Double
  ): Result<Unit> {
    val cleanUrl = backendUrl.trim().trimEnd('/')
    val secretRef = db.collection("admin_secrets").document(ADMIN_SECRET_FAMGATEWAY_DOC_ID)
    val configRef = db.collection("app_config").document(APP_CONFIG_DOC_ID)
    return try {
      val batch = db.batch()
      batch.set(
        secretRef,
        mapOf(
          "apiKey" to apiKey.trim().take(256),
          "webhookSecret" to webhookSecret.trim().take(256),
          "backendUrl" to cleanUrl.take(300),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        configRef,
        mapOf(
          "adMobEnabled" to adMobEnabled,
          "bannerAdUnitId" to bannerAdUnitId.trim().take(120),
          "rewardedAdUnitId" to rewardedAdUnitId.trim().take(120),
          "startingVirtualCash" to startingVirtualCash.coerceIn(1000.0, 100_000_000.0),
          "rewardedAdBonus" to rewardedAdBonus.coerceIn(0.0, 10_000_000.0),
          "backendBaseUrl" to cleanUrl.take(300),
          "enableReferralCard" to currentConfig.enableReferralCard,
          "referralCardTitle" to currentConfig.referralCardTitle.trim().take(80),
          "referralCardSubtitle" to currentConfig.referralCardSubtitle.trim().take(200),
          "referralButtonLabel" to currentConfig.referralButtonLabel.trim().take(40),
          "referralUrl" to currentConfig.referralUrl.trim().take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "admin_secrets/$ADMIN_SECRET_FAMGATEWAY_DOC_ID")
      Result.failure(e)
    }
  }

  suspend fun saveAsset(asset: MarketAsset): Result<Unit> {
    val cleanSymbol = asset.symbol.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
    require(cleanSymbol.isNotBlank()) { "Valid asset symbol required" }
    val docRef = db.collection("market_assets").document(cleanSymbol)
    return try {
      docRef.set(
        mapOf(
          "symbol" to cleanSymbol,
          "name" to asset.name.trim().take(80),
          "category" to asset.category,
          "sector" to asset.sector.trim().take(40).ifBlank { "General" },
          "basePrice" to asset.basePrice.coerceIn(1.0, 10_000_000.0),
          "volatility" to asset.volatility.coerceIn(0.001, 0.25),
          "unitLabel" to asset.unitLabel.trim().take(32).ifBlank { "1 Share" },
          "active" to asset.active,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun seedDefaultAssets(): Result<Unit> {
    return try {
      val batch = db.batch()
      DefaultMarketCatalog.defaultAssets.forEach { a ->
        val ref = db.collection("market_assets").document(a.symbol)
        batch.set(
          ref,
          mapOf(
            "symbol" to a.symbol,
            "name" to a.name,
            "category" to a.category,
            "sector" to a.sector,
            "basePrice" to a.basePrice,
            "volatility" to a.volatility,
            "unitLabel" to a.unitLabel,
            "active" to a.active,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "market_assets")
      Result.failure(e)
    }
  }

  suspend fun testBackendHealth(backendUrl: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val clean = backendUrl.trim().trimEnd('/')
      require(clean.startsWith("https://") || clean.startsWith("http://")) { "Enter valid http/https backend URL" }
      val req = Request.Builder().url("$clean/health").get().build()
      httpClient.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          Result.failure(IllegalStateException("HTTP ${resp.code}: $body"))
        } else {
          Result.success("Healthy (${resp.code}): $body")
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
