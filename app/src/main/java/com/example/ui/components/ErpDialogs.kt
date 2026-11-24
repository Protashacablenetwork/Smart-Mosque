package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.data.model.ComplaintItem
import com.example.data.model.MeetingRecord
import com.example.data.model.MosqueSchedule
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun todayDateIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun MosqueScheduleDialog(
    strings: AppStrings,
    existing: MosqueSchedule,
    onDismiss: () -> Unit,
    onSave: (MosqueSchedule) -> Unit
) {
    var mosqueName by remember { mutableStateOf(existing.mosqueName) }
    var fajr by remember { mutableStateOf(existing.fajrTime) }
    var sunrise by remember { mutableStateOf(existing.sunriseTime ?: "06:22 AM") }
    var dhuhr by remember { mutableStateOf(existing.dhuhrTime) }
    var asr by remember { mutableStateOf(existing.asrTime) }
    var maghrib by remember { mutableStateOf(existing.maghribTime) }
    var isha by remember { mutableStateOf(existing.ishaTime) }
    var jumuah by remember { mutableStateOf(existing.jumuahTime) }
    var khutbah by remember { mutableStateOf(existing.khutbahTopic ?: "") }
    var sehri by remember { mutableStateOf(existing.sehriEnd ?: "") }
    var iftar by remember { mutableStateOf(existing.iftarTime ?: "") }
    var imam by remember { mutableStateOf(existing.imamName ?: "") }
    var imamPhone by remember { mutableStateOf(existing.imamPhone ?: "") }
    var muazzin by remember { mutableStateOf(existing.muazzinName ?: "") }
    var muazzinPhone by remember { mutableStateOf(existing.muazzinPhone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editMosqueSchedule) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = mosqueName, onValueChange = { if (it.length <= 120) mosqueName = it }, label = { Text(strings.mosqueName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fajr, onValueChange = { if (it.length <= 20) fajr = it }, label = { Text(strings.fajr) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sunrise, onValueChange = { if (it.length <= 20) sunrise = it }, label = { Text(strings.sunrise) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dhuhr, onValueChange = { if (it.length <= 20) dhuhr = it }, label = { Text(strings.dhuhr) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = asr, onValueChange = { if (it.length <= 20) asr = it }, label = { Text(strings.asr) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maghrib, onValueChange = { if (it.length <= 20) maghrib = it }, label = { Text(strings.maghrib) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = isha, onValueChange = { if (it.length <= 20) isha = it }, label = { Text(strings.isha) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = jumuah, onValueChange = { if (it.length <= 30) jumuah = it }, label = { Text(strings.jumuah) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = khutbah, onValueChange = { if (it.length <= 200) khutbah = it }, label = { Text(strings.khutbahTopic) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sehri, onValueChange = { if (it.length <= 20) sehri = it }, label = { Text(strings.sehriEnd) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = iftar, onValueChange = { if (it.length <= 20) iftar = it }, label = { Text(strings.iftarTime) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = imam, onValueChange = { if (it.length <= 100) imam = it }, label = { Text(strings.imamInfo) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imamPhone, onValueChange = { if (it.length <= 30) imamPhone = it }, label = { Text(strings.imamPhone) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = muazzin, onValueChange = { if (it.length <= 100) muazzin = it }, label = { Text(strings.muazzinInfo) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = muazzinPhone, onValueChange = { if (it.length <= 30) muazzinPhone = it }, label = { Text(strings.muazzinPhone) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        existing.copy(
                            mosqueName = mosqueName, fajrTime = fajr,
                            sunriseTime = sunrise.takeIf { it.isNotBlank() },
                            dhuhrTime = dhuhr, asrTime = asr, maghribTime = maghrib, ishaTime = isha, jumuahTime = jumuah,
                            khutbahTopic = khutbah.takeIf { it.isNotBlank() },
                            sehriEnd = sehri.takeIf { it.isNotBlank() },
                            iftarTime = iftar.takeIf { it.isNotBlank() },
                            imamName = imam.takeIf { it.isNotBlank() },
                            imamPhone = imamPhone.takeIf { it.isNotBlank() },
                            muazzinName = muazzin.takeIf { it.isNotBlank() },
                            muazzinPhone = muazzinPhone.takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = mosqueName.isNotBlank(),
                modifier = Modifier.testTag("save_schedule_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationFormDialog(
    strings: AppStrings,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (donorName: String, anonymous: Boolean, fundType: String, amount: Double, method: String, trxRef: String?, date: String, note: String?, account: BankAccount?) -> Unit
) {
    var donorName by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var fundType by remember { mutableStateOf("general") }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("cash") }
    var trxRef by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(todayDateIso()) }
    var noteText by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull { it.active }) }
    var accExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.recordDonation) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.anonymousDonor)
                    Switch(checked = anonymous, onCheckedChange = { anonymous = it })
                }
                if (!anonymous) {
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { if (it.length <= 100) donorName = it },
                        label = { Text(strings.donorName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("donation_donor_input")
                    )
                }
                Text(strings.fundType)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "general" to "General", "zakat" to "Zakat", "fitrah" to "Fitrah",
                        "sadqah" to "Sadqah", "construction" to "Construction", "orphan_welfare" to "Orphan/Aid"
                    ).forEach { (key, label) ->
                        FilterChip(selected = fundType == key, onClick = { fundType = key }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("donation_amount_input")
                )
                Text(strings.paymentMethod)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cash" to "Cash", "bkash" to "bKash", "nagad" to "Nagad", "bank" to "Bank").forEach { (k, l) ->
                        FilterChip(selected = method == k, onClick = { method = k }, label = { Text(l) })
                    }
                }
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = !accExpanded }) {
                        OutlinedTextField(
                            value = selectedAccount?.accountName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectAccountOptional) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                            accounts.filter { it.active }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.accountName} (${formatCurrency(acc.balance)})") },
                                    onClick = { selectedAccount = acc; accExpanded = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(value = trxRef, onValueChange = { if (it.length <= 80) trxRef = it }, label = { Text(strings.transactionRef) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dateText, onValueChange = { if (it.length <= 20) dateText = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = noteText, onValueChange = { if (it.length <= 300) noteText = it }, label = { Text(strings.noteOptional) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount != null && amount > 0.0) {
                        onSave(donorName, anonymous, fundType, amount, method, trxRef.takeIf { it.isNotBlank() }, dateText, noteText.takeIf { it.isNotBlank() }, selectedAccount)
                    }
                },
                enabled = (anonymous || donorName.isNotBlank()) && amount != null && amount > 0.0,
                modifier = Modifier.testTag("save_donation_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun BankAccountDialog(
    strings: AppStrings,
    existing: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, number: String?, balance: Double, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.accountName ?: "") }
    var type by remember { mutableStateOf(existing?.accountType ?: "cash") }
    var number by remember { mutableStateOf(existing?.accountNumber ?: "") }
    var balanceText by remember { mutableStateOf(existing?.balance?.toString() ?: "0") }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    val balance = balanceText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addBankAccount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 100) name = it }, label = { Text(strings.accountName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cash" to "Cash", "bank" to "Bank", "bkash" to "bKash", "nagad" to "Nagad").forEach { (k, l) ->
                        FilterChip(selected = type == k, onClick = { type = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = number, onValueChange = { if (it.length <= 50) number = it }, label = { Text(strings.accountNumber) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balanceText, onValueChange = { balanceText = it }, label = { Text(strings.openingBalance) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.activeStatus)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (balance != null) onSave(name, type, number.takeIf { it.isNotBlank() }, balance, active) },
                enabled = name.isNotBlank() && balance != null
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (category: String, periodLabel: String, amount: Double) -> Unit
) {
    val categories = listOf("Imam & Staff", "Maintenance", "Electricity & Utility", "Charity", "Event", "Other")
    var category by remember { mutableStateOf(categories.first()) }
    var catExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addBudget) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true, label = { Text(strings.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = period, onValueChange = { if (it.length <= 30) period = it }, label = { Text(strings.periodLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(strings.budgetAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (amount != null && amount >= 0.0) onSave(category, period, amount) },
                enabled = period.isNotBlank() && amount != null && amount >= 0.0
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun MeetingFormDialog(
    strings: AppStrings,
    existing: MeetingRecord?,
    onDismiss: () -> Unit,
    onSave: (title: String, date: String, time: String?, location: String?, agenda: String?, attendees: String?, decisions: String, assignedTo: String?, deadline: String?, status: String) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(existing?.meetingDate ?: todayDateIso()) }
    var time by remember { mutableStateOf(existing?.meetingTime ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var agenda by remember { mutableStateOf(existing?.agenda ?: "") }
    var attendees by remember { mutableStateOf(existing?.attendees ?: "") }
    var decisions by remember { mutableStateOf(existing?.decisions ?: "") }
    var assignedTo by remember { mutableStateOf(existing?.assignedTo ?: "") }
    var deadline by remember { mutableStateOf(existing?.deadline ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "scheduled") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addMeeting) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { if (it.length <= 120) title = it }, label = { Text(strings.meetingTitle) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { if (it.length <= 20) date = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = time, onValueChange = { if (it.length <= 20) time = it }, label = { Text(strings.meetingTime) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = location, onValueChange = { if (it.length <= 150) location = it }, label = { Text(strings.locationLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = agenda, onValueChange = { if (it.length <= 1000) agenda = it }, label = { Text(strings.meetingAgendaLabel) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = attendees, onValueChange = { if (it.length <= 500) attendees = it }, label = { Text(strings.attendees) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = decisions, onValueChange = { if (it.length <= 2000) decisions = it }, label = { Text(strings.decisions) }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = assignedTo, onValueChange = { if (it.length <= 100) assignedTo = it }, label = { Text(strings.assignedTo) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = deadline, onValueChange = { if (it.length <= 20) deadline = it }, label = { Text(strings.deadline) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("scheduled", "in_progress", "completed").forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.replace("_", " ")) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title, date,
                        time.takeIf { it.isNotBlank() },
                        location.takeIf { it.isNotBlank() },
                        agenda.takeIf { it.isNotBlank() },
                        attendees.takeIf { it.isNotBlank() },
                        decisions.ifBlank { agenda.ifBlank { "-" } },
                        assignedTo.takeIf { it.isNotBlank() },
                        deadline.takeIf { it.isNotBlank() },
                        status
                    )
                },
                enabled = title.isNotBlank() && (decisions.isNotBlank() || agenda.isNotBlank())
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareFormDialog(
    strings: AppStrings,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (recordType: String, beneficiary: String, phone: String?, plot: String?, amount: Double, date: String, note: String?, account: BankAccount?) -> Unit
) {
    var recordType by remember { mutableStateOf("medical") }
    var beneficiary by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plot by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("0") }
    var date by remember { mutableStateOf(todayDateIso()) }
    var note by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<BankAccount?>(null) }
    var accExpanded by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addWelfare) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("medical" to "Medical", "zakat_aid" to "Zakat Aid", "emergency_relief" to "Relief", "education" to "Education", "burial" to "Burial / Grave").forEach { (k, l) ->
                        FilterChip(selected = recordType == k, onClick = { recordType = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = beneficiary, onValueChange = { if (it.length <= 100) beneficiary = it }, label = { Text(strings.beneficiaryName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { if (it.length <= 30) phone = it }, label = { Text(strings.phone) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (recordType == "burial") {
                    OutlinedTextField(value = plot, onValueChange = { if (it.length <= 40) plot = it }, label = { Text(strings.plotNumber) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (accounts.isNotEmpty() && amount > 0.0) {
                    ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = !accExpanded }) {
                        OutlinedTextField(
                            value = selectedAccount?.accountName ?: "", onValueChange = {}, readOnly = true,
                            label = { Text(strings.selectAccountOptional) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                            accounts.filter { it.active }.forEach { acc ->
                                DropdownMenuItem(text = { Text("${acc.accountName} (${formatCurrency(acc.balance)})") }, onClick = { selectedAccount = acc; accExpanded = false })
                            }
                        }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { if (it.length <= 20) date = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 400) note = it }, label = { Text(strings.noteOptional) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(recordType, beneficiary, phone.takeIf { it.isNotBlank() }, plot.takeIf { it.isNotBlank() }, amount, date, note.takeIf { it.isNotBlank() }, selectedAccount) },
                enabled = beneficiary.isNotBlank() && amount >= 0.0
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun ComplaintFormDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.submitComplaint) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = subject, onValueChange = { if (it.length <= 120) subject = it }, label = { Text(strings.complaintSubject) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { if (it.length <= 1500) message = it }, label = { Text(strings.complaintMessage) }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(subject, message) }, enabled = subject.isNotBlank() && message.isNotBlank()) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun ComplaintReplyDialog(
    strings: AppStrings,
    item: ComplaintItem,
    onDismiss: () -> Unit,
    onSave: (status: String, reply: String?) -> Unit
) {
    var status by remember { mutableStateOf(item.status) }
    var reply by remember { mutableStateOf(item.adminReply ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.replyComplaint) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.subject)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("open", "reviewed", "resolved").forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.replaceFirstChar { it.uppercase() }) })
                    }
                }
                OutlinedTextField(value = reply, onValueChange = { if (it.length <= 1000) reply = it }, label = { Text(strings.adminReplyLabel) }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(status, reply.takeIf { it.isNotBlank() }) }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
