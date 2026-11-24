package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.data.model.CommitteeMember
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.data.model.SocietyMember
import com.example.data.model.SocietyRoles
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppThemeMode
import com.example.data.prefs.AutomationPrefs
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer

@Composable
fun BackupRestoreScreen(
    strings: AppStrings,
    collectionCounts: List<Pair<String, Int>>,
    estimatedJsonKb: Int,
    onExportBackupJson: () -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalRecords = remember(collectionCounts) { collectionCounts.sumOf { it.second } }

    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .getOrNull()?.let { json -> if (json.isNotBlank()) onRestoreBackupJson(json) }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(strings.dataSizeLabel, "~$estimatedJsonKb KB", Icons.Filled.Storage, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
                    MetricTile(strings.totalRecordsLabel, totalRecords.toString(), Icons.Filled.Backup, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.backupRestoreTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onExportBackupJson, modifier = Modifier.weight(1f).testTag("export_json_backup_button")) {
                                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.exportBackup, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { jsonPicker.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier.weight(1f).testTag("restore_json_backup_button")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.restoreBackup, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.collectionBreakdown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        collectionCounts.forEachIndexed { idx, (name, count) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                StatusPill(count.toString(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            if (idx < collectionCounts.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityRolesScreen(
    strings: AppStrings,
    currentUserId: String,
    effectiveRole: String,
    isSuperAdmin: Boolean,
    onSetUserRole: (uid: String, role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var targetUid by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(SocietyRoles.ACCOUNTANT) }

    val assignableRoles = remember(isSuperAdmin) {
        if (isSuperAdmin) SocietyRoles.ALL_ROLES
        else SocietyRoles.ALL_ROLES.filter { it != SocietyRoles.SUPER_ADMIN }
    }

    val matrixRows = remember {
        listOf(
            Triple(SocietyRoles.SUPER_ADMIN, "Full System + Role Promotion + Backup/Restore", SemanticSuccess to SemanticSuccessContainer),
            Triple(SocietyRoles.ADMIN, "All Operations + Deletes + Backup/Roles (except Super Admin)", SemanticSuccess to SemanticSuccessContainer),
            Triple(SocietyRoles.ACCOUNTANT, "Payments, Donations, Dues, Expenses, Accounts, Budgets, Welfare", InfoBlue to InfoBlueContainer),
            Triple(SocietyRoles.MANAGER, "Families, Members, Qurbani, Prayer, Events, Committee, Meetings, Notices", InfoBlue to InfoBlueContainer),
            Triple(SocietyRoles.VIEWER, "Read-only Staff Audit, Reports, Families & Finance Ledger", SemanticWarning to SemanticWarningContainer),
            Triple(SocietyRoles.MEMBER, "Public Portal + Linked Family Digital ID & Feedback", SemanticWarning to SemanticWarningContainer)
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.accessMatrixTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        matrixRows.forEachIndexed { idx, (role, perms, colors) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusPill(role.replace('_', ' ').uppercase(), colors.second, colors.first)
                                Text(perms, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (idx < matrixRows.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.promoteAdminTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (currentUserId.isNotBlank()) {
                            Text("Current Role: ${effectiveRole.uppercase()} • UID: $currentUserId", style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = targetUid,
                            onValueChange = { targetUid = it },
                            label = { Text(strings.promoteAdminHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("role_target_uid_input")
                        )
                        Text(strings.selectRoleLabel, style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            assignableRoles.forEach { role ->
                                FilterChip(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role },
                                    label = { Text(role.replace('_', ' ').uppercase()) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                onSetUserRole(targetUid.trim(), selectedRole)
                                targetUid = ""
                            },
                            enabled = targetUid.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("update_user_role_button")
                        ) {
                            Text(strings.promoteButton)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    strings: AppStrings,
    currentUserId: String,
    effectiveRole: String,
    language: AppLanguage,
    themeMode: AppThemeMode,
    automationPrefs: AutomationPrefs,
    isStaffReader: Boolean,
    families: List<Family>,
    members: List<SocietyMember>,
    payments: List<PaymentRecord>,
    committees: List<CommitteeMember>,
    onToggleLanguage: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onUpdateAutomationPrefs: (AutomationPrefs) -> Unit,
    modifier: Modifier = Modifier
) {
    var reminderDays by remember(automationPrefs) { mutableStateOf(automationPrefs.dueReminderDays.toString()) }
    var reminderTime by remember(automationPrefs) { mutableStateOf(automationPrefs.reminderTime) }
    var autoReceipt by remember(automationPrefs) { mutableStateOf(automationPrefs.autoReceipt) }
    var showChart by remember(automationPrefs) { mutableStateOf(automationPrefs.showDashboardChart) }
    var integrityReport by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.themeSetting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = themeMode == AppThemeMode.LIGHT, onClick = { onSetThemeMode(AppThemeMode.LIGHT) }, label = { Text(strings.themeLight) })
                            FilterChip(selected = themeMode == AppThemeMode.DARK, onClick = { onSetThemeMode(AppThemeMode.DARK) }, label = { Text(strings.themeDark) })
                            FilterChip(selected = themeMode == AppThemeMode.SYSTEM, onClick = { onSetThemeMode(AppThemeMode.SYSTEM) }, label = { Text(strings.themeSystem) })
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.languageSetting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onToggleLanguage) {
                                Text(if (language == AppLanguage.BANGLA) "বাংলা → English" else "English → বাংলা")
                            }
                        }
                        if (currentUserId.isNotBlank()) {
                            HorizontalDivider()
                            Text("Role: ${effectiveRole.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("UID: $currentUserId", style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.automationTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = reminderDays,
                                onValueChange = { if (it.length <= 2) reminderDays = it.filter(Char::isDigit) },
                                label = { Text(strings.dueReminderDaysLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = reminderTime,
                                onValueChange = { if (it.length <= 10) reminderTime = it },
                                label = { Text(strings.reminderTimeLabel) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.autoReceiptToggle, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = autoReceipt, onCheckedChange = { autoReceipt = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.showChartToggle, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = showChart, onCheckedChange = { showChart = it })
                        }
                        Button(
                            onClick = {
                                val days = (reminderDays.toIntOrNull() ?: 3).coerceIn(1, 30)
                                onUpdateAutomationPrefs(
                                    AutomationPrefs(
                                        dueReminderDays = days,
                                        reminderTime = reminderTime.ifBlank { "09:00" },
                                        autoReceipt = autoReceipt,
                                        showDashboardChart = showChart
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_automation_prefs_button")
                        ) { Text(strings.save) }
                    }
                }
            }

            if (isStaffReader) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.systemHealthTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val famIds = families.map { it.id }.toSet()
                                    val orphanMembers = members.count { it.familyId !in famIds }
                                    val orphanPayments = payments.count { it.familyId !in famIds }
                                    val orphanCommittees = committees.count { !it.familyId.isNullOrBlank() && it.familyId !in famIds }
                                    integrityReport = if (orphanMembers == 0 && orphanPayments == 0 && orphanCommittees == 0) {
                                        strings.integrityCleanMsg
                                    } else {
                                        "Orphan references — Members: $orphanMembers, Payments: $orphanPayments, Committee: $orphanCommittees"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("run_integrity_scan_button")
                            ) {
                                Text(strings.runIntegrityCheck)
                            }
                            integrityReport?.let { msg ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SemanticSuccess, modifier = Modifier.size(16.dp))
                                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
