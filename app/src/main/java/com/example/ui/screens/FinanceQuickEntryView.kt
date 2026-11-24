package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.DonationRecord
import com.example.data.model.DueItem
import com.example.data.model.ExpenseRecord
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.data.model.WelfareRecord
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SidebarGradientTop

@Composable
internal fun AccountsBalanceHeroCard(
    strings: AppStrings,
    currentBalance: Double,
    totalIncome: Double,
    totalExpense: Double
) {
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
                    .offset(x = 32.dp, y = (-32).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.dashCurrentBalance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = formatCurrency(currentBalance),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontFamily = AppMonoFontFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = strings.dashCurrentBalance,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroSummaryMetricBox(
                        label = strings.dashTotalIncome,
                        value = formatCurrency(totalIncome),
                        icon = Icons.Filled.ArrowDownward,
                        arrowTint = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )
                    HeroSummaryMetricBox(
                        label = strings.dashTotalExpense,
                        value = formatCurrency(totalExpense),
                        icon = Icons.Filled.ArrowUpward,
                        arrowTint = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryMetricBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    arrowTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = arrowTint, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountsQuickEntryCard(
    strings: AppStrings,
    todayStr: String,
    families: List<Family>,
    dues: List<DueItem>,
    bankAccounts: List<BankAccount>,
    onRecordPayment: (family: Family, due: DueItem?, amount: Double, method: String, date: String, account: BankAccount?) -> Unit,
    onRecordDonation: (donorName: String, anonymous: Boolean, fundType: String, amount: Double, method: String, date: String, account: BankAccount?) -> Unit,
    onAddExpense: (title: String, category: String, amount: Double, date: String, account: BankAccount?) -> Unit
) {
    var entryTab by remember { mutableIntStateOf(0) } // 0=Chanda, 1=Income, 2=Expense
    var entryDate by remember(todayStr) { mutableStateOf(todayStr) }
    var amountText by remember { mutableStateOf("") }

    val activeFamilies = remember(families) { families.filter { it.active }.ifEmpty { families } }
    val activeDues = remember(dues) { dues.filter { it.active }.ifEmpty { dues } }
    val activeAccounts = remember(bankAccounts) { bankAccounts.filter { it.active } }

    var selectedFamily by remember(activeFamilies) { mutableStateOf<Family?>(null) }
    var famExpanded by remember { mutableStateOf(false) }

    var selectedDue by remember(activeDues) { mutableStateOf<DueItem?>(null) }
    var dueExpanded by remember { mutableStateOf(false) }

    var selectedAccount by remember(activeAccounts) { mutableStateOf<BankAccount?>(null) }
    var accExpanded by remember { mutableStateOf(false) }

    // Donation fields
    var donorName by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var fundType by remember { mutableStateOf("general") }
    var payMethod by remember { mutableStateOf("cash") }

    // Expense fields
    var expenseTitle by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("maintenance") }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 3 Underlined Tabs (চাঁদা | আয় | ব্যয়)
            Row(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(strings.accTabChanda, strings.accTabIncome, strings.accTabExpense)
                tabs.forEachIndexed { idx, label ->
                    val selected = entryTab == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { entryTab = idx }
                            .testTag("quick_entry_tab_$idx"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) SidebarGradientTop else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selected) 2.5.dp else 1.dp)
                                .background(
                                    if (selected) SidebarGradientTop
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (entryTab) {
                    0 -> {
                        ExposedDropdownMenuBox(
                            expanded = famExpanded,
                            onExpandedChange = { famExpanded = !famExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFamily?.let { "${it.familyCode} — ${it.familyName}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text(strings.accSelectMember) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = famExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("quick_chanda_family_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = famExpanded,
                                onDismissRequest = { famExpanded = false }
                            ) {
                                activeFamilies.forEach { fam ->
                                    DropdownMenuItem(
                                        text = { Text("${fam.familyCode} — ${fam.familyName}") },
                                        onClick = {
                                            selectedFamily = fam
                                            famExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = entryDate,
                                onValueChange = { entryDate = it },
                                label = { Text(strings.dateLabel) },
                                trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = dueExpanded,
                                onExpandedChange = { dueExpanded = !dueExpanded },
                                modifier = Modifier.weight(1.1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedDue?.title ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text(strings.accSelectDue, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dueExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dueExpanded,
                                    onDismissRequest = { dueExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("—") },
                                        onClick = { selectedDue = null; dueExpanded = false }
                                    )
                                    activeDues.forEach { due ->
                                        DropdownMenuItem(
                                            text = { Text("${due.title} (${formatCurrency(due.amount)})") },
                                            onClick = {
                                                selectedDue = due
                                                if (amountText.isBlank() && due.amount > 0.0) {
                                                    amountText = due.amount.toLong().toString()
                                                }
                                                dueExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.anonymousDonor, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = anonymous, onCheckedChange = { anonymous = it })
                        }
                        if (!anonymous) {
                            OutlinedTextField(
                                value = donorName,
                                onValueChange = { donorName = it },
                                label = { Text(strings.donorName) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("general", "zakat", "fitrah", "sadqah", "construction", "orphan_welfare").forEach { ft ->
                                FilterChip(
                                    selected = fundType == ft,
                                    onClick = { fundType = ft },
                                    label = { Text(ft.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("cash", "bkash", "nagad", "bank").forEach { pm ->
                                FilterChip(
                                    selected = payMethod == pm,
                                    onClick = { payMethod = pm },
                                    label = { Text(pm.uppercase(), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = entryDate,
                            onValueChange = { entryDate = it },
                            label = { Text(strings.dateLabel) },
                            trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        OutlinedTextField(
                            value = expenseTitle,
                            onValueChange = { expenseTitle = it },
                            label = { Text(strings.expenseTitle) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("imam_salary", "electricity", "maintenance", "cleaning", "other").forEach { cat ->
                                FilterChip(
                                    selected = expenseCategory == cat,
                                    onClick = { expenseCategory = cat },
                                    label = { Text(cat.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = entryDate,
                            onValueChange = { entryDate = it },
                            label = { Text(strings.dateLabel) },
                            trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Amount + Account Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeAccounts.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = accExpanded,
                            onExpandedChange = { accExpanded = !accExpanded },
                            modifier = Modifier.weight(0.85f)
                        ) {
                            OutlinedTextField(
                                value = selectedAccount?.accountName ?: "Cash",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = accExpanded,
                                onDismissRequest = { accExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cash") },
                                    onClick = { selectedAccount = null; accExpanded = false }
                                )
                                activeAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.accountName) },
                                        onClick = { selectedAccount = acc; accExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text(strings.accAmountHint) },
                        leadingIcon = {
                            Text(
                                text = strings.currencySymbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.15f).testTag("quick_entry_amount_input")
                    )
                }

                val parsedAmt = amountText.trim().toDoubleOrNull() ?: 0.0
                val canSubmit = parsedAmt > 0.0 && entryDate.isNotBlank() && when (entryTab) {
                    0 -> selectedFamily != null
                    1 -> anonymous || donorName.isNotBlank()
                    else -> expenseTitle.isNotBlank()
                }

                Button(
                    onClick = {
                        when (entryTab) {
                            0 -> selectedFamily?.let { fam ->
                                onRecordPayment(fam, selectedDue, parsedAmt, "cash", entryDate.trim(), selectedAccount)
                                amountText = ""
                            }
                            1 -> {
                                val resolvedDonor = if (anonymous) strings.anonymousDonor else donorName.trim()
                                onRecordDonation(resolvedDonor, anonymous, fundType, parsedAmt, payMethod, entryDate.trim(), selectedAccount)
                                amountText = ""
                                donorName = ""
                            }
                            else -> {
                                onAddExpense(expenseTitle.trim(), expenseCategory, parsedAmt, entryDate.trim(), selectedAccount)
                                amountText = ""
                                expenseTitle = ""
                            }
                        }
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SidebarGradientTop),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("quick_entry_submit_button")
                ) {
                    Text(
                        text = strings.accSubmitButton,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountsFilterCapsule(
    strings: AppStrings,
    selectedFilter: Int,
    onSelectFilter: (Int) -> Unit
) {
    val options = listOf(
        strings.accFilterAll,
        strings.accTabIncome,
        strings.accTabExpense,
        strings.accTabChanda
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { idx, label ->
                val selected = selectedFilter == idx
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) SidebarGradientTop else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectFilter(idx) }
                        .testTag("accounts_filter_$idx")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.quickEntryFilteredList(
    strings: AppStrings,
    filterIdx: Int,
    canEditFinance: Boolean,
    payments: List<PaymentRecord>,
    donations: List<DonationRecord>,
    expenses: List<ExpenseRecord>,
    welfareRecords: List<WelfareRecord>,
    onPreviewPayment: (PaymentRecord) -> Unit,
    onDeletePayment: (String) -> Unit,
    onPreviewDonation: (DonationRecord) -> Unit,
    onDeleteDonation: (String) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    when (filterIdx) {
        1 -> {
            if (donations.isEmpty()) {
                item { EmptyStateCard(Icons.Filled.ReceiptLong, strings.emptyDonations) }
            } else {
                items(donations, key = { "qd_${it.id}" }) { d ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (d.anonymous) strings.anonymousDonor else d.donorName, fontWeight = FontWeight.Bold)
                                Text("${d.donationDate} • ${d.fundType.uppercase()} • ${d.receiptNo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("+${formatCurrency(d.amount)}", style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                            IconButton(onClick = { onPreviewDonation(d) }) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = strings.previewReceipt, tint = MaterialTheme.colorScheme.primary)
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

        2 -> {
            if (expenses.isEmpty()) {
                item { EmptyStateCard(Icons.Filled.ReceiptLong, strings.emptyExpenses) }
            } else {
                items(expenses, key = { "qe_${it.id}" }) { e ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(e.title, fontWeight = FontWeight.Bold)
                                Text("${e.expenseDate} • ${e.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("-${formatCurrency(e.amount)}", style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticError)
                            if (canEditFinance) {
                                IconButton(onClick = { onDeleteExpense(e.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        3 -> {
            if (payments.isEmpty()) {
                item { EmptyStateCard(Icons.Filled.ReceiptLong, strings.emptyPayments) }
            } else {
                items(payments, key = { "qp_${it.id}" }) { p ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.familyName, fontWeight = FontWeight.Bold)
                                val meta = listOfNotNull(p.paymentDate, p.dueTitle, p.receiptNo).joinToString(" • ")
                                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("+${formatCurrency(p.amount)}", style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                            IconButton(onClick = { onPreviewPayment(p) }) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = strings.previewReceipt, tint = MaterialTheme.colorScheme.primary)
                            }
                            if (canEditFinance) {
                                IconButton(onClick = { onDeletePayment(p.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> ledgerTabContent(strings, payments, donations, expenses, welfareRecords)
    }
}
