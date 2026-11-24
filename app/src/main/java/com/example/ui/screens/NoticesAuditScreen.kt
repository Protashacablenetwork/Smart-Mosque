package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLogEntry
import com.example.data.model.Notice
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NoticeFormDialog
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning

@Composable
fun NoticesAuditScreen(
    strings: AppStrings,
    isStaffReader: Boolean,
    canManageNotices: Boolean,
    currentUserName: String,
    notices: List<Notice>,
    auditLogs: List<AuditLogEntry>,
    onPublishNotice: (title: String, content: String, category: String, pinned: Boolean, authorName: String) -> Unit,
    onToggleNoticePinned: (id: String, pinned: Boolean) -> Unit,
    onDeleteNotice: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canManageNotices && selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = { showNoticeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_notice_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.publishNotice)
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
                if (isStaffReader) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, label = { Text("${strings.tabNotices} (${notices.size})") })
                            FilterChip(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, label = { Text("${strings.tabAuditLogs} (${auditLogs.size})") })
                        }
                    }
                }

                when (selectedSubTab) {
                    0 -> {
                        if (notices.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.Campaign, strings.noNoticesYet) }
                        } else {
                            items(notices, key = { it.id }) { notice ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                StatusPill(notice.category.uppercase(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                                if (notice.pinned) {
                                                    StatusPill(strings.pinnedLabel, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                            Text(notice.title, style = MaterialTheme.typography.titleMedium)
                                            Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (canManageNotices) {
                                            Row {
                                                IconButton(onClick = { onToggleNoticePinned(notice.id, !notice.pinned) }) {
                                                    Icon(
                                                        imageVector = if (notice.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                        contentDescription = strings.pinnedLabel,
                                                        tint = if (notice.pinned) SemanticWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteNotice(notice.id) }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (auditLogs.isEmpty()) {
                            item { EmptyStateCard(Icons.Filled.History, strings.emptyAuditLogs) }
                        } else {
                            items(auditLogs, key = { it.id }) { log ->
                                Card(
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            StatusPill(log.action, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text(log.tableName, style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (!log.summary.isNullOrBlank()) {
                                            Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (!log.beforeSummary.isNullOrBlank()) {
                                            Text("Before: ${log.beforeSummary}", style = MaterialTheme.typography.labelSmall, color = SemanticError)
                                        }
                                        if (!log.afterSummary.isNullOrBlank()) {
                                            Text("After: ${log.afterSummary}", style = MaterialTheme.typography.labelSmall, color = SemanticSuccess)
                                        }
                                        if (!log.userEmail.isNullOrBlank()) {
                                            Text(log.userEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showNoticeDialog) {
        NoticeFormDialog(
            strings = strings,
            onDismiss = { showNoticeDialog = false },
            onPublish = { title, content, category, pinned ->
                onPublishNotice(title, content, category, pinned, currentUserName)
                showNoticeDialog = false
            }
        )
    }
}
