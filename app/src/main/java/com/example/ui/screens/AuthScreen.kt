package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.localserver.LocalHttpBackend
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.LocalAuthSession
import com.example.data.prefs.ServerConfig
import com.example.data.prefs.ServerMode
import com.example.ui.components.LanguageToggleChip
import com.example.ui.i18n.stringsFor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onBackToPublic: (() -> Unit)? = null,
    serverConfig: ServerConfig = ServerConfig(),
    localBackend: LocalHttpBackend? = null,
    onLocalAuthSuccess: ((LocalAuthSession) -> Unit)? = null
) {
    if (onBackToPublic != null) {
        BackHandler(onBack = onBackToPublic)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var language by remember { mutableStateOf(AppLanguage.BANGLA) }
    val strings = stringsFor(language)

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var createEmulatorAccount by remember { mutableStateOf(false) }

    LaunchedEffect(serverConfig.mode) {
        if (serverConfig.mode == ServerMode.CLOUD_FIREBASE) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageToggleChip(
                    language = language,
                    onToggle = {
                        language = if (language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_masjid_banner),
                    contentDescription = strings.signInTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF04291E).copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD0F0E3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = if (serverConfig.mode == ServerMode.CLOUD_FIREBASE) Icons.Filled.AccountBalance else Icons.Filled.Dns,
                            contentDescription = strings.signInTitle,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(32.dp)
                        )
                    }

                    Text(
                        text = if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER) strings.localSignInTitle else strings.signInTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = when (serverConfig.mode) {
                            ServerMode.LOCAL_HTTP_SERVER -> "${strings.localSignInDesc}\n(${serverConfig.serverUrl})"
                            ServerMode.LOCAL_EMULATOR -> "${strings.emulatorSignInDesc}\n(${serverConfig.emulatorHost}:${serverConfig.authPort})"
                            ServerMode.CLOUD_FIREBASE -> strings.signInDesc
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero-Trust RBAC",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (serverConfig.mode) {
                                    ServerMode.CLOUD_FIREBASE -> "Cloud Sync"
                                    ServerMode.LOCAL_EMULATOR -> "Local Emulator"
                                    ServerMode.LOCAL_HTTP_SERVER -> "Local Server"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER || serverConfig.mode == ServerMode.LOCAL_EMULATOR) {
                        if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER || createEmulatorAccount) {
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                label = { Text(strings.fullNameInputLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("local_full_name_input")
                            )
                        }
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(strings.emailInputLabel) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("local_email_input")
                        )
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(strings.passwordOrPinLabel) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("local_password_input")
                        )

                        if (serverConfig.mode == ServerMode.LOCAL_EMULATOR) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.createAccountToggle, style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = createEmulatorAccount, onCheckedChange = { createEmulatorAccount = it })
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                val email = emailInput.trim()
                                val pass = passwordInput
                                if (email.isBlank() || pass.isBlank()) {
                                    errorMessage = "Email and Password / PIN are required"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    if (serverConfig.mode == ServerMode.LOCAL_HTTP_SERVER) {
                                        val backend = localBackend ?: LocalHttpBackend(serverConfig)
                                        val res = backend.signInLocalServer(fullNameInput, email, pass)
                                        isLoading = false
                                        res.onSuccess { session ->
                                            onLocalAuthSuccess?.invoke(session)
                                            onAuthSuccess()
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: "Local server login failed"
                                        }
                                    } else {
                                        try {
                                            val auth = Firebase.auth
                                            if (createEmulatorAccount) {
                                                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                                                if (fullNameInput.isNotBlank()) {
                                                    result.user?.updateProfile(
                                                        userProfileChangeRequest { displayName = fullNameInput.trim() }
                                                    )?.await()
                                                }
                                            } else {
                                                auth.signInWithEmailAndPassword(email, pass).await()
                                            }
                                            isLoading = false
                                            onAuthSuccess()
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = e.localizedMessage ?: "Emulator sign-in failed"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("local_sign_in_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(strings.signInSubmitButton, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    if (serverConfig.mode != ServerMode.LOCAL_HTTP_SERVER) {
                        val googleButtonModifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                        val clickGoogle = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                        if (serverConfig.mode == ServerMode.CLOUD_FIREBASE) {
                            Button(
                                onClick = clickGoogle,
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = googleButtonModifier
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(strings.signInButton, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = clickGoogle,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = googleButtonModifier
                            ) {
                                Text(strings.signInButton, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    if (onBackToPublic != null) {
                        TextButton(
                            onClick = onBackToPublic,
                            modifier = Modifier.testTag("back_to_public_button")
                        ) {
                            Text(strings.backToPublic)
                        }
                    }
                }
            }
        }
    }
}
