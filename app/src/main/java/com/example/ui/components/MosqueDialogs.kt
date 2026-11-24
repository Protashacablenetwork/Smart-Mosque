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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CommitteeMember
import com.example.data.model.Family
import com.example.data.model.MeetingRecord
import com.example.data.model.SocietyAsset
import com.example.data.model.SocietyDocument
import com.example.data.model.SocietyEvent
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun currentIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun SinglePrayerTimeDialog(
    strings: AppStrings,
    prayerLabel: String,
    currentTime: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var time by remember { mutableStateOf(currentTime) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.editMosqueSchedule} — $prayerLabel") },
        text = {
            OutlinedTextField(
                value = time,
                onValueChange = { if (it.length <= 20) time = it },
                label = { Text(prayerLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("single_prayer_time_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(time.trim()) },
                enabled = time.isNotBlank(),
                modifier = Modifier.testTag("save_single_prayer_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitteeFormDialog(
    strings: AppStrings,
    existing: CommitteeMember?,
    families: List<Family>,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, phone: String?, family: Family?, term: String?, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var term by remember { mutableStateOf(existing?.term ?: "২০২৫–২০২৭") }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var selectedFamily by remember { mutableStateOf(families.find { it.id == existing?.familyId }) }
    var famExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCommitteeMember) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 100) name = it }, label = { Text(strings.memberName) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("committee_name_input"))
                OutlinedTextField(value = role, onValueChange = { if (it.length <= 80) role = it }, label = { Text(strings.committeeRoleLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("committee_role_input"))
                OutlinedTextField(value = phone, onValueChange = { if (it.length <= 30) phone = it }, label = { Text(strings.phone) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = term, onValueChange = { if (it.length <= 40) term = it }, label = { Text(strings.committeeTermLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (families.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = famExpanded, onExpandedChange = { famExpanded = !famExpanded }) {
                        OutlinedTextField(
                            value = selectedFamily?.familyName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectFamily) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = famExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = famExpanded, onDismissRequest = { famExpanded = false }) {
                            DropdownMenuItem(text = { Text("—") }, onClick = { selectedFamily = null; famExpanded = false })
                            families.forEach { f ->
                                DropdownMenuItem(text = { Text("${f.familyCode} — ${f.familyName}") }, onClick = { selectedFamily = f; famExpanded = false })
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.activeStatus)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, role, phone.takeIf { it.isNotBlank() }, selectedFamily, term.takeIf { it.isNotBlank() }, active) },
                enabled = name.isNotBlank() && role.isNotBlank(),
                modifier = Modifier.testTag("save_committee_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun EventFormDialog(
    strings: AppStrings,
    existing: SocietyEvent?,
    onDismiss: () -> Unit,
    onSave: (title: String, date: String, time: String?, location: String?, type: String, description: String?, rsvp: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(existing?.eventDate ?: currentIsoDate()) }
    var time by remember { mutableStateOf(existing?.eventTime ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var eventType by remember { mutableStateOf(existing?.eventType ?: "religious") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var rsvp by remember { mutableStateOf(existing?.rsvpEnabled ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addEvent) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { if (it.length <= 120) title = it }, label = { Text(strings.eventTitleLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("event_title_input"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { if (it.length <= 20) date = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = time, onValueChange = { if (it.length <= 20) time = it }, label = { Text(strings.eventTimeLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = location, onValueChange = { if (it.length <= 150) location = it }, label = { Text(strings.locationLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(strings.eventTypeLabel)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("religious" to "Religious", "social" to "Social", "education" to "Education", "informational" to "Info").forEach { (k, l) ->
                        FilterChip(selected = eventType == k, onClick = { eventType = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = description, onValueChange = { if (it.length <= 1500) description = it }, label = { Text(strings.descriptionLabel) }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.rsvpEnabledLabel)
                    Switch(checked = rsvp, onCheckedChange = { rsvp = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, date, time.takeIf { it.isNotBlank() }, location.takeIf { it.isNotBlank() }, eventType, description.takeIf { it.isNotBlank() }, rsvp) },
                enabled = title.isNotBlank() && date.isNotBlank(),
                modifier = Modifier.testTag("save_event_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun AssetFormDialog(
    strings: AppStrings,
    existing: SocietyAsset?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, value: Double, purchaseDate: String?, condition: String, location: String?, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Electronics") }
    var valueText by remember { mutableStateOf(existing?.value?.toString() ?: "") }
    var purchaseDate by remember { mutableStateOf(existing?.purchaseDate ?: currentIsoDate()) }
    var condition by remember { mutableStateOf(existing?.condition ?: "good") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    val parsedValue = valueText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addAsset) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 120) name = it }, label = { Text(strings.assetNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("asset_name_input"))
                OutlinedTextField(value = category, onValueChange = { if (it.length <= 60) category = it }, label = { Text(strings.category) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = valueText, onValueChange = { valueText = it }, label = { Text(strings.assetValueLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = purchaseDate, onValueChange = { if (it.length <= 20) purchaseDate = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text(strings.conditionLabel)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("good" to "Good", "needs_repair" to "Needs Repair", "damaged" to "Damaged").forEach { (k, l) ->
                        FilterChip(selected = condition == k, onClick = { condition = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = location, onValueChange = { if (it.length <= 150) location = it }, label = { Text(strings.locationLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 400) note = it }, label = { Text(strings.noteOptional) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (parsedValue != null && parsedValue >= 0.0) onSave(name, category, parsedValue, purchaseDate.takeIf { it.isNotBlank() }, condition, location.takeIf { it.isNotBlank() }, note.takeIf { it.isNotBlank() }) },
                enabled = name.isNotBlank() && parsedValue != null && parsedValue >= 0.0,
                modifier = Modifier.testTag("save_asset_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun DocumentFormDialog(
    strings: AppStrings,
    existing: SocietyDocument?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, docDate: String, fileName: String?, note: String?) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Legal") }
    var docDate by remember { mutableStateOf(existing?.docDate ?: currentIsoDate()) }
    var fileName by remember { mutableStateOf(existing?.fileName ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addDocument) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { if (it.length <= 120) title = it }, label = { Text(strings.docTitleLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("document_title_input"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = category, onValueChange = { if (it.length <= 60) category = it }, label = { Text(strings.category) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = docDate, onValueChange = { if (it.length <= 20) docDate = it }, label = { Text(strings.dateLabel) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = fileName, onValueChange = { if (it.length <= 150) fileName = it }, label = { Text(strings.docFileNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 500) note = it }, label = { Text(strings.noteOptional) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, category, docDate, fileName.takeIf { it.isNotBlank() }, note.takeIf { it.isNotBlank() }) },
                enabled = title.isNotBlank() && docDate.isNotBlank(),
                modifier = Modifier.testTag("save_document_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun MeetingDetailDialog(
    strings: AppStrings,
    meeting: MeetingRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(meeting.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${meeting.meetingDate} ${meeting.meetingTime ?: ""}".trim(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (!meeting.location.isNullOrBlank()) {
                    Text("${strings.locationLabel}: ${meeting.location}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                if (!meeting.agenda.isNullOrBlank()) {
                    Text(strings.meetingAgendaLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(meeting.agenda, style = MaterialTheme.typography.bodyMedium)
                }
                Text(strings.decisions, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(meeting.decisions, style = MaterialTheme.typography.bodyMedium)
                if (!meeting.attendees.isNullOrBlank()) {
                    Text("${strings.attendees}: ${meeting.attendees}", style = MaterialTheme.typography.bodySmall)
                }
                if (!meeting.assignedTo.isNullOrBlank()) {
                    Text("${strings.assignedTo}: ${meeting.assignedTo}", style = MaterialTheme.typography.bodySmall)
                }
                if (!meeting.deadline.isNullOrBlank()) {
                    Text("${strings.deadline}: ${meeting.deadline}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
