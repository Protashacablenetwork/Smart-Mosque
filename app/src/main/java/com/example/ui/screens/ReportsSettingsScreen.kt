package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DonationRecord
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.Notice
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicSummary
import com.example.data.model.WelfareRecord
import com.example.ui.components.BreakdownBarList
import com.example.ui.components.CollectionVsExpenseChart
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatKg
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.util.ReportExporter

@Composable
fun ReportsScreen(
    strings: AppStrings,
    isStaffReader: Boolean,
    mosqueName: String,
    summary: PublicSummary,
    notices: List<Notice>,
    families: List<Family>,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    welfareRecords: List<WelfareRecord>,
    initialSubTab: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember(isStaffReader) {
        mutableIntStateOf(if (!isStaffReader) 1 else (initialSubTab ?: 0))
    }
    LaunchedEffect(initialSubTab, isStaffReader) {
        selectedTab = if (!isStaffReader) 1 else (initialSubTab ?: 0)
    }

    val expenseByCategory = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    val collectionByMethod = remember(payments, donations) {
        val merged = mutableMapOf<String, Double>()
        payments.forEach { p -> val k = p.paymentMethod.uppercase(); merged[k] = (merged[k] ?: 0.0) + p.amount }
        donations.forEach { d -> val k = d.paymentMethod.uppercase(); merged[k] = (merged[k] ?: 0.0) + d.amount }
        merged.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    val topPayingFamilies = remember(payments, families) {
        val famNames = families.associateBy({ it.id }, { it.familyName })
        payments.groupBy { it.familyId }
            .map { (fid, list) ->
                val name = famNames[fid] ?: list.firstOrNull()?.familyName ?: fid
                name to list.sumOf { it.amount }
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isStaffReader) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text(strings.tabFinancialAnalytics) },
                            modifier = Modifier.testTag("tab_financial_analytics")
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text(strings.tabPublicTransparency) },
                            modifier = Modifier.testTag("tab_public_transparency")
                        )
                    }
                }
            }

            if (selectedTab == 0 && isStaffReader) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { ReportExporter.exportPublicSummaryPdf(context, summary, notices, payments, expenses, donations, welfareRecords) },
                            modifier = Modifier.weight(1f).testTag("analytics_export_pdf")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportPdf)
                        }
                        OutlinedButton(
                            onClick = { ReportExporter.exportPublicSummaryCsv(context, summary, payments, expenses, donations, welfareRecords) },
                            modifier = Modifier.weight(1f).testTag("analytics_export_csv")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportCsv)
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
                            Text(strings.financialSummary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            SummaryRow(strings.totalCollection, formatCurrency(summary.totalCollection), SemanticSuccess)
                            HorizontalDivider()
                            SummaryRow(strings.totalDonations, formatCurrency(summary.totalDonations), SemanticSuccess)
                            HorizontalDivider()
                            SummaryRow(strings.totalExpense, formatCurrency(summary.totalExpense), SemanticError)
                            HorizontalDivider()
                            SummaryRow(strings.totalWelfare, formatCurrency(summary.totalWelfare), InfoBlue)
                            HorizontalDivider()
                            SummaryRow(strings.netBalance, formatCurrency(summary.balance), SemanticSuccess)
                            HorizontalDivider()
                            SummaryRow(strings.outstandingDues, formatCurrency(summary.totalOutstanding), InfoBlue)
                            HorizontalDivider()
                            SummaryRow(strings.totalQurbaniMeat, "${formatKg(summary.totalQurbaniMeatKg)} (${summary.deliveredDistributions}/${summary.totalDistributions})", MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

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

                item { BreakdownBarList(strings.methodBreakdown, collectionByMethod, SemanticSuccess) }
                item { BreakdownBarList(strings.categoryBreakdown, expenseByCategory, SemanticError) }

                if (topPayingFamilies.isNotEmpty()) {
                    item { BreakdownBarList(strings.topPayingFamilies, topPayingFamilies, InfoBlue) }
                }

                if (summary.recentMonths.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(strings.monthlySummaryTable, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                summary.recentMonths.forEachIndexed { idx, month ->
                                    val inc = summary.recentCollections.getOrElse(idx) { 0.0 }
                                    val exp = summary.recentExpenses.getOrElse(idx) { 0.0 }
                                    val net = inc - exp
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(month, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("+${formatCurrency(inc)} / -${formatCurrency(exp)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                formatCurrency(net),
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily),
                                                fontWeight = FontWeight.Bold,
                                                color = if (net >= 0) SemanticSuccess else SemanticError
                                            )
                                            StatusPill(
                                                text = if (net >= 0) strings.profitStatus else strings.deficitStatus,
                                                containerColor = if (net >= 0) SemanticSuccessContainer else SemanticErrorContainer,
                                                contentColor = if (net >= 0) SemanticSuccess else SemanticError
                                            )
                                        }
                                    }
                                    if (idx < summary.recentMonths.lastIndex) HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.tabPublicTransparency, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(mosqueName.ifBlank { strings.appTitle }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                            Text(strings.financialSummary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            SummaryRow(strings.totalFamilies, summary.totalFamilies.toString(), MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider()
                            SummaryRow(strings.totalMembers, summary.totalMembers.toString(), MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider()
                            SummaryRow(strings.totalCollection, formatCurrency(summary.totalCollection), SemanticSuccess)
                            HorizontalDivider()
                            SummaryRow(strings.totalDonations, formatCurrency(summary.totalDonations), SemanticSuccess)
                            HorizontalDivider()
                            SummaryRow(strings.totalExpense, formatCurrency(summary.totalExpense), SemanticError)
                            HorizontalDivider()
                            SummaryRow(strings.totalWelfare, formatCurrency(summary.totalWelfare), InfoBlue)
                            HorizontalDivider()
                            SummaryRow(strings.netBalance, formatCurrency(summary.balance), SemanticSuccess)
                        }
                    }
                }

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

                if (expenseByCategory.isNotEmpty()) {
                    item { BreakdownBarList(strings.categoryBreakdown, expenseByCategory, SemanticError) }
                }

                item {
                    Button(
                        onClick = { ReportExporter.exportTransparencyPdf(context, mosqueName, summary, expenses) },
                        modifier = Modifier.fillMaxWidth().testTag("export_transparency_pdf")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportPdf)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = valueColor)
    }
}
