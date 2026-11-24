package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.prefs.AppThemeMode
import com.example.data.prefs.LanguagePreferences
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.MasjidSocietyTheme
import com.example.viewmodel.SocietyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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
                AppNavigation(isDarkTheme = isDark)
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showSignInScreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { showSignInScreen = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null && showSignInScreen) {
        AuthScreen(
            onAuthSuccess = { showSignInScreen = false },
            onBackToPublic = { showSignInScreen = false }
        )
    } else {
        val sessionKey = user?.uid ?: "guest_public"
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
                        SocietyViewModel(
                            repository = SocietyRepository(db, auth),
                            languagePrefs = LanguagePreferences(app),
                            currentUserId = user?.uid,
                            currentUserEmail = user?.email,
                            currentUserEmailVerified = user?.isEmailVerified ?: false
                        )
                    }
                }
            )
            val displayName = if (user == null) {
                "Public Guest"
            } else {
                user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Member"
            }
            MainAppScaffold(
                viewModel = viewModel,
                isSignedIn = user != null,
                isDarkTheme = isDarkTheme,
                userDisplayName = displayName,
                onRequestSignIn = { showSignInScreen = true }
            )
        }
    }
}
