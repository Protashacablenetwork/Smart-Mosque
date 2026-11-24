package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DonationRecord
import com.example.data.model.Family
import com.example.data.model.MosqueSchedule
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.util.QrCodeGenerator

@Composable
fun QrCodeBox(
    content: String,
    size: Dp = 156.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(content) { QrCodeGenerator.encode(content) }
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .padding(10.dp)
        ) {
            val modulePx = this.size.minDimension / matrix.size.toFloat()
            drawRect(color = Color.White)
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color(0xFF102018),
                            topLeft = Offset(x * modulePx, y * modulePx),
                            size = Size(modulePx + 0.5f, modulePx + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MosqueScheduleCard(
    strings: AppStrings,
    schedule: MosqueSchedule,
    canEdit: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mosque,
                            contentDescription = strings.mosqueScheduleTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = schedule.mosqueName.ifBlank { strings.mosqueScheduleTitle },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.mosqueScheduleTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (canEdit) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_mosque_schedule_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.editMosqueSchedule, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 5-Waqt + Jumuah grid (2 rows of 3)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerSlot(strings.fajr, schedule.fajrTime, Modifier.weight(1f))
                PrayerSlot(strings.dhuhr, schedule.dhuhrTime, Modifier.weight(1f))
                PrayerSlot(strings.asr, schedule.asrTime, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerSlot(strings.maghrib, schedule.maghribTime, Modifier.weight(1f))
                PrayerSlot(strings.isha, schedule.ishaTime, Modifier.weight(1f))
                PrayerSlot(strings.jumuah, schedule.jumuahTime, Modifier.weight(1f), highlight = true)
            }

            if (!schedule.khutbahTopic.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${strings.khutbahTopic}: ${schedule.khutbahTopic}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (!schedule.sehriEnd.isNullOrBlank() || !schedule.iftarTime.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!schedule.sehriEnd.isNullOrBlank()) {
                        InfoPillRow(Icons.Filled.AccessTime, "${strings.sehriEnd}: ${schedule.sehriEnd}", Modifier.weight(1f))
                    }
                    if (!schedule.iftarTime.isNullOrBlank()) {
                        InfoPillRow(Icons.Filled.AccessTime, "${strings.iftarTime}: ${schedule.iftarTime}", Modifier.weight(1f))
                    }
                }
            }

            if (!schedule.imamName.isNullOrBlank() || !schedule.muazzinName.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!schedule.imamName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${schedule.imamName}${if (!schedule.imamPhone.isNullOrBlank()) "\n${schedule.imamPhone}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!schedule.muazzinName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${schedule.muazzinName}${if (!schedule.muazzinPhone.isNullOrBlank()) "\n${schedule.muazzinPhone}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerSlot(label: String, time: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoPillRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SemanticWarning.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = SemanticWarning)
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun FamilyDigitalIdDialog(
    strings: AppStrings,
    family: Family,
    memberCount: Int,
    onDismiss: () -> Unit
) {
    val qrPayload = "MASJID|${family.familyCode}|${family.familyName}|${family.phone ?: ""}"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.digitalIdCard, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QrCodeBox(content = qrPayload, size = 168.dp)
                StatusPill(
                    text = family.familyCode,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = family.familyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (!family.phone.isNullOrBlank()) {
                    Text(family.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!family.address.isNullOrBlank()) {
                    Text(family.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Text("${strings.membersCount}: $memberCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun DonationReceiptDialog(
    strings: AppStrings,
    donation: DonationRecord,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.appTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StatusPill(
                    text = donation.receiptNo,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                ReceiptKV(strings.donorName, if (donation.anonymous) strings.anonymousDonor else donation.donorName)
                ReceiptKV(strings.fundType, donation.fundType.uppercase())
                ReceiptKV(strings.dateLabel.substringBefore(" "), donation.donationDate)
                ReceiptKV(strings.paymentMethod, donation.paymentMethod.uppercase())
                if (!donation.transactionRef.isNullOrBlank()) {
                    ReceiptKV("TrxID", donation.transactionRef)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.amount, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatCurrency(donation.amount),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AppMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = SemanticSuccess
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shareReceipt)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun ReceiptKV(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
