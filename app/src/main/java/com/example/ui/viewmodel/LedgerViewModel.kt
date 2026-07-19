package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LedgerViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Current filter mode: "DAY" or "MONTH" or "ALL"
    private val _filterMode = MutableStateFlow("ALL")
    val filterMode: StateFlow<String> = _filterMode.asStateFlow()

    // Active calendar filter selection
    private val _filterCalendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    val filterCalendar: StateFlow<Calendar> = _filterCalendar.asStateFlow()

    // Search query for remarks or categories
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Complete transaction flow
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered transaction flow based on mode, date, and query
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _filterMode,
        _filterCalendar,
        _searchQuery
    ) { transactions, mode, cal, query ->
        transactions.filter { tx ->
            val matchQuery = if (query.isBlank()) {
                true
            } else {
                tx.category.contains(query, ignoreCase = true) || 
                tx.remark.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)
            }

            if (!matchQuery) return@filter false

            when (mode) {
                "DAY" -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    txCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                }
                "MONTH" -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    txCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                }
                else -> true // "ALL"
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Statistics UI state
    val totalIncome: StateFlow<Double> = filteredTransactions
        .combine(MutableStateFlow(0.0)) { txs, _ ->
            txs.filter { it.type == "INCOME" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions
        .combine(MutableStateFlow(0.0)) { txs, _ ->
            txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category distribution for filtered transactions
    val categoryStats: StateFlow<List<CategoryShare>> = filteredTransactions
        .combine(MutableStateFlow(emptyList<CategoryShare>())) { txs, _ ->
            val expenses = txs.filter { it.type == "EXPENSE" }
            val total = expenses.sumOf { it.amount }
            if (total == 0.0) return@combine emptyList<CategoryShare>()

            expenses.groupBy { it.category }
                .map { (category, list) ->
                    val sum = list.sumOf { it.amount }
                    CategoryShare(
                        category = category,
                        amount = sum,
                        percentage = (sum / total).toFloat()
                    )
                }.sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Income category distribution
    val incomeCategoryStats: StateFlow<List<CategoryShare>> = filteredTransactions
        .combine(MutableStateFlow(emptyList<CategoryShare>())) { txs, _ ->
            val incomes = txs.filter { it.type == "INCOME" }
            val total = incomes.sumOf { it.amount }
            if (total == 0.0) return@combine emptyList<CategoryShare>()

            incomes.groupBy { it.category }
                .map { (category, list) ->
                    val sum = list.sumOf { it.amount }
                    CategoryShare(
                        category = category,
                        amount = sum,
                        percentage = (sum / total).toFloat()
                    )
                }.sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertTransaction(amount: Double, type: String, category: String, dateMillis: Long, remark: String) {
        viewModelScope.launch {
            repository.insert(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    dateMillis = dateMillis,
                    remark = remark
                )
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun setFilterMode(mode: String) {
        _filterMode.value = mode
    }

    fun setFilterDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        _filterCalendar.value = cal
    }

    fun adjustFilterPeriod(offset: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _filterCalendar.value.timeInMillis
        }
        when (_filterMode.value) {
            "DAY" -> cal.add(Calendar.DAY_OF_MONTH, offset)
            "MONTH" -> cal.add(Calendar.MONTH, offset)
            else -> {}
        }
        _filterCalendar.value = cal
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Export current filtered list to an Excel-compatible CSV file and return the sharing Uri
    fun exportToCsv(context: Context): Uri? {
        val transactionsToExport = filteredTransactions.value
        if (transactionsToExport.isEmpty()) return null

        try {
            val cacheDir = context.cacheDir
            val exportFile = File(cacheDir, "随手记账_账单导出_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv")
            val outputStream = FileOutputStream(exportFile)

            // Prepend UTF-8 BOM so Excel opens the Chinese text correctly without gibberish
            outputStream.write(0xEF)
            outputStream.write(0xBB)
            outputStream.write(0xBF)

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            // Column Headers
            writer.write("交易ID,日期,收支类型,类别,金额(元),备注")
            writer.newLine()

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (tx in transactionsToExport) {
                val dateStr = sdf.format(Date(tx.dateMillis))
                val typeStr = if (tx.type == "EXPENSE") "支出" else "收入"
                // Escape commas or quotes in remarks
                val escapedRemark = tx.remark.replace("\"", "\"\"")
                val remarkField = if (escapedRemark.contains(",") || escapedRemark.contains("\"")) {
                    "\"$escapedRemark\""
                } else {
                    escapedRemark
                }

                writer.write("${tx.id},$dateStr,$typeStr,${tx.category},${tx.amount},$remarkField")
                writer.newLine()
            }
            writer.flush()
            writer.close()
            outputStream.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(context)
                val repository = TransactionRepository(db.transactionDao())
                return LedgerViewModel(repository) as T
            }
        }
    }
}

data class CategoryShare(
    val category: String,
    val amount: Double,
    val percentage: Float
)
