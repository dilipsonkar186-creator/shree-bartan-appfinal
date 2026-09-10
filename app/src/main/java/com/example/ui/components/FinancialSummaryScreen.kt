package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.TransactionEntity
import com.example.ui.viewmodel.FolderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val MONTH_NAMES = listOf(
    "All Months", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FinancialSummaryScreen(
    viewModel: FolderViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val rawFolders by viewModel.rawFolders.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val selectedYear by viewModel.selectedSummaryYear.collectAsState()
    val selectedMonth by viewModel.selectedSummaryMonth.collectAsState()
    val selectedFolderId by viewModel.selectedSummaryFolderId.collectAsState()

    val folderIdFilter = selectedFolderId

    var showFolderDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = listOf(currentYear, currentYear - 1, currentYear - 2)

    // Filter transactions by year & month
    val filteredTransactions = remember(allTransactions, selectedYear, selectedMonth) {
        allTransactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            val txYear = cal.get(Calendar.YEAR)
            val txMonth = cal.get(Calendar.MONTH)

            val yearMatch = selectedYear <= 0 || txYear == selectedYear
            val monthMatch = selectedMonth < 0 || txMonth == selectedMonth
            yearMatch && monthMatch
        }
    }

    // Filter folders
    val activeFolders = remember(rawFolders, folderIdFilter) {
        if (folderIdFilter != null && folderIdFilter > 0) {
            rawFolders.filter { it.folder.id == folderIdFilter }
        } else {
            rawFolders
        }
    }

    // Overall metrics across filtered scope
    var grandTotalSales = 0.0
    var grandTotalReceived = 0.0

    filteredTransactions.forEach { tx ->
        val inSelectedFolder = folderIdFilter == null || folderIdFilter <= 0 || tx.folderId == folderIdFilter
        if (inSelectedFolder) {
            if (tx.type == "GOODS_PROVIDED") {
                grandTotalSales += tx.totalAmount
            } else if (tx.type == "PAYMENT_DEPOSIT") {
                grandTotalReceived += tx.totalAmount
            }
        }
    }
    val grandNetDues = grandTotalSales - grandTotalReceived

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Financial Summary & Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Goods Sales, Payments & Balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Extract Data / Copy Summary Report Button
                    IconButton(
                        onClick = {
                            val sb = StringBuilder()
                            sb.appendLine("=== CUSTOMER FOLDERS FINANCIAL SUMMARY REPORT ===")
                            sb.appendLine("Time Period: ${if (selectedMonth >= 0) MONTH_NAMES[selectedMonth + 1] else "All Months"} $selectedYear")
                            sb.appendLine("Folder Filter: ${if (folderIdFilter != null && folderIdFilter > 0) activeFolders.firstOrNull()?.folder?.name ?: "Selected" else "All Folders"}")
                            sb.appendLine("-------------------------------------------------")
                            sb.appendLine("Total Sales (Goods Provided): ₹${String.format("%.2f", grandTotalSales)}")
                            sb.appendLine("Total Received (Payments/Deposits): ₹${String.format("%.2f", grandTotalReceived)}")
                            sb.appendLine("Total Outstanding Balance: ₹${String.format("%.2f", grandNetDues)}")
                            sb.appendLine("=================================================\n")

                            activeFolders.forEach { fItem ->
                                val f = fItem.folder
                                val fTxs = filteredTransactions.filter { it.folderId == f.id }
                                var fSales = 0.0
                                var fRec = 0.0
                                fTxs.forEach { tx ->
                                    if (tx.type == "GOODS_PROVIDED") fSales += tx.totalAmount
                                    else if (tx.type == "PAYMENT_DEPOSIT") fRec += tx.totalAmount
                                }

                                sb.appendLine("[FOLDER]: ${f.name} (Area: ${f.areaTag})")
                                sb.appendLine("Total Customers: ${fItem.customerCount}")
                                sb.appendLine("Folder Sales: ₹${String.format("%.2f", fSales)} | Received: ₹${String.format("%.2f", fRec)} | Dues: ₹${String.format("%.2f", fSales - fRec)}")
                                sb.appendLine("Itemized Purchases & Payments:")
                                fTxs.forEach { tx ->
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.dateMillis))
                                    sb.appendLine("  - [$dateStr] ${tx.type}: ${tx.itemDescription} (₹${String.format("%.2f", tx.totalAmount)})")
                                }
                                sb.appendLine()
                            }

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Financial Summary Report", sb.toString()))
                            Toast.makeText(context, "Financial summary exported & copied to clipboard!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.testTag("extract_data_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Extract Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters Section (Year, Month, Folder)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Report Filters",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Folder Filter Dropdown Chip
                            Box {
                                val currentFolderLabel = if (folderIdFilter != null && folderIdFilter > 0) {
                                    rawFolders.find { it.folder.id == folderIdFilter }?.folder?.name ?: "Folder"
                                } else "All Folders"

                                FilterChip(
                                    selected = folderIdFilter != null && folderIdFilter > 0,
                                    onClick = { showFolderDropdown = true },
                                    label = { Text(currentFolderLabel) },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("folder_filter_chip")
                                )

                                DropdownMenu(
                                    expanded = showFolderDropdown,
                                    onDismissRequest = { showFolderDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Folders") },
                                        onClick = {
                                            viewModel.setSummaryFolderFilter(-1L)
                                            showFolderDropdown = false
                                        }
                                    )
                                    rawFolders.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item.folder.name) },
                                            onClick = {
                                                viewModel.setSummaryFolderFilter(item.folder.id)
                                                showFolderDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year Filter Dropdown Chip
                            Box {
                                FilterChip(
                                    selected = true,
                                    onClick = { showYearDropdown = true },
                                    label = { Text("Year: $selectedYear") },
                                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("year_filter_chip")
                                )

                                DropdownMenu(
                                    expanded = showYearDropdown,
                                    onDismissRequest = { showYearDropdown = false }
                                ) {
                                    years.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y.toString()) },
                                            onClick = {
                                                viewModel.setSummaryTimeFilter(y, selectedMonth)
                                                showYearDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Month Filter Dropdown Chip
                            Box {
                                val monthLabel = if (selectedMonth < 0) "All Months" else MONTH_NAMES[selectedMonth + 1]

                                FilterChip(
                                    selected = selectedMonth >= 0,
                                    onClick = { showMonthDropdown = true },
                                    label = { Text(monthLabel) },
                                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("month_filter_chip")
                                )

                                DropdownMenu(
                                    expanded = showMonthDropdown,
                                    onDismissRequest = { showMonthDropdown = false }
                                ) {
                                    MONTH_NAMES.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                viewModel.setSummaryTimeFilter(selectedYear, index - 1)
                                                showMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Overall Summary KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Sales", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format("₹%.2f", grandTotalSales),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Received", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format("₹%.2f", grandTotalReceived),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Folder Breakdown Section
            item {
                Text(
                    text = "Folder Financial Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(activeFolders, key = { it.folder.id }) { fItem ->
                val f = fItem.folder
                val folderTxs = filteredTransactions.filter { it.folderId == f.id }

                var fSales = 0.0
                var fReceived = 0.0
                val customerTxMap = mutableMapOf<Long, MutableList<TransactionEntity>>()

                folderTxs.forEach { tx ->
                    customerTxMap.getOrPut(tx.customerId) { mutableListOf() }.add(tx)
                    if (tx.type == "GOODS_PROVIDED") fSales += tx.totalAmount
                    else if (tx.type == "PAYMENT_DEPOSIT") fReceived += tx.totalAmount
                }
                val fDues = fSales - fReceived

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try {
                                                Color(android.graphics.Color.parseColor(f.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(f.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Area: ${f.areaTag} • ${fItem.customerCount} Customers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        HorizontalDivider()

                        // Metrics grid for folder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sales (Goods)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("₹%.2f", fSales), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Amount Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("₹%.2f", fReceived), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Balance Dues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("₹%.2f", fDues), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (fDues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                            }
                        }

                        if (folderTxs.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                "Purchased Items & Payments Breakdown (${folderTxs.size} entries):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            folderTxs.take(5).forEach { tx ->
                                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.dateMillis))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• [$dateStr] ${tx.itemDescription}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        (if (tx.type == "GOODS_PROVIDED") "+₹" else "-₹") + String.format("%.2f", tx.totalAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == "GOODS_PROVIDED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (folderTxs.size > 5) {
                                Text(
                                    "+ ${folderTxs.size - 5} more transactions...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
