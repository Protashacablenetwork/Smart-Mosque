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
}
