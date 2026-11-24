package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ComplaintItem
import com.example.data.model.DueItem
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.MosqueSchedule
import com.example.data.model.PaymentRecord
import com.example.data.model.SocietyMember
import com.example.ui.components.ComplaintFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrCodeBox
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatKg
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.util.ReportExporter

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MemberPortalScreen(
    strings: AppStrings,
    userId: String,
    userName: String,
    userEmail: String?,
    myFamily: Family?,
    myMembers: List<SocietyMember>,
    dues: List<DueItem>,
    myPayments: List<PaymentRecord>,
    myDistributions: List<MeatDistribution>,
    myComplaints: List<ComplaintItem>,
    onSubmitComplaint: (authorName: String, familyId: String?, subject: String, message: String) -> Unit,
    isStaffReader: Boolean = false,
    schedule: MosqueSchedule = MosqueSchedule(),
    onRequestSignIn: () -> Unit = {},
    allFamilies: List<Family> = emptyList(),
    allMembers: List<SocietyMember> = emptyList(),
    allPayments: List<PaymentRecord> = emptyList(),
    allDistributions: List<MeatDistribution> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isSignedIn = userId.isNotBlank()
    val context = LocalContext.current
    var showComplaintDialog by remember { mutableStateOf(false) }
    var previewPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var staffSelectedFamily by remember(myFamily, allFamilies) {
        mutableStateOf(myFamily ?: if (isStaffReader) allFamilies.firstOrNull() else null)
    }
    var famMenuExpanded by remember { mutableStateOf(false) }

    val activeFamily = if (isStaffReader && staffSelectedFamily != null) staffSelectedFamily else myFamily
    val activeMembers = remember(isStaffReader, activeFamily, myFamily, myMembers, allMembers) {
        if (isStaffReader && activeFamily != null && activeFamily.id != myFamily?.id) {
            allMembers.filter { it.familyId == activeFamily.id }
        } else myMembers
    }
    val activePayments = remember(isStaffReader, activeFamily, myFamily, myPayments, allPayments) {
        if (isStaffReader && activeFamily != null && activeFamily.id != myFamily?.id) {
            allPayments.filter { it.familyId == activeFamily.id }
        } else myPayments
    }
    val activeDistributions = remember(isStaffReader, activeFamily, myFamily, myDistributions, allDistributions) {
        if (isStaffReader && activeFamily != null && activeFamily.id != myFamily?.id) {
            allDistributions.filter { it.familyId == activeFamily.id }
        } else myDistributions
    }

    val activeDuesTotal = remember(dues) { dues.filter { it.active }.sumOf { it.amount } }
    val totalPaid = remember(activePayments) { activePayments.sumOf { it.amount } }
    val outstanding = if (activeFamily?.active == true) maxOf(0.0, activeDuesTotal - totalPaid) else 0.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isSignedIn) {
                FloatingActionButton(
                    onClick = { showComplaintDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("member_submit_complaint_fab")
                ) {
                    Icon(Icons.Filled.AddComment, contentDescription = strings.submitComplaint)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isStaffReader && allFamilies.isNotEmpty()) {
                    item {
                        androidx.compose.material3.ExposedDropdownMenuBox(
                            expanded = famMenuExpanded,
                            onExpandedChange = { famMenuExpanded = !famMenuExpanded }
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = activeFamily?.let { "${it.familyCode} — ${it.familyName}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.previewFamilyPortal) },
                                trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = famMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("staff_family_preview_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = famMenuExpanded,
                                onDismissRequest = { famMenuExpanded = false }
                            ) {
                                allFamilies.forEach { f ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("${f.familyCode} — ${f.familyName}") },
                                        onClick = { staffSelectedFamily = f; famMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isSignedIn) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(schedule.mosqueName.ifBlank { strings.appTitle }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("${strings.fajr}: ${schedule.fajrTime} • ${strings.dhuhr}: ${schedule.dhuhrTime} • ${strings.asr}: ${schedule.asrTime}", style = MaterialTheme.typography.bodySmall)
                                Text("${strings.maghrib}: ${schedule.maghribTime} • ${strings.isha}: ${schedule.ishaTime} • ${strings.jumuah}: ${schedule.jumuahTime}", style = MaterialTheme.typography.bodySmall)
                                if (!schedule.imamName.isNullOrBlank()) {
                                    Text("${strings.imamInfo}: ${schedule.imamName}${if (!schedule.imamPhone.isNullOrBlank()) " (${schedule.imamPhone})" else ""}", style = MaterialTheme.typography.labelMedium)
                                }
                                if (!schedule.muazzinName.isNullOrBlank()) {
                                    Text("${strings.muazzinInfo}: ${schedule.muazzinName}${if (!schedule.muazzinPhone.isNullOrBlank()) " (${schedule.muazzinPhone})" else ""}", style = MaterialTheme.typography.labelMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(strings.guestBannerHint, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onRequestSignIn, modifier = Modifier.fillMaxWidth().testTag("member_portal_signin")) {
                                    Text(strings.adminSignIn)
                                }
                            }
                        }
                    }
                } else if (activeFamily == null) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Badge,
                            message = strings.noLinkedFamilyHint
                        )
                    }
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (!userEmail.isNullOrBlank()) {
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    "UID: $userId",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(strings.digitalIdCard, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(activeFamily.familyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        StatusPill(
                                            text = activeFamily.familyCode,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                        if (!activeFamily.phone.isNullOrBlank()) {
                                            Text(activeFamily.phone, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (!activeFamily.address.isNullOrBlank()) {
                                            Text(activeFamily.address, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    QrCodeBox(
                                        content = "MASJID|${activeFamily.familyCode}|${activeFamily.familyName}|${activeFamily.phone ?: ""}",
                                        size = 112.dp
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusPill(
                                        text = "${strings.totalPaid}: ${formatCurrency(totalPaid)}",
                                        containerColor = SemanticSuccessContainer,
                                        contentColor = SemanticSuccess
                                    )
                                    StatusPill(
                                        text = if (outstanding > 0.0) "${strings.dueLabel}: ${formatCurrency(outstanding)}" else strings.allClear,
                                        containerColor = if (outstanding > 0.0) SemanticErrorContainer else SemanticSuccessContainer,
                                        contentColor = if (outstanding > 0.0) SemanticError else SemanticSuccess
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "${strings.navMembers} (${activeMembers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (activeMembers.isEmpty()) {
                        item { EmptyStateCard(icon = Icons.Filled.Groups, message = strings.emptyMembers) }
                    } else {
                        items(activeMembers, key = { it.id }) { m ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (m.head) Icon(Icons.Filled.Star, contentDescription = strings.familyHead, tint = SemanticWarning, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text(m.memberName, fontWeight = FontWeight.SemiBold)
                                            val meta = listOfNotNull(m.relation, m.occupation, m.phone).joinToString(" • ")
                                            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    StatusPill(
                                        text = if (m.active) strings.activeStatus else strings.inactiveStatus,
                                        containerColor = if (m.active) SemanticSuccessContainer else SemanticWarningContainer,
                                        contentColor = if (m.active) SemanticSuccess else SemanticWarning
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "${strings.tabPayments} (${activePayments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (activePayments.isEmpty()) {
                        item { EmptyStateCard(icon = Icons.Filled.Payments, message = strings.emptyPayments) }
                    } else {
                        items(activePayments, key = { it.id }) { p ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(p.receiptNo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("${p.paymentDate} • ${p.paymentMethod.uppercase()}${if (!p.dueTitle.isNullOrBlank()) " • ${p.dueTitle}" else ""}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(formatCurrency(p.amount), style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                                    TextButton(onClick = { previewPayment = p }) {
                                        Icon(Icons.Filled.ReceiptLong, contentDescription = strings.previewReceipt, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (activeDistributions.isNotEmpty()) {
                        item {
                            Text(
                                text = strings.meatDistribution,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(activeDistributions, key = { it.id }) { d ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text("${strings.perFamilyShare}: ${formatKg(d.allocatedKg)}", fontWeight = FontWeight.SemiBold)
                                            if (!d.receivedBy.isNullOrBlank()) {
                                                Text("${strings.receivedBy}: ${d.receivedBy}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    StatusPill(
                                        text = if (d.status == "delivered") strings.deliveredLabel else strings.pendingLabel,
                                        containerColor = if (d.status == "delivered") SemanticSuccessContainer else SemanticWarningContainer,
                                        contentColor = if (d.status == "delivered") SemanticSuccess else SemanticWarning
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSignedIn) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.tabComplaints} (${myComplaints.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showComplaintDialog = true }) {
                                Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.submitComplaint)
                            }
                        }
                    }
                    if (myComplaints.isEmpty()) {
                        item { EmptyStateCard(icon = Icons.Filled.Feedback, message = strings.emptyComplaints) }
                    } else {
                        items(myComplaints, key = { it.id }) { c ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(c.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusPill(
                                            text = c.status.uppercase(),
                                            containerColor = if (c.status == "resolved") SemanticSuccessContainer else SemanticWarningContainer,
                                            contentColor = if (c.status == "resolved") SemanticSuccess else SemanticWarning
                                        )
                                    }
                                    Text(c.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!c.adminReply.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${strings.adminReplyLabel}: ${c.adminReply}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showComplaintDialog) {
        ComplaintFormDialog(
            strings = strings,
            onDismiss = { showComplaintDialog = false },
            onSubmit = { subject, message ->
                onSubmitComplaint(userName, activeFamily?.id, subject, message)
                showComplaintDialog = false
            }
        )
    }

    previewPayment?.let { payment ->
        ReceiptPreviewDialog(
            strings = strings,
            payment = payment,
            onDismiss = { previewPayment = null },
            onShare = { ReportExporter.sharePaymentReceiptText(context, payment) }
        )
    }
}
