package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurumtrade.shared.auth.signOutUser
import com.aurumtrade.shared.model.TerminalThemeMode
import com.aurumtrade.shared.model.UiState
import com.aurumtrade.shared.theme.AurumTradeTheme
import com.example.R
import com.example.data.FamGatewayClient
import com.example.data.TradingRepository
import com.example.ui.components.ProFeature
import com.example.ui.components.ProFeaturePreviewBottomSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChartsTradeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.PortfolioSipScreen
import com.example.ui.screens.ProProfileScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class UserTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  MARKET("market", "Market", Icons.Rounded.ShowChart, Icons.Outlined.ShowChart),
  TRADE("trade", "Trade", Icons.Rounded.CandlestickChart, Icons.Outlined.CandlestickChart),
  PORTFOLIO("portfolio", "Portfolio", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
  LEADERBOARD("leaderboard", "Ranks", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents),
  PRO("pro", "Pro", Icons.Rounded.WorkspacePremium, Icons.Outlined.WorkspacePremium)
}

@Composable
fun AurumUserApp() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AurumTradeTheme(themeMode = TerminalThemeMode.DARK_OLED) {
      AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    }
  } else {
    val uid = user.uid
    key(uid) {
      AuthenticatedTerminalRoot(currentUserId = uid)
    }
  }
}

