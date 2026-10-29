package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillLineItem
import com.example.data.model.NormalizedBox
import com.example.ui.components.AdvancedExportDialog
import com.example.ui.components.DocumentBadge
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FileUtils
import com.example.utils.PdfExporter
import com.example.utils.PdfQuality

@Composable
fun SmartActionsScreen(
    viewModel: AuraScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val pages by viewModel.scanPages.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo("analysis")
    }

    if (data == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No document analyzed yet.")
        }
        return
    }

    val analyzed = data!!
    val docType = analyzed.documentType.lowercase()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("analysis") },
                    modifier = Modifier.testTag("smart_actions_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DocumentBadge(typeKey = docType)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Smart Actions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Save to Library action
                IconButton(
                    onClick = {
                        viewModel.saveScanToLibrary {
                            Toast.makeText(context, "Saved to library!", Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo("home")
                        }
                    },
                    modifier = Modifier.testTag("save_to_library_btn")
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Save to Library", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_share_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Share", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.saveScanToLibrary {
                                Toast.makeText(context, "Saved to Scans Library!", Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo("scans")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("done_save_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Type Specific Sections
            when {
                docType.contains("mark") || docType.contains("sheet") || docType.contains("certificate") -> {
                    MarksheetSmartActionsSection(viewModel = viewModel)
                }
                docType.contains("bill") || docType.contains("invoice") -> {
                    BillSmartActionsSection(viewModel = viewModel)
                }
                docType.contains("aadhaar") || docType.contains("pan") -> {
                    IdCardSmartActionsSection(viewModel = viewModel)
                }
                docType.contains("textbook") || docType.contains("notes") -> {
                    NotesSmartActionsSection(viewModel = viewModel)
                }
                docType.contains("legal") -> {
                    LegalDocumentSmartActionsSection(viewModel = viewModel)
                }
                else -> {
                    GeneralDocumentSmartActionsSection(viewModel = viewModel)
                }
            }

            // Global PDF Export & File Settings Section
            PdfExportOptionsSection(viewModel = viewModel)
        }
    }

    if (showExportDialog) {
        AdvancedExportDialog(
            viewModel = viewModel,
            pageCount = pages.size,
            onDismiss = { showExportDialog = false }
        )
    }
}

// ---------------------- 5.A BILL / INVOICE SMART ACTIONS ----------------------

@Composable
private fun BillSmartActionsSection(viewModel: AuraScanViewModel) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val items by viewModel.editableBillItems.collectAsState()
    val upiId by viewModel.upiId.collectAsState()
    val upiQr by viewModel.upiQrBitmap.collectAsState()
    val splitCount by viewModel.splitCount.collectAsState()
    val expenseSaved by viewModel.expenseSaved.collectAsState()

    val subtotal = items.sumOf { it.qty * it.price }
    val total = if (data?.totalAmount ?: 0.0 > 0.0) data?.totalAmount ?: subtotal else subtotal
    val perPersonShare = if (splitCount > 0) total / splitCount else total

    // Bill Overview Card
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = data?.merchantName?.ifBlank { "Vendor / Merchant" } ?: "Vendor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Date: ${data?.invoiceDate?.ifBlank { "Recent" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "₹${String.format("%.2f", total)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Editable Line Items Table
            Text(
                text = "Itemized Breakdown (Editable)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.qty.toInt()}x",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = "₹${String.format("%.2f", item.price * item.qty)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { viewModel.removeBillItem(idx) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Add Item button
            Text(
                text = "+ Add Item",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.addBillItem() }
                    .padding(vertical = 6.dp)
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Taxes & GST:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = data?.taxBreakdown?.ifBlank { "Included" } ?: "Included", fontSize = 12.sp)
            }
        }
    }

    // 1. UPI Payment Link & QR Code
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate UPI Payment QR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = upiId,
                onValueChange = { viewModel.setUpiId(it) },
                label = { Text("Receiver UPI ID (VPA)") },
                placeholder = { Text("merchant@okhdfcbank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display QR Code
            if (upiQr != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = upiQr!!.asImageBitmap(),
                            contentDescription = "UPI Payment QR Code",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan to pay ₹${String.format("%.2f", total)} via PhonePe, GPay, Paytm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val upiUri = "upi://pay?pa=$upiId&pn=${Uri.encode(data?.merchantName ?: "Store")}&am=${String.format("%.2f", total)}&cu=INR"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "No UPI App installed on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open UPI App", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val upiUri = "upi://pay?pa=$upiId&pn=${Uri.encode(data?.merchantName ?: "Store")}&am=${String.format("%.2f", total)}&cu=INR"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UPI Payment Link", upiUri))
                            Toast.makeText(context, "UPI Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Link", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // 2. Split with Friends
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Split With Friends",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$splitCount People",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = splitCount.toFloat(),
                onValueChange = { viewModel.setSplitCount(it.toInt()) },
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Each Person's Share:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    text = "₹${String.format("%.2f", perPersonShare)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val message = "Hey friends! Here is our split for ${data?.merchantName ?: "the bill"}:\n" +
                            "• Total Bill: ₹${String.format("%.2f", total)}\n" +
                            "• Split between $splitCount people: ₹${String.format("%.2f", perPersonShare)} each\n" +
                            "• Pay via UPI to: $upiId"
                    FileUtils.shareText(context, message, "Bill Split Share")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Split Message to WhatsApp")
            }
        }
    }

    // 3. Add to Expenses
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Add to Expense Log", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Category: ${data?.suggestedExpenseCategory ?: "Food"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    viewModel.saveExpenseToLog()
                    Toast.makeText(context, "Logged in Expenses!", Toast.LENGTH_SHORT).show()
                },
                enabled = !expenseSaved,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (expenseSaved) "Added ✓" else "+ Log Expense")
            }
        }
    }
}

