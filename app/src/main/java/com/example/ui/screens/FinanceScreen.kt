package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.BudgetItem
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.data.model.WelfareRecord
import com.example.ui.components.BankAccountDialog
import com.example.ui.components.BudgetFormDialog
import com.example.ui.components.DonationFormDialog
import com.example.ui.components.DonationReceiptDialog
import com.example.ui.components.DueFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpenseFormDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.PaymentFormDialog
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(
    strings: AppStrings,
    canEditFinance: Boolean,
    canDeleteAdmin: Boolean,
    families: List<Family>,
    dues: List<DueItem>,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    bankAccounts: List<BankAccount>,
    budgets: List<BudgetItem>,
    welfareRecords: List<WelfareRecord>,
    initialSubTab: Int = 0,
    autoReceiptPreview: Boolean = false,
    quickPayFamily: Family? = null,
    quickPayAmount: Double? = null,
    onConsumeQuickPay: () -> Unit = {},
    onCreateDue: (title: String, dueType: String, amount: Double, month: Long, year: Long, active: Boolean) -> Unit,
    onToggleDueActive: (id: String, active: Boolean) -> Unit,
    onDeleteDue: (id: String) -> Unit,
    onRecordPayment: (family: Family, due: DueItem?, amount: Double, method: String, date: String, note: String?, account: BankAccount?, onRecorded: (PaymentRecord) -> Unit) -> Unit,
    onDeletePayment: (id: String) -> Unit,
    onRecordDonation: (donorName: String, anonymous: Boolean, fundType: String, amount: Double, method: String, trxRef: String?, date: String, note: String?, account: BankAccount?, onRecorded: (DonationRecord) -> Unit) -> Unit,
    onDeleteDonation: (id: String) -> Unit,
    onAddExpense: (title: String, category: String, amount: Double, date: String, note: String?, account: BankAccount?) -> Unit,
    onDeleteExpense: (id: String) -> Unit,
    onSaveBankAccount: (id: String?, name: String, type: String, number: String?, balance: Double, active: Boolean) -> Unit,
    onDeleteBankAccount: (id: String) -> Unit,
    onSaveBudget: (id: String?, category: String, periodLabel: String, budgetAmount: Double) -> Unit,
    onDeleteBudget: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(initialSubTab) } // 0=QuickEntry, 1=Donations, 2=Dues, 3=Expenses, 4=Accounts, 5=Budgets, 6=Ledger
    LaunchedEffect(initialSubTab) {
        selectedSubTab = initialSubTab
    }
    var quickListFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Income, 2=Expense, 3=Chanda
    var showPaymentDialog by remember(quickPayFamily) { mutableStateOf(quickPayFamily != null && canEditFinance) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showDueDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var previewPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var previewDonation by remember { mutableStateOf<DonationRecord?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val totalPayments = remember(payments) { payments.sumOf { it.amount } }
    val totalDonations = remember(donations) { donations.sumOf { it.amount } }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val totalWelfare = remember(welfareRecords) { welfareRecords.sumOf { it.amount } }
    val totalIncome = totalPayments + totalDonations
    val totalOutflow = totalExpenses + totalWelfare
    val currentBalance = totalIncome - totalOutflow

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canEditFinance && selectedSubTab in 1..5) {
                FloatingActionButton(
                    onClick = {
                        when (selectedSubTab) {
                            1 -> showDonationDialog = true
                            2 -> showDueDialog = true
                            3 -> showExpenseDialog = true
                            4 -> { editingAccount = null; showAccountDialog = true }
                            5 -> showBudgetDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("finance_add_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.save)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedSubTab != 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(selected = false, onClick = { selectedSubTab = 0 }, label = { Text(strings.navAccountsTab) }, modifier = Modifier.testTag("subtab_quick_entry"))
                            FilterChip(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, label = { Text("${strings.tabDonations} (${donations.size})") }, modifier = Modifier.testTag("subtab_donations"))
                            FilterChip(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, label = { Text("${strings.tabDues} (${dues.size})") }, modifier = Modifier.testTag("subtab_dues"))
                            FilterChip(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, label = { Text("${strings.tabExpenses} (${expenses.size})") }, modifier = Modifier.testTag("subtab_expenses"))
                            FilterChip(selected = selectedSubTab == 4, onClick = { selectedSubTab = 4 }, label = { Text("${strings.tabAccounts} (${bankAccounts.size})") })
                            FilterChip(selected = selectedSubTab == 5, onClick = { selectedSubTab = 5 }, label = { Text("${strings.tabBudgets} (${budgets.size})") })
                            FilterChip(selected = selectedSubTab == 6, onClick = { selectedSubTab = 6 }, label = { Text(strings.tabLedger) })
                        }
                    }
                }

                when (selectedSubTab) {
                    0 -> {
                        item {
                            AccountsBalanceHeroCard(
                                strings = strings,
                                currentBalance = currentBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalOutflow
                            )
                        }
                        if (canEditFinance) {
                            item {
                                AccountsQuickEntryCard(
                                    strings = strings,
                                    todayStr = todayStr,
                                    families = families,
                                    dues = dues,
                                    bankAccounts = bankAccounts,
                                    onRecordPayment = { fam, due, amt, method, date, acc ->
                                        onRecordPayment(fam, due, amt, method, date, null, acc) { rec ->
                                            if (autoReceiptPreview) previewPayment = rec
                                        }
                                    },
                                    onRecordDonation = { donor, anon, fund, amt, method, date, acc ->
                                        onRecordDonation(donor, anon, fund, amt, method, null, date, null, acc) { rec ->
                                            if (autoReceiptPreview) previewDonation = rec
                                        }
                                    },
                                    onAddExpense = { title, cat, amt, date, acc ->
                                        onAddExpense(title, cat, amt, date, null, acc)
                                    }
                                )
                            }
                        }
                        item {
                            AccountsFilterCapsule(
                                strings = strings,
                                selectedFilter = quickListFilter,
                                onSelectFilter = { quickListFilter = it }
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(selected = false, onClick = { selectedSubTab = 2 }, label = { Text("${strings.tabDues} (${dues.size})") }, modifier = Modifier.testTag("subtab_dues"))
                                FilterChip(selected = false, onClick = { selectedSubTab = 4 }, label = { Text("${strings.tabAccounts} (${bankAccounts.size})") }, modifier = Modifier.testTag("subtab_accounts"))
                                FilterChip(selected = false, onClick = { selectedSubTab = 5 }, label = { Text("${strings.tabBudgets} (${budgets.size})") }, modifier = Modifier.testTag("subtab_budgets"))
                                FilterChip(selected = false, onClick = { selectedSubTab = 6 }, label = { Text(strings.tabLedger) }, modifier = Modifier.testTag("subtab_ledger"))
                            }
                        }
                        quickEntryFilteredList(
                            strings = strings,
                            filterIdx = quickListFilter,
                            canEditFinance = canEditFinance,
                            payments = payments,
                            donations = donations,
                            expenses = expenses,
                            welfareRecords = welfareRecords,
                            onPreviewPayment = { previewPayment = it },
                            onDeletePayment = onDeletePayment,
                            onPreviewDonation = { previewDonation = it },
                            onDeleteDonation = onDeleteDonation,
                            onDeleteExpense = onDeleteExpense
                        )
                    }

                    1 -> donationsTabContent(
                        strings = strings,
                        donations = donations,
                        canEditFinance = canEditFinance,
                        onPreviewDonation = { previewDonation = it },
                        onDeleteDonation = onDeleteDonation
                    )

                    2 -> {
                        if (dues.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.RequestQuote, strings.emptyDues) }
                        } else {
                            items(dues, key = { it.id }) { due ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(due.title, style = MaterialTheme.typography.titleMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                val typeText = when (due.dueType) {
                                                    "monthly" -> strings.dueTypeMonthly
                                                    "special" -> strings.dueTypeSpecial
                                                    else -> strings.dueTypeYearly
                                                }
                                                StatusPill(
                                                    typeText,
                                                    if (due.active) SemanticSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                    if (due.active) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(if (due.dueType == "monthly") "${due.dueMonth}/${due.dueYear}" else "${due.dueYear}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(formatCurrency(due.amount), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                        if (canEditFinance) {
                                            Switch(checked = due.active, onCheckedChange = { onToggleDueActive(due.id, it) })
                                            IconButton(onClick = { onDeleteDue(due.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            MetricTile(strings.totalExpense, formatCurrency(totalExpenses), Icons.Filled.ShoppingBag, SemanticError, SemanticErrorContainer)
                        }
                        if (expenses.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.ShoppingBag, strings.emptyExpenses) }
                        } else {
                            items(expenses, key = { it.id }) { expense ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(expense.title, style = MaterialTheme.typography.titleMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                StatusPill(expense.category, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                                if (!expense.voucherNo.isNullOrBlank()) {
                                                    StatusPill(expense.voucherNo, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            val meta = listOfNotNull(expense.expenseDate, expense.accountName, expense.note).joinToString(" • ")
                                            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(formatCurrency(expense.amount), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticError)
                                        if (canEditFinance) {
                                            IconButton(onClick = { onDeleteExpense(expense.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> accountsTabContent(
                        strings = strings,
                        accounts = bankAccounts,
                        canEditFinance = canEditFinance,
                        canDeleteAdmin = canDeleteAdmin,
                        onEditAccount = { editingAccount = it; showAccountDialog = true },
                        onDeleteAccount = onDeleteBankAccount
                    )

                    5 -> budgetsTabContent(
                        strings = strings,
                        budgets = budgets,
                        expenses = expenses,
                        canEditFinance = canEditFinance,
                        onDeleteBudget = onDeleteBudget
                    )

                    else -> ledgerTabContent(strings, payments, donations, expenses, welfareRecords)
                }
            }
        }
    }

    if (showPaymentDialog) {
        if (families.isEmpty()) {
            showPaymentDialog = false
            onConsumeQuickPay()
        } else {
            PaymentFormDialog(
                strings = strings,
                families = families,
                dues = dues,
                accounts = bankAccounts,
                initialFamily = quickPayFamily,
                initialAmount = quickPayAmount,
                onDismiss = { showPaymentDialog = false; onConsumeQuickPay() },
                onSave = { fam, due, amt, method, date, note, acc ->
                    onRecordPayment(fam, due, amt, method, date, note, acc) { rec ->
                        if (autoReceiptPreview) previewPayment = rec
                    }
                    showPaymentDialog = false
                    onConsumeQuickPay()
                }
            )
        }
    }

    if (showDonationDialog) {
        DonationFormDialog(
            strings = strings,
            accounts = bankAccounts,
            onDismiss = { showDonationDialog = false },
            onSave = { donor, anon, fund, amt, method, trxRef, date, note, acc ->
                onRecordDonation(donor, anon, fund, amt, method, trxRef, date, note, acc) { rec ->
                    if (autoReceiptPreview) previewDonation = rec
                }
                showDonationDialog = false
            }
        )
    }

    if (showDueDialog) {
        DueFormDialog(
            strings = strings,
            onDismiss = { showDueDialog = false },
            onSave = { title, type, amt, month, year, active ->
                onCreateDue(title, type, amt, month, year, active)
                showDueDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        ExpenseFormDialog(
            strings = strings,
            accounts = bankAccounts,
            onDismiss = { showExpenseDialog = false },
            onSave = { title, cat, amt, date, note, acc ->
                onAddExpense(title, cat, amt, date, note, acc)
                showExpenseDialog = false
            }
        )
    }

    if (showAccountDialog) {
        BankAccountDialog(
            strings = strings,
            existing = editingAccount,
            onDismiss = { showAccountDialog = false },
            onSave = { name, type, number, bal, active ->
                onSaveBankAccount(editingAccount?.id, name, type, number, bal, active)
                showAccountDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        BudgetFormDialog(
            strings = strings,
            onDismiss = { showBudgetDialog = false },
            onSave = { cat, period, amt ->
                onSaveBudget(null, cat, period, amt)
                showBudgetDialog = false
            }
        )
    }

    previewPayment?.let { payment ->
        ReceiptPreviewDialog(strings, payment, { previewPayment = null }, { ReportExporter.sharePaymentReceiptText(context, payment) })
    }

    previewDonation?.let { donation ->
        DonationReceiptDialog(strings, donation, { previewDonation = null }, { ReportExporter.shareDonationReceiptText(context, donation) })
    }
}
