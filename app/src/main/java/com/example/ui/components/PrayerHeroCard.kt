package com.example.ui.components

import android.icu.util.IslamicCalendar
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MosqueSchedule
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SidebarGradientTop
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class WaqtEntry(
    val bnName: String,
    val enName: String,
    val timeLabel: String,
    val minuteOfDay: Int
)

private fun parseMinuteOfDay(raw: String, fallbackMinute: Int): Int {
    val clean = raw.trim().uppercase(Locale.US)
    val match = Regex("(\\d{1,2}):(\\d{2})\\s*(AM|PM)?").find(clean) ?: return fallbackMinute
    var hour = match.groupValues[1].toIntOrNull() ?: return fallbackMinute
    val minute = match.groupValues[2].toIntOrNull() ?: 0
    val amPm = match.groupValues[3]
    if (amPm == "PM" && hour < 12) hour += 12
    if (amPm == "AM" && hour == 12) hour = 0
    return (hour * 60 + minute).coerceIn(0, 1439)
}

private fun formatBanglaDigits(str: String): String {
    val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return str.map { if (it in '0'..'9') bn[it - '0'] else it }.joinToString("")
}

private fun formatDurationHms(totalSeconds: Int, isBangla: Boolean): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    val formatted = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    return if (isBangla) formatBanglaDigits(formatted) else formatted
}

private fun buildHijriDateString(cal: Calendar, isBangla: Boolean): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val hijri = IslamicCalendar(cal.time)
            val day = hijri.get(IslamicCalendar.DAY_OF_MONTH)
            val month = hijri.get(IslamicCalendar.MONTH)
            val year = hijri.get(IslamicCalendar.YEAR)
            val bnMonths = listOf(
                "মুহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
                "জুমাদাল উলা", "জুমাদাস সানি", "রজব", "শাবান",
                "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
            )
            val enMonths = listOf(
                "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
                "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah"
            )
            val mName = if (isBangla) bnMonths.getOrElse(month) { "" } else enMonths.getOrElse(month) { "" }
            if (isBangla) formatBanglaDigits("$day $mName, $year হিজরি") else "$day $mName, $year AH"
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}

private fun buildGregorianDateString(cal: Calendar, isBangla: Boolean): String {
    if (!isBangla) {
        return SimpleDateFormat("dd MMM, yyyy", Locale.US).format(cal.time)
    }
    val bnMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = bnMonths.getOrElse(cal.get(Calendar.MONTH)) { "" }
    val year = cal.get(Calendar.YEAR)
    return formatBanglaDigits("$day $month, $year")
}

@Composable
fun HomePrayerHeroCard(
    strings: AppStrings,
    schedule: MosqueSchedule,
    isBangla: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tickMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tickMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cal = remember(tickMillis) {
        Calendar.getInstance().apply { timeInMillis = tickMillis }
    }
    val nowSecOfDay = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)

    val waqts = remember(schedule) {
        listOf(
            WaqtEntry("ফজর", "Fajr", schedule.fajrTime, parseMinuteOfDay(schedule.fajrTime, 5 * 60 + 15)),
            WaqtEntry("যোহর", "Dhuhr", schedule.dhuhrTime, parseMinuteOfDay(schedule.dhuhrTime, 13 * 60 + 15)),
            WaqtEntry("আসর", "Asr", schedule.asrTime, parseMinuteOfDay(schedule.asrTime, 16 * 60 + 15)),
            WaqtEntry("মাগরিব", "Maghrib", schedule.maghribTime, parseMinuteOfDay(schedule.maghribTime, 17 * 60 + 30)),
            WaqtEntry("এশা", "Isha", schedule.ishaTime, parseMinuteOfDay(schedule.ishaTime, 19 * 60 + 30))
        ).sortedBy { it.minuteOfDay }
    }

    val nextIndex = waqts.indexOfFirst { it.minuteOfDay * 60 > nowSecOfDay }
    val nextWaqt = if (nextIndex >= 0) waqts[nextIndex] else waqts.first()
    val prevWaqt = if (nextIndex > 0) waqts[nextIndex - 1] else waqts.last()

    val remainingSec = if (nextIndex >= 0) {
        nextWaqt.minuteOfDay * 60 - nowSecOfDay
    } else {
        (24 * 3600 - nowSecOfDay) + nextWaqt.minuteOfDay * 60
    }
    val elapsedSec = if (nowSecOfDay >= prevWaqt.minuteOfDay * 60) {
        nowSecOfDay - prevWaqt.minuteOfDay * 60
    } else {
        (24 * 3600 - prevWaqt.minuteOfDay * 60) + nowSecOfDay
    }
    val totalWindowSec = (elapsedSec + remainingSec).coerceAtLeast(1)
    val progress = (elapsedSec.toFloat() / totalWindowSec.toFloat()).coerceIn(0f, 1f)

    val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val min = cal.get(Calendar.MINUTE)
    val sec = cal.get(Calendar.SECOND)
    val isPm = cal.get(Calendar.AM_PM) == Calendar.PM
    val clockRaw = String.format(Locale.US, "%02d:%02d:%02d", hour12, min, sec)
    val clockText = if (isBangla) {
        "${formatBanglaDigits(clockRaw)} ${if (isPm) "অপরাহ্ণ" else "পূর্বাহ্ণ"}"
    } else {
        "$clockRaw ${if (isPm) "PM" else "AM"}"
    }

    val gregDate = buildGregorianDateString(cal, isBangla)
    val hijriDate = buildHijriDateString(cal, isBangla)

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE7D9A7)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("home_prayer_hero_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Dark Emerald Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SidebarGradientTop)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = gregDate,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hijriDate.isNotBlank()) {
                            Text(
                                text = hijriDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.jumuah,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = schedule.jumuahTime,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = ShellGoldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Card Body: Live Clock & Next Jamaat
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.currentTimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = clockText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontFamily = AppMonoFontFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.nextJamaatLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBangla) nextWaqt.bnName else nextWaqt.enName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SidebarGradientTop
                        )
                        Text(
                            text = nextWaqt.timeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Elapsed & Remaining Progress
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.waqtElapsedLabel}: ${formatDurationHms(elapsedSec, isBangla)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${strings.jamaatRemainingLabel}: ${formatDurationHms(remainingSec, isBangla)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = SidebarGradientTop,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}
