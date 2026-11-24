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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Family
import com.example.data.model.PaymentRecord
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.util.ReportExporter

@Composable
fun MasikKhataScreen(
    strings: AppStrings,
    families: List<Family>,
    payments: List<PaymentRecord>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedYear by remember { mutableIntStateOf(2026) }
    val years = listOf(2025, 2026, 2027, 2028)
    val months = (1..12).toList()
    val monthNames = listOf("জানু", "ফেব", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্ট", "অক্টো", "নভে", "ডিসে")

    // Filter payments for selected year
    val yearPayments = remember(payments, selectedYear) {
        payments.filter { it.paymentDate.startsWith(selectedYear.toString()) }
    }

    // Map familyId -> (month 1..12 -> list of paid amounts in that month)
    val familyMonthPaidMap = remember(yearPayments) {
        val map = mutableMapOf<String, MutableMap<Int, Double>>()
        yearPayments.forEach { p ->
            val monthInt = p.paymentDate.drop(5).take(2).toIntOrNull() ?: 1
            val famMap = map.getOrPut(p.familyId) { mutableMapOf() }
            famMap[monthInt] = (famMap[monthInt] ?: 0.0) + p.amount
        }
        map
    }

    // Monthly totals for footer
    val monthlyTotals = remember(familyMonthPaidMap) {
        val totals = MutableList(12) { 0.0 }
        familyMonthPaidMap.values.forEach { famMap ->
            famMap.forEach { (m, amt) ->
                if (m in 1..12) totals[m - 1] += amt
            }
        }
        totals
    }
    val grandTotalYear = monthlyTotals.sum()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("মাসিক চাঁদা আদায়ের খাতা", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("সাল: $selectedYear • কোন সদস্য কোন মাসে জমা দিয়েছেন তার রেজিস্টার", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        // Export summary CSV or report
                                        ReportExporter.exportPublicSummaryCsv(context, com.example.data.model.PublicSummary(totalFamilies = families.size.toLong(), totalCollection = grandTotalYear), payments, emptyList())
                                    },
                                    modifier = Modifier.testTag("export_khata_csv_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("রিপোর্ট ডাউনলোড", maxLines = 1)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("বছর নির্বাচন:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            years.forEach { yr ->
                                FilterChip(
                                    selected = selectedYear == yr,
                                    onClick = { selectedYear = yr },
                                    label = { Text(yr.toString()) }
                                )
                            }
                        }
                    }
                }
            }

            if (families.isEmpty()) {
                item {
                    EmptyStateCard(Icons.Filled.CalendarMonth, "কোনো পরিবার নিবন্ধিত নেই। আগে পরিবার যোগ করুন।")
                }
            } else {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Horizontal scrollable table matching user's image
                            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                Column {
                                    // Table Header
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCell("ক্রম", width = 50.dp, isHeader = true)
                                            TableCell("দাতার নাম", width = 140.dp, isHeader = true)
                                            TableCell("মোবাইল", width = 110.dp, isHeader = true)
                                            TableCell("মাসিক চাঁদা", width = 85.dp, isHeader = true)
                                            monthNames.forEach { m ->
                                                TableCell(m, width = 65.dp, isHeader = true)
                                            }
                                            TableCell("মোট ($selectedYear)", width = 95.dp, isHeader = true)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Table Rows
                                    families.forEachIndexed { index, family ->
                                        val famMap = familyMonthPaidMap[family.id] ?: emptyMap()
                                        val famTotal = famMap.values.sum()
                                        val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

                                        Surface(color = rowBg) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TableCell((index + 1).toString(), width = 50.dp, align = TextAlign.Center)
                                                TableCell(family.familyName, width = 140.dp)
                                                TableCell(family.phone ?: "-", width = 110.dp, fontFamily = AppMonoFontFamily)
                                                TableCell("৳ 500", width = 85.dp, fontFamily = AppMonoFontFamily) // standard monthly due template

                                                months.forEach { m ->
                                                    val paidAmt = famMap[m] ?: 0.0
                                                    val cellText = if (paidAmt > 0.0) "৳${paidAmt.toInt()}" else "—"
                                                    val cellColor = if (paidAmt > 0.0) SemanticSuccess else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    
                                                    Box(
                                                        modifier = Modifier.width(65.dp).padding(horizontal = 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = cellText,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily),
                                                            fontWeight = if (paidAmt > 0.0) FontWeight.Bold else FontWeight.Normal,
                                                            color = cellColor,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }

                                                TableCell("৳${famTotal.toInt()}", width = 95.dp, fontFamily = AppMonoFontFamily, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (index < families.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        }
                                    }

                                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)

                                    // Footer Total Row
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCell("মোট", width = 50.dp, isHeader = true)
                                            TableCell("মোট আদায়", width = 140.dp, isHeader = true)
                                            TableCell("-", width = 110.dp, isHeader = true)
                                            TableCell("-", width = 85.dp, isHeader = true)
                                            monthlyTotals.forEach { mTot ->
                                                Box(
                                                    modifier = Modifier.width(65.dp).padding(horizontal = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (mTot > 0.0) "৳${mTot.toInt()}" else "0",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            TableCell("৳${grandTotalYear.toInt()}", width = 95.dp, isHeader = true, fontFamily = AppMonoFontFamily)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "নিয়ম: প্রতি মাসে চাঁদা পেলে সেই মাসের ঘরে টাকার অঙ্ক দেখাবে। বছর শেষে ডান পাশে মোট লিখে খাতা মিলিয়ে নিন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    align: TextAlign = TextAlign.Start,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    fontWeight: FontWeight? = null
) {
    Box(
        modifier = Modifier.width(width).padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = when (align) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodySmall,
            fontFamily = fontFamily,
            fontWeight = fontWeight ?: if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = align,
            maxLines = 1,
            fontSize = 12.sp
        )
    }
}
