package com.aurumtrade.shared.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlin.math.abs

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write")
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

fun formatInr(amount: Double, showSign: Boolean = false, decimals: Int = 2): String {
  val sign = when {
    amount < 0 -> "-"
    showSign && amount > 0 -> "+"
    else -> ""
  }
  val absVal = abs(amount)
  val intPart = absVal.toLong()
  val frac = absVal - intPart

  val intStr = formatIndianInteger(intPart)
  val fracStr = if (decimals > 0) {
    val symbols = DecimalFormatSymbols(Locale.US)
    val pattern = "." + "0".repeat(decimals)
    DecimalFormat(pattern, symbols).format(frac).drop(1) // ".xx"
  } else ""
  return "${sign}₹${intStr}${if (decimals > 0) ".$fracStr" else ""}"
}

private fun formatIndianInteger(number: Long): String {
  val s = number.toString()
  if (s.length <= 3) return s
  val lastThree = s.takeLast(3)
  val remaining = s.dropLast(3)
  val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
  return "$grouped,$lastThree"
}

fun formatCompactInr(amount: Double): String {
  val sign = if (amount < 0) "-" else ""
  val a = abs(amount)
  return when {
    a >= 10_000_000 -> String.format(Locale.US, "%s₹%.2fCr", sign, a / 10_000_000.0)
    a >= 100_000 -> String.format(Locale.US, "%s₹%.2fL", sign, a / 100_000.0)
    a >= 1_000 -> String.format(Locale.US, "%s₹%.1fK", sign, a / 1_000.0)
    else -> formatInr(amount, decimals = 0)
  }
}

fun formatPct(pct: Double, showSign: Boolean = true): String {
  val sign = if (showSign && pct > 0) "+" else ""
  return String.format(Locale.US, "%s%.2f%%", sign, pct)
}
