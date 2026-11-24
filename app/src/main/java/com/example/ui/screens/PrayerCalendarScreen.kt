package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MeetingRecord
import com.example.data.model.MosqueSchedule
import com.example.data.model.SocietyEvent
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MosqueScheduleDialog
import com.example.ui.components.SinglePrayerTimeDialog
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SidebarGradientBottom
import com.example.ui.theme.SidebarGradientMid
import com.example.ui.theme.SidebarGradientTop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class PrayerTileSlot(
    val key: String,
    val label: String,
    val time: String
)

private fun parseTimeMinutes(timeStr: String?): Int? {
    if (timeStr.isNullOrBlank()) return null
    val trimmed = timeStr.trim()
    val formats = listOf("hh:mm a", "h:mm a", "HH:mm")
    for (pattern in formats) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(trimmed)
        }.getOrNull()
        if (parsed != null) {
            val c = Calendar.getInstance().apply { time = parsed }
            return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        }
    }
    return null
}

private fun determineCurrentAndNextWaqt(
    strings: AppStrings,
    schedule: MosqueSchedule
): Pair<PrayerTileSlot, PrayerTileSlot?> {
    val slots = listOf(
        PrayerTileSlot("fajr", strings.fajr, schedule.fajrTime),
        PrayerTileSlot("sunrise", strings.sunrise, schedule.sunriseTime ?: "06:22 AM"),
        PrayerTileSlot("dhuhr", strings.dhuhr, schedule.dhuhrTime),
        PrayerTileSlot("asr", strings.asr, schedule.asrTime),
        PrayerTileSlot("maghrib", strings.maghrib, schedule.maghribTime),
        PrayerTileSlot("isha", strings.isha, schedule.ishaTime)
    )
    val now = Calendar.getInstance()
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val timed = slots.mapNotNull { slot -> parseTimeMinutes(slot.time)?.let { it to slot } }.sortedBy { it.first }
    if (timed.isEmpty()) return slots.first() to slots.getOrNull(1)
    val current = timed.lastOrNull { it.first <= nowMin }?.second ?: timed.last().second
    val next = timed.firstOrNull { it.first > nowMin }?.second ?: timed.first().second
    return current to next
}

