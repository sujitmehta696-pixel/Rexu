package com.aurumtrade.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aurumtrade.shared.R
import com.aurumtrade.shared.model.TerminalThemeMode

val SpaceGroteskFontFamily = FontFamily(
  Font(R.font.space_grotesk, FontWeight.Normal),
  Font(R.font.space_grotesk, FontWeight.SemiBold),
  Font(R.font.space_grotesk, FontWeight.Bold)
)

val InterFontFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val TabularMonoStyle = TextStyle(
  fontFamily = JetBrainsMonoFontFamily,
  fontFeatureSettings = "tnum"
)

@Immutable
data class AurumTradingColors(
  val bullishEmerald: Color,
  val bullishSoft: Color,
  val bearishCrimson: Color,
  val bearishSoft: Color,
  val aurumGold: Color,
  val aurumGoldSoft: Color,
  val cardBorder: Color,
  val chartGrid: Color,
  val ema20Amber: Color,
  val ema50Cyan: Color,
  val sma200Violet: Color,
  val vwapPink: Color,
  val isMidnightGold: Boolean,
  val isDark: Boolean
)

val LocalAurumTradingColors = staticCompositionLocalOf {
  AurumTradingColors(
    bullishEmerald = Color(0xFF00E59B),
    bullishSoft = Color(0x2200E59B),
    bearishCrimson = Color(0xFFFF4D6A),
    bearishSoft = Color(0x22FF4D6A),
    aurumGold = Color(0xFFF5C451),
    aurumGoldSoft = Color(0x26F5C451),
    cardBorder = Color(0xFF1B2438),
    chartGrid = Color(0xFF151C2C),
    ema20Amber = Color(0xFFFFB703),
    ema50Cyan = Color(0xFF38BDF8),
    sma200Violet = Color(0xFFA78BFA),
    vwapPink = Color(0xFFF472B6),
    isMidnightGold = false,
    isDark = true
  )
}

private val DarkOledColorScheme = darkColorScheme(
  primary = Color(0xFF00E59B),
  onPrimary = Color(0xFF031B12),
  primaryContainer = Color(0xFF0A3327),
  onPrimaryContainer = Color(0xFF7AF5CB),
  secondary = Color(0xFFF5C451),
  onSecondary = Color(0xFF211600),
  secondaryContainer = Color(0xFF332609),
  onSecondaryContainer = Color(0xFFFFDF8E),
  tertiary = Color(0xFF38BDF8),
  onTertiary = Color(0xFF031926),
  background = Color(0xFF04060B),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF0B101B),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF121929),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF243047),
  error = Color(0xFFFF4D6A),
  onError = Color(0xFF2D040C)
)

private val MidnightGoldColorScheme = darkColorScheme(
  primary = Color(0xFFFFD166),
  onPrimary = Color(0xFF1F1500),
  primaryContainer = Color(0xFF3B2A06),
  onPrimaryContainer = Color(0xFFFFE7A3),
  secondary = Color(0xFF00E59B),
  onSecondary = Color(0xFF031B12),
  secondaryContainer = Color(0xFF0C3326),
  onSecondaryContainer = Color(0xFF7AF5CB),
  tertiary = Color(0xFFE09F3E),
  onTertiary = Color(0xFF1E1200),
  background = Color(0xFF060503),
  onBackground = Color(0xFFFAF3E0),
  surface = Color(0xFF120F09),
  onSurface = Color(0xFFFAF3E0),
  surfaceVariant = Color(0xFF1D170E),
  onSurfaceVariant = Color(0xFFC2B497),
  outline = Color(0xFF3B301B),
  error = Color(0xFFFF5A76),
  onError = Color(0xFF2D040C)
)

private val NeonTerminalColorScheme = darkColorScheme(
  primary = Color(0xFF00F0FF),
  onPrimary = Color(0xFF001F24),
  primaryContainer = Color(0xFF062C33),
  onPrimaryContainer = Color(0xFF90F7FF),
  secondary = Color(0xFF39FF14),
  onSecondary = Color(0xFF031E00),
  secondaryContainer = Color(0xFF0C3308),
  onSecondaryContainer = Color(0xFFB4FFA6),
  tertiary = Color(0xFFFF2E93),
  onTertiary = Color(0xFF290015),
  background = Color(0xFF02040A),
  onBackground = Color(0xFFE6FBFF),
  surface = Color(0xFF080E1A),
  onSurface = Color(0xFFE6FBFF),
  surfaceVariant = Color(0xFF0F182B),
  onSurfaceVariant = Color(0xFF7DD3FC),
  outline = Color(0xFF164E63),
  error = Color(0xFFFF3366),
  onError = Color(0xFF2B000D)
)

