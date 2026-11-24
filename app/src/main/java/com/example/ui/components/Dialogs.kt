package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.DueItem
import com.example.data.model.Family
import com.example.data.model.SocietyMember
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun FamilyFormDialog(
    strings: AppStrings,
    existing: Family?,
    suggestedCode: String,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, phone: String?, address: String?, linkedEmail: String?, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.familyName ?: "") }
    var code by remember { mutableStateOf(existing?.familyCode ?: suggestedCode) }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var linkedEmail by remember { mutableStateOf(existing?.linkedEmail ?: "") }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) strings.addFamily else strings.editFamily) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text(strings.familyName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("family_name_input")
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 30) code = it },
                    label = { Text(strings.familyCode) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("family_code_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 30) phone = it },
                    label = { Text(strings.phone) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("family_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { if (it.length <= 200) address = it },
                    label = { Text(strings.address) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("family_address_input")
                )
                OutlinedTextField(
                    value = linkedEmail,
                    onValueChange = { if (it.length <= 120) linkedEmail = it },
                    label = { Text(strings.linkedEmailLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("family_email_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (active) strings.activeStatus else strings.inactiveStatus)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        code,
                        phone.takeIf { it.isNotBlank() },
                        address.takeIf { it.isNotBlank() },
                        linkedEmail.takeIf { it.isNotBlank() },
                        active
                    )
                },
                enabled = name.isNotBlank() && code.isNotBlank(),
                modifier = Modifier.testTag("save_family_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFormDialog(
    strings: AppStrings,
    existing: SocietyMember?,
    families: List<Family> = emptyList(),
    preselectedFamily: Family? = null,
    onDismiss: () -> Unit,
    onSave: (family: Family?, name: String, phone: String?, relation: String?, occupation: String?, head: Boolean, active: Boolean) -> Unit
) {
    var selectedFamily by remember {
        mutableStateOf(
            preselectedFamily ?: families.firstOrNull { it.id == existing?.familyId } ?: families.firstOrNull()
        )
    }
    var familyExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(existing?.memberName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var relation by remember { mutableStateOf(existing?.relation ?: "") }
    var occupation by remember { mutableStateOf(existing?.occupation ?: "") }
    var head by remember { mutableStateOf(existing?.head ?: false) }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) strings.addMember else strings.editMember) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (preselectedFamily == null && families.isNotEmpty() && existing == null) {
                    ExposedDropdownMenuBox(
                        expanded = familyExpanded,
                        onExpandedChange = { familyExpanded = !familyExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFamily?.let { "${it.familyCode} - ${it.familyName}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectFamily) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = familyExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = familyExpanded,
                            onDismissRequest = { familyExpanded = false }
                        ) {
                            families.forEach { fam ->
                                DropdownMenuItem(
                                    text = { Text("${fam.familyCode} - ${fam.familyName}") },
                                    onClick = {
                                        selectedFamily = fam
                                        familyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text(strings.memberName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 30) phone = it },
                    label = { Text(strings.phone) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relation,
                    onValueChange = { if (it.length <= 40) relation = it },
                    label = { Text(strings.relation) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { if (it.length <= 60) occupation = it },
                    label = { Text(strings.occupation) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.familyHead)
                    Switch(checked = head, onCheckedChange = { head = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.activeStatus)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        selectedFamily,
                        name,
                        phone.takeIf { it.isNotBlank() },
                        relation.takeIf { it.isNotBlank() },
                        occupation.takeIf { it.isNotBlank() },
                        head,
                        active
                    )
                },
                enabled = name.isNotBlank() && (preselectedFamily != null || existing != null || selectedFamily != null),
                modifier = Modifier.testTag("save_member_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun DueFormDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (title: String, dueType: String, amount: Double, month: Long, year: Long, active: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueType by remember { mutableStateOf("monthly") }
    var amountText by remember { mutableStateOf("") }
    var monthText by remember { mutableStateOf("11") }
    var yearText by remember { mutableStateOf("2026") }
    var active by remember { mutableStateOf(true) }

    val amount = amountText.toDoubleOrNull()
    val month = (monthText.toLongOrNull() ?: 0L).coerceIn(0L, 12L)
    val year = (yearText.toLongOrNull() ?: 2026L).coerceIn(2000L, 2100L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addDue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text(strings.dueTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("due_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = dueType == "monthly",
                        onClick = { dueType = "monthly" },
                        label = { Text(strings.dueTypeMonthly) }
                    )
                    FilterChip(
                        selected = dueType == "yearly",
                        onClick = { dueType = "yearly"; monthText = "0" },
                        label = { Text(strings.dueTypeYearly) }
                    )
                    FilterChip(
                        selected = dueType == "special",
                        onClick = { dueType = "special"; monthText = "0" },
                        label = { Text(strings.dueTypeSpecial) }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("due_amount_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dueType == "monthly") {
                        OutlinedTextField(
                            value = monthText,
                            onValueChange = { monthText = it },
                            label = { Text(strings.monthLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text(strings.yearLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.activeDueToggle)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (amount != null) onSave(title, dueType, amount, month, year, active) },
                enabled = title.isNotBlank() && amount != null && amount >= 0.0,
                modifier = Modifier.testTag("save_due_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormDialog(
    strings: AppStrings,
    families: List<Family>,
    dues: List<DueItem>,
    accounts: List<BankAccount> = emptyList(),
    initialFamily: Family? = null,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onSave: (family: Family, due: DueItem?, amount: Double, method: String, date: String, note: String?, account: BankAccount?) -> Unit
) {
    var selectedFamily by remember { mutableStateOf(initialFamily ?: families.firstOrNull()) }
    var selectedDue by remember { mutableStateOf<DueItem?>(null) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull { it.active }) }
    var familyExpanded by remember { mutableStateOf(false) }
    var dueExpanded by remember { mutableStateOf(false) }
    var accExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf(initialAmount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
    var method by remember { mutableStateOf("cash") }
    var dateText by remember { mutableStateOf(todayIso()) }
    var noteText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.recordPayment) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = familyExpanded,
                    onExpandedChange = { familyExpanded = !familyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedFamily?.let { "${it.familyCode} - ${it.familyName}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectFamily) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = familyExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = familyExpanded,
                        onDismissRequest = { familyExpanded = false }
                    ) {
                        families.forEach { fam ->
                            DropdownMenuItem(
                                text = { Text("${fam.familyCode} - ${fam.familyName}") },
                                onClick = {
                                    selectedFamily = fam
                                    familyExpanded = false
                                }
                            )
                        }
                    }
                }

                if (dues.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = dueExpanded,
                        onExpandedChange = { dueExpanded = !dueExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDue?.title ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectDueOptional) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dueExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dueExpanded,
                            onDismissRequest = { dueExpanded = false }
                        ) {
                            dues.filter { it.active }.forEach { due ->
                                DropdownMenuItem(
                                    text = { Text("${due.title} (${formatCurrency(due.amount)})") },
                                    onClick = {
                                        selectedDue = due
                                        if (amountText.isBlank()) amountText = due.amount.toString()
                                        dueExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text(strings.paymentMethod)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cash" to "Cash", "bkash" to "bKash", "nagad" to "Nagad", "bank" to "Bank").forEach { (key, label) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(label) }
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accExpanded,
                        onExpandedChange = { accExpanded = !accExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.accountName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectAccountOptional) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accExpanded,
                            onDismissRequest = { accExpanded = false }
                        ) {
                            accounts.filter { it.active }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.accountName} (${formatCurrency(acc.balance)})") },
                                    onClick = {
                                        selectedAccount = acc
                                        accExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 20) dateText = it },
                    label = { Text(strings.dateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 300) noteText = it },
                    label = { Text(strings.noteOptional) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fam = selectedFamily
                    if (fam != null && amount != null && amount > 0.0) {
                        onSave(fam, selectedDue, amount, method, dateText, noteText.takeIf { it.isNotBlank() }, selectedAccount)
                    }
                },
                enabled = selectedFamily != null && amount != null && amount > 0.0,
                modifier = Modifier.testTag("save_payment_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    strings: AppStrings,
    accounts: List<BankAccount> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amount: Double, date: String, note: String?, account: BankAccount?) -> Unit
) {
    val categories = listOf("Imam & Staff", "Maintenance", "Electricity & Utility", "Charity", "Event", "Other")
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var catExpanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull { it.active }) }
    var accExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(todayIso()) }
    var noteText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addExpense) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text(strings.expenseTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                )
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accExpanded,
                        onExpandedChange = { accExpanded = !accExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.accountName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectAccountOptional) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accExpanded,
                            onDismissRequest = { accExpanded = false }
                        ) {
                            accounts.filter { it.active }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.accountName} (${formatCurrency(acc.balance)})") },
                                    onClick = {
                                        selectedAccount = acc
                                        accExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 20) dateText = it },
                    label = { Text(strings.dateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 300) noteText = it },
                    label = { Text(strings.noteOptional) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (amount != null && amount > 0.0) onSave(title, category, amount, dateText, noteText.takeIf { it.isNotBlank() }, selectedAccount) },
                enabled = title.isNotBlank() && amount != null && amount > 0.0,
                modifier = Modifier.testTag("save_expense_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun NoticeFormDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onPublish: (title: String, content: String, category: String, pinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }
    var pinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.publishNotice) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text(strings.noticeTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("general", "prayer", "finance", "qurbani").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 2000) content = it },
                    label = { Text(strings.noticeContent) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("notice_content_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.pinNotice)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(title, content, category, pinned) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.testTag("publish_notice_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
