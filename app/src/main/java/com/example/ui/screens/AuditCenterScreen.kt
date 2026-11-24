package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.model.AuditLogEntry
import com.example.ui.components.BreakdownBarList
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AuditCenterScreen(
    strings: AppStrings,
    auditLogs: List<AuditLogEntry>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedTable by remember { mutableStateOf("all") }
    var selectedAction by remember { mutableStateOf("all") }

    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    val distinctTables = remember(auditLogs) {
        auditLogs.map { it.tableName }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val distinctActions = remember(auditLogs) {
        auditLogs.map { it.action }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val latestDate = remember(auditLogs) {
        auditLogs.firstOrNull()?.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "—"
    }

    val filtered = remember(auditLogs, query, selectedTable, selectedAction) {
        val q = query.trim().lowercase()
        auditLogs.filter { log ->
            val matchesTable = selectedTable == "all" || log.tableName == selectedTable
            val matchesAction = selectedAction == "all" || log.action == selectedAction
            val matchesQuery = q.isEmpty() ||
                log.action.lowercase().contains(q) ||
                log.tableName.lowercase().contains(q) ||
                (log.summary?.lowercase()?.contains(q) == true) ||
                (log.userEmail?.lowercase()?.contains(q) == true)
            matchesTable && matchesAction && matchesQuery
        }
    }

    val tableActivity = remember(auditLogs) {
        auditLogs.groupBy { it.tableName.ifBlank { "other" } }
            .mapValues { (_, list) -> list.size.toDouble() }
            .entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile(strings.totalLogsLabel, auditLogs.size.toString(), Icons.Filled.History, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
                        MetricTile(strings.shownLogsLabel, filtered.size.toString(), Icons.Filled.FilterList, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile(strings.tablesCountLabel, distinctTables.size.toString(), Icons.Filled.TableChart, PurpleAccent, PurpleContainer, modifier = Modifier.weight(1f))
                        MetricTile(strings.latestDateLabel, latestDate, Icons.Filled.Today, SemanticWarning, SemanticWarningContainer, modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(strings.searchAuditHint) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("audit_search_input")
                    )
                    OutlinedButton(
                        onClick = { ReportExporter.exportAuditLogsCsv(context, filtered) },
                        enabled = filtered.isNotEmpty(),
                        modifier = Modifier.testTag("export_audit_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.exportAuditCsv)
                    }
                }
            }

            if (distinctTables.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = selectedTable == "all", onClick = { selectedTable = "all" }, label = { Text(strings.allItems) })
                        distinctTables.forEach { tbl ->
                            FilterChip(selected = selectedTable == tbl, onClick = { selectedTable = tbl }, label = { Text(tbl) })
                        }
                    }
                }
            }

            if (distinctActions.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = selectedAction == "all", onClick = { selectedAction = "all" }, label = { Text(strings.allItems) })
                        distinctActions.forEach { act ->
                            FilterChip(selected = selectedAction == act, onClick = { selectedAction = act }, label = { Text(act) })
                        }
                    }
                }
            }

            if (tableActivity.isNotEmpty()) {
                item {
                    BreakdownBarList(
                        title = strings.tableWiseActivity,
                        items = tableActivity,
                        barColor = PurpleAccent,
                        valueFormatter = { it.toInt().toString() }
                    )
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyStateCard(Icons.Filled.History, strings.emptyAuditLogs) }
            } else {
                items(filtered, key = { it.id }) { log ->
                    val ts = log.createdAt?.toDate()?.let { timeFmt.format(it) } ?: "—"
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(log.action, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                    StatusPill(log.tableName, PurpleContainer, PurpleAccent)
                                }
                                Text(ts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!log.summary.isNullOrBlank()) {
                                Text(log.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
