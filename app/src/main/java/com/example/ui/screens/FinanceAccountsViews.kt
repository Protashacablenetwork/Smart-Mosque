package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.BudgetItem
import com.example.data.model.DonationRecord
import com.example.data.model.ExpenseRecord
import com.example.data.model.PaymentRecord
import com.example.data.model.WelfareRecord
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnEmeraldPrimaryContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning

internal data class UnifiedLedgerEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
    val amount: Double,
    val isInflow: Boolean
)

internal fun LazyListScope.donationsTabContent(
    strings: AppStrings,
    donations: List<DonationRecord>,
    canEditFinance: Boolean,
    onPreviewDonation: (DonationRecord) -> Unit,
    onDeleteDonation: (String) -> Unit
) {
    val totalDon = donations.sumOf { it.amount }
    item {
        MetricTile(
            title = strings.totalDonations,
            value = formatCurrency(totalDon),
            icon = Icons.Filled.VolunteerActivism,
            accentColor = OnEmeraldPrimaryContainer,
            containerColor = EmeraldPrimaryContainer
        )
    }
    if (donations.isEmpty()) {
        item { EmptyStateCard(Icons.Filled.VolunteerActivism, strings.emptyDonations) }
    } else {
        items(donations, key = { it.id }) { d ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(d.receiptNo, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            StatusPill(d.fundType.uppercase(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(formatCurrency(d.amount), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                    }
                    Text(if (d.anonymous) strings.anonymousDonor else d.donorName, style = MaterialTheme.typography.titleMedium)
                    val meta = listOfNotNull(d.donationDate, d.paymentMethod.uppercase(), d.transactionRef?.let { "TrxID: $it" }, d.accountName).joinToString(" • ")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Row {
                            TextButton(onClick = { onPreviewDonation(d) }) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.previewReceipt)
                            }
                            if (canEditFinance) {
                                IconButton(onClick = { onDeleteDonation(d.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.accountsTabContent(
    strings: AppStrings,
    accounts: List<BankAccount>,
    canEditFinance: Boolean,
    canDeleteAdmin: Boolean,
    onEditAccount: (BankAccount) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    val totalCashBalance = accounts.filter { it.active }.sumOf { it.balance }
    item {
        MetricTile(
            title = strings.netBalance,
            value = formatCurrency(totalCashBalance),
            icon = Icons.Filled.AccountBalance,
            accentColor = OnEmeraldPrimaryContainer,
            containerColor = EmeraldPrimaryContainer
        )
    }
    if (accounts.isEmpty()) {
        item { EmptyStateCard(Icons.Filled.AccountBalance, strings.emptyAccounts) }
    } else {
        items(accounts, key = { it.id }) { acc ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(acc.accountType.uppercase(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(acc.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (!acc.accountNumber.isNullOrBlank()) {
                            Text(acc.accountNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(formatCurrency(acc.balance), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                    if (canEditFinance) {
                        IconButton(onClick = { onEditAccount(acc) }) { Icon(Icons.Filled.Edit, contentDescription = strings.save) }
                    }
                    if (canDeleteAdmin) {
                        IconButton(onClick = { onDeleteAccount(acc.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.budgetsTabContent(
    strings: AppStrings,
    budgets: List<BudgetItem>,
    expenses: List<ExpenseRecord>,
    canEditFinance: Boolean,
    onDeleteBudget: (String) -> Unit
) {
    if (budgets.isEmpty()) {
        item { EmptyStateCard(Icons.Filled.PieChart, strings.emptyBudgets) }
    } else {
        items(budgets, key = { it.id }) { b ->
            val spent = expenses.filter { it.category == b.category && it.expenseDate.startsWith(b.periodLabel) }.sumOf { it.amount }
            val ratio = if (b.budgetAmount > 0.0) (spent / b.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
            val overBudget = spent > b.budgetAmount
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(b.periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatCurrency(b.budgetAmount), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold)
                        if (canEditFinance) {
                            IconButton(onClick = { onDeleteBudget(b.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        color = if (overBudget) SemanticError else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${strings.spentLabel}: ${formatCurrency(spent)}", style = MaterialTheme.typography.labelSmall, color = if (overBudget) SemanticError else SemanticWarning)
                        Text("${strings.remainingLabel}: ${formatCurrency(b.budgetAmount - spent)}", style = MaterialTheme.typography.labelSmall, color = SemanticSuccess)
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.ledgerTabContent(
    strings: AppStrings,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    welfareRecords: List<WelfareRecord>
) {
    val entries = (
        payments.map { UnifiedLedgerEntry("p_${it.id}", it.familyName, "${it.receiptNo} • ${it.paymentMethod.uppercase()}", it.paymentDate, it.amount, true) } +
            donations.map {
                val donor = if (it.anonymous) strings.anonymousDonor else it.donorName
                UnifiedLedgerEntry("d_${it.id}", donor, "${it.fundType.uppercase()} • ${it.receiptNo}", it.donationDate, it.amount, true)
            } +
            expenses.map { UnifiedLedgerEntry("e_${it.id}", it.title, "${it.category} • ${it.voucherNo ?: "-"}", it.expenseDate, it.amount, false) } +
            welfareRecords.filter { it.amount > 0.0 }.map { UnifiedLedgerEntry("w_${it.id}", it.beneficiaryName, it.recordType.replace('_', ' '), it.recordDate, it.amount, false) }
        ).sortedByDescending { it.date }

    if (entries.isEmpty()) {
        item { EmptyStateCard(Icons.Filled.ReceiptLong, strings.emptyPayments) }
    } else {
        items(entries, key = { it.id }) { e ->
            Card(
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${e.date} • ${e.subtitle}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = (if (e.isInflow) "+" else "-") + formatCurrency(e.amount),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (e.isInflow) SemanticSuccess else SemanticError
                    )
                }
            }
        }
    }
}
