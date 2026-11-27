package com.aurumtrade.admin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurumtrade.admin.R
import com.aurumtrade.admin.data.AdminRepository
import com.aurumtrade.shared.auth.GoogleSignInButton
import com.aurumtrade.shared.auth.attemptAutoSignIn
import com.aurumtrade.shared.auth.signOutUser
import com.aurumtrade.shared.model.ADMIN_EMAIL
import com.aurumtrade.shared.model.TerminalThemeMode
import com.aurumtrade.shared.theme.AurumTradeTheme
import com.aurumtrade.shared.theme.LocalAurumTradingColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class AdminTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  PLANS("plans", "Plans", Icons.Rounded.WorkspacePremium, Icons.Outlined.WorkspacePremium),
  GATEWAY("gateway", "Gateway", Icons.Rounded.Key, Icons.Outlined.Key),
  ASSETS("assets", "Assets", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2),
  USERS("users", "Users", Icons.Rounded.Group, Icons.Outlined.Group)
}

@Composable
fun AurumAdminApp() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  AurumTradeTheme(themeMode = TerminalThemeMode.MIDNIGHT_GOLD) {
    val user = currentUser
    when {
      user == null -> {
        AdminAuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
      }
      !user.isEmailVerified || !user.email.equals(ADMIN_EMAIL, ignoreCase = true) -> {
        AccessDeniedScreen(signedInEmail = user.email ?: "Unknown")
      }
      else -> {
        key(user.uid) {
          AdminConsoleRoot(adminUid = user.uid)
        }
      }
    }
  }
}

@Composable
private fun AdminAuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val trading = LocalAurumTradingColors.current
  val webClientId = remember(context) {
    runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
  }
  var errorMsg by remember { mutableStateOf<String?>(null) }

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

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(trading.aurumGoldSoft)
            .border(1.5.dp, trading.aurumGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Rounded.AdminPanelSettings,
            contentDescription = "Aurum Admin",
            tint = trading.aurumGold,
            modifier = Modifier.size(38.dp)
          )
        }
        Text("Aurum Admin Console", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
          text = "Restricted strictly to verified administrator ($ADMIN_EMAIL). Manage FamGateway secrets, Pro plans, AdMob & assets.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
        if (!errorMsg.isNullOrBlank()) {
          Text(errorMsg!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        GoogleSignInButton(
          webClientId = webClientId,
          label = "Admin Sign In with Google",
          onAuthSuccess = {
            errorMsg = null
            onAuthSuccess()
          },
          onAuthError = { errorMsg = it }
        )
      }
    }
  }
}

@Composable
private fun AccessDeniedScreen(signedInEmail: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val trading = LocalAurumTradingColors.current

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(28.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        Icons.Rounded.Block,
        contentDescription = "Access Denied",
        tint = trading.bearishCrimson,
        modifier = Modifier.size(56.dp)
      )
      Spacer(Modifier.height(16.dp))
      Text("Admin Access Denied", style = MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(8.dp))
      Text(
        text = "Signed in as $signedInEmail.\nThis APK is strictly locked to $ADMIN_EMAIL.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(22.dp))
      OutlinedButton(
        onClick = {
          signOutUser(CredentialManager.create(context), scope)
        },
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Sign Out & Switch Account")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminConsoleRoot(adminUid: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val viewModel: AdminViewModel = viewModel(
    key = adminUid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
        AdminViewModel(AdminRepository(db), adminUid)
      }
    }
  )

  val plansState by viewModel.plansState.collectAsStateWithLifecycle()
  val gatewaySecretState by viewModel.gatewaySecretState.collectAsStateWithLifecycle()
  val appConfigState by viewModel.appConfigState.collectAsStateWithLifecycle()
  val assetsState by viewModel.assetsState.collectAsStateWithLifecycle()
  val usersState by viewModel.usersState.collectAsStateWithLifecycle()

  var activeTab by remember { mutableStateOf(AdminTab.PLANS) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { snackbarHostState.showSnackbar(it) }
  }

  if (activeTab != AdminTab.PLANS) {
    BackHandler { activeTab = AdminTab.PLANS }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Aurum Admin",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
          },
          actions = {
            IconButton(
              onClick = { signOutUser(CredentialManager.create(context), scope) },
              modifier = Modifier.testTag("admin_sign_out")
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign Out")
            }
          }
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            AdminTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("admin_nav_${tab.route}")
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
        if (useRail) {
          NavigationRail {
            AdminTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label
                  )
                },
                label = { Text(tab.label) }
              )
            }
          }
        }

        when (activeTab) {
          AdminTab.PLANS -> AdminPlansScreen(
            plansState = plansState,
            onSavePlan = viewModel::savePlan,
            onSyncAllPlans = viewModel::saveAndSyncPlans,
            onDeletePlan = viewModel::deletePlan,
            onSeedDefaults = viewModel::seedDefaultPlans
          )
          AdminTab.GATEWAY -> AdminGatewayConfigScreen(
            secretState = gatewaySecretState,
            appConfigState = appConfigState,
            onSaveAll = viewModel::saveGatewayAndConfig,
            onSaveReferral = viewModel::saveReferralSettings,
            onTestHealth = viewModel::testBackendHealth
          )
          AdminTab.ASSETS -> AdminAssetsScreen(
            assetsState = assetsState,
            onSaveAsset = viewModel::saveAsset,
            onSeedDefaults = viewModel::seedDefaultAssets
          )
          AdminTab.USERS -> AdminUsersScreen(usersState = usersState)
        }
      }
    }
  }
}
