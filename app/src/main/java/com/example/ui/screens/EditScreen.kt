package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FilterType
import kotlin.math.roundToInt

@Composable
fun EditScreen(
    viewModel: AuraScanViewModel,
    modifier: Modifier = Modifier
) {
    val pages by viewModel.scanPages.collectAsState()
    val selectedIndex by viewModel.selectedPageIndex.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val applyToAll by viewModel.applyToAllPages.collectAsState()

    val cornerTL by viewModel.cornerTL.collectAsState()
    val cornerTR by viewModel.cornerTR.collectAsState()
    val cornerBR by viewModel.cornerBR.collectAsState()
    val cornerBL by viewModel.cornerBL.collectAsState()
    val isDetectingEdges by viewModel.isDetectingEdges.collectAsState()

    var isCropMode by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo("home")
    }

    if (pages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No pages to edit. Tap scan to start.")
        }
        return
    }

    val currentBitmap = pages.getOrNull(selectedIndex) ?: pages.first()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("home") },
                    modifier = Modifier.testTag("edit_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "Edit Page ${selectedIndex + 1} of ${pages.size}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    // Reset Button (returns to original capture)
                    IconButton(
                        onClick = { viewModel.resetCurrentPage() },
                        modifier = Modifier.testTag("reset_page_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Delete Page Button
                    IconButton(
                        onClick = {
                            if (pages.size > 1) {
                                viewModel.removePageAt(selectedIndex)
                            } else {
                                viewModel.navigateTo("home")
                            }
                        },
                        modifier = Modifier.testTag("delete_page_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        bottomBar = {
            // One-Handed Bottom Controls Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Multi-page Thumbnail Strip (reorder / select / delete)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    itemsIndexed(pages) { idx, pageBmp ->
                        val isSelected = idx == selectedIndex
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectPage(idx) }
                        ) {
                            Image(
                                bitmap = pageBmp.asImageBitmap(),
                                contentDescription = "Page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            // Page number badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topStart = 4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Add more pages button
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { viewModel.navigateTo("camera") }
                                .testTag("add_more_pages_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }

                // Filters Row + "Apply to all pages" toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Apply to all",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = applyToAll,
                            onCheckedChange = { viewModel.toggleApplyToAllPages(it) },
                            modifier = Modifier.size(width = 36.dp, height = 24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(FilterType.entries.size) { idx ->
                        val filter = FilterType.entries[idx]
                        val isSelected = activeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }

                // Bottom Action Bar: Rotate Left, Rotate Right, Clean Up, Perspective Crop, Auto-detect
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate Left
                    FilledTonalButton(
                        onClick = { viewModel.rotateLeftCurrentPage() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(width = 46.dp, height = 40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left", modifier = Modifier.size(18.dp))
                    }

                    // Rotate Right
                    FilledTonalButton(
                        onClick = { viewModel.rotateRightCurrentPage() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(width = 46.dp, height = 40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right", modifier = Modifier.size(18.dp))
                    }

                    // Clean Up
                    FilledTonalButton(
                        onClick = { viewModel.cleanUpCurrentPage() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("clean_up_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clean Up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Auto-Detect Edges with Gemini
                    FilledTonalButton(
                        onClick = {
                            isCropMode = true
                            viewModel.autoDetectEdges()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("auto_detect_edges_btn")
                    ) {
                        if (isDetectingEdges) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Edges", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Perspective Crop Toggle / Apply
                    Button(
                        onClick = {
                            if (isCropMode) {
                                viewModel.applyPerspectiveCrop()
                                isCropMode = false
                            } else {
                                isCropMode = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCropMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("crop_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isCropMode) Icons.Default.Check else Icons.Default.Crop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isCropMode) "Apply" else "Crop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Next Action: Proceed to Gemini Analysis
                Button(
                    onClick = { viewModel.analyzeCurrentScan() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_with_ai_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue to AI Analysis",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            // Main Document Image Viewport with interactive 4-corner perspective handles
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color(0xFF192221), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current.density
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                // Display Image
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Current Scanned Document",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )

                // 4-Corner Draggable Quad Overlay when in Crop Mode
                if (isCropMode) {
                    val p0 = Offset(cornerTL.x * boxWidthPx, cornerTL.y * boxHeightPx)
                    val p1 = Offset(cornerTR.x * boxWidthPx, cornerTR.y * boxHeightPx)
                    val p2 = Offset(cornerBR.x * boxWidthPx, cornerBR.y * boxHeightPx)
                    val p3 = Offset(cornerBL.x * boxWidthPx, cornerBL.y * boxHeightPx)

                    // Draw perspective quad outline
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawLine(Color.Cyan, p0, p1, strokeWidth = 3f)
                                drawLine(Color.Cyan, p1, p2, strokeWidth = 3f)
                                drawLine(Color.Cyan, p2, p3, strokeWidth = 3f)
                                drawLine(Color.Cyan, p3, p0, strokeWidth = 3f)
                            }
                    ) {
                        // Top-Left Corner Handle
                        CornerHandle(
                            position = p0,
                            label = "TL",
                            onDrag = { dx, dy ->
                                viewModel.updateCornerTL(
                                    cornerTL.x + (dx / boxWidthPx),
                                    cornerTL.y + (dy / boxHeightPx)
                                )
                            }
                        )

                        // Top-Right Corner Handle
                        CornerHandle(
                            position = p1,
                            label = "TR",
                            onDrag = { dx, dy ->
                                viewModel.updateCornerTR(
                                    cornerTR.x + (dx / boxWidthPx),
                                    cornerTR.y + (dy / boxHeightPx)
                                )
                            }
                        )

                        // Bottom-Right Corner Handle
                        CornerHandle(
                            position = p2,
                            label = "BR",
                            onDrag = { dx, dy ->
                                viewModel.updateCornerBR(
                                    cornerBR.x + (dx / boxWidthPx),
                                    cornerBR.y + (dy / boxHeightPx)
                                )
                            }
                        )

                        // Bottom-Left Corner Handle
                        CornerHandle(
                            position = p3,
                            label = "BL",
                            onDrag = { dx, dy ->
                                viewModel.updateCornerBL(
                                    cornerBL.x + (dx / boxWidthPx),
                                    cornerBL.y + (dy / boxHeightPx)
                                )
                            }
                        )
                    }

                    // Hint Banner during Crop Mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Drag corners to fit document, then tap Apply",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Crop",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { isCropMode = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerHandle(
    position: Offset,
    label: String,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val handleSize = 34.dp
    val handleSizePx = with(LocalDensity.current) { handleSize.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - handleSizePx / 2f).roundToInt(),
                    (position.y - handleSizePx / 2f).roundToInt()
                )
            }
            .size(handleSize)
            .background(Color.Cyan, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.Black, CircleShape)
        )
    }
}
