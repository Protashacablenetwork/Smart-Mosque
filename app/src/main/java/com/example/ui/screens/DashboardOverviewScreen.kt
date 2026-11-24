package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommitteeMember
import com.example.data.model.DonationRecord
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.Notice
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicSummary
import com.example.data.model.SocietyEvent
import com.example.data.model.WelfareRecord
import com.example.ui.components.CollectionVsExpenseChart
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.ui.theme.SidebarGradientTop
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardOverviewScreen(
    strings: AppStrings,
    isStaffReader: Boolean,
    showDashboardChart: Boolean,
    summary: PublicSummary,
    notices: List<Notice>,
    events: List<SocietyEvent>,
    families: List<Family>,
    committees: List<CommitteeMember>,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    welfareRecords: List<WelfareRecord>,
    onNavigate: (destId: Int, subTab: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val periodStats = remember(payments, donations, expenses, welfareRecords, summary, isStaffReader) {
        val cal = Calendar.getInstance()
        val ymFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val thisMonth = ymFmt.format(cal.time)
        val today = isoFmt.format(cal.time)

        val calPrevMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonth = ymFmt.format(calPrevMonth.time)

        val calWeekStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val thisWeekStart = isoFmt.format(calWeekStart.time)
        val calPrevWeekStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
        val prevWeekStart = isoFmt.format(calPrevWeekStart.time)
        val calPrevWeekEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val prevWeekEnd = isoFmt.format(calPrevWeekEnd.time)

        if (!isStaffReader) {
            val lastIdx = summary.recentMonths.indexOf(thisMonth)
            val prevIdx = summary.recentMonths.indexOf(lastMonth)
            val tmInc = if (lastIdx >= 0) summary.recentCollections.getOrElse(lastIdx) { 0.0 } else 0.0
            val tmExp = if (lastIdx >= 0) summary.recentExpenses.getOrElse(lastIdx) { 0.0 } else 0.0
            val lmInc = if (prevIdx >= 0) summary.recentCollections.getOrElse(prevIdx) { 0.0 } else 0.0
            val lmExp = if (prevIdx >= 0) summary.recentExpenses.getOrElse(prevIdx) { 0.0 } else 0.0
            listOf(tmInc, tmExp, lmInc, lmExp, 0.0, 0.0, 0.0)
        } else {
            val tmInc = payments.filter { it.paymentDate.startsWith(thisMonth) }.sumOf { it.amount } +
                donations.filter { it.donationDate.startsWith(thisMonth) }.sumOf { it.amount }
            val tmExp = expenses.filter { it.expenseDate.startsWith(thisMonth) }.sumOf { it.amount } +
                welfareRecords.filter { it.recordDate.startsWith(thisMonth) }.sumOf { it.amount }
            val lmInc = payments.filter { it.paymentDate.startsWith(lastMonth) }.sumOf { it.amount } +
                donations.filter { it.donationDate.startsWith(lastMonth) }.sumOf { it.amount }
            val lmExp = expenses.filter { it.expenseDate.startsWith(lastMonth) }.sumOf { it.amount } +
                welfareRecords.filter { it.recordDate.startsWith(lastMonth) }.sumOf { it.amount }
            val tmDon = donations.filter { it.donationDate.startsWith(thisMonth) }.sumOf { it.amount }
            val twDon = donations.filter { it.donationDate in thisWeekStart..today }.sumOf { it.amount }
            val lwDon = donations.filter { it.donationDate in prevWeekStart..prevWeekEnd }.sumOf { it.amount }
            listOf(tmInc, tmExp, lmInc, lmExp, tmDon, twDon, lwDon)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Emerald Hero Balance Card (Screenshot 8)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SidebarGradientTop),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-30).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text(
                                    text = strings.dashCurrentBalance,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                            Text(
                                text = formatCurrency(summary.balance),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontFamily = AppMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. প্রধান ফিচারসমূহ (2x2 Grid - Screenshot 8)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dashMainFeatures, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(
                            label = strings.dashTotalIncome,
                            value = formatCurrency(summary.totalIncome),
                            icon = Icons.Filled.ArrowDownward,
                            iconTint = SemanticSuccess,
                            iconBg = SemanticSuccessContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(3, 0) }
                        )
                        CompactStatCard(
                            label = strings.dashTotalExpense,
                            value = formatCurrency(summary.totalOutflow),
                            icon = Icons.Filled.ArrowUpward,
                            iconTint = SemanticError,
                            iconBg = SemanticErrorContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(3, 3) }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(
                            label = strings.totalMembers,
                            value = summary.totalMembers.toString(),
                            icon = Icons.Filled.Groups,
                            iconTint = InfoBlue,
                            iconBg = InfoBlueContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { if (isStaffReader) onNavigate(2, 0) else onNavigate(9, 0) }
                        )
                        CompactStatCard(
                            label = strings.dashNoticesEvents,
                            value = (notices.size + events.size).toString(),
                            icon = Icons.Filled.Notifications,
                            iconTint = SemanticWarning,
                            iconBg = SemanticWarningContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(5, 0) }
                        )
                    }
                }
            }

            // 3. বিস্তারিত আর্থিক বিবরণী (কমপ্যাক্ট) (Screenshot 9)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dashCompactFinancial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(strings.dashThisMonthIncome, formatCurrency(periodStats[0]), Icons.Filled.CalendarToday, SidebarGradientTop, EmeraldPrimaryContainer, Modifier.weight(1f))
                        CompactStatCard(strings.dashThisMonthExpense, formatCurrency(periodStats[1]), Icons.Filled.CalendarToday, SemanticError, SemanticErrorContainer, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(strings.dashLastMonthIncome, formatCurrency(periodStats[2]), Icons.Filled.History, InfoBlue, InfoBlueContainer, Modifier.weight(1f))
                        CompactStatCard(strings.dashLastMonthExpense, formatCurrency(periodStats[3]), Icons.Filled.Schedule, SemanticWarning, SemanticWarningContainer, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(strings.dashThisMonthDonation, formatCurrency(periodStats[4]), Icons.Filled.VolunteerActivism, PurpleAccent, PurpleContainer, Modifier.weight(1f))
                        CompactStatCard(strings.dashThisWeekDonation, formatCurrency(periodStats[5]), Icons.Filled.AutoAwesome, SemanticWarning, SemanticWarningContainer, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactStatCard(strings.dashLastWeekDonation, formatCurrency(periodStats[6]), Icons.Filled.Schedule, InfoBlue, InfoBlueContainer, Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. সদস্য ক্যাটাগরি (Screenshot 10)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.dashMemberCategories, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    CategoryRowCard(
                        title = strings.tabCommittee,
                        subtitle = "${committees.size} ${strings.dashMembersUnit}",
                        icon = Icons.Filled.VerifiedUser,
                        onClick = { if (isStaffReader) onNavigate(8, 0) else onNavigate(9, 0) }
                    )
                    CategoryRowCard(
                        title = strings.navFamilies,
                        subtitle = "${if (families.isNotEmpty()) families.size else summary.totalFamilies} ${strings.dashMembersUnit}",
                        icon = Icons.Filled.FamilyRestroom,
                        onClick = { if (isStaffReader) onNavigate(1, 0) else onNavigate(9, 0) }
                    )
                    CategoryRowCard(
                        title = strings.navAnalyticsTransparency,
                        subtitle = strings.tabPublicTransparency,
                        icon = Icons.Filled.AdminPanelSettings,
                        onClick = { onNavigate(6, 0) }
                    )
                }
            }

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
        }
    }
}

@Composable
private fun CompactStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = iconBg, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CategoryRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = EmeraldPrimaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = SidebarGradientTop, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
