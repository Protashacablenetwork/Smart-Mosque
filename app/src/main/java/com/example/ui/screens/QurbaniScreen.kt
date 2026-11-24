package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.example.data.model.QurbaniBatch
import com.example.ui.components.AddQurbaniRecipientDialog
import com.example.ui.components.MeatDeliveryDialog
import com.example.ui.components.QurbaniBatchDialog
import com.example.ui.components.RecalculateMeatDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SidebarGradientTop
import java.util.Calendar

private val QurbaniGoldAccent = Color(0xFFF6C324)
private val QurbaniGoldInk = Color(0xFF122019)

private fun toBanglaDigits(input: String): String {
    val bangla = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return input.map { ch -> if (ch in '0'..'9') bangla[ch - '0'] else ch }.joinToString("")
}

@Composable
fun QurbaniScreen(
    strings: AppStrings,
    canEdit: Boolean,
    canDelete: Boolean,
    families: List<Family>,
    batches: List<QurbaniBatch>,
    distributions: List<MeatDistribution>,
    onCreateBatch: (eventName: String, animalInfo: String?, purchaseCost: Double?, totalMeatKg: Double, eventDate: String, activeFamilies: List<Family>, autoDistribute: Boolean) -> Unit,
    onLoadSocietyFamilies: (year: String, activeFamilies: List<Family>, existingYearBatches: List<QurbaniBatch>, existingDistributions: List<MeatDistribution>) -> Unit,
    onAddRecipient: (year: String, targetBatch: QurbaniBatch?, family: Family, allocatedKg: Double) -> Unit,
    onRecalculateShare: (batch: QurbaniBatch, batchDistributions: List<MeatDistribution>, totalMeatKgOverride: Double?) -> Unit,
    onDeleteBatch: (batchId: String) -> Unit,
    onDeleteDistribution: (MeatDistribution) -> Unit,
    onDeliverMeat: (dist: MeatDistribution, deliveredKg: Double, receivedBy: String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeFamilies = remember(families) { families.filter { it.active } }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val availableYears = remember(batches, currentYear) {
        val fromBatches = batches.mapNotNull { it.eventDate.take(4).takeIf { y -> y.length == 4 && y.all(Char::isDigit) } }
        val defaults = listOf((currentYear + 1).toString(), currentYear.toString(), (currentYear - 1).toString(), (currentYear - 2).toString())
        (fromBatches + defaults).distinct().sortedDescending()
    }
    var selectedYear by rememberSaveable { mutableStateOf(currentYear.toString()) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    // Default to Tab 1 ("মাংস বণ্টন" / Distribution) matching the reference screenshot
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }

    val yearBatches = remember(batches, selectedYear) {
        batches.filter { it.eventDate.startsWith(selectedYear) }
    }
    var selectedBatchId by remember(yearBatches) { mutableStateOf(yearBatches.firstOrNull()?.id) }
    val selectedBatch = yearBatches.firstOrNull { it.id == selectedBatchId } ?: yearBatches.firstOrNull()

    val yearBatchIds = remember(yearBatches) { yearBatches.map { it.id }.toSet() }
    val yearDistributions = remember(distributions, yearBatchIds) {
        distributions.filter { it.batchId in yearBatchIds }
    }

    var sortMode by remember { mutableStateOf("code_asc") }
    var statusFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    var showCreateBatchDialog by remember { mutableStateOf(false) }
    var showAddRecipientDialog by remember { mutableStateOf(false) }
    var recalculatingBatch by remember { mutableStateOf<QurbaniBatch?>(null) }
    var deliveringItem by remember { mutableStateOf<MeatDistribution?>(null) }

    val allSelectedBatchDists = remember(distributions, selectedBatch) {
        if (selectedBatch == null) emptyList() else distributions.filter { it.batchId == selectedBatch.id }
    }

    val filteredDistributions = remember(allSelectedBatchDists, statusFilter, searchQuery, sortMode) {
        val statusFiltered = when (statusFilter) {
            "pending" -> allSelectedBatchDists.filter { it.status == "pending" }
            "delivered" -> allSelectedBatchDists.filter { it.status == "delivered" }
            else -> allSelectedBatchDists
        }
        val q = searchQuery.trim()
        val searched = if (q.isEmpty()) statusFiltered
        else statusFiltered.filter {
            it.familyName.contains(q, ignoreCase = true) ||
                it.familyCode.contains(q, ignoreCase = true) ||
                (it.receivedBy?.contains(q, ignoreCase = true) == true)
        }
        when (sortMode) {
            "code_desc" -> searched.sortedByDescending { it.familyCode }
            "pending_first" -> searched.sortedWith(compareBy<MeatDistribution> { it.status != "pending" }.thenBy { it.familyCode })
            else -> searched.sortedBy { it.familyCode }
        }
    }

    val isBangla = strings.qurbaniTabDistribution == "মাংস বণ্টন"
    val localizedYear = if (isBangla) toBanglaDigits(selectedYear) else selectedYear

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canEdit && (selectedTab == 0 || selectedTab == 1)) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateBatchDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(strings.qurbaniNewAnimalFab, fontWeight = FontWeight.Bold) },
                    containerColor = QurbaniGoldAccent,
                    contentColor = QurbaniGoldInk,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_qurbani_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Emerald Top Header + Year Picker + 3-Tab Segmented Capsule (matching screenshot)
            Surface(
                color = SidebarGradientTop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            if (onNavigateBack != null) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag("qurbani_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                            Text(
                                text = strings.qurbaniTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { yearDropdownExpanded = true }
                                    .testTag("qurbani_year_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = strings.yearLabel,
                                        tint = QurbaniGoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = localizedYear,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = yearDropdownExpanded,
                                onDismissRequest = { yearDropdownExpanded = false }
                            ) {
                                availableYears.forEach { yr ->
                                    val yrLabel = if (isBangla) toBanglaDigits(yr) else yr
                                    DropdownMenuItem(
                                        text = { Text(yrLabel, fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedYear = yr
                                            yearDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Segmented Capsule
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QurbaniSegmentedTab(
                                label = strings.qurbaniTabAnimals,
                                icon = Icons.Filled.Pets,
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                testTag = "qurbani_tab_animals",
                                modifier = Modifier.weight(1f)
                            )
                            QurbaniSegmentedTab(
                                label = strings.qurbaniTabDistribution,
                                icon = Icons.Filled.VolunteerActivism,
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                testTag = "qurbani_tab_distribution",
                                modifier = Modifier.weight(1f)
                            )
                            QurbaniSegmentedTab(
                                label = strings.qurbaniTabSummary,
                                icon = Icons.Filled.BarChart,
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                testTag = "qurbani_tab_summary",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedTab) {
                    0 -> QurbaniAnimalsTab(
                        strings = strings,
                        canDelete = canDelete,
                        yearBatches = yearBatches,
                        distributions = distributions,
                        onSelectBatchForDistribution = { b ->
                            selectedBatchId = b.id
                            selectedTab = 1
                        },
                        onDeleteBatch = onDeleteBatch
                    )
                    1 -> QurbaniDistributionTab(
                        strings = strings,
                        localizedYear = localizedYear,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        yearBatches = yearBatches,
                        selectedBatch = selectedBatch,
                        allBatchDistributions = allSelectedBatchDists,
                        filteredDistributions = filteredDistributions,
                        statusFilter = statusFilter,
                        searchQuery = searchQuery,
                        onSelectBatch = { selectedBatchId = it },
                        onToggleSort = {
                            sortMode = when (sortMode) {
                                "code_asc" -> "code_desc"
                                "code_desc" -> "pending_first"
                                else -> "code_asc"
                            }
                        },
                        onClickAddRecipient = { showAddRecipientDialog = true },
                        onLoadSociety = {
                            onLoadSocietyFamilies(selectedYear, activeFamilies, yearBatches, distributions)
                        },
                        onStatusFilterChange = { statusFilter = it },
                        onSearchQueryChange = { searchQuery = it },
                        onClickRecalculate = { recalculatingBatch = it },
                        onDeliverMeat = { deliveringItem = it },
                        onDeleteDistribution = onDeleteDistribution
                    )
                    else -> QurbaniSummaryTab(
                        strings = strings,
                        localizedYear = localizedYear,
                        yearBatches = yearBatches,
                        yearDistributions = yearDistributions,
                        selectedBatch = selectedBatch
                    )
                }
            }
        }
    }

    if (showCreateBatchDialog) {
        QurbaniBatchDialog(
            strings = strings,
            selectedYear = selectedYear,
            activeFamilyCount = activeFamilies.size,
            onDismiss = { showCreateBatchDialog = false },
            onCreate = { name, animalInfo, purchaseCost, totalKg, date, autoDist ->
                onCreateBatch(name, animalInfo, purchaseCost, totalKg, date, activeFamilies, autoDist)
                showCreateBatchDialog = false
            }
        )
    }

    if (showAddRecipientDialog) {
        val includedIds = remember(allSelectedBatchDists) { allSelectedBatchDists.map { it.familyId }.toSet() }
        AddQurbaniRecipientDialog(
            strings = strings,
            families = activeFamilies.ifEmpty { families },
            alreadyIncludedFamilyIds = includedIds,
            defaultAllocatedKg = selectedBatch?.distributionPerFamily ?: 0.0,
            onDismiss = { showAddRecipientDialog = false },
            onAdd = { fam, kg ->
                onAddRecipient(selectedYear, selectedBatch, fam, kg)
                showAddRecipientDialog = false
            }
        )
    }

    recalculatingBatch?.let { batch ->
        val distsForBatch = distributions.filter { it.batchId == batch.id }
        RecalculateMeatDialog(
            strings = strings,
            batch = batch,
            recipientCount = distsForBatch.size,
            onDismiss = { recalculatingBatch = null },
            onConfirm = { newTotalKg ->
                onRecalculateShare(batch, distsForBatch, newTotalKg)
                recalculatingBatch = null
            }
        )
    }

    deliveringItem?.let { dist ->
        MeatDeliveryDialog(
            strings = strings,
            distribution = dist,
            onDismiss = { deliveringItem = null },
            onConfirm = { kg, receiver ->
                onDeliverMeat(dist, kg, receiver)
                deliveringItem = null
            }
        )
    }
}

@Composable
private fun QurbaniSegmentedTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) QurbaniGoldAccent else Color.Transparent
    val fgColor = if (selected) QurbaniGoldInk else Color.White.copy(alpha = 0.92f)
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fgColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
