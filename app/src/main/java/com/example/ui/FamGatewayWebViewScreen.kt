package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.aurumtrade.shared.util.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UPI_SCHEMES = setOf("upi", "intent", "phonepe", "tez", "paytmmp", "famapp")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FamGatewayWebViewOverlay(
  checkoutUrl: String,
  planName: String,
  amountInr: Double,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val trading = LocalAurumTradingColors.current

  BackHandler(onBack = onClose)

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top secure checkout header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = "Secure UPI Checkout",
            tint = trading.bullishEmerald,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(8.dp))
          Column {
            Text(
              text = "FamGateway Auto-UPI • $planName",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = "${formatInr(amountInr)} • Auto-verifying every 3.5s",
              style = TabularMonoStyle.copy(
                fontSize = 11.sp,
                color = trading.aurumGold
              )
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = trading.bullishEmerald
          )
          Spacer(Modifier.width(8.dp))
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("close_checkout_webview")
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Cancel Checkout"
            )
          }
        }
      }

      // Strictly In-App WebView: NEVER open Chrome for http/https; ONLY intercept UPI deep links
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f),
        factory = { ctx ->
          WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()

                if (scheme == "http" || scheme == "https") {
                  // Strictly keep all web pages inside this In-App WebView
                  return false
                }

                if (scheme in UPI_SCHEMES) {
                  launchUpiIntent(context, uri)
                  return true
                }
                return false
              }
            }
            loadUrl(checkoutUrl)
          }
        },
        update = { webView ->
          if (webView.url != checkoutUrl && webView.url == null) {
            webView.loadUrl(checkoutUrl)
          }
        }
      )
    }
  }
}

private fun launchUpiIntent(context: android.content.Context, uri: Uri) {
  runCatching {
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val intent = if (scheme == "intent") {
      Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
    } else {
      Intent(Intent.ACTION_VIEW, uri)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }
}

@Composable
fun ProActivatedCelebrationOverlay(
  planName: String,
  expiresAtMillis: Long,
  onContinue: () -> Unit
) {
  val trading = LocalAurumTradingColors.current
  val expiryFormatted = if (expiresAtMillis > 0L) {
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiresAtMillis))
  } else "Active"

  BackHandler(onBack = onContinue)

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(28.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(92.dp)
          .clip(CircleShape)
          .background(trading.aurumGoldSoft)
          .border(2.dp, trading.aurumGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.WorkspacePremium,
          contentDescription = "Pro Activated",
          tint = trading.aurumGold,
          modifier = Modifier.size(48.dp)
        )
      }

      Spacer(Modifier.height(22.dp))

      Text(
        text = "Pro Activated!",
        style = MaterialTheme.typography.displayLarge.copy(
          color = trading.aurumGold,
          fontWeight = FontWeight.Bold
        ),
        textAlign = TextAlign.Center
      )

      Spacer(Modifier.height(10.dp))

      Text(
        text = "$planName unlocked automatically via FamGateway UPI. Valid until $expiryFormatted.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(Modifier.height(22.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val unlocked = listOf(
          "Interactive 1m/5m/15m/1D Candlesticks + EMA & RSI",
          "5x Intraday Leverage & Auto SL/TP Bracket Orders",
          "Auto-SIP Wealth Simulator & 10Y Projection",
          "Pro Analytics, Midnight Gold Theme & 100% Ad-Free"
        )
        unlocked.forEach { perk ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = trading.bullishEmerald,
              modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(text = perk, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      Spacer(Modifier.height(26.dp))

      Button(
        onClick = onContinue,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = trading.aurumGold,
          contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
          .testTag("pro_activated_continue_button")
      ) {
        Text(
          text = "Enter Pro Terminal",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}
