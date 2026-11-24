package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MeatDistribution
import com.example.data.model.QurbaniBatch
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatKg
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer
import com.example.ui.theme.SidebarGradientTop
import com.example.util.ReportExporter

@Composable
fun QurbaniAnimalsTab(
    strings: AppStrings,
    canDelete: Boolean,
    yearBatches: List<QurbaniBatch>,
    distributions: List<MeatDistribution>,
    onSelectBatchForDistribution: (QurbaniBatch) -> Unit,
    onDeleteBatch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (yearBatches.isEmpty()) {
            item { EmptyStateCard(Icons.Filled.Pets, strings.qurbaniEmptyAnimalsYear) }
        } else {
            items(yearBatches, key = { it.id }) { batch ->
                val batchDists = distributions.filter { it.batchId == batch.id }
                val deliveredCount = batchDists.count { it.status == "delivered" }
                Card(
                    onClick = { onSelectBatchForDistribution(batch) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("qurbani_animal_card_${batch.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = batch.eventName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusPill(
                                    text = batch.status.uppercase(),
                                    containerColor = if (batch.status == "completed") SemanticSuccessContainer else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (batch.status == "completed") SemanticSuccess else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (canDelete) {
                                    IconButton(
                                        onClick = { onDeleteBatch(batch.id) },
                                        modifier = Modifier.testTag("delete_qurbani_batch_${batch.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = SemanticError)
                                    }
                                }
                            }
                        }
                        if (!batch.animalInfo.isNullOrBlank()) {
                            Text(batch.animalInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${strings.totalMeat}: ${formatKg(batch.totalMeatKg)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily))
                            batch.purchaseCost?.let {
                                Text(formatCurrency(it), style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${strings.perFamilyShare}: ${formatKg(batch.distributionPerFamily)}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppMonoFontFamily), color = MaterialTheme.colorScheme.primary)
                            Text("${strings.deliveredLabel}: $deliveredCount / ${batchDists.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(batch.eventDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun QurbaniDistributionTab(
    strings: AppStrings,
    localizedYear: String,
    canEdit: Boolean,
    canDelete: Boolean,
    yearBatches: List<QurbaniBatch>,
    selectedBatch: QurbaniBatch?,
    allBatchDistributions: List<MeatDistribution>,
    filteredDistributions: List<MeatDistribution>,
    statusFilter: String,
    searchQuery: String,
    onSelectBatch: (String) -> Unit,
    onToggleSort: () -> Unit,
    onClickAddRecipient: () -> Unit,
    onLoadSociety: () -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClickRecalculate: (QurbaniBatch) -> Unit,
    onDeliverMeat: (MeatDistribution) -> Unit,
    onDeleteDistribution: (MeatDistribution) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$localizedYear ${strings.qurbaniDistListTitleSuffix}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleSort,
                        modifier = Modifier.testTag("qurbani_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canEdit) {
                        FilledTonalButton(
                            onClick = onClickAddRecipient,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("qurbani_add_recipient_button")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.qurbaniAddRecipient, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (allBatchDistributions.isEmpty() && (selectedBatch == null || yearBatches.all { b -> allBatchDistributions.isEmpty() })) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = strings.qurbaniEmptyDistSuffix,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "$localizedYear ${strings.qurbaniEmptyDistSuffix}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (canEdit) {
                        Button(
                            onClick = onLoadSociety,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SidebarGradientTop,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("load_society_qurbani_button")
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.qurbaniLoadSociety, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (yearBatches.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(yearBatches, key = { it.id }) { b ->
                                FilterChip(
                                    selected = b.id == selectedBatch?.id,
                                    onClick = { onSelectBatch(b.id) },
                                    label = { Text(b.eventName) }
                                )
                            }
                        }
                    }
                }

                selectedBatch?.let { batch ->
                    val deliveredCount = allBatchDistributions.count { it.status == "delivered" }
                    val totalCount = allBatchDistributions.size.coerceAtLeast(1)
                    val progress = deliveredCount.toFloat() / totalCount.toFloat()

                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(batch.eventName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            "${strings.totalMeat}: ${formatKg(batch.totalMeatKg)} • ${strings.perFamilyShare}: ${formatKg(batch.distributionPerFamily)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    if (canEdit) {
                                        TextButton(
                                            onClick = { onClickRecalculate(batch) },
                                            modifier = Modifier.testTag("qurbani_recalculate_button")
                                        ) {
                                            Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.qurbaniRecalculateShare)
                                        }
                                    }
                                }
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${strings.deliveredLabel}: $deliveredCount / ${allBatchDistributions.size}", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { ReportExporter.exportQurbaniDistributionPdf(context, batch, allBatchDistributions) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                                        }
                                        OutlinedButton(
                                            onClick = { ReportExporter.exportQurbaniDistributionCsv(context, batch, allBatchDistributions) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("CSV", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchFamilies) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("qurbani_search_input")
                    )
                }

                item {
                    val deliveredCount = allBatchDistributions.count { it.status == "delivered" }
                    val pendingCount = allBatchDistributions.count { it.status == "pending" }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = statusFilter == "all", onClick = { onStatusFilterChange("all") }, label = { Text("${strings.allItems} (${allBatchDistributions.size})") }) }
                        item { FilterChip(selected = statusFilter == "pending", onClick = { onStatusFilterChange("pending") }, label = { Text("${strings.pendingLabel} ($pendingCount)") }) }
                        item { FilterChip(selected = statusFilter == "delivered", onClick = { onStatusFilterChange("delivered") }, label = { Text("${strings.deliveredLabel} ($deliveredCount)") }) }
                    }
                }

                items(filteredDistributions, key = { it.id }) { dist ->
                    val isDelivered = dist.status == "delivered"
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusPill(dist.familyCode, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dist.familyName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("${strings.perFamilyShare}: ${formatKg(dist.allocatedKg)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppMonoFontFamily))
                                if (isDelivered && !dist.receivedBy.isNullOrBlank()) {
                                    Text("${strings.receivedBy}: ${dist.receivedBy} (${formatKg(dist.deliveredKg)})", style = MaterialTheme.typography.labelSmall, color = SemanticSuccess)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isDelivered) {
                                    StatusPill(strings.deliveredLabel, SemanticSuccessContainer, SemanticSuccess)
                                } else if (canEdit) {
                                    Button(
                                        onClick = { onDeliverMeat(dist) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("deliver_meat_${dist.familyCode}")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.confirmDelivery, maxLines = 1)
                                    }
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteDistribution(dist) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = SemanticError)
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

@Composable
fun QurbaniSummaryTab(
    strings: AppStrings,
    localizedYear: String,
    yearBatches: List<QurbaniBatch>,
    yearDistributions: List<MeatDistribution>,
    selectedBatch: QurbaniBatch?
) {
    val context = LocalContext.current
    val totalAnimals = yearBatches.size
    val totalCost = yearBatches.sumOf { it.purchaseCost ?: 0.0 }
    val totalMeat = yearBatches.sumOf { it.totalMeatKg }
    val deliveredMeat = yearDistributions.filter { it.status == "delivered" }.sumOf { it.deliveredKg }
    val remainingMeat = (totalMeat - deliveredMeat).coerceAtLeast(0.0)
    val avgPerFamily = if (yearDistributions.isNotEmpty() && totalMeat > 0.0) {
        totalMeat / yearDistributions.size.toDouble()
    } else {
        yearBatches.firstOrNull()?.distributionPerFamily ?: 0.0
    }
    val deliveredCount = yearDistributions.count { it.status == "delivered" }
    val totalDistCount = yearDistributions.size
    val progress = if (totalDistCount > 0) deliveredCount.toFloat() / totalDistCount.toFloat() else 0f

    LazyColumn(
        modifier = Modifier.widthIn(max = 880.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "$localizedYear • ${strings.qurbaniTabSummary}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    title = strings.qurbaniTotalAnimals,
                    value = totalAnimals.toString(),
                    icon = Icons.Filled.Pets,
                    accentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = strings.qurbaniTotalCost,
                    value = formatCurrency(totalCost),
                    icon = Icons.Filled.AttachMoney,
                    accentColor = GoldSecondary,
                    containerColor = GoldSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    title = strings.totalMeat,
                    value = formatKg(totalMeat),
                    icon = Icons.Filled.Restaurant,
                    accentColor = InfoBlue,
                    containerColor = InfoBlueContainer,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = strings.perFamilyShare,
                    value = formatKg(avgPerFamily),
                    icon = Icons.Filled.Balance,
                    accentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    title = strings.qurbaniDistributedMeat,
                    value = formatKg(deliveredMeat),
                    icon = Icons.Filled.CheckCircle,
                    accentColor = SemanticSuccess,
                    containerColor = SemanticSuccessContainer,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = strings.qurbaniRemainingMeat,
                    value = formatKg(remainingMeat),
                    icon = Icons.Filled.Inventory,
                    accentColor = SemanticWarning,
                    containerColor = SemanticWarningContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.deliveredLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("$deliveredCount / $totalDistCount", style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily))
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    if (selectedBatch != null) {
                        val batchDists = yearDistributions.filter { it.batchId == selectedBatch.id }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { ReportExporter.exportQurbaniDistributionPdf(context, selectedBatch, batchDists) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.exportQurbaniPdf)
                            }
                            OutlinedButton(
                                onClick = { ReportExporter.exportQurbaniDistributionCsv(context, selectedBatch, batchDists) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.exportQurbaniCsv)
                            }
                        }
                    }
                }
            }
        }
    }
}
