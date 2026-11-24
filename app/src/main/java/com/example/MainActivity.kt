package com.example

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.SocietyRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.localserver.LocalHttpBackend
import com.example.data.localserver.ServerRuntimeState
import com.example.data.prefs.AppThemeMode
import com.example.data.prefs.LanguagePreferences
import com.example.data.prefs.LocalAuthSession
import com.example.data.prefs.ServerConfig
import com.example.data.prefs.ServerMode
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.MasjidSocietyTheme
import com.example.viewmodel.SocietyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { LanguagePreferences(context.applicationContext) }
            val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MasjidSocietyTheme(darkTheme = isDark) {
                AppNavigation(
                    isDarkTheme = isDark,
                    onRestartApp = {
                        Toast.makeText(context, "Applying server mode...", Toast.LENGTH_SHORT).show()
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        if (launchIntent != null) startActivity(launchIntent)
                        finish()
                        Process.killProcess(Process.myPid())
                    }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    auth: FirebaseAuth = Firebase.auth,
    onRestartApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val prefs = remember { LanguagePreferences(appContext) }
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val initialConfig by produceState<ServerConfig?>(initialValue = null, prefs) {
        val loaded = prefs.serverConfigFlow.first()
        ServerRuntimeState.configureEmulatorIfNeeded(appContext, loaded, auth)
        value = loaded
    }

    val readyConfig = initialConfig
    if (readyConfig == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val serverConfig by prefs.serverConfigFlow.collectAsStateWithLifecycle(initialValue = readyConfig)
    val localSession by prefs.localAuthSessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val firebaseUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val localBackend = remember { LocalHttpBackend(readyConfig, localSession) }

    LaunchedEffect(serverConfig, localSession) {
        localBackend.updateRuntime(serverConfig, localSession)
        if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER) {
            localBackend.startSyncLoop()
            localBackend.refreshSnapshot()
        } else {
            localBackend.stopSyncLoop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { localBackend.stopSyncLoop() }
    }

    var showSignInScreen by remember { mutableStateOf(false) }

    LaunchedEffect(serverConfig.mode) {
        if (serverConfig.mode == ServerMode.CLOUD_FIREBASE && auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { showSignInScreen = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val isLocalHttpMode = serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER
    val activeUid: String? = if (isLocalHttpMode) localSession?.userId else firebaseUser?.uid
    val activeEmail: String? = if (isLocalHttpMode) localSession?.email else firebaseUser?.email
    val activeDisplayName: String = when {
        isLocalHttpMode -> localSession?.fullName?.takeIf { it.isNotBlank() } ?: "Public Guest"
        firebaseUser == null -> "Public Guest"
        else -> firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: firebaseUser?.email ?: "Member"
    }
    val activeEmailVerified: Boolean = when {
        isLocalHttpMode -> localSession != null
        serverConfig.mode == ServerMode.LOCAL_EMULATOR -> firebaseUser != null
        else -> firebaseUser?.isEmailVerified ?: false
    }

    if (activeUid == null && showSignInScreen) {
        AuthScreen(
            onAuthSuccess = { showSignInScreen = false },
            onBackToPublic = { showSignInScreen = false },
            serverConfig = serverConfig,
            localBackend = localBackend,
            onLocalAuthSuccess = { newSession: LocalAuthSession ->
                scope.launch {
                    prefs.saveLocalAuthSession(newSession)
                }
            }
        )
    } else {
        val backendKey = when (serverConfig.mode) {
            ServerMode.CLOUD_FIREBASE -> "cloud"
            ServerMode.LOCAL_EMULATOR -> "emu_${serverConfig.emulatorHost}_${serverConfig.firestorePort}"
            ServerMode.LOCAL_HTTP_SERVER -> "http_${serverConfig.serverUrl}_${serverConfig.apiToken.hashCode()}"
        }
        val sessionKey = "${backendKey}_${activeUid ?: "guest_public"}"
        key(sessionKey) {
            val viewModel: SocietyViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val activeLocalBackend = if (isLocalHttpMode) localBackend else null
                        SocietyViewModel(
                            repository = SocietyRepository(db, auth, activeLocalBackend),
                            languagePrefs = LanguagePreferences(app),
                            currentUserId = activeUid,
                            currentUserEmail = activeEmail,
                            currentUserEmailVerified = activeEmailVerified,
                            localBackend = localBackend,
                            localFallbackRole = if (isLocalHttpMode) localSession?.role else null
                        )
                    }
                }
            )
            MainAppScaffold(
                viewModel = viewModel,
                isSignedIn = activeUid != null,
                isDarkTheme = isDarkTheme,
                userDisplayName = activeDisplayName,
                onRequestSignIn = { showSignInScreen = true },
                onRestartRequired = onRestartApp
            )
        }
    }
}