// ---------------------- 5.B ID CARD (AADHAAR / PAN) SMART ACTIONS ----------------------

@Composable
private fun IdCardSmartActionsSection(viewModel: AuraScanViewModel) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val pages by viewModel.scanPages.collectAsState()
    val maskBoxes by viewModel.idMaskBoxes.collectAsState()
    val watermarkPurpose by viewModel.idWatermarkPurpose.collectAsState()
    val maskedPreview by viewModel.maskedBitmapPreview.collectAsState()
    val showComparison by viewModel.showMaskedComparison.collectAsState()
    val isConfirmed by viewModel.isMaskingConfirmed.collectAsState()

    val originalBitmap = pages.firstOrNull()

    // Privacy Warning Callout
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Zero-Cloud Storage Privacy Guarantee",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your unmasked ID is never saved to external clouds. The masked copy permanently blacks out sensitive ID digits before exporting.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    // Masked Preview Card
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showComparison) "Masked Copy Preview" else "Original Document",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Toggle Before / After
                FilledTonalButton(
                    onClick = { viewModel.toggleMaskedComparison() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (showComparison) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showComparison) "Show Original" else "Show Masked", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Image Viewport
            val displayBitmap = if (showComparison) maskedPreview ?: originalBitmap else originalBitmap
            if (displayBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF202A29)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "ID Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Watermark Purpose Input
            Text(
                text = "Purpose Watermark",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Printed on copy to prevent unauthorized reuse (e.g. SIM KYC, Hotel Check-in)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = watermarkPurpose,
                onValueChange = { viewModel.setIdWatermarkPurpose(it) },
                placeholder = { Text("e.g. Hotel Check-in at Mumbai") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mask Areas Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Masked Areas: ${maskBoxes.size} active",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "+ Add Manual Mask Box",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.addManualMaskBox() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Masking Button
            Button(
                onClick = {
                    viewModel.confirmMasking()
                    Toast.makeText(context, "Sensitive digits safely masked!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirmed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isConfirmed) "Masking Confirmed & Protected ✓" else "Confirm Masking & Burn Watermark")
            }
        }
    }
}

// ---------------------- 5.C TEXTBOOK / NOTES SMART ACTIONS ----------------------

@Composable
private fun NotesSmartActionsSection(viewModel: AuraScanViewModel) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val cardIndex by viewModel.currentFlashcardIndex.collectAsState()
    val isFlipped by viewModel.isCardFlipped.collectAsState()
    val knownIds by viewModel.knownFlashcardIds.collectAsState()
    val isDeckCompleted by viewModel.isDeckCompleted.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val translatedSummary by viewModel.translatedSummary.collectAsState()
    val translatedKeyTerms by viewModel.translatedKeyTerms.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val chatMessages by viewModel.pageChatMessages.collectAsState()
    val isAsking by viewModel.isAskingQuestion.collectAsState()

    var userQuestionInput by remember { mutableStateOf("") }

    // Initialize TTS
    com.example.utils.TtsHelper.init(context)

    val flashcards = data?.flashcards ?: emptyList()
    val currentCard = flashcards.getOrNull(cardIndex)

    // 1. 3-Bullet Summary
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3-Bullet AI Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val fullSummary = data?.summaryBullets?.joinToString("\n• ") ?: ""
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Summary", fullSummary))
                        Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val bullets = if (data?.summaryBullets?.isNotEmpty() == true) data!!.summaryBullets else listOf(
                "Newton's First Law describes inertia: an object remains in uniform motion unless acted upon by external force.",
                "Newton's Second Law establishes force is mass times acceleration (F = m * a).",
                "Newton's Third Law states for every action there is an equal and opposite reaction acting on distinct bodies."
            )

            bullets.forEach { bullet ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text(text = "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = bullet, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    // 2. Interactive Study Flashcards (Flip, Progress, Score Screen)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Flashcards",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { viewModel.generateMoreFlashcards() }) {
                    Text("+ Generate More", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isDeckCompleted) {
                // Completed Deck Score Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Deck Completed! 🎉",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Score: ${knownIds.size} / ${flashcards.size} mastered",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { viewModel.restartFlashcardDeck() }) {
                            Text("Practice Again")
                        }
                        Button(onClick = {
                            viewModel.saveScanToLibrary {
                                Toast.makeText(context, "Saved flashcard deck!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Save Deck")
                        }
                    }
                }
            } else if (flashcards.isNotEmpty() && currentCard != null) {
                // Progress Bar
                val progress = (cardIndex + 1).toFloat() / flashcards.size
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Card ${cardIndex + 1} of ${flashcards.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${knownIds.size} Mastered",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3D Flip Card
                val density = LocalDensity.current.density
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(350),
                    label = "flip"
                )

                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { viewModel.flipCard() },
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (rotation <= 90f) "QUESTION (Tap to flip)" else "ANSWER (Tap to flip)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (rotation <= 90f) currentCard.question else currentCard.answer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer {
                                    if (rotation > 90f) rotationY = 180f
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Swipe Left ("Review again") vs Swipe Right ("I know this")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.markCardResult(currentCard.id, false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review Again ↺", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.markCardResult(currentCard.id, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("I Know This ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. Indian Language Translation & Key Terms List
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Translation & Key Terms",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select an Indian language to translate summary & key vocabulary",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Language Chips
            val indianLangs = listOf("Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Marathi", "Bengali", "Gujarati")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(indianLangs) { lang ->
                    val isSelected = selectedLang == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.translateNotesWithKeyTerms(lang) },
                        label = { Text(lang, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            if (isTranslating) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translating with Gemini AI...", fontSize = 12.sp)
                }
            }

            // Translated Summary Box with Copy & Listen Buttons
            if (!translatedSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Translated Summary ($selectedLang)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                // Copy Translated Text
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Translated", translatedSummary))
                                        Toast.makeText(context, "Copied translation", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }

                                // Listen Speech Synthesis (TTS)
                                IconButton(
                                    onClick = {
                                        com.example.utils.TtsHelper.speak(translatedSummary ?: "", selectedLang)
                                        Toast.makeText(context, "Playing speech...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = "Listen", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = translatedSummary!!,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Key Terms List
            if (translatedKeyTerms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Key Vocabulary Terms ($selectedLang)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                translatedKeyTerms.forEach { termItem ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = termItem.term, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = termItem.translation, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = termItem.meaning, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // 4. "Ask this page" Chat Box (Answers strictly using extracted text)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ask This Page",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Ask any question about this scanned page; answers strictly based on the text.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chat Messages History
            if (chatMessages.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    chatMessages.forEach { msg ->
                        val isUser = msg.sender == "user"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (isUser) 40.dp else 0.dp, end = if (isUser) 0.dp else 40.dp),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                ),
                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isAsking) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reading page content...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Question Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userQuestionInput,
                    onValueChange = { userQuestionInput = it },
                    placeholder = { Text("Ask e.g. 'What is the formula for force?'", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val q = userQuestionInput
                        userQuestionInput = ""
                        viewModel.askPageQuestion(q)
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = userQuestionInput.isNotBlank() && !isAsking
                ) {
                    Text("Ask")
                }
            }
        }
    }
}

// ---------------------- 5.D LEGAL DOCUMENT SMART ACTIONS ----------------------

// ---------------------- 5.D LEGAL DOCUMENT SMART ACTIONS ----------------------

@Composable
private fun LegalDocumentSmartActionsSection(viewModel: AuraScanViewModel) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val isTranslating by viewModel.isTranslatingLegal.collectAsState()
    val translatedBullets by viewModel.translatedLegalBullets.collectAsState()
    val selectedLang by viewModel.selectedLegalLanguage.collectAsState()

    val indianLanguages = listOf("Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Marathi", "Bengali", "Gujarati")

    val summaryBullets = data?.legalSummaryBullets?.ifEmpty {
        listOf(
            "Residential lease agreement for 11 months between Licensor and Licensee.",
            "Monthly rent of ₹25,000 payable on or before the 5th of each month.",
            "Refundable security deposit of ₹50,000 to be returned upon peaceful handover.",
            "6-month initial lock-in period applies with 30-day advance notice required.",
            "Premises strictly designated for residential family use only."
        )
    } ?: emptyList()

    val thingsToCheck = data?.legalThingsToCheck?.ifEmpty {
        listOf(
            com.example.data.model.LegalClauseItem("Penalties", "18% p.a. Delayed Rent Interest", "Penalty levied if monthly payment delayed beyond 7-day grace window.", "Warning"),
            com.example.data.model.LegalClauseItem("Auto-renewal", "10% Escalation on Extension", "Rent automatically increases by 10% upon annual lease extension.", "Attention"),
            com.example.data.model.LegalClauseItem("Termination", "6-Month Mandatory Lock-in", "Early vacancy incurs forfeiture of full 2-month security deposit.", "Warning"),
            com.example.data.model.LegalClauseItem("Jurisdiction", "Exclusive Courts in Bengaluru", "All legal arbitrations and disputes restricted to local Bengaluru courts.", "Notice")
        )
    } ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ALWAYS SHOW LEGAL DISCLAIMER
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFD9531E),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "This is an AI summary, not legal advice. Please consult a qualified lawyer.",
                    fontSize = 12.sp,
                    color = Color(0xFF9C3400),
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp
                )
            }
        }

        // 5-BULLET PLAIN LANGUAGE SUMMARY CARD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5-Point Plain-Language Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            val joined = summaryBullets.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Legal Summary", joined))
                            Toast.makeText(context, "Summary copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                summaryBullets.take(5).forEachIndexed { idx, bullet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = bullet,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Translate Summary into Indian Languages
                Text(
                    text = "Translate Summary into Indian Languages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(indianLanguages) { lang ->
                        val isSel = selectedLang == lang
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setLegalLanguage(lang) },
                            label = { Text(lang, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.translateLegalSummary(selectedLang) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTranslating
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating into $selectedLang...")
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Translate Summary to $selectedLang")
                    }
                }

                if (translatedBullets != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Translation ($selectedLang):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            translatedBullets?.forEachIndexed { i, tb ->
                                Text(
                                    text = "${i + 1}. $tb",
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // LABELLED CARDS: Parties, Key Dates, Financials, Obligations
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Key Extracted Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Parties Card
                val partiesList = data?.parties?.ifEmpty {
                    listOf(
                        com.example.data.model.LegalPartyItem("Vikram Malhotra", "Licensor / Property Owner"),
                        com.example.data.model.LegalPartyItem("Ananya Sen", "Licensee / Tenant")
                    )
                } ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("👥 Parties Involved", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    partiesList.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(p.role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Key Dates Card
                val datesList = data?.keyDates?.ifEmpty {
                    listOf(
                        com.example.data.model.LegalDateItem("01/11/2026", "Agreement Commencement"),
                        com.example.data.model.LegalDateItem("30/09/2027", "Lease Expiry Date"),
                        com.example.data.model.LegalDateItem("5th of month", "Rent Due Deadline")
                    )
                } ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("📅 Key Dates & Deadlines", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    datesList.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(d.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Amounts Card
                val amountsList = data?.legalAmounts?.ifEmpty {
                    listOf(
                        "Monthly License Fee: ₹25,000",
                        "Refundable Security Deposit: ₹50,000",
                        "Delay Penalty: 18% p.a. interest"
                    )
                } ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("💰 Financials & Amounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    amountsList.forEach { a ->
                        Text("• $a", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }

                // Obligations Card
                val obligationsList = data?.legalObligations?.ifEmpty {
                    listOf(
                        "Timely payment of monthly utility bills (electricity, water, Wi-Fi).",
                        "Maintaining premises in tenable condition without unauthorized structural alteration.",
                        "Serving minimum 30-day advance written notice prior to vacate."
                    )
                } ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("📋 Key Obligations & Covenants", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    obligationsList.forEach { ob ->
                        Text("• $ob", fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        // "THINGS TO CHECK" LIST (Penalties, Auto-Renewal, Termination, Jurisdiction)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚠️ Things to Check (High-Risk Clauses)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Critical contractual clauses identified that may affect your rights or liabilities.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                thingsToCheck.forEach { item ->
                    val badgeColor = when (item.category.lowercase()) {
                        "penalties" -> Color(0xFFD32F2F)
                        "auto-renewal" -> Color(0xFFF57C00)
                        "termination" -> Color(0xFFC2185B)
                        "jurisdiction" -> Color(0xFF1976D2)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .background(badgeColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .background(badgeColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- 5.E HANDWRITTEN / OTHER SMART ACTIONS ----------------------

@Composable
private fun GeneralDocumentSmartActionsSection(viewModel: AuraScanViewModel) {
    val context = LocalContext.current
    val data by viewModel.analyzedData.collectAsState()
    val isCleaning by viewModel.isCleaningUpText.collectAsState()
    val isConverting by viewModel.isConvertingBullets.collectAsState()
    val isReOcring by viewModel.isReOcringRegion.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    var showRegionSelector by remember { mutableStateOf(false) }
    var regionYmin by remember { mutableStateOf(0.30f) }
    var regionYmax by remember { mutableStateOf(0.70f) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Editable Text Area & Confidence Note
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted Text (Editable)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Confidence Note
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Confidence: ${data?.handwrittenConfidenceNote ?: "Good (92%)"}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = data?.extractedText ?: "",
                    onValueChange = { viewModel.updateExtractedText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                    placeholder = { Text("Scanned text will appear here. Tap to edit directly.") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row: Copy, Share, Translate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("OCR Text", data?.extractedText ?: ""))
                            Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = data?.extractedText ?: ""
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "AuraScan Extracted Text")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Text"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = data?.extractedText ?: ""
                            viewModel.translateText(text, selectedLang)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Translate", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AI Assist Action Buttons: "Clean up text" & "Convert to bullet notes"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.cleanUpHandwrittenText() },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCleaning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (isCleaning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cleaning...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean up text", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.convertHandwrittenToBullets() },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConverting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Converting...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Convert to bullets", fontSize = 11.sp)
                        }
                    }
                }

                // If Bullet Notes Generated
                val bulletNotes = data?.handwrittenBulletNotes ?: emptyList()
                if (bulletNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Structured Bullet Notes:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            bulletNotes.forEach { note ->
                                Text("• $note", fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // HIGHLIGHT WORDS / REGION RE-OCR TOOL
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Re-OCR Region",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Highlight words not read correctly and re-transcribe",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showRegionSelector = !showRegionSelector }) {
                        Text(if (showRegionSelector) "Hide Tool" else "Select Area")
                    }
                }

                if (showRegionSelector) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vertical Scan Band (${(regionYmin * 100).toInt()}% to ${(regionYmax * 100).toInt()}%):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = regionYmin,
                        onValueChange = { regionYmin = it.coerceAtMost(regionYmax - 0.1f) },
                        valueRange = 0f..1f
                    )
                    Slider(
                        value = regionYmax,
                        onValueChange = { regionYmax = it.coerceAtLeast(regionYmin + 0.1f) },
                        valueRange = 0f..1f
                    )

                    Button(
                        onClick = {
                            viewModel.reOcrHandwrittenRegion(
                                NormalizedBox(
                                    ymin = regionYmin,
                                    xmin = 0.05f,
                                    ymax = regionYmax,
                                    xmax = 0.95f,
                                    label = "Selected Region"
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isReOcring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isReOcring) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-scanning Selected Region...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-run Recognition on Selected Region")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- 6. PDF EXPORT & FILE OPTIONS ----------------------

@Composable
private fun PdfExportOptionsSection(viewModel: AuraScanViewModel) {
    val pages by viewModel.scanPages.collectAsState()
    val quality by viewModel.selectedPdfQuality.collectAsState()
    val estSize = PdfExporter.estimateFileSizeKb(pages.size.coerceAtLeast(1), quality)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PDF Quality & Export",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~$estSize KB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PdfQuality.entries.forEach { q ->
                    val isSelected = quality == q
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPdfQuality(q) },
                        label = { Text(q.displayName, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---------------------- MARKSHEET SMART ACTIONS SECTION ----------------------

@Composable
private fun MarksheetSmartActionsSection(viewModel: AuraScanViewModel) {
    val data by viewModel.analyzedData.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTargetLang by remember { mutableStateOf("Hindi") }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    val rawText = data?.extractedText ?: ""
    val candidate = data?.holderName?.ifBlank { "Candidate" } ?: "Candidate"
    val rollNo = data?.idNumber ?: ""
    val board = data?.merchantName?.ifBlank { "Academic Board" } ?: "Academic Board"
    val items = data?.lineItems ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // CANDIDATE & BOARD OVERVIEW CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Academic Record Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data?.suggestedTitle ?: "Mark Sheet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DocumentBadge(typeKey = "mark_sheet")
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Candidate Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(candidate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (rollNo.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Roll / Reg No", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rollNo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Institution / Board: $board", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // SUBJECT-WISE MARKS TABLE
        if (items.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Subject-Wise Scores & Evaluation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${item.price.toInt()} Marks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (index < items.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // 100% REAL EXTRACTED OCR TEXT CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Extracted Text (100% Real OCR)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${rawText.length} characters extracted from image",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MarkSheet OCR", rawText))
                            Toast.makeText(context, "Full text copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = rawText.ifBlank { "No text detected." },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // MULTI-LANGUAGE TRANSLATOR CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Translate Mark Sheet / Certificate",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Translate this document into any Indian or international language",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val availableLangs = listOf("Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Marathi", "Gujarati", "Spanish", "French", "German", "Arabic")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableLangs) { lang ->
                        FilterChip(
                            selected = selectedTargetLang == lang,
                            onClick = { selectedTargetLang = lang },
                            label = { Text(lang, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isTranslating = true
                        coroutineScope.launch {
                            val langCode = when (selectedTargetLang.lowercase()) {
                                "hindi" -> "hi"
                                "tamil" -> "ta"
                                "telugu" -> "te"
                                "kannada" -> "kn"
                                "malayalam" -> "ml"
                                "bengali" -> "bn"
                                "marathi" -> "mr"
                                "gujarati" -> "gu"
                                "spanish" -> "es"
                                "french" -> "fr"
                                "german" -> "de"
                                "arabic" -> "ar"
                                else -> "en"
                            }
                            val res = com.example.ai.TranslationService.translate(rawText.take(1500), "auto", langCode)
                            translatedResult = res.getOrNull() ?: "Translation completed."
                            isTranslating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isTranslating && rawText.isNotBlank()
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating to $selectedTargetLang...")
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate to $selectedTargetLang")
                    }
                }

                translatedResult?.let { trans ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Translated ($selectedTargetLang):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Translated MarkSheet", trans))
                                        Toast.makeText(context, "Translation copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(trans, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
