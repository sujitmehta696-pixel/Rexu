package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.aurumtrade.shared.auth.GoogleSignInButton
import com.aurumtrade.shared.auth.attemptAutoSignIn
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.aurumtrade.shared.theme.TabularMonoStyle
import com.example.R

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val trading = LocalAurumTradingColors.current
  val webClientId = remember(context) {
    runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
  }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      webClientId = webClientId,
      credentialManager = CredentialManager.create(context),
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              trading.aurumGold.copy(alpha = 0.14f),
              trading.bullishEmerald.copy(alpha = 0.08f),
              Color.Transparent
            )
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 460.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(trading.aurumGoldSoft)
            .border(1.5.dp, trading.aurumGold.copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.CandlestickChart,
            contentDescription = "Aurum Trade",
            tint = trading.aurumGold,
            modifier = Modifier.size(40.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "AURUM TRADE",
            style = TabularMonoStyle.copy(
              fontSize = 12.sp,
              letterSpacing = 2.4.sp,
              color = trading.aurumGold,
              fontWeight = FontWeight.Bold
            )
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Luxury Paper Trading Terminal",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = "Practice NSE Stocks, Nifty 50, 24K Digital Gold & Auto-SIP compounding with ₹1,00,000 virtual cash.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }

        // Simulated market preview pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TickerPreviewPill(
            symbol = "NIFTY 50",
            price = "24,368.40",
            delta = "+0.84%",
            positive = true,
            modifier = Modifier.weight(1f)
          )
          TickerPreviewPill(
            symbol = "GOLD 24K",
            price = "₹7,785.00",
            delta = "+1.12%",
            positive = true,
            modifier = Modifier.weight(1f)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, trading.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          FeatureRow(Icons.Rounded.AutoGraph, "Real-time simulated NSE, Gold & Mutual Fund ticks")
          FeatureRow(Icons.Rounded.CandlestickChart, "Multi-timeframe Candlesticks, EMA 20/50 & RSI")
          FeatureRow(Icons.Rounded.WorkspacePremium, "Instant FamGateway Auto-UPI Pro unlock")
        }

        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
          )
        }

        GoogleSignInButton(
          webClientId = webClientId,
          label = "Continue with Google",
          onAuthSuccess = {
            errorMessage = null
            onAuthSuccess()
          },
          onAuthError = { msg -> errorMessage = msg }
        )
      }
    }
  }
}

@Composable
private fun TickerPreviewPill(
  symbol: String,
  price: String,
  delta: String,
  positive: Boolean,
  modifier: Modifier = Modifier
) {
  val trading = LocalAurumTradingColors.current
  val accent = if (positive) trading.bullishEmerald else trading.bearishCrimson
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, trading.cardBorder, RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Text(
      text = symbol,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = price,
      style = TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    )
    Text(
      text = delta,
      style = TabularMonoStyle.copy(fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold)
    )
  }
}

@Composable
private fun FeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String
) {
  val trading = LocalAurumTradingColors.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = trading.aurumGold,
      modifier = Modifier.size(18.dp)
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
