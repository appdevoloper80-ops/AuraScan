package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.TranslationService
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.RewardedAdBannerCard
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FileUtils
import com.example.utils.IndianCurrencyFormatter
import com.example.utils.TtsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ToolsScreen(
    viewModel: AuraScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedToolTab by remember { mutableStateOf("all") } // all, translate, handwriting, marksheet, expense, rewards

    // Translation State
    var translateInputText by remember { mutableStateOf("") }
    var translateSourceLang by remember { mutableStateOf(TranslationService.SUPPORTED_LANGUAGES[0]) } // Auto
    var translateTargetLang by remember { mutableStateOf(TranslationService.SUPPORTED_LANGUAGES[2]) } // Hindi
    var translatedOutputText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    // Handwriting State
    var handwritingText by remember { mutableStateOf("") }
    var handwritingBullets by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFormattingBullets by remember { mutableStateOf(false) }

    // Expense State
    val allExpenses by viewModel.allExpenses.collectAsState()
    val selectedCal by viewModel.selectedExpenseMonth.collectAsState()
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val monthExpenses = remember(allExpenses, selectedCal) {
        val targetYear = selectedCal.get(Calendar.YEAR)
        val targetMonth = selectedCal.get(Calendar.MONTH)
        allExpenses.filter { expense ->
            val expCal = Calendar.getInstance().apply { timeInMillis = expense.date }
            expCal.get(Calendar.YEAR) == targetYear && expCal.get(Calendar.MONTH) == targetMonth
        }
    }
    val totalSpent = monthExpenses.sumOf { it.amount }
    val totalGst = monthExpenses.sumOf { it.gstAmount }
    val categoryTotals = remember(monthExpenses) {
        val map = mutableMapOf<String, Double>()
        for (exp in monthExpenses) {
            val cat = exp.category.ifBlank { "Other" }
            map[cat] = (map[cat] ?: 0.0) + exp.amount
        }
        map
    }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val monthName = monthFormat.format(selectedCal.time)

    // Prepopulate text from last scan if available
    val lastScannedData by viewModel.analyzedData.collectAsState()

    BackHandler {
        if (selectedToolTab != "all") {
            selectedToolTab = "all"
        } else {
            viewModel.navigateTo("home")
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Tools & Utilities",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% OCR, Multi-language Translator & Mark Sheet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedToolTab == "expense") {
                        IconButton(
                            onClick = {
                                viewModel.exportExpensesToCsv { file ->
                                    FileUtils.shareFile(context, file, "text/csv", "AuraScan Expenses CSV")
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TOOL CATEGORY FILTER TABS
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        "all" to "All Tools",
                        "translate" to "Translator",
                        "handwriting" to "Handwriting to Text",
                        "marksheet" to "10th Mark Sheet",
                        "expense" to "Expense Tracker",
                        "rewards" to "Rewards"
                    )
                    items(tabs) { (key, label) ->
                        FilterChip(
                            selected = selectedToolTab == key,
                            onClick = { selectedToolTab = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedToolTab == key) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            AuraBottomNavBar(
                currentRoute = "tools",
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
            // ------------------ REWARDS CARD (Always visible or on tab) ------------------
            if (selectedToolTab == "all" || selectedToolTab == "rewards") {
                item {
                    RewardedAdBannerCard(viewModel = viewModel)
                }
            }

            // ------------------ TAB: ALL TOOLS OVERVIEW ------------------
            if (selectedToolTab == "all") {
                item {
                    Text(
                        text = "Document Services",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Grid of Main Tools
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. Universal Translator Tool Card
                        ToolFeatureCard(
                            title = "Universal Language Translator",
                            subtitle = "Translate text or full documents from any language to any language",
                            icon = Icons.Default.Translate,
                            badgeColor = Color(0xFF1976D2),
                            onClick = { selectedToolTab = "translate" }
                        )

                        // 2. Handwriting to Text Card
                        ToolFeatureCard(
                            title = "Handwritten Notes to Digital Text",
                            subtitle = "Convert handwritten notebook pages, assignments, and memos into clean editable text",
                            icon = Icons.Default.Draw,
                            badgeColor = Color(0xFF7B1FA2),
                            onClick = { selectedToolTab = "handwriting" }
                        )

                        // 3. Mark Sheet & Certificate Digitizer Card
                        ToolFeatureCard(
                            title = "10th Mark Sheet & Certificate Scanner",
                            subtitle = "Digitize 10th/12th mark sheets, extract student name, roll number, and subject-wise score table",
                            icon = Icons.Default.School,
                            badgeColor = Color(0xFF00796B),
                            onClick = { selectedToolTab = "marksheet" }
                        )

                        // 4. Expense & GST Tracker Card
                        ToolFeatureCard(
                            title = "Automated Expense & GST Log",
                            subtitle = "Auto-track spending, calculate monthly GST breakdown, and export CSV reports",
                            icon = Icons.Default.ReceiptLong,
                            badgeColor = Color(0xFFE65100),
                            onClick = { selectedToolTab = "expense" }
                        )
                    }
                }
            }

            // ------------------ TAB: UNIVERSAL TRANSLATOR ------------------
            if (selectedToolTab == "translate") {
                item {
                    UniversalTranslatorPanel(
                        inputText = translateInputText,
                        onInputTextChanged = { translateInputText = it },
                        sourceLang = translateSourceLang,
                        onSourceLangChanged = { translateSourceLang = it },
                        targetLang = translateTargetLang,
                        onTargetLangChanged = { translateTargetLang = it },
                        outputText = translatedOutputText,
                        isTranslating = isTranslating,
                        onTranslate = {
                            if (translateInputText.isNotBlank()) {
                                isTranslating = true
                                coroutineScope.launch {
                                    val res = TranslationService.translate(
                                        translateInputText,
                                        translateSourceLang.code,
                                        translateTargetLang.code
                                    )
                                    translatedOutputText = res.getOrNull() ?: "Translation completed."
                                    isTranslating = false
                                }
                            }
                        },
                        onLoadFromLastScan = {
                            val scanText = lastScannedData?.extractedText ?: ""
                            if (scanText.isNotBlank()) {
                                translateInputText = scanText
                                Toast.makeText(context, "Loaded text from current scan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No scanned text found yet. Scan a document first!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // ------------------ TAB: HANDWRITING TO TEXT ------------------
            if (selectedToolTab == "handwriting") {
                item {
                    HandwritingToTextPanel(
                        viewModel = viewModel,
                        handwritingText = handwritingText,
                        onTextChanged = { handwritingText = it },
                        bullets = handwritingBullets,
                        isFormatting = isFormattingBullets,
                        onFormatBullets = {
                            isFormattingBullets = true
                            val lines = handwritingText.split("\n").filter { it.isNotBlank() }
                            handwritingBullets = lines.map { "• ${it.trim()}" }
                            isFormattingBullets = false
                        },
                        onLoadFromLastScan = {
                            val text = lastScannedData?.extractedText ?: ""
                            if (text.isNotBlank()) {
                                handwritingText = text
                                Toast.makeText(context, "Loaded text from current scan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No scan available. Tap 'Capture New Document' below!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // ------------------ TAB: 10TH MARK SHEET & ACADEMIC ------------------
            if (selectedToolTab == "marksheet") {
                item {
                    MarksheetScannerPanel(
                        viewModel = viewModel,
                        lastScannedData = lastScannedData
                    )
                }
            }

            // ------------------ TAB: EXPENSE TRACKER ------------------
            if (selectedToolTab == "expense") {
                // Month Selector
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousExpenseMonth() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = monthName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(onClick = { viewModel.nextExpenseMonth() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }
                    }
                }

                // Total Spent & Total GST
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total Spent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = IndianCurrencyFormatter.formatRupees(totalSpent),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${monthExpenses.size} bills logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total GST Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = IndianCurrencyFormatter.formatRupees(totalGst),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Auto-extracted tax", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                if (monthExpenses.isNotEmpty() && totalSpent > 0) {
                    item {
                        CategoryDonutChartCard(
                            categoryTotals = categoryTotals,
                            totalSpent = totalSpent
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scanned Bills ($monthName)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (monthExpenses.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("No expenses logged for $monthName", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Scan a restaurant bill or GST invoice and tap 'Add to expenses' to auto-track spend.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.startNewScan() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan a Bill")
                                }
                            }
                        }
                    }
                } else {
                    items(monthExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(expense = expense, onClick = { editingExpense = expense })
                    }
                }
            }
        }
    }

    editingExpense?.let { exp ->
        EditExpenseDialog(
            expense = exp,
            onDismiss = { editingExpense = null },
            onUpdate = { updated ->
                viewModel.updateExpense(updated)
                editingExpense = null
                Toast.makeText(context, "Expense updated", Toast.LENGTH_SHORT).show()
            },
            onDelete = { id ->
                viewModel.deleteExpense(id)
                editingExpense = null
                Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ---------------------- COMPONENT: TOOL FEATURE CARD ----------------------

@Composable
private fun ToolFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }

            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------- COMPONENT: UNIVERSAL TRANSLATOR PANEL ----------------------

@Composable
private fun UniversalTranslatorPanel(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    sourceLang: TranslationService.Language,
    onSourceLangChanged: (TranslationService.Language) -> Unit,
    targetLang: TranslationService.Language,
    onTargetLangChanged: (TranslationService.Language) -> Unit,
    outputText: String,
    isTranslating: Boolean,
    onTranslate: () -> Unit,
    onLoadFromLastScan: () -> Unit
) {
    val context = LocalContext.current
    var sourceDropdownOpen by remember { mutableStateOf(false) }
    var targetDropdownOpen by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Universal Document Translator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onLoadFromLastScan) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste Scan", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Pickers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Language
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sourceDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(sourceLang.name, fontSize = 12.sp, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = sourceDropdownOpen,
                        onDismissRequest = { sourceDropdownOpen = false }
                    ) {
                        TranslationService.SUPPORTED_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.name} (${lang.nativeName})") },
                                onClick = {
                                    onSourceLangChanged(lang)
                                    sourceDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)

                // Target Language
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { targetDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(targetLang.name, fontSize = 12.sp, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = targetDropdownOpen,
                        onDismissRequest = { targetDropdownOpen = false }
                    ) {
                        TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.name} (${lang.nativeName})") },
                                onClick = {
                                    onTargetLangChanged(lang)
                                    targetDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = { Text("Type, paste text, or load from scanned document...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Translate Button
            Button(
                onClick = onTranslate,
                enabled = !isTranslating && inputText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translating...")
                } else {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate to ${targetLang.name}")
                }
            }

            // Translated Output Box
            if (outputText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Translated (${targetLang.name}):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row {
                                // Speak (TTS)
                                IconButton(
                                    onClick = {
                                        TtsHelper.speak(outputText, targetLang.name)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen", modifier = Modifier.size(18.dp))
                                }

                                // Copy
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Translation", outputText))
                                        Toast.makeText(context, "Translation copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                }

                                // Share
                                IconButton(
                                    onClick = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, outputText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Translated Text"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(outputText, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ---------------------- COMPONENT: HANDWRITING TO TEXT PANEL ----------------------

@Composable
private fun HandwritingToTextPanel(
    viewModel: AuraScanViewModel,
    handwritingText: String,
    onTextChanged: (String) -> Unit,
    bullets: List<String>,
    isFormatting: Boolean,
    onFormatBullets: () -> Unit,
    onLoadFromLastScan: () -> Unit
) {
    val context = LocalContext.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Draw, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Handwriting to Text (OCR)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onLoadFromLastScan) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Scan", fontSize = 12.sp)
                }
            }

            Text(
                text = "Capture notebook pages, letters, or assignments to transcribe them into clean digital text.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action row to capture or load
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.startNewScan() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Handwritten Page", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = handwritingText,
                onValueChange = onTextChanged,
                placeholder = { Text("Extracted digital text will appear here. You can also paste or edit freely...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onFormatBullets,
                    enabled = handwritingText.isNotBlank() && !isFormatting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Format into Bullets", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Handwriting OCR", handwritingText))
                        Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = handwritingText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy All Text", fontSize = 12.sp)
                }
            }

            // Formatted Bullet points
            if (bullets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Structured Study / Bullet Notes:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        bullets.forEach { b ->
                            Text(b, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- COMPONENT: 10TH MARK SHEET PANEL ----------------------

@Composable
private fun MarksheetScannerPanel(
    viewModel: AuraScanViewModel,
    lastScannedData: com.example.data.model.AnalyzedDocumentData?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("10th Mark Sheet & Academic Scanner", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("CBSE, ICSE, State Boards & University transcripts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Our 100% on-device OCR automatically extracts:\n• Student / Candidate Name\n• Roll Number & Registration Code\n• Board / School Name\n• Subject-wise scores table & evaluation\n• Pass status and division grade",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.startNewScan() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan 10th Mark Sheet Now")
            }

            // If a document was just analyzed
            if (lastScannedData != null && (lastScannedData.documentType == "mark_sheet" || lastScannedData.holderName.isNotBlank())) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Active Document: ${lastScannedData.suggestedTitle}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (lastScannedData.holderName.isNotBlank()) {
                            Text("Candidate: ${lastScannedData.holderName}", fontSize = 12.sp)
                        }
                        if (lastScannedData.idNumber.isNotBlank()) {
                            Text("Roll No: ${lastScannedData.idNumber}", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.navigateTo("smart_actions") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Full Transcript & Actions", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- DONUT CHART COMPONENT ----------------------

@Composable
private fun CategoryDonutChartCard(
    categoryTotals: Map<String, Double>,
    totalSpent: Double
) {
    val categoryColors = listOf(
        Color(0xFF2E7D32), // Green - Food
        Color(0xFF1976D2), // Blue - Travel
        Color(0xFFE65100), // Orange - Shopping
        Color(0xFF7B1FA2), // Purple - Utilities
        Color(0xFF00838F), // Teal - Healthcare
        Color(0xFF546E7A)  // Grey - Other
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Spend by Category",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val stroke = 24.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                        val arcSize = Size(diameter, diameter)

                        var colorIdx = 0
                        categoryTotals.forEach { (_, amount) ->
                            val sweep = (amount / totalSpent * 360f).toFloat()
                            val color = categoryColors[colorIdx % categoryColors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                            colorIdx++
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${categoryTotals.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cats",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var idx = 0
                    categoryTotals.entries.take(4).forEach { (cat, amount) ->
                        val color = categoryColors[idx % categoryColors.size]
                        val pct = ((amount / totalSpent) * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cat, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        idx++
                    }
                }
            }
        }
    }
}

// ---------------------- EXPENSE ITEM CARD ----------------------

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    val formattedDate = dateFormat.format(Date(expense.date))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (expense.category.lowercase()) {
                        "food", "dining" -> Icons.Default.Fastfood
                        "travel", "transport" -> Icons.Default.Flight
                        "shopping" -> Icons.Default.ShoppingBag
                        "utilities" -> Icons.Default.Lightbulb
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchant.ifBlank { "Unknown Merchant" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedDate • ${expense.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = IndianCurrencyFormatter.formatRupees(expense.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (expense.gstAmount > 0) {
                    Text(
                        text = "GST: ${IndianCurrencyFormatter.formatRupees(expense.gstAmount)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------------------- EDIT EXPENSE DIALOG ----------------------

@Composable
private fun EditExpenseDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onUpdate: (ExpenseEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    var merchant by remember { mutableStateOf(expense.merchant) }
    var amountText by remember { mutableStateOf(expense.amount.toString()) }
    var gstText by remember { mutableStateOf(expense.gstAmount.toString()) }
    var category by remember { mutableStateOf(expense.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scanned Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gstText,
                    onValueChange = { gstText = it },
                    label = { Text("GST Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: expense.amount
                    val gst = gstText.toDoubleOrNull() ?: expense.gstAmount
                    onUpdate(
                        expense.copy(
                            merchant = merchant,
                            amount = amount,
                            gstAmount = gst,
                            category = category
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onDelete(expense.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
