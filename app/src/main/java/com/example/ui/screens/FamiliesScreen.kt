package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DueItem
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.data.model.SocietyMember
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FamilyDigitalIdDialog
import com.example.ui.components.FamilyFormDialog
import com.example.ui.components.MemberFormDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.util.ReportExporter

@Composable
fun FamiliesScreen(
    strings: AppStrings,
    canEdit: Boolean,
    canDelete: Boolean,
    canCollectPayment: Boolean,
    families: List<Family>,
    members: List<SocietyMember>,
    dues: List<DueItem>,
    payments: List<PaymentRecord>,
    initialDirectoryMode: Int = 0,
    onSaveFamily: (id: String?, name: String, code: String, phone: String?, address: String?, linkedEmail: String?, active: Boolean) -> Unit,
    onDeleteFamily: (id: String) -> Unit,
    onSaveMember: (id: String?, familyId: String, familyName: String, memberName: String, phone: String?, relation: String?, occupation: String?, head: Boolean, active: Boolean) -> Unit,
    onDeleteMember: (id: String) -> Unit,
    onQuickCollect: (family: Family, dueAmount: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var directoryMode by remember(initialDirectoryMode) { mutableIntStateOf(initialDirectoryMode) }
    var searchQuery by remember { mutableStateOf("") }
    var activeOnly by remember { mutableStateOf(false) }
    var showFamilyDialog by remember { mutableStateOf(false) }
    var editingFamily by remember { mutableStateOf<Family?>(null) }
    var selectedFamilyDetail by remember { mutableStateOf<Family?>(null) }
    var qrFamily by remember { mutableStateOf<Family?>(null) }
    var showMemberDialogForFamily by remember { mutableStateOf<Family?>(null) }
    var showGlobalMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<SocietyMember?>(null) }

    val activeDuesTotal = remember(dues) { dues.filter { it.active }.sumOf { it.amount } }
    val paidByFamily = remember(payments) { payments.groupBy { it.familyId }.mapValues { (_, l) -> l.sumOf { it.amount } } }
    val paymentsByFamily = remember(payments) { payments.groupBy { it.familyId } }
    val membersByFamily = remember(members) { members.groupBy { it.familyId } }

    val filteredFamilies = remember(families, searchQuery, activeOnly) {
        families.filter { fam ->
            val matchesFilter = !activeOnly || fam.active
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                fam.familyName.contains(q, ignoreCase = true) ||
                fam.familyCode.contains(q, ignoreCase = true) ||
                (fam.phone?.contains(q, ignoreCase = true) == true) ||
                (fam.address?.contains(q, ignoreCase = true) == true) ||
                (fam.linkedEmail?.contains(q, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    val filteredMembers = remember(members, searchQuery, activeOnly) {
        members.filter { m ->
            val matchesFilter = !activeOnly || m.active
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                m.memberName.contains(q, ignoreCase = true) ||
                m.familyName.contains(q, ignoreCase = true) ||
                (m.phone?.contains(q, ignoreCase = true) == true) ||
                (m.occupation?.contains(q, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        if (directoryMode == 0) {
                            editingFamily = null
                            showFamilyDialog = true
                        } else {
                            editingMember = null
                            showGlobalMemberDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_family_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = if (directoryMode == 0) strings.addFamily else strings.addMember)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = directoryMode == 0, onClick = { directoryMode = 0 }, label = { Text("${strings.navFamilies} (${families.size})") })
                        FilterChip(selected = directoryMode == 1, onClick = { directoryMode = 1 }, label = { Text("${strings.navMembers} (${members.size})") })
                    }
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (directoryMode == 0) strings.searchFamilies else strings.searchMembers) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("search_families_input")
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !activeOnly,
                            onClick = { activeOnly = false },
                            label = { Text("${strings.allItems} (${if (directoryMode == 0) families.size else members.size})") }
                        )
                        FilterChip(
                            selected = activeOnly,
                            onClick = { activeOnly = true },
                            label = {
                                val count = if (directoryMode == 0) families.count { it.active } else members.count { it.active }
                                Text("${strings.activeOnly} ($count)")
                            }
                        )
                    }
                }

                if (directoryMode == 0) {
                    if (filteredFamilies.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.FamilyRestroom, strings.emptyFamilies) }
                    } else {
                        items(filteredFamilies, key = { it.id }) { family ->
                            val famMembers = membersByFamily[family.id].orEmpty()
                            val headMember = famMembers.firstOrNull { it.head }
                            val totalPaid = paidByFamily[family.id] ?: 0.0
                            val outstanding = if (family.active) maxOf(0.0, activeDuesTotal - totalPaid) else 0.0

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedFamilyDetail = family }.testTag("family_card_${family.familyCode}"),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                            StatusPill(family.familyCode, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(family.familyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        StatusPill(
                                            if (family.active) strings.activeStatus else strings.inactiveStatus,
                                            if (family.active) SemanticSuccessContainer else SemanticWarningContainer,
                                            if (family.active) SemanticSuccess else SemanticWarning
                                        )
                                    }
                                    if (!family.phone.isNullOrBlank() || !family.address.isNullOrBlank() || !family.linkedEmail.isNullOrBlank()) {
                                        val contactLine = listOfNotNull(family.phone, family.address, family.linkedEmail).joinToString(" • ")
                                        Text(contactLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusPill("${strings.totalPaid}: ${formatCurrency(totalPaid)}", SemanticSuccessContainer, SemanticSuccess)
                                        StatusPill(
                                            if (outstanding > 0.0) "${strings.dueLabel}: ${formatCurrency(outstanding)}" else strings.allClear,
                                            if (outstanding > 0.0) SemanticErrorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            if (outstanding > 0.0) SemanticError else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("${strings.membersCount}: ${famMembers.size}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily), color = MaterialTheme.colorScheme.primary)
                                            if (headMember != null) {
                                                Text("${strings.familyHead}: ${headMember.memberName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { qrFamily = family }) {
                                                Icon(Icons.Filled.QrCode2, contentDescription = strings.digitalIdCard, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            if (outstanding > 0.0) {
                                                IconButton(onClick = {
                                                    val msg = "আসসালামু আলাইকুম, ${family.familyName} (${family.familyCode}) পরিবারের ${formatCurrency(outstanding)} চাঁদা বকেয়া রয়েছে। পরিশোধের অনুরোধ রইল। — ${strings.appTitle}"
                                                    ReportExporter.sendSmsReminder(context, family.phone, msg)
                                                }) {
                                                    Icon(Icons.Filled.Sms, contentDescription = strings.sendReminder, tint = SemanticWarning)
                                                }
                                            }
                                            if (canEdit) {
                                                IconButton(onClick = { showMemberDialogForFamily = family }) {
                                                    Icon(Icons.Filled.PersonAdd, contentDescription = strings.addMember)
                                                }
                                                IconButton(onClick = { editingFamily = family; showFamilyDialog = true }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = strings.editFamily)
                                                }
                                            }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteFamily(family.id) }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (filteredMembers.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.Groups, strings.emptyMembers) }
                    } else {
                        items(filteredMembers, key = { it.id }) { member ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (member.head) Icon(Icons.Filled.Star, contentDescription = strings.familyHead, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                                            Text(member.memberName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(listOfNotNull(member.familyName, member.relation).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (!member.occupation.isNullOrBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.WorkOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(member.occupation, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (!member.phone.isNullOrBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(member.phone, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    if (canEdit) {
                                        Row {
                                            IconButton(onClick = { editingMember = member }) {
                                                Icon(Icons.Filled.Edit, contentDescription = strings.editMember)
                                            }
                                            IconButton(onClick = { onDeleteMember(member.id) }) {
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
        }
    }

    if (showFamilyDialog) {
        val suggested = "FAM-${(families.size + 1).toString().padStart(3, '0')}"
        FamilyFormDialog(
            strings = strings,
            existing = editingFamily,
            suggestedCode = suggested,
            onDismiss = { showFamilyDialog = false },
            onSave = { name, code, phone, address, linkedEmail, active ->
                onSaveFamily(editingFamily?.id, name, code, phone, address, linkedEmail, active)
                showFamilyDialog = false
            }
        )
    }

    qrFamily?.let { fam ->
        FamilyDigitalIdDialog(
            strings = strings,
            family = fam,
            memberCount = membersByFamily[fam.id].orEmpty().size,
            onDismiss = { qrFamily = null }
        )
    }

    showMemberDialogForFamily?.let { fam ->
        MemberFormDialog(
            strings = strings,
            existing = null,
            preselectedFamily = fam,
            onDismiss = { showMemberDialogForFamily = null },
            onSave = { _, memberName, phone, relation, occupation, head, active ->
                onSaveMember(null, fam.id, fam.familyName, memberName, phone, relation, occupation, head, active)
                showMemberDialogForFamily = null
            }
        )
    }

    if (showGlobalMemberDialog || editingMember != null) {
        val target = editingMember
        MemberFormDialog(
            strings = strings,
            existing = target,
            families = families,
            onDismiss = { showGlobalMemberDialog = false; editingMember = null },
            onSave = { fam, memberName, phone, relation, occupation, head, active ->
                val resolvedFamId = target?.familyId ?: fam?.id
                val resolvedFamName = target?.familyName ?: fam?.familyName
                if (resolvedFamId != null && resolvedFamName != null) {
                    onSaveMember(target?.id, resolvedFamId, resolvedFamName, memberName, phone, relation, occupation, head, active)
                }
                showGlobalMemberDialog = false
                editingMember = null
            }
        )
    }

    selectedFamilyDetail?.let { fam ->
        val famMembers = membersByFamily[fam.id].orEmpty()
        val famPayments = paymentsByFamily[fam.id].orEmpty()
        val totalPaid = paidByFamily[fam.id] ?: 0.0
        val outstanding = if (fam.active) maxOf(0.0, activeDuesTotal - totalPaid) else 0.0

        AlertDialog(
            onDismissRequest = { selectedFamilyDetail = null },
            title = { Text("${fam.familyCode} • ${fam.familyName}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!fam.phone.isNullOrBlank()) Text("${strings.phone}: ${fam.phone}", style = MaterialTheme.typography.bodySmall)
                    if (!fam.address.isNullOrBlank()) Text("${strings.address}: ${fam.address}", style = MaterialTheme.typography.bodySmall)
                    if (!fam.linkedEmail.isNullOrBlank()) Text("${strings.linkedEmailLabel}: ${fam.linkedEmail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("${strings.totalPaid}: ${formatCurrency(totalPaid)}", SemanticSuccessContainer, SemanticSuccess)
                        if (outstanding > 0.0) StatusPill("${strings.dueLabel}: ${formatCurrency(outstanding)}", SemanticErrorContainer, SemanticError)
                    }
                    HorizontalDivider()
                    Text(strings.navMembers, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (famMembers.isEmpty()) {
                        Text(strings.emptyMembers, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        famMembers.forEach { m ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.memberName, fontWeight = FontWeight.SemiBold)
                                    val meta = listOfNotNull(m.relation, m.occupation, m.phone).joinToString(" • ")
                                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
                                }
                                if (canEdit) {
                                    IconButton(onClick = { onDeleteMember(m.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    if (famPayments.isNotEmpty()) {
                        Text(strings.tabPayments, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        famPayments.take(5).forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${p.paymentDate} • ${p.receiptNo}", style = MaterialTheme.typography.bodySmall)
                                Text(formatCurrency(p.amount), style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily), color = SemanticSuccess)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectedFamilyDetail = null; qrFamily = fam }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR ID", maxLines = 1)
                        }
                        if (canEdit) {
                            OutlinedButton(onClick = { selectedFamilyDetail = null; showMemberDialogForFamily = fam }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addMember, maxLines = 1)
                            }
                        }
                        if (canCollectPayment) {
                            Button(onClick = { selectedFamilyDetail = null; onQuickCollect(fam, outstanding) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.collectNow, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedFamilyDetail = null }) { Text(strings.cancel) } }
        )
    }
}
