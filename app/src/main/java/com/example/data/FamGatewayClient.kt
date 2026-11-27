package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FamOrderResponse(
  val orderId: String,
  val checkoutUrl: String,
  val amountInr: Double,
  val planName: String,
  val validityDays: Long
)

data class FamStatusResponse(
  val orderId: String,
  val status: String,
  val verified: Boolean,
  val planName: String = "",
  val planExpiresAtMillis: Long = 0L
)

class FamGatewayClient {
  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun createOrder(
    backendBaseUrl: String,
    idToken: String,
    planId: String
  ): Result<FamOrderResponse> = withContext(Dispatchers.IO) {
    try {
      val base = backendBaseUrl.trim().trimEnd('/')
      require(base.startsWith("https://") || base.startsWith("http://")) {
        "Backend URL is not configured. Admin must set Backend URL in Aurum Admin -> Gateway."
      }
      val bodyJson = JSONObject().apply {
        put("planId", planId)
      }.toString()

      val request = Request.Builder()
        .url("$base/api/famgateway/create-order")
        .addHeader("Authorization", "Bearer $idToken")
        .post(bodyJson.toRequestBody(jsonMediaType))
        .build()

      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        val json = runCatching { JSONObject(raw) }.getOrNull()
        if (!response.isSuccessful) {
          val msg = json?.optString("error")?.takeIf { it.isNotBlank() }
            ?: "Backend returned HTTP ${response.code}"
          return@withContext Result.failure(IllegalStateException(msg))
        }
        val orderId = json?.optString("orderId").orEmpty()
        val checkoutUrl = json?.optString("checkout_url").orEmpty()
        if (orderId.isBlank() || checkoutUrl.isBlank()) {
          return@withContext Result.failure(IllegalStateException("Invalid order response from server"))
        }
        Result.success(
          FamOrderResponse(
            orderId = orderId,
            checkoutUrl = checkoutUrl,
            amountInr = json?.optDouble("amountInr", 0.0) ?: 0.0,
            planName = json?.optString("planName", "Pro Plan") ?: "Pro Plan",
            validityDays = json?.optLong("validityDays", 30L) ?: 30L
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun checkOrderStatus(
    backendBaseUrl: String,
    idToken: String,
    orderId: String
  ): Result<FamStatusResponse> = withContext(Dispatchers.IO) {
    try {
      val base = backendBaseUrl.trim().trimEnd('/')
      val request = Request.Builder()
        .url("$base/api/famgateway/status?orderId=${java.net.URLEncoder.encode(orderId, "UTF-8")}")
        .addHeader("Authorization", "Bearer $idToken")
        .get()
        .build()

      httpClient.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        val json = runCatching { JSONObject(raw) }.getOrNull()
        if (!response.isSuccessful) {
          val msg = json?.optString("error")?.takeIf { it.isNotBlank() }
            ?: "Status HTTP ${response.code}"
          return@withContext Result.failure(IllegalStateException(msg))
        }
        val status = json?.optString("status", "PENDING") ?: "PENDING"
        val verified = json?.optBoolean("verified", false) ?: false
        Result.success(
          FamStatusResponse(
            orderId = orderId,
            status = status,
            verified = verified,
            planName = json?.optString("planName", "") ?: "",
            planExpiresAtMillis = json?.optLong("planExpiresAtMillis", 0L) ?: 0L
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
