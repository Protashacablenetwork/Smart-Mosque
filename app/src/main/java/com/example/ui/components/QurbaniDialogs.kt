package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.QurbaniBatch
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun currentIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QurbaniBatchDialog(
    strings: AppStrings,
    selectedYear: String,
    activeFamilyCount: Int,
    onDismiss: () -> Unit,
    onCreate: (
        eventName: String,
        animalInfo: String?,
        purchaseCost: Double?,
        totalMeatKg: Double,
        eventDate: String,
        autoDistribute: Boolean
    ) -> Unit
) {
    val defaultDate = remember(selectedYear) {
        val today = currentIsoDate()
        if (today.startsWith(selectedYear)) today else "$selectedYear-06-17"
    }
    var eventName by remember { mutableStateOf("ঈদুল আযহা $selectedYear") }
    var selectedAnimalType by remember { mutableStateOf("গরু") }
    var animalInfo by remember { mutableStateOf("১টি গরু") }
    var purchaseCostText by remember { mutableStateOf("") }
    var totalKgText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(defaultDate) }
    var autoDistribute by remember { mutableStateOf(activeFamilyCount > 0) }

    val totalKg = totalKgText.toDoubleOrNull()
    val purchaseCost = purchaseCostText.toDoubleOrNull()
    val perFamilyPreview = if (totalKg != null && totalKg > 0.0 && activeFamilyCount > 0) {
        totalKg / activeFamilyCount.toDouble()
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createQurbaniBatch) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { if (it.length <= 120) eventName = it },
                    label = { Text(strings.eventName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("qurbani_event_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("গরু", "ছাগল", "মহিষ", "উট", "ভেড়া").forEach { animal ->
                        FilterChip(
                            selected = selectedAnimalType == animal,
                            onClick = {
                                selectedAnimalType = animal
                                animalInfo = "১টি $animal"
                            },
                            label = { Text(animal) }
                        )
                    }
                }
                OutlinedTextField(
                    value = animalInfo,
                    onValueChange = { if (it.length <= 150) animalInfo = it },
                    label = { Text(strings.animalInfo) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purchaseCostText,
                    onValueChange = { purchaseCostText = it },
                    label = { Text(strings.purchaseCost) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalKgText,
                    onValueChange = { totalKgText = it },
                    label = { Text(strings.totalMeatKg) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("qurbani_kg_input")
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 20) dateText = it },
                    label = { Text(strings.dateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (activeFamilyCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.qurbaniAutoDistributeToggle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(checked = autoDistribute, onCheckedChange = { autoDistribute = it })
                    }
                }
                Text(
                    text = "${strings.activeFamiliesCount}: $activeFamilyCount\n${strings.perFamilyShare}: ${formatKg(perFamilyPreview)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalKg != null && totalKg > 0.0) {
                        onCreate(
                            eventName,
                            animalInfo.takeIf { it.isNotBlank() },
                            purchaseCost,
                            totalKg,
                            dateText,
                            autoDistribute && activeFamilyCount > 0
                        )
                    }
                },
                enabled = eventName.isNotBlank() && totalKg != null && totalKg > 0.0,
                modifier = Modifier.testTag("save_qurbani_button")
            ) { Text(if (autoDistribute && activeFamilyCount > 0) strings.calculateEqualShare else strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun MeatDeliveryDialog(
    strings: AppStrings,
    distribution: MeatDistribution,
    onDismiss: () -> Unit,
    onConfirm: (deliveredKg: Double, receivedBy: String) -> Unit
) {
    var kgText by remember { mutableStateOf(if (distribution.allocatedKg > 0.0) distribution.allocatedKg.toString() else "1.0") }
    var receiver by remember { mutableStateOf(distribution.receivedBy ?: "") }
    val kg = kgText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.confirmDelivery}: ${distribution.familyName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = receiver,
                    onValueChange = { if (it.length <= 100) receiver = it },
                    label = { Text(strings.receivedBy) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("meat_receiver_input")
                )
                OutlinedTextField(
                    value = kgText,
                    onValueChange = { kgText = it },
                    label = { Text(strings.deliveredKg) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (kg != null && kg > 0.0) onConfirm(kg, receiver) },
                enabled = receiver.isNotBlank() && kg != null && kg > 0.0,
                modifier = Modifier.testTag("confirm_delivery_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQurbaniRecipientDialog(
    strings: AppStrings,
    families: List<Family>,
    alreadyIncludedFamilyIds: Set<String>,
    defaultAllocatedKg: Double,
    onDismiss: () -> Unit,
    onAdd: (family: Family, allocatedKg: Double) -> Unit
) {
    val availableFamilies = remember(families, alreadyIncludedFamilyIds) {
        val missing = families.filter { it.id !in alreadyIncludedFamilyIds }
        missing.ifEmpty { families }
    }
    var selectedFamily by remember(availableFamilies) { mutableStateOf(availableFamilies.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var kgText by remember { mutableStateOf(if (defaultAllocatedKg > 0.0) defaultAllocatedKg.toString() else "0") }
    val kg = kgText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.qurbaniSelectFamilyAdd) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (availableFamilies.isEmpty()) {
                    Text(strings.emptyFamilies, style = MaterialTheme.typography.bodyMedium)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFamily?.let { "${it.familyCode} • ${it.familyName}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectFamily) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("qurbani_recipient_family_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableFamilies.forEach { fam ->
                                DropdownMenuItem(
                                    text = { Text("${fam.familyCode} • ${fam.familyName}") },
                                    onClick = {
                                        selectedFamily = fam
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = kgText,
                        onValueChange = { kgText = it },
                        label = { Text(strings.qurbaniAllocatedKgInput) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qurbani_recipient_kg_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fam = selectedFamily
                    if (fam != null && kg != null && kg >= 0.0) onAdd(fam, kg)
                },
                enabled = selectedFamily != null && kg != null && kg >= 0.0,
                modifier = Modifier.testTag("confirm_add_recipient_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun RecalculateMeatDialog(
    strings: AppStrings,
    batch: QurbaniBatch,
    recipientCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (totalMeatKg: Double) -> Unit
) {
    var totalKgText by remember(batch.id) { mutableStateOf(if (batch.totalMeatKg > 0.0) batch.totalMeatKg.toString() else "") }
    val totalKg = totalKgText.toDoubleOrNull()
    val previewShare = if (totalKg != null && totalKg > 0.0 && recipientCount > 0) totalKg / recipientCount else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.qurbaniRecalculateShare) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(batch.eventName, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = totalKgText,
                    onValueChange = { totalKgText = it },
                    label = { Text(strings.totalMeatKg) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recalculate_meat_kg_input")
                )
                Text(
                    text = "${strings.activeFamiliesCount}: $recipientCount\n${strings.perFamilyShare}: ${formatKg(previewShare)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (totalKg != null && totalKg >= 0.0) onConfirm(totalKg) },
                enabled = totalKg != null && totalKg >= 0.0,
                modifier = Modifier.testTag("confirm_recalculate_button")
            ) { Text(strings.calculateEqualShare) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
