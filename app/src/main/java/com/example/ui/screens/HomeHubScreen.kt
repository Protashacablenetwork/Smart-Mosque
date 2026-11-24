package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MosqueSchedule
import com.example.data.model.Notice
import com.example.data.model.PublicSummary
import com.example.data.model.SocietyEvent
import com.example.data.model.UiState
import com.example.ui.components.HomePrayerHeroCard
import com.example.ui.components.MosqueScheduleDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SidebarGradientTop

private data class HubTile(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeHubScreen(
    strings: AppStrings,
    isBangla: Boolean,
    isSignedIn: Boolean,
    isStaffReader: Boolean,
    isAdmin: Boolean,
    canEditSchedule: Boolean,
    summaryState: UiState<PublicSummary>,
    scheduleState: UiState<MosqueSchedule>,
    noticesState: UiState<List<Notice>>,
    events: List<SocietyEvent>,
    onSaveSchedule: (MosqueSchedule) -> Unit,
    onSeedDemo: () -> Unit,
    onRequestSignIn: () -> Unit,
    onNavigate: (destId: Int, subTab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = (summaryState as? UiState.Success)?.data ?: PublicSummary()
    val schedule = (scheduleState as? UiState.Success)?.data ?: MosqueSchedule()
    val notices = (noticesState as? UiState.Success)?.data ?: emptyList()

    var showScheduleDialog by remember { mutableStateOf(false) }
    var showZakatDialog by remember { mutableStateOf(false) }

    val serviceTiles = remember(strings, canEditSchedule, isStaffReader) {
        listOf(
            HubTile(strings.tilePrayer, Icons.Filled.Schedule, "hub_prayer") { onNavigate(11, 0) },
            HubTile(strings.tileCalendar, Icons.Filled.CalendarMonth, "hub_calendar") { onNavigate(11, 0) },
            HubTile(strings.tileMosque, Icons.Filled.Mosque, "hub_mosque") {
                if (canEditSchedule) showScheduleDialog = true else onNavigate(11, 0)
            },
            HubTile(strings.tileZakat, Icons.Filled.Calculate, "hub_zakat") { showZakatDialog = true },
            HubTile(strings.tileQurbani, Icons.Filled.Pets, "hub_qurbani") {
                if (isStaffReader) onNavigate(4, 0) else onNavigate(9, 0)
            },
            HubTile(strings.tileDonate, Icons.Filled.VolunteerActivism, "hub_donate") {
                if (isStaffReader) onNavigate(3, 1) else onNavigate(6, 0)
            },
            HubTile(strings.tileNotices, Icons.Filled.Campaign, "hub_notices") { onNavigate(5, 0) },
            HubTile(strings.tileEvents, Icons.Filled.Event, "hub_events") { onNavigate(12, 0) }
        )
    }

    val adminTiles = remember(strings, isStaffReader, canEditSchedule) {
        if (!isStaffReader) emptyList()
        else listOf(
            HubTile(strings.tileMembers, Icons.Filled.Groups, "hub_members") { onNavigate(2, 0) },
            HubTile(strings.tileFamilies, Icons.Filled.FamilyRestroom, "hub_families") { onNavigate(1, 0) },
            HubTile(strings.tileCommittee, Icons.Filled.Badge, "hub_committee") { onNavigate(8, 0) },
            HubTile(strings.tileJamaat, Icons.Filled.Schedule, "hub_jamaat") {
                if (canEditSchedule) showScheduleDialog = true else onNavigate(11, 0)
            },
            HubTile(strings.tileMonthlyDues, Icons.Filled.ReceiptLong, "hub_dues") { onNavigate(10, 2) },
            HubTile(strings.tileMyAccounts, Icons.Filled.AccountBalance, "hub_accounts") { onNavigate(3, 4) },
            HubTile(strings.tileFundCollect, Icons.Filled.Payments, "hub_collect") { onNavigate(3, 0) },
            HubTile(strings.tileReports, Icons.Filled.Assessment, "hub_reports") { onNavigate(6, 0) }
        )
    }

    val managementTiles = remember(strings, isStaffReader, canEditSchedule) {
        if (!isStaffReader) emptyList()
        else listOf(
            HubTile(strings.tileAssets, Icons.Filled.Inventory2, "hub_assets") { onNavigate(13, 0) },
            HubTile(strings.tileDocuments, Icons.Filled.Description, "hub_docs") { onNavigate(13, 1) },
            HubTile(strings.tileWelfare, Icons.Filled.VolunteerActivism, "hub_welfare") { onNavigate(8, 2) },
            HubTile(strings.tileMeetings, Icons.Filled.Groups, "hub_meetings") { onNavigate(8, 1) },
            HubTile(strings.tileFeedback, Icons.Filled.Feedback, "hub_feedback") { onNavigate(8, 3) },
            HubTile(strings.tileKhutbah, Icons.Filled.RecordVoiceOver, "hub_khutbah") {
                if (canEditSchedule) showScheduleDialog = true else onNavigate(11, 0)
            },
            HubTile(strings.tileBudgets, Icons.Filled.PieChart, "hub_budgets") { onNavigate(3, 5) },
            HubTile(strings.tileAuditLogs, Icons.Filled.History, "hub_audit") { onNavigate(14, 0) }
        )
    }

    val otherTiles = remember(strings, isAdmin, isSignedIn) {
        buildList {
            add(HubTile(strings.tileMyFamily, Icons.Filled.Mosque, "hub_my_family") { onNavigate(9, 0) })
            add(HubTile(strings.tileTransparency, Icons.Filled.VerifiedUser, "hub_transparency") { onNavigate(6, 1) })
            if (isAdmin) {
                add(HubTile(strings.tileBackup, Icons.Filled.Backup, "hub_backup") { onNavigate(15, 0) })
                add(HubTile(strings.tileSecurity, Icons.Filled.AdminPanelSettings, "hub_security") { onNavigate(16, 0) })
            } else {
                add(HubTile(strings.navSettingsTab, Icons.Filled.Settings, "hub_settings") { onNavigate(7, 0) })
                if (!isSignedIn) {
                    add(HubTile(strings.adminSignIn, Icons.Filled.AdminPanelSettings, "hub_signin") { onRequestSignIn() })
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Live Prayer & Calendar Hero Card (Screenshot 1)
            item {
                HomePrayerHeroCard(
                    strings = strings,
                    schedule = schedule,
                    isBangla = isBangla,
                    onClick = { if (canEditSchedule) showScheduleDialog = true else onNavigate(11, 0) }
                )
            }

            // 2. Notices & Events Header + Featured Card (Screenshot 1)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = SidebarGradientTop, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = strings.noticesAndEvents,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { onNavigate(5, 0) }) {
                            Text(strings.seeAllLabel, color = SidebarGradientTop, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SidebarGradientTop, modifier = Modifier.size(16.dp))
                        }
                    }

                    val latestNotice = notices.firstOrNull()
                    val nextEvent = events.firstOrNull()
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigate(if (latestNotice != null) 5 else 12, 0) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (latestNotice != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        if (latestNotice.pinned) Icon(Icons.Filled.PushPin, contentDescription = null, tint = SidebarGradientTop, modifier = Modifier.size(15.dp))
                                        Text(latestNotice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    StatusPill(latestNotice.category.uppercase(), EmeraldPrimaryContainer, SidebarGradientTop)
                                }
                                Text(latestNotice.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else if (nextEvent != null) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(nextEvent.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusPill(nextEvent.eventDate, EmeraldPrimaryContainer, SidebarGradientTop)
                                }
                                Text(listOfNotNull(nextEvent.eventTime, nextEvent.location, nextEvent.description).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.netBalance, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatCurrency(summary.balance), style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                                    }
                                    if (!isSignedIn) {
                                        Button(
                                            onClick = onRequestSignIn,
                                            colors = ButtonDefaults.buttonColors(containerColor = SidebarGradientTop)
                                        ) { Text(strings.adminSignIn) }
                                    } else if (canEditSchedule && summary.totalFamilies == 0L) {
                                        Button(
                                            onClick = onSeedDemo,
                                            colors = ButtonDefaults.buttonColors(containerColor = SidebarGradientTop)
                                        ) {
                                            Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.seedDemoData)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Feature Hub Sections (Screenshots 3-7)
            item {
                FeatureGridSection(
                    title = strings.hubServicesTitle,
                    headerIcon = Icons.Filled.Star,
                    badgeText = "${strings.totalFamilies}: ${summary.totalFamilies}",
                    tiles = serviceTiles
                )
            }

            if (adminTiles.isNotEmpty()) {
                item {
                    FeatureGridSection(
                        title = strings.hubAdminFeaturesTitle,
                        headerIcon = Icons.Filled.GridView,
                        badgeText = null,
                        tiles = adminTiles
                    )
                }
            }

            if (managementTiles.isNotEmpty()) {
                item {
                    FeatureGridSection(
                        title = strings.hubMosqueMgmtTitle,
                        headerIcon = Icons.Filled.Mosque,
                        badgeText = null,
                        tiles = managementTiles
                    )
                }
            }

            item {
                FeatureGridSection(
                    title = strings.hubOtherFeaturesTitle,
                    headerIcon = Icons.Filled.GridView,
                    badgeText = null,
                    tiles = otherTiles
                )
            }
        }
    }

    if (showScheduleDialog) {
        MosqueScheduleDialog(
            strings = strings,
            existing = schedule,
            onDismiss = { showScheduleDialog = false },
            onSave = { updated -> onSaveSchedule(updated); showScheduleDialog = false }
        )
    }

    if (showZakatDialog) {
        ZakatCalculatorDialog(
            strings = strings,
            onDismiss = { showZakatDialog = false }
        )
    }
}

@Composable
private fun FeatureGridSection(
    title: String,
    headerIcon: ImageVector,
    badgeText: String?,
    tiles: List<HubTile>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(headerIcon, contentDescription = null, tint = SidebarGradientTop, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SidebarGradientTop,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(4).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = tile.onClick)
                                .testTag(tile.testTag)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldPrimaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = tile.icon,
                                            contentDescription = tile.title,
                                            tint = SidebarGradientTop,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    repeat(4 - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZakatCalculatorDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    var cashText by remember { mutableStateOf("") }
    var goldText by remember { mutableStateOf("") }
    var investText by remember { mutableStateOf("") }
    var debtText by remember { mutableStateOf("") }

    val cash = cashText.toDoubleOrNull() ?: 0.0
    val gold = goldText.toDoubleOrNull() ?: 0.0
    val invest = investText.toDoubleOrNull() ?: 0.0
    val debt = debtText.toDoubleOrNull() ?: 0.0
    val netWealth = (cash + gold + invest - debt).coerceAtLeast(0.0)
    val payableZakat = netWealth * 0.025

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.zakatCalculatorTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text(strings.zakatCashLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("zakat_cash_input")
                )
                OutlinedTextField(
                    value = goldText,
                    onValueChange = { goldText = it },
                    label = { Text(strings.zakatGoldValueLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = investText,
                    onValueChange = { investText = it },
                    label = { Text(strings.zakatInvestmentsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = debtText,
                    onValueChange = { debtText = it },
                    label = { Text(strings.zakatDebtsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.zakatPayableLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatCurrency(payableZakat),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = SemanticSuccess
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
