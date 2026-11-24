package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "masjid_settings")

enum class AppLanguage(val code: String, val label: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "EN")
}

enum class AppThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

enum class ServerMode(val code: String) {
    CLOUD_FIREBASE("cloud"),
    LOCAL_EMULATOR("emulator"),
    LOCAL_HTTP_SERVER("local_http");

    companion object {
        fun fromCode(code: String?): ServerMode = when (code) {
            LOCAL_EMULATOR.code -> LOCAL_EMULATOR
            LOCAL_HTTP_SERVER.code -> LOCAL_HTTP_SERVER
            else -> CLOUD_FIREBASE
        }
    }
}

data class ServerConfig(
    val mode: ServerMode = ServerMode.CLOUD_FIREBASE,
    val serverUrl: String = "http://10.0.2.2:3000",
    val emulatorHost: String = "10.0.2.2",
    val firestorePort: Int = 8080,
    val authPort: Int = 9099,
    val apiToken: String = ""
)

data class LocalAuthSession(
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val token: String = ""
)

data class AutomationPrefs(
    val dueReminderDays: Int = 7,
    val reminderTime: String = "08:00",
    val autoReceipt: Boolean = true,
    val showDashboardChart: Boolean = true
)

class LanguagePreferences(private val context: Context) {
    private val languageKey = stringPreferencesKey("app_language")
    private val themeKey = stringPreferencesKey("app_theme_mode")
    private val reminderDaysKey = intPreferencesKey("auto_due_reminder_days")
    private val reminderTimeKey = stringPreferencesKey("auto_reminder_time")
    private val autoReceiptKey = booleanPreferencesKey("auto_receipt_enabled")
    private val showChartKey = booleanPreferencesKey("show_dashboard_chart")

    private val serverModeKey = stringPreferencesKey("server_mode")
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val emulatorHostKey = stringPreferencesKey("emulator_host")
    private val firestorePortKey = intPreferencesKey("emulator_firestore_port")
    private val authPortKey = intPreferencesKey("emulator_auth_port")
    private val apiTokenKey = stringPreferencesKey("server_api_token")

    private val localUidKey = stringPreferencesKey("local_auth_uid")
    private val localNameKey = stringPreferencesKey("local_auth_name")
    private val localEmailKey = stringPreferencesKey("local_auth_email")
    private val localRoleKey = stringPreferencesKey("local_auth_role")
    private val localTokenKey = stringPreferencesKey("local_auth_token")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[languageKey]) {
            AppLanguage.ENGLISH.code -> AppLanguage.ENGLISH
            else -> AppLanguage.BANGLA
        }
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            AppThemeMode.DARK.code -> AppThemeMode.DARK
            AppThemeMode.LIGHT.code -> AppThemeMode.LIGHT
            else -> AppThemeMode.SYSTEM
        }
    }

    val automationPrefsFlow: Flow<AutomationPrefs> = context.dataStore.data.map { prefs ->
        AutomationPrefs(
            dueReminderDays = (prefs[reminderDaysKey] ?: 7).coerceIn(1, 30),
            reminderTime = prefs[reminderTimeKey] ?: "08:00",
            autoReceipt = prefs[autoReceiptKey] ?: true,
            showDashboardChart = prefs[showChartKey] ?: true
        )
    }

    val serverConfigFlow: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        ServerConfig(
            mode = ServerMode.fromCode(prefs[serverModeKey]),
            serverUrl = prefs[serverUrlKey] ?: "http://10.0.2.2:3000",
            emulatorHost = prefs[emulatorHostKey] ?: "10.0.2.2",
            firestorePort = (prefs[firestorePortKey] ?: 8080).coerceIn(1, 65535),
            authPort = (prefs[authPortKey] ?: 9099).coerceIn(1, 65535),
            apiToken = prefs[apiTokenKey] ?: ""
        )
    }

    val localAuthSessionFlow: Flow<LocalAuthSession?> = context.dataStore.data.map { prefs ->
        val uid = prefs[localUidKey]?.takeIf { it.isNotBlank() } ?: return@map null
        LocalAuthSession(
            userId = uid,
            fullName = prefs[localNameKey] ?: "Member",
            email = prefs[localEmailKey] ?: "member@local",
            role = prefs[localRoleKey] ?: "member",
            token = prefs[localTokenKey] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.code
        }
    }

    suspend fun updateAutomationPrefs(updated: AutomationPrefs) {
        context.dataStore.edit { prefs ->
            prefs[reminderDaysKey] = updated.dueReminderDays.coerceIn(1, 30)
            prefs[reminderTimeKey] = updated.reminderTime.trim().ifBlank { "08:00" }.take(10)
            prefs[autoReceiptKey] = updated.autoReceipt
            prefs[showChartKey] = updated.showDashboardChart
        }
    }

    suspend fun updateServerConfig(config: ServerConfig) {
        val cleanUrl = config.serverUrl.trim().trimEnd('/').let { raw ->
            when {
                raw.isBlank() -> "http://10.0.2.2:3000"
                raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
                else -> "http://$raw"
            }
        }
        val cleanHost = config.emulatorHost.trim().ifBlank { "10.0.2.2" }
        context.dataStore.edit { prefs ->
            prefs[serverModeKey] = config.mode.code
            prefs[serverUrlKey] = cleanUrl
            prefs[emulatorHostKey] = cleanHost
            prefs[firestorePortKey] = config.firestorePort.coerceIn(1, 65535)
            prefs[authPortKey] = config.authPort.coerceIn(1, 65535)
            prefs[apiTokenKey] = config.apiToken.trim()
        }
    }

    suspend fun saveLocalAuthSession(session: LocalAuthSession?) {
        context.dataStore.edit { prefs ->
            if (session == null) {
                prefs.remove(localUidKey)
                prefs.remove(localNameKey)
                prefs.remove(localEmailKey)
                prefs.remove(localRoleKey)
                prefs.remove(localTokenKey)
            } else {
                prefs[localUidKey] = session.userId
                prefs[localNameKey] = session.fullName
                prefs[localEmailKey] = session.email
                prefs[localRoleKey] = session.role
                prefs[localTokenKey] = session.token
            }
        }
    }
}