@Composable
private fun AuthenticatedTerminalRoot(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val viewModel: TerminalViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
        val dbId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        TerminalViewModel(
          repository = TradingRepository(db),
          famGatewayClient = FamGatewayClient(),
          currentUserId = currentUserId
        )
      }
    }
  )

  val userState by viewModel.userState.collectAsStateWithLifecycle()
  val positionsState by viewModel.positionsState.collectAsStateWithLifecycle()
  val pendingOrdersState by viewModel.pendingOrdersState.collectAsStateWithLifecycle()
  val tradesState by viewModel.tradesState.collectAsStateWithLifecycle()
  val customWatchlistsState by viewModel.customWatchlistsState.collectAsStateWithLifecycle()
  val basketsState by viewModel.basketsState.collectAsStateWithLifecycle()
  val sipPlansState by viewModel.sipPlansState.collectAsStateWithLifecycle()
  val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
  val plansState by viewModel.subscriptionPlansState.collectAsStateWithLifecycle()
  val appConfig by viewModel.appConfigState.collectAsStateWithLifecycle()
  val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
  val quotes by viewModel.quotes.collectAsStateWithLifecycle()
  val signals by viewModel.signals.collectAsStateWithLifecycle()
  val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
  val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()

  val profile = (userState as? UiState.Success)?.data
  val positions = (positionsState as? UiState.Success)?.data.orEmpty()
  val pendingOrders = (pendingOrdersState as? UiState.Success)?.data.orEmpty()
  val trades = (tradesState as? UiState.Success)?.data.orEmpty()
  val customWatchlists = (customWatchlistsState as? UiState.Success)?.data.orEmpty()
  val baskets = (basketsState as? UiState.Success)?.data.orEmpty()
  val sipPlans = (sipPlansState as? UiState.Success)?.data.orEmpty()
  val alerts = (alertsState as? UiState.Success)?.data.orEmpty()

  val themeMode = remember(profile?.preferredTheme, profile?.hasValidProAccess) {
    val raw = TerminalThemeMode.safeFrom(profile?.preferredTheme ?: TerminalThemeMode.DARK_OLED.name)
    if (raw.isProOnly && profile?.hasValidProAccess != true) {
      TerminalThemeMode.DARK_OLED
    } else raw
  }

  var activeTab by remember { mutableStateOf(UserTab.MARKET) }
  var showRewardedAdDialog by remember { mutableStateOf(false) }
  var previewedProFeature by remember { mutableStateOf<ProFeature?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  if (activeTab != UserTab.MARKET) {
    BackHandler { activeTab = UserTab.MARKET }
  }

  AurumTradeTheme(themeMode = themeMode) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
          if (!useNavRail) {
            NavigationBar(
              windowInsets = WindowInsets.navigationBars,
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              UserTab.entries.forEach { tab ->
                val selected = activeTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { activeTab = tab },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  modifier = Modifier.testTag("nav_${tab.route}")
                )
              }
            }
          }
        }
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          if (useNavRail) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
              UserTab.entries.forEach { tab ->
                val selected = activeTab == tab
                NavigationRailItem(
                  selected = selected,
                  onClick = { activeTab = tab },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label
                    )
                  },
                  label = { Text(tab.label) },
                  modifier = Modifier.testTag("nav_rail_${tab.route}")
                )
              }
            }
          }

          when (activeTab) {
            UserTab.MARKET -> MarketScreen(
              profile = profile,
              quotes = quotes,
              signals = signals,
              customWatchlists = customWatchlists,
              appConfig = appConfig,
              onSelectAsset = { symbol ->
                viewModel.selectSymbol(symbol)
                activeTab = UserTab.TRADE
              },
              onToggleWatchlist = viewModel::toggleWatchlist,
              onSaveCustomWatchlist = viewModel::saveCustomWatchlist,
              onPlaceOptionOrder = viewModel::placeOptionOrder,
              onLockedFeatureClick = { previewedProFeature = it },
              onNavigateToPro = { activeTab = UserTab.PRO }
            )

            UserTab.TRADE -> ChartsTradeScreen(
              profile = profile,
              quotes = quotes,
              selectedSymbol = selectedSymbol,
              positions = positions,
              baskets = baskets,
              alerts = alerts,
              appConfig = appConfig,
              onSelectSymbol = viewModel::selectSymbol,
              onPlaceBrokerOrder = viewModel::placeBrokerOrder,
              onSaveBasket = viewModel::saveBasket,
              onExecuteBasket = viewModel::executeBasket,
              onDeleteBasket = viewModel::deleteBasket,
              onCreateAlert = viewModel::createPriceAlert,
              onDeleteAlert = viewModel::deletePriceAlert,
              onWatchAdClick = { showRewardedAdDialog = true },
              onLockedFeatureClick = { previewedProFeature = it }
            )

            UserTab.PORTFOLIO -> PortfolioSipScreen(
              profile = profile,
              positions = positions,
              pendingOrders = pendingOrders,
              trades = trades,
              sipPlans = sipPlans,
              quotes = quotes,
              onClosePosition = viewModel::placeQuickSellPosition,
              onCancelPendingOrder = viewModel::cancelPendingOrder,
              onResetPortfolio = viewModel::resetPortfolio,
              onRefillProCash = { viewModel.refillProVirtualCash(100_000.0) },
              onCreateSip = viewModel::createSipPlan,
              onRunSipNow = viewModel::runSipInstallmentNow,
              onDeleteSip = viewModel::deleteSipPlan,
              onUpdateTradeTag = viewModel::updateTradeJournalTag,
              onExportCsvUri = { viewModel.exportTradesCsvUri(context) },
              onExportPdfUri = { viewModel.exportTradesPdfUri(context) },
              onLockedFeatureClick = { previewedProFeature = it }
            )

            UserTab.LEADERBOARD -> LeaderboardScreen(
              currentUserId = currentUserId,
              leaderboardState = leaderboardState,
              onSyncRank = viewModel::syncUserLeaderboard
            )

            UserTab.PRO -> ProProfileScreen(
              profile = profile,
              plansState = plansState,
              appConfig = appConfig,
              checkoutState = checkoutState,
              onSelectTheme = viewModel::setTheme,
              onSubscribeClick = viewModel::startFamGatewayCheckout,
              onWatchAdClick = { showRewardedAdDialog = true },
              onLockedFeatureClick = { previewedProFeature = it },
              onSignOut = {
                signOutUser(
                  credentialManager = CredentialManager.create(context),
                  scope = scope
                )
              }
            )
          }
        }
      }

      previewedProFeature?.let { feature ->
        ProFeaturePreviewBottomSheet(
          feature = feature,
          onDismiss = { previewedProFeature = null },
          onUnlockPro = { activeTab = UserTab.PRO }
        )
      }

      // Full-screen In-App FamGateway WebView Checkout
      when (val cs = checkoutState) {
        is CheckoutState.WebViewOpen -> {
          FamGatewayWebViewOverlay(
            checkoutUrl = cs.checkoutUrl,
            planName = cs.planName,
            amountInr = cs.amountInr,
            onClose = viewModel::closeCheckout
          )
        }
        is CheckoutState.ProActivated -> {
          ProActivatedCelebrationOverlay(
            planName = cs.planName,
            expiresAtMillis = cs.expiresAtMillis,
            onContinue = viewModel::closeCheckout
          )
        }
        else -> Unit
      }

      if (showRewardedAdDialog) {
        RewardedAdDialog(
          appConfig = appConfig,
          onRewardEarned = {
            showRewardedAdDialog = false
            viewModel.claimRewardedAdBonus()
          },
          onDismiss = { showRewardedAdDialog = false }
        )
      }
    }
  }
}