private val DaylightColorScheme = lightColorScheme(
  primary = Color(0xFF008F63),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFD1FAE5),
  onPrimaryContainer = Color(0xFF023E2B),
  secondary = Color(0xFFB7791F),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFFEF3C7),
  onSecondaryContainer = Color(0xFF452803),
  tertiary = Color(0xFF0284C7),
  onTertiary = Color(0xFFFFFFFF),
  background = Color(0xFFF4F7FB),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE8EEF6),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = Color(0xFFD92D4C),
  onError = Color(0xFFFFFFFF)
)

val AurumTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.5).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.25).sp
  ),
  titleLarge = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 16.sp
  ),
  labelSmall = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontFeatureSettings = "tnum"
  )
)

@Composable
fun AurumTradeTheme(
  themeMode: TerminalThemeMode = TerminalThemeMode.DARK_OLED,
  content: @Composable () -> Unit
) {
  val colorScheme = when (themeMode) {
    TerminalThemeMode.DARK_OLED -> DarkOledColorScheme
    TerminalThemeMode.LIGHT -> DaylightColorScheme
    TerminalThemeMode.MIDNIGHT_GOLD -> MidnightGoldColorScheme
    TerminalThemeMode.NEON_TERMINAL -> NeonTerminalColorScheme
  }

  val tradingColors = when (themeMode) {
    TerminalThemeMode.DARK_OLED -> AurumTradingColors(
      bullishEmerald = Color(0xFF00E59B),
      bullishSoft = Color(0x2200E59B),
      bearishCrimson = Color(0xFFFF4D6A),
      bearishSoft = Color(0x22FF4D6A),
      aurumGold = Color(0xFFF5C451),
      aurumGoldSoft = Color(0x26F5C451),
      cardBorder = Color(0xFF1A2338),
      chartGrid = Color(0xFF141B2D),
      ema20Amber = Color(0xFFFFB703),
      ema50Cyan = Color(0xFF38BDF8),
      sma200Violet = Color(0xFFA78BFA),
      vwapPink = Color(0xFFF472B6),
      isMidnightGold = false,
      isDark = true
    )
    TerminalThemeMode.MIDNIGHT_GOLD -> AurumTradingColors(
      bullishEmerald = Color(0xFF00E59B),
      bullishSoft = Color(0x2200E59B),
      bearishCrimson = Color(0xFFFF5A76),
      bearishSoft = Color(0x22FF5A76),
      aurumGold = Color(0xFFFFD166),
      aurumGoldSoft = Color(0x2BFFD166),
      cardBorder = Color(0xFF3B301B),
      chartGrid = Color(0xFF211A0E),
      ema20Amber = Color(0xFFFFD166),
      ema50Cyan = Color(0xFF60A5FA),
      sma200Violet = Color(0xFFC084FC),
      vwapPink = Color(0xFFFB7185),
      isMidnightGold = true,
      isDark = true
    )
    TerminalThemeMode.NEON_TERMINAL -> AurumTradingColors(
      bullishEmerald = Color(0xFF39FF14),
      bullishSoft = Color(0x2439FF14),
      bearishCrimson = Color(0xFFFF2E93),
      bearishSoft = Color(0x24FF2E93),
      aurumGold = Color(0xFF00F0FF),
      aurumGoldSoft = Color(0x2400F0FF),
      cardBorder = Color(0xFF164E63),
      chartGrid = Color(0xFF0B2239),
      ema20Amber = Color(0xFFFDE047),
      ema50Cyan = Color(0xFF00F0FF),
      sma200Violet = Color(0xFFD946EF),
      vwapPink = Color(0xFFFF79C6),
      isMidnightGold = false,
      isDark = true
    )
    TerminalThemeMode.LIGHT -> AurumTradingColors(
      bullishEmerald = Color(0xFF008F63),
      bullishSoft = Color(0x1C008F63),
      bearishCrimson = Color(0xFFD92D4C),
      bearishSoft = Color(0x1CD92D4C),
      aurumGold = Color(0xFFB7791F),
      aurumGoldSoft = Color(0x22B7791F),
      cardBorder = Color(0xFFD8E1EC),
      chartGrid = Color(0xFFE5ECF4),
      ema20Amber = Color(0xFFD97706),
      ema50Cyan = Color(0xFF0284C7),
      sma200Violet = Color(0xFF7C3AED),
      vwapPink = Color(0xFFDB2777),
      isMidnightGold = false,
      isDark = false
    )
  }

  CompositionLocalProvider(LocalAurumTradingColors provides tradingColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AurumTypography,
      content = content
    )
  }
}
