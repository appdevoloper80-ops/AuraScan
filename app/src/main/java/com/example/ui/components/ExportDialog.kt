package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.ExportFormat
import com.example.utils.FileUtils
import com.example.utils.PageSizeOption
import com.example.utils.PdfExporter
import com.example.utils.PdfQuality
import java.io.File

@Composable
fun AdvancedExportDialog(
    viewModel: AuraScanViewModel,
    pageCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedQuality by remember { mutableStateOf(PdfQuality.MEDIUM) }
    var selectedPageSize by remember { mutableStateOf(PageSizeOption.A4) }

    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()

    val estSize = PdfExporter.estimateSizeFormatted(pageCount.coerceAtLeast(1), selectedQuality)

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export & Share",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$pageCount page(s) • Est. $estSize",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // 1. FORMAT SELECTOR
                Text(
                    text = "Export Format",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (fmt in ExportFormat.entries) {
                        val isSelected = selectedFormat == fmt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFormat = fmt }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (fmt) {
                                        ExportFormat.PDF -> Icons.Default.PictureAsPdf
                                        ExportFormat.SEARCHABLE_PDF -> Icons.Default.Search
                                        ExportFormat.JPG_IMAGES -> Icons.Default.Image
                                        ExportFormat.PLAIN_TEXT -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fmt.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (fmt == ExportFormat.SEARCHABLE_PDF) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("OCR Index", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (selectedFormat == ExportFormat.PDF || selectedFormat == ExportFormat.SEARCHABLE_PDF) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. QUALITY SELECTOR
                    Text(
                        text = "Quality & Compression",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (q in PdfQuality.entries) {
                            val isSel = selectedQuality == q
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedQuality = q },
                                label = {
                                    Text(
                                        text = when (q) {
                                            PdfQuality.LOW -> "Low (<1MB)"
                                            PdfQuality.MEDIUM -> "Med (Std)"
                                            PdfQuality.HIGH -> "High (HQ)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. PAGE SIZE SELECTOR
                    Text(
                        text = "Page Size",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (ps in PageSizeOption.entries) {
                            val isSel = selectedPageSize == ps
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedPageSize = ps },
                                label = { Text(ps.displayName.split(" ").first(), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator for Large Exports
                if (isExporting) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Exporting document (${(exportProgress * 100).toInt()}%)...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // SHARE BUTTON
                Button(
                    onClick = {
                        viewModel.exportDocumentWithOptions(
                            format = selectedFormat,
                            quality = selectedQuality,
                            pageSize = selectedPageSize
                        ) { file ->
                            Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                            FileUtils.shareFile(
                                context = context,
                                file = file,
                                mimeType = selectedFormat.mimeType,
                                title = "Share AuraScan ${selectedFormat.displayName}"
                            )
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export & Share (${selectedFormat.displayName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
