@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.viewmodel.CategoryShare
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

// Category configurations with beautiful Material Icons and vibrant colors
data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

val EXPENSE_CATEGORIES = listOf(
    CategoryInfo("餐饮", Icons.Default.Restaurant, Color(0xFFFF9800)),
    CategoryInfo("购物", Icons.Default.ShoppingCart, Color(0xFFE91E63)),
    CategoryInfo("交通", Icons.Default.DirectionsCar, Color(0xFF03A9F4)),
    CategoryInfo("娱乐", Icons.Default.Gamepad, Color(0xFF9C27B0)),
    CategoryInfo("居住", Icons.Default.Home, Color(0xFF3F51B5)),
    CategoryInfo("人情往来", Icons.Default.CardGiftcard, Color(0xFFFF5722)),
    CategoryInfo("医疗", Icons.Default.LocalHospital, Color(0xFF4CAF50)),
    CategoryInfo("其它", Icons.Default.Category, Color(0xFF607D8B))
)

val INCOME_CATEGORIES = listOf(
    CategoryInfo("工资", Icons.Default.Work, Color(0xFF4CAF50)),
    CategoryInfo("兼职", Icons.Default.MonetizationOn, Color(0xFF8BC34A)),
    CategoryInfo("理财", Icons.Default.TrendingUp, Color(0xFF009688)),
    CategoryInfo("红包", Icons.Default.Redeem, Color(0xFFFFC107)),
    CategoryInfo("其它", Icons.Default.Category, Color(0xFF9E9E9E))
)

fun getCategoryColor(name: String): Color {
    val expenseMatch = EXPENSE_CATEGORIES.firstOrNull { it.name == name }
    if (expenseMatch != null) return expenseMatch.color
    val incomeMatch = INCOME_CATEGORIES.firstOrNull { it.name == name }
    if (incomeMatch != null) return incomeMatch.color
    return Color(0xFF757575)
}

fun getCategoryIcon(name: String): ImageVector {
    val expenseMatch = EXPENSE_CATEGORIES.firstOrNull { it.name == name }
    if (expenseMatch != null) return expenseMatch.icon
    val incomeMatch = INCOME_CATEGORIES.firstOrNull { it.name == name }
    if (incomeMatch != null) return incomeMatch.icon
    return Icons.Default.Category
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val filterCalendar by viewModel.filterCalendar.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(bottom = 8.dp)
            ) {
                // Main Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "随手记账",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Mode selections: All, Day, Month
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(2.dp)
                    ) {
                        listOf(
                            "ALL" to "全部",
                            "DAY" to "按天",
                            "MONTH" to "按月"
                        ).forEach { (modeKey, label) ->
                            val isSelected = filterMode == modeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable { viewModel.setFilterMode(modeKey) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sub-header for date selection navigation
                if (filterMode != "ALL") {
                    val sdf = when (filterMode) {
                        "DAY" -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                        "MONTH" -> SimpleDateFormat("yyyy年MM月", Locale.getDefault())
                        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    }
                    val dateStr = sdf.format(filterCalendar.time)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.adjustFilterPeriod(-1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Period",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Clicking on the date text opens the system DatePickerDialog
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val datePickerDialog = DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            viewModel.setFilterDate(y, m, d)
                                        },
                                        filterCalendar.get(Calendar.YEAR),
                                        filterCalendar.get(Calendar.MONTH),
                                        filterCalendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    datePickerDialog.show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Choose Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { viewModel.adjustFilterPeriod(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Period",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Fast Summary Bar (Income vs Expense)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Expense",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "本期支出",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "¥${String.format("%.2f", totalExpense)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp)
                                .align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Income",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "本期收入",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "¥${String.format("%.2f", totalIncome)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp)
                                .align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val balance = totalIncome - totalExpense
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (balance >= 0) Icons.Default.Savings else Icons.Default.PriceChange,
                                    contentDescription = "Balance",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "结余",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "¥${String.format("%.2f", balance)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (balance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Bills List") },
                    label = { Text("明细清单") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Reports") },
                    label = { Text("统计分析") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tools") },
                    label = { Text("高级工具") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BillsListTab(
                    transactions = filteredTransactions,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onTransactionClick = { transactionToEdit = it }
                )
                1 -> ReportsTab(viewModel = viewModel)
                2 -> ToolsTab(viewModel = viewModel)
            }
        }
    }

    // Add / Edit Transaction Form Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, type, category, dateMillis, remark ->
                viewModel.insertTransaction(amount, type, category, dateMillis, remark)
                showAddDialog = false
            }
        )
    }

    if (transactionToEdit != null) {
        val currentTx = transactionToEdit!!
        EditTransactionDialog(
            transaction = currentTx,
            onDismiss = { transactionToEdit = null },
            onConfirm = { updated ->
                viewModel.updateTransaction(updated)
                transactionToEdit = null
            },
            onDelete = {
                viewModel.deleteTransaction(currentTx)
                transactionToEdit = null
            }
        )
    }
}

