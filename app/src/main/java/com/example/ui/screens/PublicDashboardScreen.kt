package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.MosqueSchedule
import com.example.data.model.Notice
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicSummary
import com.example.data.model.UiState
import com.example.data.model.WelfareRecord
import com.example.ui.components.CollectionVsExpenseChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HeroGreetingBanner
import com.example.ui.components.MetricTile
import com.example.ui.components.MosqueScheduleCard
import com.example.ui.components.MosqueScheduleDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatKg
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.GoldSecondaryContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.OnEmeraldPrimaryContainer
import com.example.ui.theme.OnGoldSecondaryContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.util.ReportExporter

private data class RecentTxn(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
    val amount: Double,
    val isIncome: Boolean
)

@Composable
fun PublicDashboardScreen(
    strings: AppStrings,
    isSignedIn: Boolean,
    isStaffReader: Boolean,
    isFinanceAdmin: Boolean,
    canEditSchedule: Boolean,
    effectiveRole: String,
    showDashboardChart: Boolean,
    summaryState: UiState<PublicSummary>,
    scheduleState: UiState<MosqueSchedule>,
    noticesState: UiState<List<Notice>>,
    families: List<Family>,
    dues: List<DueItem>,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    welfareRecords: List<WelfareRecord>,
    onSaveSchedule: (MosqueSchedule) -> Unit,
    onSeedDemo: () -> Unit,
    onRequestSignIn: () -> Unit,
    onQuickNavigate: (destId: Int) -> Unit,
    onQuickPayFamily: (family: Family, dueAmount: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary = (summaryState as? UiState.Success)?.data ?: PublicSummary()
    val schedule = (scheduleState as? UiState.Success)?.data ?: MosqueSchedule()
    val notices = (noticesState as? UiState.Success)?.data ?: emptyList()
    var showScheduleDialog by remember { mutableStateOf(false) }

    val topDebtors = remember(families, dues, payments, isStaffReader) {
        if (!isStaffReader) emptyList()
        else {
            val activeDuesTotal = dues.filter { it.active }.sumOf { it.amount }
            if (activeDuesTotal <= 0.0) emptyList()
            else {
                val paidMap = payments.groupBy { it.familyId }.mapValues { (_, list) -> list.sumOf { it.amount } }
                families.filter { it.active }
                    .map { fam -> fam to maxOf(0.0, activeDuesTotal - (paidMap[fam.id] ?: 0.0)) }
                    .filter { it.second > 0.0 }
                    .sortedByDescending { it.second }
                    .take(5)
            }
        }
    }

    val recentTxns = remember(payments, donations, expenses, welfareRecords, isStaffReader) {
        if (!isStaffReader) emptyList()
        else {
            val payItems = payments.take(5).map { RecentTxn("p_${it.id}", it.familyName, it.receiptNo, it.paymentDate, it.amount, true) }
            val donItems = donations.take(5).map {
                val donor = if (it.anonymous) strings.anonymousDonor else it.donorName
                RecentTxn("d_${it.id}", donor, "${it.fundType.uppercase()} • ${it.receiptNo}", it.donationDate, it.amount, true)
            }
            val expItems = expenses.take(5).map { RecentTxn("e_${it.id}", it.title, it.category, it.expenseDate, it.amount, false) }
            val welItems = welfareRecords.filter { it.amount > 0.0 }.take(4).map { RecentTxn("w_${it.id}", it.beneficiaryName, it.recordType.replace('_', ' '), it.recordDate, it.amount, false) }
            (payItems + donItems + expItems + welItems).sortedByDescending { it.date }.take(7)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val roleBadge = when {
                    !isSignedIn -> strings.guestBadge
                    isStaffReader -> "${strings.adminBadge} • ${effectiveRole.uppercase()}"
                    else -> strings.memberBadge
                }
                HeroGreetingBanner(
                    title = "${strings.greetingSalam} — ${schedule.mosqueName.ifBlank { strings.appTitle }}",
                    subtitle = strings.heroPublicSubtitle,
                    badgeText = roleBadge
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(strings.netBalance, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD0F0E3))
                        Text(
                            text = formatCurrency(summary.balance),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AppMonoFontFamily),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isSignedIn) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.guestBannerHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            )
                            Button(onClick = onRequestSignIn, modifier = Modifier.testTag("guest_sign_in_cta")) {
                                Text(strings.adminSignIn)
                            }
                        }
                    }
                }
            }

            item {
                MosqueScheduleCard(
                    strings = strings,
                    schedule = schedule,
                    canEdit = canEditSchedule,
                    onEdit = { showScheduleDialog = true }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.quickActions,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(onClick = { onQuickNavigate(11) }, leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp)) }, label = { Text(strings.navPrayerCalendar) })
                        AssistChip(onClick = { onQuickNavigate(12) }, leadingIcon = { Icon(Icons.Filled.Event, null, Modifier.size(16.dp)) }, label = { Text(strings.navEvents) })
                        if (isStaffReader) {
                            AssistChip(onClick = { onQuickNavigate(1) }, leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }, label = { Text(strings.navFamilies) })
                            if (isFinanceAdmin) {
                                AssistChip(onClick = { onQuickNavigate(3) }, leadingIcon = { Icon(Icons.Filled.Payments, null, Modifier.size(16.dp)) }, label = { Text(strings.recordPayment) })
                            }
                            AssistChip(onClick = { onQuickNavigate(4) }, leadingIcon = { Icon(Icons.Filled.Restaurant, null, Modifier.size(16.dp)) }, label = { Text(strings.navQurbani) })
                            AssistChip(onClick = { onQuickNavigate(8) }, leadingIcon = { Icon(Icons.Filled.Diversity3, null, Modifier.size(16.dp)) }, label = { Text(strings.navCommunity) })
                        }
                        AssistChip(onClick = { onQuickNavigate(5) }, leadingIcon = { Icon(Icons.Filled.Campaign, null, Modifier.size(16.dp)) }, label = { Text(strings.navNotices) })
                        AssistChip(onClick = { onQuickNavigate(6) }, leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(16.dp)) }, label = { Text(strings.navAnalyticsTransparency) })
                    }
                }
            }

            if (summaryState is UiState.Loading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (summaryState is UiState.Error) {
                item { EmptyStateCard(icon = Icons.Filled.AccountBalanceWallet, message = summaryState.message) }
            }

            item { DashboardKpiGrid(strings = strings, summary = summary) }

            if (showDashboardChart) {
                item {
                    CollectionVsExpenseChart(
                        title = if (summary.recentMonths.isNotEmpty()) strings.sixMonthChartTitle else strings.incomeVsExpense,
                        collectionLabel = strings.totalCollection,
                        expenseLabel = strings.totalExpense,
                        collection = summary.totalIncome,
                        expense = summary.totalOutflow,
                        recentMonths = summary.recentMonths,
                        recentCollections = summary.recentCollections,
                        recentExpenses = summary.recentExpenses
                    )
                }
            }

            if (!summary.latestQurbaniEvent.isNullOrBlank()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Restaurant, contentDescription = strings.latestQurbani, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${strings.latestQurbani}: ${summary.latestQurbaniEvent}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${strings.totalMeat}: ${formatKg(summary.latestTotalMeatKg ?: 0.0)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${strings.perFamilyShare}: ${formatKg(summary.latestPerFamilyKg ?: 0.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            if (isStaffReader && topDebtors.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(strings.topDebtors, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            topDebtors.forEachIndexed { idx, (fam, due) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${fam.familyCode} • ${fam.familyName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${strings.dueLabel}: ${formatCurrency(due)}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily), color = SemanticError)
                                    }
                                    if (isFinanceAdmin) {
                                        TextButton(onClick = { onQuickPayFamily(fam, due) }) { Text(strings.collectNow) }
                                    }
                                }
                                if (idx < topDebtors.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            if (isStaffReader && recentTxns.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(strings.recentTransactions, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            recentTxns.forEachIndexed { idx, txn ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(txn.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${txn.date} • ${txn.subtitle}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = (if (txn.isIncome) "+" else "-") + formatCurrency(txn.amount),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = if (txn.isIncome) SemanticSuccess else SemanticError
                                    )
                                }
                                if (idx < recentTxns.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { ReportExporter.exportPublicSummaryPdf(context, summary, notices, payments, expenses, donations, welfareRecords) },
                        modifier = Modifier.weight(1f).testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportPdf)
                    }
                    OutlinedButton(
                        onClick = { ReportExporter.exportPublicSummaryCsv(context, summary, payments, expenses, donations, welfareRecords) },
                        modifier = Modifier.weight(1f).testTag("export_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportCsv)
                    }
                }
            }

            if (canEditSchedule && summary.totalFamilies == 0L && notices.isEmpty()) {
                item {
                    Button(
                        onClick = onSeedDemo,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("seed_demo_button")
                    ) {
                        Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.seedDemoData)
                    }
                }
            }

            item {
                Text(strings.publicNotices, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 4.dp))
            }

            if (notices.isEmpty()) {
                item { EmptyStateCard(icon = Icons.Filled.Campaign, message = strings.noNoticesYet) }
            } else {
                items(notices, key = { it.id }) { notice ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (notice.pinned) Icon(Icons.Filled.PushPin, contentDescription = strings.pinnedLabel, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                                    Text(notice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                StatusPill(notice.category.uppercase(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
}

@Composable
private fun DashboardKpiGrid(strings: AppStrings, summary: PublicSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(strings.totalFamilies, summary.totalFamilies.toString(), Icons.Filled.FamilyRestroom, OnEmeraldPrimaryContainer, EmeraldPrimaryContainer, modifier = Modifier.weight(1f))
            MetricTile(strings.totalMembers, summary.totalMembers.toString(), Icons.Filled.Groups, OnGoldSecondaryContainer, GoldSecondaryContainer, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(strings.totalCollection, formatCurrency(summary.totalCollection), Icons.AutoMirrored.Filled.TrendingUp, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
            MetricTile(strings.totalDonations, formatCurrency(summary.totalDonations), Icons.Filled.VolunteerActivism, OnEmeraldPrimaryContainer, EmeraldPrimaryContainer, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(strings.totalExpense, formatCurrency(summary.totalExpense), Icons.Filled.SouthWest, SemanticError, SemanticErrorContainer, modifier = Modifier.weight(1f))
            MetricTile(strings.totalWelfare, formatCurrency(summary.totalWelfare), Icons.Filled.Diversity3, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(strings.netBalance, formatCurrency(summary.balance), Icons.Filled.AccountBalance, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
            MetricTile(strings.outstandingDues, formatCurrency(summary.totalOutstanding), Icons.Filled.WarningAmber, SemanticWarning, SemanticWarningContainer, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(strings.totalQurbaniMeat, formatKg(summary.totalQurbaniMeatKg), Icons.Filled.Restaurant, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
            MetricTile(strings.meatDistribution, "${summary.deliveredDistributions} / ${summary.totalDistributions}", Icons.Filled.LocalShipping, OnEmeraldPrimaryContainer, EmeraldPrimaryContainer, modifier = Modifier.weight(1f))
        }
    }
}
