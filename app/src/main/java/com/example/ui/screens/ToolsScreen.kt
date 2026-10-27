package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.RewardedAdBannerCard
import com.example.ui.viewmodel.AuraScanViewModel
import com.example.utils.FileUtils
import com.example.utils.IndianCurrencyFormatter
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
    val allExpenses by viewModel.allExpenses.collectAsState()
    val selectedCal by viewModel.selectedExpenseMonth.collectAsState()

    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Filter expenses for selected month
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

    // Category breakdown
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Tools & Expenses",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automated bill log with GST & category analytics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Export CSV Button
                    IconButton(
                        onClick = {
                            viewModel.exportExpensesToCsv { file ->
                                FileUtils.shareFile(context, file, "text/csv", "AuraScan Expenses CSV")
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
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
            // GOOGLE ADMOB REWARDED TEST AD (ca-app-pub-3940256099942544/5224354917)
            item {
                RewardedAdBannerCard(viewModel = viewModel)
            }

            // MONTH SELECTOR STRIP
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

            // MONTHLY STATS SUMMARY CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Spent
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Total Spent",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = IndianCurrencyFormatter.formatRupees(totalSpent),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${monthExpenses.size} bills logged",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Total GST Paid
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Total GST Paid",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = IndianCurrencyFormatter.formatRupees(totalGst),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Auto-extracted tax",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // CATEGORY DONUT CHART (Recharts Equivalent on Jetpack Compose Canvas)
            if (monthExpenses.isNotEmpty() && totalSpent > 0) {
                item {
                    CategoryDonutChartCard(
                        categoryTotals = categoryTotals,
                        totalSpent = totalSpent
                    )
                }
            }

            // BILLS LIST TITLE & COUNT
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned Bills ($monthName)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to edit",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // EMPTY STATE WHEN NO BILLS
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
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No expenses logged for $monthName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan a restaurant bill or GST invoice and tap 'Add to expenses' to auto-track spend.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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
                // LIST OF EXPENSES
                items(monthExpenses, key = { it.id }) { expense ->
                    ExpenseItemCard(
                        expense = expense,
                        onClick = { editingExpense = expense }
                    )
                }
            }
        }
    }

    // EDIT / DELETE EXPENSE DIALOG
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
                // Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidth = 26.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = Size(diameter, diameter)

                        categoryTotals.entries.forEachIndexed { index, entry ->
                            val sweepAngle = ((entry.value / totalSpent) * 360f).toFloat()
                            val color = categoryColors[index % categoryColors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle.coerceAtLeast(4f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${categoryTotals.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Categories",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryTotals.entries.forEachIndexed { index, entry ->
                        val color = categoryColors[index % categoryColors.size]
                        val pct = ((entry.value / totalSpent) * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = entry.key,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${IndianCurrencyFormatter.formatRupees(entry.value, false)} ($pct%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- EXPENSE CARD COMPONENT ----------------------

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(expense.date) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        sdf.format(Date(expense.date))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(getCategoryColor(expense.category).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = null,
                        tint = getCategoryColor(expense.category),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.merchant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ${expense.category}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (expense.gstAmount > 0.0) {
                        Text(
                            text = "GST: ${IndianCurrencyFormatter.formatRupees(expense.gstAmount)}",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = IndianCurrencyFormatter.formatRupees(expense.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tax incl.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------- EDIT / DELETE EXPENSE DIALOG ----------------------

@Composable
private fun EditExpenseDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onUpdate: (ExpenseEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    var merchant by remember { mutableStateOf(expense.merchant) }
    var amountStr by remember { mutableStateOf(expense.amount.toString()) }
    var category by remember { mutableStateOf(expense.category) }
    var note by remember { mutableStateOf(expense.note) }

    val categories = listOf("Food", "Travel", "Shopping", "Utilities", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Expense", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
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
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector chips
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSel = category.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: expense.amount
                    val sub = (amt * 0.95)
                    val gst = amt - sub
                    onUpdate(
                        expense.copy(
                            merchant = merchant,
                            amount = amt,
                            subtotal = sub,
                            gstAmount = gst,
                            category = category,
                            note = note
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
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun getCategoryIcon(cat: String): ImageVector {
    return when (cat.lowercase()) {
        "food" -> Icons.Default.Fastfood
        "travel" -> Icons.Default.Flight
        "shopping" -> Icons.Default.ShoppingBag
        "utilities" -> Icons.Default.Lightbulb
        else -> Icons.Default.ReceiptLong
    }
}

private fun getCategoryColor(cat: String): Color {
    return when (cat.lowercase()) {
        "food" -> Color(0xFF2E7D32)
        "travel" -> Color(0xFF1976D2)
        "shopping" -> Color(0xFFE65100)
        "utilities" -> Color(0xFF7B1FA2)
        else -> Color(0xFF546E7A)
    }
}