// ======================== TABS IMPLEMENTATION ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsListTab(
    transactions: List<Transaction>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_input"),
            placeholder = { Text("搜索备注或类别金额...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Empty Data",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无收支明细",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "你可以点击右下角的 '+' 按钮，或者在高级工具中一键生成测试数据！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Group transactions by simple date string
            val grouped = remember(transactions) {
                transactions.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.format(cal.time)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                grouped.forEach { (dateStr, items) ->
                    // Calculate header sums
                    val dayExpense = items.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val dayIncome = items.filter { it.type == "INCOME" }.sumOf { it.amount }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayExpense > 0) {
                                    Text(
                                        "支: ¥${String.format("%.1f", dayExpense)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                                if (dayIncome > 0) {
                                    Text(
                                        "收: ¥${String.format("%.1f", dayIncome)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    items(items, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = getCategoryColor(transaction.category)
            val icon = getCategoryIcon(transaction.category)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.remark.isNotBlank()) {
                    Text(
                        text = transaction.remark,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = (if (transaction.type == "EXPENSE") "-" else "+") + "¥${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (transaction.type == "EXPENSE") MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun ReportsTab(viewModel: LedgerViewModel) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val categoryStats by viewModel.categoryStats.collectAsStateWithLifecycle()
    val incomeCategoryStats by viewModel.incomeCategoryStats.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()

    var statsTypeTab by remember { mutableStateOf(0) } // 0: 支出, 1: 收入

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "本期暂无账单，无法生成可视化报表",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Trend visual section
            item {
                Text(
                    text = "收支走势分析",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TrendChart(transactions = filteredTransactions)
                    }
                }
            }

            // Category shares section with a segmented button row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "分类占比构成",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tab toggle for Expense vs Income Category Shares
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(2.dp)
                    ) {
                        listOf("支出", "收入").forEachIndexed { index, label ->
                            val isSelected = statsTypeTab == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable { statsTypeTab = index }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val activeStats = if (statsTypeTab == 0) categoryStats else incomeCategoryStats
            val activeTotal = if (statsTypeTab == 0) totalExpense else totalIncome

            if (activeStats.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无此类型相关的账单数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Interactive Donut Chart Representation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(8.dp)
                            ) {
                                DonutChartRing(
                                    categoryStats = activeStats,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (statsTypeTab == 0) "总支出" else "总收入",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "¥${String.format("%.2f", activeTotal)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats breakdown bars
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                activeStats.forEach { stat ->
                                    CategoryStatProgressRow(stat = stat)
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
fun TrendChart(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    // Generate bars representing the daily totals or categories in the current filter
    val groupedByDate = remember(transactions) {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val map = TreeMap<String, Pair<Double, Double>>() // key: DateString, value: (Income, Expense)

        // Prepopulate past 7 dates to prevent blank space if data size is small
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            val dateStr = sdf.format(cal.time)
            map[dateStr] = Pair(0.0, 0.0)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Aggregate actual data
        transactions.forEach { tx ->
            val dateStr = sdf.format(Date(tx.dateMillis))
            val currentPair = map[dateStr] ?: Pair(0.0, 0.0)
            val updatedPair = if (tx.type == "INCOME") {
                Pair(currentPair.first + tx.amount, currentPair.second)
            } else {
                Pair(currentPair.first, currentPair.second + tx.amount)
            }
            map[dateStr] = updatedPair
        }
        map.toList().takeLast(7) // take last 7 days sorted in chronological order
    }

    val maxAmount = remember(groupedByDate) {
        val highest = groupedByDate.maxOfOrNull { maxOf(it.second.first, it.second.second) } ?: 1.0
        if (highest == 0.0) 1.0 else highest
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            groupedByDate.forEach { (dateStr, pair) ->
                val income = pair.first
                val expense = pair.second

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .height(130.dp)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Income Bar (Green)
                        if (income > 0) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight((income / maxAmount).toFloat().coerceIn(0.03f, 1.0f))
                                    .background(Color(0xFF4CAF50), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Expense Bar (Red)
                        if (expense > 0) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight((expense / maxAmount).toFloat().coerceIn(0.03f, 1.0f))
                                    .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart Legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF4CAF50), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DonutChartRing(categoryStats: List<CategoryShare>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = 16.dp.toPx()
        var startAngle = -90f

        categoryStats.forEach { stat ->
            val sweepAngle = stat.percentage * 360f
            val color = getCategoryColor(stat.category)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryStatProgressRow(stat: CategoryShare) {
    val color = getCategoryColor(stat.category)
    val icon = getCategoryIcon(stat.category)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stat.category,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stat.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${String.format("%.1f", stat.percentage * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "¥${String.format("%.2f", stat.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Linear Progress bar with custom color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stat.percentage)
                    .fillMaxHeight()
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun ToolsTab(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "高级数据与工具",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // EXPORT DATA CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Excel/CSV",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "数据导出为 Excel (CSV)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "支持将当前过滤条件下的账单明细快速导出为带有 UTF-8 BOM 编码的 CSV 格式，能够直接在电脑 Excel、WPS 等工具上打开无乱码，随时发送给其他 AI 助手做深入账单趋势与财务分析。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val uri = viewModel.exportToCsv(context)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "发送/分享账单导出数据"))
                        } else {
                            Toast.makeText(context, "没有可供导出的账单数据！", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_csv_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出当前筛选账单")
                }
            }
        }

        // GENERATE DEMO DATA CARD (VERY HELPFUL FOR RAPID TESTING)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Demo Data",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "测试数据生成器",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "新用户账单为空？没关系！点击下方按钮将一键插入 18 笔真实、丰富的模拟账单（包含餐饮、兼职工资、交通等，遍布最近 15 天），立刻体验图表分析和趋势走向！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        generateDummyData(viewModel)
                        Toast.makeText(context, "成功生成 18 笔模拟收支数据！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("generate_demo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Generate")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("一键插入 18 笔模拟账单")
                }
            }
        }

        // QUICK DELETE ALL DATA (FOR CLEAN RESTART)
        if (transactions.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    transactions.forEach { viewModel.deleteTransaction(it) }
                    Toast.makeText(context, "数据已全部清空", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear All")
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键清空全部数据")
            }
        }
    }
}

// Generate realistic dummy data for rapid applet evaluation
fun generateDummyData(viewModel: LedgerViewModel) {
    val cal = Calendar.getInstance()
    val rand = Random()

    val dummyTransactions = listOf(
        // Expenses
        Triple("餐饮", 32.50, "吃火锅午餐"),
        Triple("餐饮", 15.00, "买喜茶拿铁咖啡"),
        Triple("交通", 8.00, "坐地铁通勤"),
        Triple("购物", 129.00, "拼多多买入收纳盒"),
        Triple("娱乐", 45.00, "电影院观影票"),
        Triple("其它", 12.00, "路边便利店买纸巾"),
        Triple("餐饮", 48.00, "晚餐外卖黄焖鸡"),
        Triple("人情往来", 200.00, "同事生日礼金红包"),
        Triple("交通", 22.50, "打滴滴快车回家"),
        Triple("餐饮", 18.00, "买煎饼果子早餐"),
        Triple("居住", 1500.00, "缴纳本月自来水燃气物业费"),
        Triple("医疗", 35.80, "买感冒退烧药"),

        // Incomes
        Triple("工资", 5800.00, "收到本月基本工资"),
        Triple("兼职", 450.00, "软件外包开发首期款"),
        Triple("理财", 78.50, "基金分红理财收益"),
        Triple("红包", 100.00, "微信亲友红包"),
        Triple("兼职", 280.00, "翻译兼职稿费"),
        Triple("其它", 50.00, "二手闲置出闲置物品")
    )

    dummyTransactions.forEach { (category, amount, remark) ->
        val isExpense = category in listOf("餐饮", "购物", "交通", "娱乐", "居住", "人情往来", "医疗", "其它")
        val type = if (isExpense) "EXPENSE" else "INCOME"

        // Space them across the last 15 days
        val daysOffset = rand.nextInt(15)
        val itemCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysOffset)
            // random hour & minute to make it dynamic
            set(Calendar.HOUR_OF_DAY, rand.nextInt(24))
            set(Calendar.MINUTE, rand.nextInt(60))
        }

        viewModel.insertTransaction(
            amount = amount,
            type = type,
            category = category,
            dateMillis = itemCal.timeInMillis,
            remark = remark
        )
    }
}

// ======================== DIALOGS IMPLEMENTATION ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, category: String, dateMillis: Long, remark: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategory by remember { mutableStateOf("餐饮") }
    var remark by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val categories = if (type == "EXPENSE") EXPENSE_CATEGORIES else INCOME_CATEGORIES

    // Auto-align selected category when type toggles
    LaunchedEffect(type) {
        selectedCategory = if (type == "EXPENSE") "餐饮" else "工资"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "记一笔账单",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher: Income vs Expense Capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == "EXPENSE") MaterialTheme.colorScheme.error else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "支出",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == "INCOME") Color(0xFF4CAF50) else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "收入",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        // Allow decimals up to 2 decimal places and skip any non-numeric letters
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountStr = input
                        }
                    },
                    label = { Text("账单金额 (元)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_amount_input"),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Amount Icon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Date Picker Trigger
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = {},
                    label = { Text("消费日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val dialog = DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    dateMillis = cal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Date")
                        }
                    }
                )

                // Category Grid Selection
                Text(
                    "选择账目类别",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Remark Input
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注/明细说明 (选填)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_remark_input"),
                    placeholder = { Text("例：购买喜茶、早餐煎饼果子...") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(context, "请输入有效的金额！", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(amount, type, selectedCategory, dateMillis, remark)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("保存记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var remark by remember { mutableStateOf(transaction.remark) }
    var dateMillis by remember { mutableStateOf(transaction.dateMillis) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val categories = if (type == "EXPENSE") EXPENSE_CATEGORIES else INCOME_CATEGORIES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "修改/删除账单",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == "EXPENSE") MaterialTheme.colorScheme.error else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "支出",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == "INCOME") Color(0xFF4CAF50) else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "收入",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountStr = input
                        }
                    },
                    label = { Text("账单金额 (元)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Amount Icon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Date Picker Trigger
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = {},
                    label = { Text("消费日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val dialog = DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    dateMillis = cal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Edit Date")
                        }
                    }
                )

                // Category Grid Selection
                Text(
                    "选择账目类别",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Remark Input
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注/明细说明 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DELETE Button on left side
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Icon")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }

                Row {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "请输入有效的金额！", Toast.LENGTH_SHORT).show()
                            } else {
                                onConfirm(
                                    transaction.copy(
                                        amount = amount,
                                        type = type,
                                        category = selectedCategory,
                                        dateMillis = dateMillis,
                                        remark = remark
                                    )
                                )
                            }
                        }
                    ) {
                        Text("保存修改")
                    }
                }
            }
        }
    )
}
