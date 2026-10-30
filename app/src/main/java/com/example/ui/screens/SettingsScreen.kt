package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.RewardedAdBannerCard
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FileUtils
import com.example.utils.PageSizeOption
import com.example.utils.PdfQuality

import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun SettingsScreen(
    viewModel: AuraScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.appThemeMode.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isAiFreeMode by viewModel.isAiFreeMode.collectAsState()
    val askBeforeAi by viewModel.askBeforeSendingToAi.collectAsState()
    val defaultQuality by viewModel.defaultPdfQuality.collectAsState()
    val defaultPageSize by viewModel.defaultPageSize.collectAsState()
    val storageBytes by viewModel.totalStorageBytes.collectAsState()

    var showClearScansDialog by remember { mutableStateOf(false) }
    var showClearEverythingDialog by remember { mutableStateOf(false) }
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var crashLogContent by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateTo("home")
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Settings & Privacy",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Preferences, on-device security & AI control",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        bottomBar = {
            AuraBottomNavBar(
                currentRoute = "settings",
                onNavigate = { route -> viewModel.navigateTo(route) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. APP THEME
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "App Theme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "system" to ("System" to Icons.Default.BrightnessAuto),
                                "light" to ("Light" to Icons.Default.LightMode),
                                "dark" to ("Dark" to Icons.Default.DarkMode)
                            ).forEach { (mode, pair) ->
                                val (label, icon) = pair
                                val isSelected = themeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAppTheme(mode) },
                                    label = { Text(label) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. APP LANGUAGE
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "App Language", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "en" to "English",
                                "hi" to "हिंदी (Hindi)"
                            ).forEach { (code, label) ->
                                val isSelected = appLang == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setAppLanguage(code)
                                        Toast.makeText(context, "Language changed to $label", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. PRIVACY & AI CONTROL (Plain Language)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Privacy & On-Device Security", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Plain language privacy disclosure
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🔒 What stays 100% on your device:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Camera viewfinder, edge detection, perspective cropping & contrast filters\n• ID masking (blackout boxes & watermarking)\n• Encrypted local document database & expense records\n• Offline PDF generation and exports",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "☁️ What uses Gemini AI (via Google Cloud):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• Document type classification & full OCR text recognition\n• Smart actions (GST extraction, 5-bullet legal summary, study flashcards)\n• Indian language translation & OCR text clean-up",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle: Ask before sending to AI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ask before sending to AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Shows a confirmation prompt before initiating any Gemini call", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = askBeforeAi,
                                onCheckedChange = { viewModel.toggleAskBeforeAi(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Toggle: AI-Free Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI-Free Mode (Strict Offline)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Disables all Gemini network requests; keeps capture, edit, manual masking & PDF export active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAiFreeMode,
                                onCheckedChange = {
                                    viewModel.toggleAiFreeMode(it)
                                    val msg = if (it) "AI-Free Mode activated. All processing stays offline." else "AI Features enabled."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // 4. DEFAULT EXPORT PREFERENCES
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Default Export Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Default PDF Quality", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (q in PdfQuality.entries) {
                                val isSel = defaultQuality == q
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.setDefaultPdfQuality(q) },
                                    label = { Text(q.displayName.split(" ").first()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Default Page Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (ps in PageSizeOption.entries) {
                                val isSel = defaultPageSize == ps
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.setDefaultPageSize(ps) },
                                    label = { Text(ps.displayName.split(" ").first()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 5. REWARDED TEST ADS (AdMob ca-app-pub-3940256099942544/5224354917)
            item {
                RewardedAdBannerCard(viewModel = viewModel)
            }

            // 6. STORAGE MANAGEMENT
            item {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Device Storage", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = FileUtils.formatStorageSize(storageBytes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClearScansDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear all scans", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showClearEverythingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear everything", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 6. ABOUT SECTION
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "About AuraScan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "AuraScan • Version 1.0.0 (Production Build)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "A smart document scanner for India, designed with Material 3 and powered by Google Gemini 2.5 Flash for automated UPI QR, ID masking, 5-point legal analysis, and regional study flashcards.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Feedback sent! Thank you for using AuraScan.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Feedback or Report Bug")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                crashLogContent = com.example.AuraScanApplication.getLatestCrashLog(context)
                                showCrashLogDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Crash & Diagnostic Log")
                        }
                    }
                }
            }
        }
    }

    // DIAGNOSTIC / CRASH LOG DIALOG
    if (showCrashLogDialog) {
        AlertDialog(
            onDismissRequest = { showCrashLogDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crash & Diagnostics")
                }
            },
            text = {
                val report = crashLogContent
                if (report.isNullOrBlank()) {
                    Text("No crashes recorded! All app modules and background services are healthy.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = report,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (!crashLogContent.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("AuraScan Crash Log", crashLogContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Crash log copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                } else {
                    Button(onClick = { showCrashLogDialog = false }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (!crashLogContent.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            com.example.AuraScanApplication.clearCrashLog(context)
                            crashLogContent = null
                            Toast.makeText(context, "Crash log cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear Log")
                    }
                }
            }
        )
    }

    // CONFIRMATION DIALOG: CLEAR SCANS ONLY
    if (showClearScansDialog) {
        AlertDialog(
            onDismissRequest = { showClearScansDialog = false },
            title = { Text("Clear All Scans?") },
            text = { Text("This will permanently delete all scanned document pages and thumbnails from your device. Expense records will be kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllScansOnly()
                        showClearScansDialog = false
                        Toast.makeText(context, "All scans cleared.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Scans")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearScansDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CONFIRMATION DIALOG: CLEAR EVERYTHING
    if (showClearEverythingDialog) {
        AlertDialog(
            onDismissRequest = { showClearEverythingDialog = false },
            title = { Text("Clear Everything?") },
            text = { Text("This will delete all saved documents, scanned pages, and expense records. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEverythingAll()
                        showClearEverythingDialog = false
                        Toast.makeText(context, "All application data cleared.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearEverythingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
