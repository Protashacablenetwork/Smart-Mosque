package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.localserver.ConnectionTestResult
import com.example.data.localserver.ServerRuntimeState
import com.example.data.prefs.ServerConfig
import com.example.data.prefs.ServerMode
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.util.ReportExporter

@Composable
fun ServerHostingCard(
    strings: AppStrings,
    serverConfig: ServerConfig,
    testResult: ConnectionTestResult?,
    isTestingConnection: Boolean,
    onTestConnection: (url: String, token: String) -> Unit,
    onSaveServerConfig: (ServerConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mode by remember(serverConfig) { mutableStateOf(serverConfig.mode) }
    var serverUrl by remember(serverConfig) { mutableStateOf(serverConfig.serverUrl) }
    var apiToken by remember(serverConfig) { mutableStateOf(serverConfig.apiToken) }
    var emulatorHost by remember(serverConfig) { mutableStateOf(serverConfig.emulatorHost) }
    var firestorePort by remember(serverConfig) { mutableStateOf(serverConfig.firestorePort.toString()) }
    var authPort by remember(serverConfig) { mutableStateOf(serverConfig.authPort.toString()) }

    val activeLabel = when (serverConfig.mode) {
        ServerMode.CLOUD_FIREBASE -> strings.serverModeCloud
        ServerMode.LOCAL_EMULATOR -> "${strings.serverModeEmulator} (${serverConfig.emulatorHost})"
        ServerMode.LOCAL_HTTP_SERVER -> "${strings.serverModeLocalHttp} (${serverConfig.serverUrl})"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Dns, contentDescription = strings.serverSettingsTitle, tint = MaterialTheme.colorScheme.primary)
                Text(strings.serverSettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(strings.serverSettingsDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(
                text = activeLabel,
                containerColor = InfoBlueContainer,
                contentColor = InfoBlue
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == ServerMode.CLOUD_FIREBASE,
                    onClick = { mode = ServerMode.CLOUD_FIREBASE },
                    leadingIcon = { Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.serverModeCloud) },
                    modifier = Modifier.testTag("server_mode_cloud_chip")
                )
                FilterChip(
                    selected = mode == ServerMode.LOCAL_HTTP_SERVER,
                    onClick = { mode = ServerMode.LOCAL_HTTP_SERVER },
                    leadingIcon = { Icon(Icons.Filled.Lan, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.serverModeLocalHttp) },
                    modifier = Modifier.testTag("server_mode_local_http_chip")
                )
                FilterChip(
                    selected = mode == ServerMode.LOCAL_EMULATOR,
                    onClick = { mode = ServerMode.LOCAL_EMULATOR },
                    leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.serverModeEmulator) },
                    modifier = Modifier.testTag("server_mode_emulator_chip")
                )
            }

            if (mode == ServerMode.LOCAL_HTTP_SERVER) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Emulator" to "http://10.0.2.2:3000",
                        "Localhost" to "http://127.0.0.1:3000",
                        "LAN PC" to "http://192.168.1.10:3000"
                    ).forEach { (label, preset) ->
                        FilterChip(
                            selected = serverUrl.trim().trimEnd('/') == preset,
                            onClick = { serverUrl = preset },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(strings.serverUrlLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppMonoFontFamily),
                    modifier = Modifier.fillMaxWidth().testTag("local_server_url_input")
                )
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text(strings.serverTokenLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("local_server_token_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onTestConnection(serverUrl, apiToken) },
                        enabled = !isTestingConnection && serverUrl.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("test_local_server_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(strings.testConnectionButton, maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            ReportExporter.shareLocalServerScript(context, ServerRuntimeState.companionNodeServerScript)
                        },
                        modifier = Modifier.weight(1f).testTag("share_local_server_script_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("server.js", maxLines = 1)
                    }
                }

                testResult?.let { res ->
                    Surface(
                        color = if (res.ok) SemanticSuccessContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (res.ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = if (res.ok) SemanticSuccess else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = res.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (res.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else if (mode == ServerMode.LOCAL_EMULATOR) {
                OutlinedTextField(
                    value = emulatorHost,
                    onValueChange = { emulatorHost = it },
                    label = { Text(strings.emulatorHostLabel) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = AppMonoFontFamily),
                    modifier = Modifier.fillMaxWidth().testTag("emulator_host_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firestorePort,
                        onValueChange = { firestorePort = it.filter(Char::isDigit).take(5) },
                        label = { Text(strings.firestorePortLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("emulator_firestore_port_input")
                    )
                    OutlinedTextField(
                        value = authPort,
                        onValueChange = { authPort = it.filter(Char::isDigit).take(5) },
                        label = { Text(strings.authPortLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("emulator_auth_port_input")
                    )
                }
            }

            HorizontalDivider()
            Button(
                onClick = {
                    onSaveServerConfig(
                        ServerConfig(
                            mode = mode,
                            serverUrl = ServerRuntimeState.normalizeUrl(serverUrl),
                            emulatorHost = emulatorHost.trim().ifBlank { "10.0.2.2" },
                            firestorePort = (firestorePort.toIntOrNull() ?: 8080).coerceIn(1, 65535),
                            authPort = (authPort.toIntOrNull() ?: 9099).coerceIn(1, 65535),
                            apiToken = apiToken.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_server_config_button")
            ) {
                Text(strings.saveApplyServerButton)
            }
        }
    }
}
