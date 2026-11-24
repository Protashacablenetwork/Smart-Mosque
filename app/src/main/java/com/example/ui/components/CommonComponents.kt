package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentRecord
import com.example.data.prefs.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    return "৳ ${nf.format(amount)}"
}

fun formatCompactCurrency(amount: Double): String {
    return when {
        amount >= 100_000 -> "৳${(amount / 1000).toInt()}k"
        amount >= 1000 -> String.format(Locale.US, "৳%.1fk", amount / 1000.0)
        else -> "৳${amount.toInt()}"
    }
}

fun formatKg(kg: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 3
    }
    return "${nf.format(kg)} kg"
}

@Composable
fun LanguageToggleChip(
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .testTag("language_toggle_button"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = if (language == AppLanguage.BANGLA) "বাংলা / EN" else "EN / বাংলা",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun HeroGreetingBanner(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
    bottomContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A7D52), Color(0xFF0A5C3D))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-50).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
                if (bottomContent != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    bottomContent()
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 22.dp, y = (-22).dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.08f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CollectionVsExpenseChart(
    title: String,
    collectionLabel: String,
    expenseLabel: String,
    collection: Double,
    expense: Double,
    recentMonths: List<String> = emptyList(),
    recentCollections: List<Double> = emptyList(),
    recentExpenses: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    val collectionColor = SemanticSuccess
    val expenseColor = SemanticError
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = collectionColor, label = collectionLabel)
                    LegendDot(color = expenseColor, label = expenseLabel)
                }
            }

            if (recentMonths.isNotEmpty() &&
                recentCollections.size == recentMonths.size &&
                recentExpenses.size == recentMonths.size
            ) {
                val maxSeriesVal = maxOf(
                    recentCollections.maxOrNull() ?: 0.0,
                    recentExpenses.maxOrNull() ?: 0.0,
                    100.0
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .height(140.dp)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = formatCompactCurrency(maxSeriesVal),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCompactCurrency(maxSeriesVal / 2.0),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "৳0",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = AppMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            listOf(0f, size.height * 0.5f, size.height).forEach { y ->
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (y < size.height) dash else null
                                )
                            }

                            val groupCount = recentMonths.size
                            val groupWidth = size.width / groupCount
                            val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                            val gap = 3.dp.toPx()

                            recentMonths.indices.forEach { i ->
                                val colVal = recentCollections[i]
                                val expVal = recentExpenses[i]
                                val colH = ((colVal / maxSeriesVal).toFloat().coerceIn(0f, 1f)) * (size.height - 4.dp.toPx())
                                val expH = ((expVal / maxSeriesVal).toFloat().coerceIn(0f, 1f)) * (size.height - 4.dp.toPx())

                                val centerX = groupWidth * i + groupWidth / 2f
                                val colX = centerX - barWidth - gap / 2f
                                val expX = centerX + gap / 2f

                                if (colH > 0f) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(collectionColor, collectionColor.copy(alpha = 0.72f))
                                        ),
                                        topLeft = Offset(colX, size.height - colH),
                                        size = Size(barWidth, colH.coerceAtLeast(3.dp.toPx())),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                                if (expH > 0f) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(expenseColor, expenseColor.copy(alpha = 0.72f))
                                        ),
                                        topLeft = Offset(expX, size.height - expH),
                                        size = Size(barWidth, expH.coerceAtLeast(3.dp.toPx())),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            recentMonths.forEach { ym ->
                                val shortLabel = ym.takeLast(2)
                                Text(
                                    text = shortMonthLabel(shortLabel),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                val maxVal = maxOf(collection, expense, 1.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$collectionLabel: ${formatCurrency(collection)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = collectionColor
                    )
                    Text(
                        text = "$expenseLabel: ${formatCurrency(expense)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = expenseColor
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    val barHeight = 14.dp.toPx()
                    val gap = 12.dp.toPx()
                    val colWidth = ((collection / maxVal).toFloat().coerceIn(0.02f, 1f)) * size.width
                    val expWidth = ((expense / maxVal).toFloat().coerceIn(0.02f, 1f)) * size.width

                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(collectionColor, collectionColor.copy(alpha = 0.78f))),
                        topLeft = Offset(0f, 0f),
                        size = Size(colWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )

                    val secondY = barHeight + gap
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(0f, secondY),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(expenseColor, expenseColor.copy(alpha = 0.78f))),
                        topLeft = Offset(0f, secondY),
                        size = Size(expWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                    )
                }
            }
        }
    }
}

private fun shortMonthLabel(mm: String): String = when (mm) {
    "01" -> "Jan"
    "02" -> "Feb"
    "03" -> "Mar"
    "04" -> "Apr"
    "05" -> "May"
    "06" -> "Jun"
    "07" -> "Jul"
    "08" -> "Aug"
    "09" -> "Sep"
    "10" -> "Oct"
    "11" -> "Nov"
    "12" -> "Dec"
    else -> mm
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun BreakdownBarList(
    title: String,
    items: List<Pair<String, Double>>,
    barColor: Color,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = ::formatCurrency
) {
    val maxVal = (items.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (items.isEmpty()) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { (label, amount) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = valueFormatter(amount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily),
                                fontWeight = FontWeight.SemiBold,
                                color = barColor
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            drawRoundRect(
                                color = trackColor,
                                size = size,
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            val w = ((amount / maxVal).toFloat().coerceIn(0.03f, 1f)) * size.width
                            drawRoundRect(
                                color = barColor,
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptPreviewDialog(
    strings: AppStrings,
    payment: PaymentRecord,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.appTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StatusPill(
                    text = payment.receiptNo,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider()
                ReceiptLine(strings.selectFamily, payment.familyName)
                if (!payment.dueTitle.isNullOrBlank()) {
                    ReceiptLine(strings.dueTitle, payment.dueTitle)
                }
                ReceiptLine(strings.dateLabel.substringBefore(" "), payment.paymentDate)
                ReceiptLine(strings.paymentMethod, payment.paymentMethod.uppercase())
                if (!payment.note.isNullOrBlank()) {
                    ReceiptLine(strings.noteOptional.substringBefore(" "), payment.note)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.amount, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatCurrency(payment.amount),
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = message,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
