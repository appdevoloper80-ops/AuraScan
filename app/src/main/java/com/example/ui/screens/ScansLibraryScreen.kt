package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.DocumentBadge
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScansLibraryScreen(
    viewModel: AuraScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allDocs by viewModel.allDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val isGrid by viewModel.isGridView.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedDocIds.collectAsState()
    val totalBytes by viewModel.totalStorageBytes.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    BackHandler {
        if (isMultiSelect) {
            viewModel.toggleMultiSelectMode()
        } else {
            viewModel.navigateTo("home")
        }
    }

    // Filter documents
    val filtered = allDocs.filter { doc ->
        val matchesQuery = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.extractedText.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (categoryFilter) {
            "all" -> true
            "aadhaar" -> doc.documentType == "aadhaar"
            "pan" -> doc.documentType == "pan"
            "bills" -> doc.documentType == "bill_invoice"
            "notes" -> doc.documentType == "textbook_notes"
            "legal" -> doc.documentType == "legal_document"
            "other" -> doc.documentType == "other" || doc.documentType == "handwritten_note"
            else -> true
        }
        matchesQuery && matchesCategory
    }

    // Sort documents
    val sortedDocs = when (sortBy) {
        "oldest" -> filtered.sortedBy { it.timestamp }
        "name" -> filtered.sortedBy { it.title.lowercase() }
        else -> filtered.sortedByDescending { it.timestamp }
    }

    val storageFormatted = formatBytes(totalBytes)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Header with Storage Used
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isMultiSelect) "${selectedIds.size} Selected" else "Scans Library",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${allDocs.size} documents • $storageFormatted on device",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multi-select toggle
                        IconButton(
                            onClick = { viewModel.toggleMultiSelectMode() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isMultiSelect) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = "Select Multiple",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Grid vs List Toggle
                        IconButton(
                            onClick = { viewModel.toggleGridView() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sort Menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest First") },
                                    onClick = {
                                        viewModel.setSortBy("newest")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest First") },
                                    onClick = {
                                        viewModel.setSortBy("oldest")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)") },
                                    onClick = {
                                        viewModel.setSortBy("name")
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isMultiSelect && selectedIds.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Delete Selected
                        OutlinedButton(
                            onClick = { viewModel.deleteSelectedDocuments() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete (${selectedIds.size})")
                        }

                        // Combine into 1 PDF
                        Button(
                            onClick = {
                                viewModel.combineMultipleScans(selectedIds.toList()) { file ->
                                    Toast.makeText(context, "Scans combined into single PDF!", Toast.LENGTH_SHORT).show()
                                    FileUtils.shareFile(context, file, "application/pdf", "AuraScan Combined PDF")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Combine PDF (${selectedIds.size})", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                AuraBottomNavBar(
                    currentRoute = "scans",
                    onNavigate = { route -> viewModel.navigateTo(route) }
                )
            }
        },
        floatingActionButton = {
            if (!isMultiSelect) {
                FloatingActionButton(
                    onClick = { viewModel.startNewScan() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("library_new_scan_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Scan New")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search titles or OCR extracted text...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Document Type Filter Chips (All, Aadhaar, PAN, Bills, Notes, Legal, Other)
            val filters = listOf(
                "all" to "All",
                "aadhaar" to "Aadhaar",
                "pan" to "PAN",
                "bills" to "Bills",
                "notes" to "Notes",
                "legal" to "Legal",
                "other" to "Other"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filters) { (key, label) ->
                    val isSelected = categoryFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(key) },
                        label = { Text(label, fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scans Content: Empty State vs Grid vs List
            if (sortedDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (searchQuery.isNotBlank() || categoryFilter != "all") "No matching documents" else "No saved scans yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different keyword" else "Scan your bills, Aadhaar, textbook notes, and legal docs to see them here",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.startNewScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_state_scan_first_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan your first document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isGrid) {
                // Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedDocs, key = { it.id }) { doc ->
                        GridScanCard(
                            document = doc,
                            isSelected = selectedIds.contains(doc.id),
                            isMultiSelect = isMultiSelect,
                            onToggleSelect = { viewModel.toggleDocSelection(doc.id) },
                            onClick = {
                                if (isMultiSelect) {
                                    viewModel.toggleDocSelection(doc.id)
                                } else {
                                    viewModel.openDocDetail(doc)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    viewModel.toggleMultiSelectMode()
                                    viewModel.toggleDocSelection(doc.id)
                                }
                            },
                            onRename = {
                                docToRename = doc
                                renameInputText = doc.title
                            },
                            onDelete = {
                                viewModel.deleteWithUndo(doc) {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted '${doc.title}'",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoLastDelete()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // List View
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedDocs, key = { it.id }) { doc ->
                        ListScanRow(
                            document = doc,
                            isSelected = selectedIds.contains(doc.id),
                            isMultiSelect = isMultiSelect,
                            onToggleSelect = { viewModel.toggleDocSelection(doc.id) },
                            onClick = {
                                if (isMultiSelect) {
                                    viewModel.toggleDocSelection(doc.id)
                                } else {
                                    viewModel.openDocDetail(doc)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    viewModel.toggleMultiSelectMode()
                                    viewModel.toggleDocSelection(doc.id)
                                }
                            },
                            onRename = {
                                docToRename = doc
                                renameInputText = doc.title
                            },
                            onDelete = {
                                viewModel.deleteWithUndo(doc) {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted '${doc.title}'",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoLastDelete()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (docToRename != null) {
        AlertDialog(
            onDismissRequest = { docToRename = null },
            title = { Text("Rename Scan") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Document Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(docToRename!!.id, renameInputText)
                        docToRename = null
                        Toast.makeText(context, "Renamed document", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridScanCard(
    document: DocumentEntity,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val firstImagePath = document.pageImagePaths.split(",").firstOrNull { it.isNotBlank() }
    val pageCount = document.pageImagePaths.split(",").count { it.isNotBlank() }.coerceAtLeast(1)
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(document.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("grid_card_${document.id}")
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!firstImagePath.isNullOrBlank() && File(firstImagePath).exists()) {
                    AsyncImage(
                        model = File(firstImagePath),
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Checkbox for multi-select
                if (isMultiSelect) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }

                // Page count pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$pageCount p",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info Details
            Column(modifier = Modifier.padding(10.dp)) {
                DocumentBadge(typeKey = document.documentType)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListScanRow(
    document: DocumentEntity,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val firstImagePath = document.pageImagePaths.split(",").firstOrNull { it.isNotBlank() }
    val pageCount = document.pageImagePaths.split(",").count { it.isNotBlank() }.coerceAtLeast(1)
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(document.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!firstImagePath.isNullOrBlank() && File(firstImagePath).exists()) {
                    AsyncImage(
                        model = File(firstImagePath),
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DocumentBadge(typeKey = document.documentType)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$pageCount ${if (pageCount == 1) "page" else "pages"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${String.format("%.1f", kb)} KB"
    val mb = kb / 1024.0
    return "${String.format("%.1f", mb)} MB"
}