@Composable
fun PrayerCalendarScreen(
    strings: AppStrings,
    schedule: MosqueSchedule,
    events: List<SocietyEvent>,
    meetings: List<MeetingRecord>,
    canEditSchedule: Boolean,
    onSaveSchedule: (MosqueSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSlot by remember { mutableStateOf<PrayerTileSlot?>(null) }
    var showFullDialog by remember { mutableStateOf(false) }

    val initialCal = remember { Calendar.getInstance() }
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDateIso by remember { mutableStateOf<String?>(null) }

    val (currentWaqt, nextWaqt) = remember(schedule, strings) {
        determineCurrentAndNextWaqt(strings, schedule)
    }

    val slots = remember(schedule, strings) {
        listOf(
            PrayerTileSlot("fajr", strings.fajr, schedule.fajrTime),
            PrayerTileSlot("sunrise", strings.sunrise, schedule.sunriseTime ?: "06:22 AM"),
            PrayerTileSlot("dhuhr", strings.dhuhr, schedule.dhuhrTime),
            PrayerTileSlot("asr", strings.asr, schedule.asrTime),
            PrayerTileSlot("maghrib", strings.maghrib, schedule.maghribTime),
            PrayerTileSlot("isha", strings.isha, schedule.ishaTime)
        )
    }

    val monthCalendar = remember(monthOffset) {
        (initialCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = remember(monthCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(monthCalendar.time)
    }
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time) }

    val activeDates = remember(events, meetings) {
        (events.map { it.eventDate } + meetings.map { it.meetingDate }).toSet()
    }

    val filteredEvents = remember(events, selectedDateIso, todayIso) {
        if (selectedDateIso != null) events.filter { it.eventDate == selectedDateIso }
        else events.filter { it.eventDate >= todayIso }.ifEmpty { events }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PrayerHeroBanner(
                    strings = strings,
                    mosqueName = schedule.mosqueName.ifBlank { strings.mosqueScheduleTitle },
                    currentWaqt = currentWaqt,
                    nextWaqt = nextWaqt,
                    canEditSchedule = canEditSchedule,
                    onEditFull = { showFullDialog = true }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.mosqueScheduleTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (canEditSchedule) {
                            Text(
                                text = strings.tapTimeToEdit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    slots.chunked(3).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                val isCurrent = slot.key == currentWaqt.key
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(enabled = canEditSchedule) { editingSlot = slot }
                                        .testTag("prayer_slot_${slot.key}")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (slot.key == "sunrise") {
                                            Icon(Icons.Filled.WbSunny, contentDescription = slot.label, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = slot.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = slot.time,
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                JumuahAndImamCard(
                    strings = strings,
                    schedule = schedule,
                    canEditSchedule = canEditSchedule,
                    onEdit = { showFullDialog = true }
                )
            }

            item {
                MosqueMonthCalendarCard(
                    strings = strings,
                    monthCalendar = monthCalendar,
                    monthTitle = monthTitle,
                    todayIso = todayIso,
                    selectedDateIso = selectedDateIso,
                    activeDates = activeDates,
                    onPrevMonth = { monthOffset -= 1 },
                    onNextMonth = { monthOffset += 1 },
                    onSelectDate = { iso ->
                        selectedDateIso = if (selectedDateIso == iso) null else iso
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDateIso != null) "${strings.upcomingEventsTitle} ($selectedDateIso)" else strings.upcomingEventsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDateIso != null) {
                        OutlinedButton(onClick = { selectedDateIso = null }) {
                            Text(strings.allItems)
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item { EmptyStateCard(Icons.Filled.Event, strings.emptyEvents) }
            } else {
                items(filteredEvents.take(10), key = { it.id }) { event ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = listOfNotNull(event.eventDate, event.eventTime, event.location).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = event.eventType.uppercase(),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    editingSlot?.let { slot ->
        SinglePrayerTimeDialog(
            strings = strings,
            prayerLabel = slot.label,
            currentTime = slot.time,
            onDismiss = { editingSlot = null },
            onSave = { newTime ->
                val updated = when (slot.key) {
                    "fajr" -> schedule.copy(fajrTime = newTime)
                    "sunrise" -> schedule.copy(sunriseTime = newTime)
                    "dhuhr" -> schedule.copy(dhuhrTime = newTime)
                    "asr" -> schedule.copy(asrTime = newTime)
                    "maghrib" -> schedule.copy(maghribTime = newTime)
                    "isha" -> schedule.copy(ishaTime = newTime)
                    else -> schedule
                }
                onSaveSchedule(updated)
                editingSlot = null
            }
        )
    }

    if (showFullDialog) {
        MosqueScheduleDialog(
            strings = strings,
            existing = schedule,
            onDismiss = { showFullDialog = false },
            onSave = { updated ->
                onSaveSchedule(updated)
                showFullDialog = false
            }
        )
    }
}

@Composable
private fun PrayerHeroBanner(
    strings: AppStrings,
    mosqueName: String,
    currentWaqt: PrayerTileSlot,
    nextWaqt: PrayerTileSlot?,
    canEditSchedule: Boolean,
    onEditFull: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SidebarGradientTop, SidebarGradientMid, SidebarGradientBottom)))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Mosque, contentDescription = null, tint = Color(0xFFD4AF37))
                        Text(mosqueName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (canEditSchedule) {
                        IconButton(onClick = onEditFull, modifier = Modifier.testTag("prayer_edit_all_button")) {
                            Icon(Icons.Filled.Edit, contentDescription = strings.editMosqueSchedule, tint = Color.White)
                        }
                    }
                }
                StatusPill(
                    text = strings.currentWaqtBadge,
                    containerColor = Color(0xFF1F543F),
                    contentColor = Color(0xFFD0F0E3)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(currentWaqt.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        if (nextWaqt != null && nextWaqt.key != currentWaqt.key) {
                            Text("${nextWaqt.label}: ${nextWaqt.time}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA7C7B7))
                        }
                    }
                    Text(
                        text = currentWaqt.time,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AppMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}

@Composable
private fun JumuahAndImamCard(
    strings: AppStrings,
    schedule: MosqueSchedule,
    canEditSchedule: Boolean,
    onEdit: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${strings.jumuah}: ${schedule.jumuahTime}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (canEditSchedule) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.editMosqueSchedule, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (!schedule.khutbahTopic.isNullOrBlank()) {
                Text("${strings.khutbahTopic}: ${schedule.khutbahTopic}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!schedule.sehriEnd.isNullOrBlank() || !schedule.iftarTime.isNullOrBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!schedule.sehriEnd.isNullOrBlank()) {
                        StatusPill("${strings.sehriEnd}: ${schedule.sehriEnd}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (!schedule.iftarTime.isNullOrBlank()) {
                        StatusPill("${strings.iftarTime}: ${schedule.iftarTime}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            if (!schedule.imamName.isNullOrBlank() || !schedule.muazzinName.isNullOrBlank()) {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!schedule.imamName.isNullOrBlank()) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("${schedule.imamName}${schedule.imamPhone?.let { "\n$it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (!schedule.muazzinName.isNullOrBlank()) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text("${schedule.muazzinName}${schedule.muazzinPhone?.let { "\n$it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MosqueMonthCalendarCard(
    strings: AppStrings,
    monthCalendar: Calendar,
    monthTitle: String,
    todayIso: String,
    selectedDateIso: String?,
    activeDates: Set<String>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = monthCalendar.get(Calendar.YEAR)
    val month = monthCalendar.get(Calendar.MONTH) + 1

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.islamicCalendarTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(monthTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dow ->
                    Text(
                        text = dow,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - firstDayOfWeek + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val iso = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
                            val isToday = iso == todayIso
                            val isSelected = iso == selectedDateIso
                            val hasEvent = iso in activeDates
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDate(iso) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasEvent) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)
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
