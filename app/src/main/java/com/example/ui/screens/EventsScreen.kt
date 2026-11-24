package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SocietyEvent
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EventFormDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsScreen(
    strings: AppStrings,
    canManageOps: Boolean,
    events: List<SocietyEvent>,
    onSaveEvent: (id: String?, title: String, eventDate: String, eventTime: String?, location: String?, eventType: String, description: String?, rsvpEnabled: Boolean) -> Unit,
    onDeleteEvent: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf("all") }
    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<SocietyEvent?>(null) }

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val upcomingCount = remember(events, todayIso) { events.count { it.eventDate >= todayIso } }
    val completedCount = events.size - upcomingCount

    val filtered = remember(events, selectedType) {
        if (selectedType == "all") events else events.filter { it.eventType == selectedType }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canManageOps) {
                FloatingActionButton(
                    onClick = { editingEvent = null; showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("events_add_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.addEvent)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(strings.totalEventsLabel, events.size.toString(), Icons.Filled.Event, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
                        MetricTile(strings.upcomingLabel, upcomingCount.toString(), Icons.Filled.Schedule, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
                        MetricTile(strings.completedLabel, completedCount.toString(), Icons.Filled.CheckCircle, PurpleAccent, PurpleContainer, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to strings.allItems,
                            "religious" to "Religious",
                            "social" to "Social",
                            "education" to "Education",
                            "informational" to "Info"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("event_filter_$key")
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item { EmptyStateCard(Icons.Filled.Event, strings.emptyEvents) }
                } else {
                    items(filtered, key = { it.id }) { event ->
                        val isUpcoming = event.eventDate >= todayIso
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (event.rsvpEnabled) {
                                            StatusPill("RSVP", PurpleContainer, PurpleAccent)
                                        }
                                        StatusPill(
                                            text = event.eventType.uppercase(),
                                            containerColor = if (isUpcoming) SemanticSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isUpcoming) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${event.eventDate}${event.eventTime?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (!event.location.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(event.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (!event.description.isNullOrBlank()) {
                                    Text(event.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (canManageOps) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { editingEvent = event; showDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = strings.save)
                                        }
                                        IconButton(onClick = { onDeleteEvent(event.id) }) {
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

    if (showDialog) {
        EventFormDialog(
            strings = strings,
            existing = editingEvent,
            onDismiss = { showDialog = false },
            onSave = { title, date, time, location, type, desc, rsvp ->
                onSaveEvent(editingEvent?.id, title, date, time, location, type, desc, rsvp)
                showDialog = false
            }
        )
    }
}
