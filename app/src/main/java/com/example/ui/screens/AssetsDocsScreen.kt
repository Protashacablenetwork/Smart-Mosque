package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SocietyAsset
import com.example.data.model.SocietyDocument
import com.example.ui.components.AssetFormDialog
import com.example.ui.components.DocumentFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticErrorContainer
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessContainer
import com.example.ui.theme.SemanticWarning
import com.example.ui.theme.SemanticWarningContainer

@Composable
fun AssetsDocsScreen(
    strings: AppStrings,
    canWrite: Boolean,
    canDeleteAdmin: Boolean,
    assets: List<SocietyAsset>,
    documents: List<SocietyDocument>,
    initialSubTab: Int = 0,
    onSaveAsset: (id: String?, name: String, category: String, value: Double, purchaseDate: String?, condition: String, location: String?, note: String?) -> Unit,
    onDeleteAsset: (id: String) -> Unit,
    onSaveDocument: (id: String?, title: String, category: String, docDate: String, fileName: String?, note: String?) -> Unit,
    onDeleteDocument: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(initialSubTab) } // 0=Assets, 1=Documents
    LaunchedEffect(initialSubTab) {
        selectedTab = initialSubTab
    }
    var showAssetDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<SocietyAsset?>(null) }
    var showDocDialog by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<SocietyDocument?>(null) }

    val totalValuation = remember(assets) { assets.sumOf { it.value } }
    val needsRepairCount = remember(assets) { assets.count { it.condition != "good" } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canWrite) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) { editingAsset = null; showAssetDialog = true }
                        else { editingDoc = null; showDocDialog = true }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("assets_docs_add_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.save)
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile(strings.totalAssetsCount, assets.size.toString(), Icons.Filled.Inventory2, InfoBlue, InfoBlueContainer, modifier = Modifier.weight(1f))
                            MetricTile(strings.totalValuationLabel, formatCurrency(totalValuation), Icons.Filled.AccountBalance, SemanticSuccess, SemanticSuccessContainer, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile(strings.totalDocumentsLabel, documents.size.toString(), Icons.Filled.Description, PurpleAccent, PurpleContainer, modifier = Modifier.weight(1f))
                            MetricTile(strings.needsRepairLabel, needsRepairCount.toString(), Icons.Filled.Build, SemanticWarning, SemanticWarningContainer, modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("${strings.tabAssets} (${assets.size})") },
                            modifier = Modifier.testTag("subtab_assets")
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("${strings.tabDocuments} (${documents.size})") },
                            modifier = Modifier.testTag("subtab_documents")
                        )
                    }
                }

                if (selectedTab == 0) {
                    if (assets.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.Inventory2, strings.emptyAssets) }
                    } else {
                        items(assets, key = { it.id }) { asset ->
                            val (pillContainer, pillContent) = when (asset.condition) {
                                "good" -> SemanticSuccessContainer to SemanticSuccess
                                "needs_repair" -> SemanticWarningContainer to SemanticWarning
                                else -> SemanticErrorContainer to SemanticError
                            }
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(formatCurrency(asset.value), style = MaterialTheme.typography.titleSmall.copy(fontFamily = AppMonoFontFamily), fontWeight = FontWeight.Bold, color = SemanticSuccess)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        StatusPill(asset.category, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                        StatusPill(asset.condition.replace('_', ' ').uppercase(), pillContainer, pillContent)
                                    }
                                    val meta = listOfNotNull(asset.purchaseDate, asset.location, asset.note).joinToString(" • ")
                                    if (meta.isNotBlank()) {
                                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (canWrite || canDeleteAdmin) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (canWrite) {
                                                IconButton(onClick = { editingAsset = asset; showAssetDialog = true }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = strings.save)
                                                }
                                            }
                                            if (canDeleteAdmin) {
                                                IconButton(onClick = { onDeleteAsset(asset.id) }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (documents.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.Description, strings.emptyDocuments) }
                    } else {
                        items(documents, key = { it.id }) { doc ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusPill(doc.category, PurpleContainer, PurpleAccent)
                                    }
                                    val meta = listOfNotNull(doc.docDate, doc.fileName).joinToString(" • ")
                                    Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (!doc.note.isNullOrBlank()) {
                                        Text(doc.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (canWrite || canDeleteAdmin) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            if (canWrite) {
                                                IconButton(onClick = { editingDoc = doc; showDocDialog = true }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = strings.save)
                                                }
                                            }
                                            if (canDeleteAdmin) {
                                                IconButton(onClick = { onDeleteDocument(doc.id) }) {
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
        }
    }

    if (showAssetDialog) {
        AssetFormDialog(
            strings = strings,
            existing = editingAsset,
            onDismiss = { showAssetDialog = false },
            onSave = { name, cat, valAmt, pDate, cond, loc, note ->
                onSaveAsset(editingAsset?.id, name, cat, valAmt, pDate, cond, loc, note)
                showAssetDialog = false
            }
        )
    }

    if (showDocDialog) {
        DocumentFormDialog(
            strings = strings,
            existing = editingDoc,
            onDismiss = { showDocDialog = false },
            onSave = { title, cat, dDate, fName, note ->
                onSaveDocument(editingDoc?.id, title, cat, dDate, fName, note)
                showDocDialog = false
            }
        )
    }
}
