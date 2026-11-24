package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.CommitteeMember
import com.example.data.model.ComplaintItem
import com.example.data.model.Family
import com.example.data.model.MeetingRecord
import com.example.data.model.WelfareRecord
import com.example.ui.components.CommitteeFormDialog
import com.example.ui.components.ComplaintFormDialog
import com.example.ui.components.ComplaintReplyDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MeetingDetailDialog
import com.example.ui.components.MeetingFormDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.WelfareFormDialog
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer

@Composable
fun CommunityScreen(
    strings: AppStrings,
    currentUserName: String,
    canManageOps: Boolean,
    canWriteWelfare: Boolean,
    canDeleteAdmin: Boolean,
    families: List<Family>,
    committees: List<CommitteeMember>,
    meetings: List<MeetingRecord>,
    welfareRecords: List<WelfareRecord>,
    complaints: List<ComplaintItem>,
    bankAccounts: List<BankAccount>,
    initialSubTab: Int = 0,
    onSaveCommittee: (id: String?, name: String, role: String, phone: String?, familyId: String?, familyName: String?, term: String?, active: Boolean) -> Unit,
    onDeleteCommittee: (String) -> Unit,
    onSaveMeeting: (id: String?, title: String, date: String, time: String?, location: String?, agenda: String?, attendees: String?, decisions: String, assignedTo: String?, deadline: String?, status: String) -> Unit,
    onDeleteMeeting: (String) -> Unit,
    onAddWelfare: (recordType: String, beneficiaryName: String, phone: String?, plotNumber: String?, amount: Double, date: String, note: String?, accountId: String?, accountName: String?) -> Unit,
    onDeleteWelfare: (String) -> Unit,
    onSubmitComplaint: (authorName: String, familyId: String?, subject: String, message: String) -> Unit,
    onReplyComplaint: (id: String, status: String, reply: String?) -> Unit,
    onDeleteComplaint: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(initialSubTab) } // 0=Committee, 1=Meetings, 2=Welfare, 3=Complaints
    LaunchedEffect(initialSubTab) {
        selectedSubTab = initialSubTab
    }
    var showCommitteeDialog by remember { mutableStateOf(false) }
    var editingCommittee by remember { mutableStateOf<CommitteeMember?>(null) }
    var showMeetingDialog by remember { mutableStateOf(false) }
    var editingMeeting by remember { mutableStateOf<MeetingRecord?>(null) }
    var viewingMeeting by remember { mutableStateOf<MeetingRecord?>(null) }
    var showWelfareDialog by remember { mutableStateOf(false) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var replyingComplaint by remember { mutableStateOf<ComplaintItem?>(null) }

    val totalWelfare = remember(welfareRecords) { welfareRecords.sumOf { it.amount } }
    val showFab = when (selectedSubTab) {
        0, 1 -> canManageOps
        2 -> canWriteWelfare
        else -> true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        when (selectedSubTab) {
                            0 -> { editingCommittee = null; showCommitteeDialog = true }
                            1 -> { editingMeeting = null; showMeetingDialog = true }
                            2 -> showWelfareDialog = true
                            else -> showComplaintDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("community_add_fab")
                ) { Icon(Icons.Filled.Add, contentDescription = strings.save) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, label = { Text("${strings.tabCommittee} (${committees.size})") })
                        FilterChip(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, label = { Text("${strings.tabMeetings} (${meetings.size})") })
                        FilterChip(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, label = { Text("${strings.tabWelfare} (${welfareRecords.size})") })
                        FilterChip(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, label = { Text("${strings.tabComplaints} (${complaints.size})") })
                    }
                }

                when (selectedSubTab) {
                    0 -> {
                        if (committees.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.Badge, strings.emptyCommittee) }
                        } else {
                            items(committees, key = { it.id }) { cm ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(cm.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                StatusPill(cm.role, PurpleContainer, PurpleAccent)
                                            }
                                            val meta = listOfNotNull(cm.phone, cm.familyName, cm.term).joinToString(" • ")
                                            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            StatusPill(
                                                text = if (cm.active) strings.activeStatus else strings.inactiveStatus,
                                                containerColor = if (cm.active) SemanticSuccessContainer else SemanticWarningContainer,
                                                contentColor = if (cm.active) SemanticSuccess else SemanticWarning
                                            )
                                        }
                                        if (canManageOps) {
                                            Row {
                                                IconButton(onClick = { editingCommittee = cm; showCommitteeDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = strings.save) }
                                                IconButton(onClick = { onDeleteCommittee(cm.id) }) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (meetings.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.EventNote, strings.emptyMeetings) }
                        } else {
                            items(meetings, key = { it.id }) { m ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { viewingMeeting = m }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            StatusPill(
                                                text = m.status.replace('_', ' ').uppercase(),
                                                containerColor = if (m.status == "completed") SemanticSuccessContainer else SemanticWarningContainer,
                                                contentColor = if (m.status == "completed") SemanticSuccess else SemanticWarning
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${m.meetingDate}${if (!m.meetingTime.isNullOrBlank()) " • ${m.meetingTime}" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            if (!m.location.isNullOrBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(m.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        if (!m.agenda.isNullOrBlank()) {
                                            Text("${strings.meetingAgendaLabel}: ${m.agenda}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(m.decisions, style = MaterialTheme.typography.bodyMedium)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            val meta = listOfNotNull(m.assignedTo?.let { "${strings.assignedTo}: $it" }, m.deadline?.let { "${strings.deadline}: $it" }).joinToString(" • ")
                                            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                            if (canManageOps) {
                                                Row {
                                                    IconButton(onClick = { editingMeeting = m; showMeetingDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = strings.save) }
                                                    IconButton(onClick = { onDeleteMeeting(m.id) }) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        item { MetricTile(strings.totalWelfare, formatCurrency(totalWelfare), Icons.Filled.VolunteerActivism, InfoBlue, InfoBlueContainer) }
                        if (welfareRecords.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.VolunteerActivism, strings.emptyWelfare) }
                        } else {
                            items(welfareRecords, key = { it.id }) { w ->
                                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                StatusPill(w.recordType.replace('_', ' ').uppercase(), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                                Text(w.recordDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(w.beneficiaryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            val extra = listOfNotNull(w.phone, w.plotNumber?.let { "${strings.plotNumber}: $it" }, w.accountName, w.note).joinToString(" • ")
                                            if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (w.amount > 0.0) {
                                            Text(formatCurrency(w.amount), style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticError)
                                        }
                                        if (canDeleteAdmin) {
                                            IconButton(onClick = { onDeleteWelfare(w.id) }) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (complaints.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.Feedback, strings.emptyComplaints) }
                        } else {
                            items(complaints, key = { it.id }) { c ->
                                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(c.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(c.authorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            StatusPill(c.status.uppercase(), if (c.status == "resolved") SemanticSuccessContainer else SemanticWarningContainer, if (c.status == "resolved") SemanticSuccess else SemanticWarning)
                                        }
                                        Text(c.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!c.adminReply.isNullOrBlank()) {
                                            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                                Text("${strings.adminReplyLabel}: ${c.adminReply}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (canManageOps) IconButton(onClick = { replyingComplaint = c }) { Icon(Icons.Filled.Reply, contentDescription = strings.replyComplaint, tint = MaterialTheme.colorScheme.primary) }
                                            if (canDeleteAdmin) IconButton(onClick = { onDeleteComplaint(c.id) }) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) }
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

    if (showCommitteeDialog) {
        CommitteeFormDialog(
            strings = strings,
            existing = editingCommittee,
            families = families,
            onDismiss = { showCommitteeDialog = false },
            onSave = { name, role, phone, fam, term, active ->
                onSaveCommittee(editingCommittee?.id, name, role, phone, fam?.id, fam?.familyName, term, active)
                showCommitteeDialog = false
            }
        )
    }

    if (showMeetingDialog) {
        MeetingFormDialog(
            strings = strings,
            existing = editingMeeting,
            onDismiss = { showMeetingDialog = false },
            onSave = { title, date, time, location, agenda, attendees, decisions, assignedTo, deadline, status ->
                onSaveMeeting(editingMeeting?.id, title, date, time, location, agenda, attendees, decisions, assignedTo, deadline, status)
                showMeetingDialog = false
            }
        )
    }

    viewingMeeting?.let { m ->
        MeetingDetailDialog(strings = strings, meeting = m, onDismiss = { viewingMeeting = null })
    }

    if (showWelfareDialog) {
        WelfareFormDialog(
            strings = strings,
            accounts = bankAccounts,
            onDismiss = { showWelfareDialog = false },
            onSave = { type, beneficiary, phone, plot, amount, date, note, acc ->
                onAddWelfare(type, beneficiary, phone, plot, amount, date, note, acc?.id, acc?.accountName)
                showWelfareDialog = false
            }
        )
    }

    if (showComplaintDialog) {
        ComplaintFormDialog(
            strings = strings,
            onDismiss = { showComplaintDialog = false },
            onSubmit = { subject, message -> onSubmitComplaint(currentUserName, null, subject, message); showComplaintDialog = false }
        )
    }

    replyingComplaint?.let { item ->
        ComplaintReplyDialog(
            strings = strings,
            item = item,
            onDismiss = { replyingComplaint = null },
            onSave = { status, reply -> onReplyComplaint(item.id, status, reply); replyingComplaint = null }
        )
    }
}
