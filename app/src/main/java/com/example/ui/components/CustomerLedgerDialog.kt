package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.viewmodel.FolderViewModel
import com.example.util.PdfExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerLedgerDialog(
    customer: CustomerEntity,
    viewModel: FolderViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.getTransactionsForCustomerFlow(customer.id)
        .collectAsState(initial = emptyList())

    var showRecordDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "GOODS" -> transactions.filter { it.type == "GOODS_PROVIDED" }
            "RETURNS" -> transactions.filter { it.type == "GOODS_RETURNED" }
            "PAYMENTS" -> transactions.filter { it.type == "PAYMENT_DEPOSIT" }
            else -> transactions
        }
    }

    val totalGoodsProvided = remember(transactions) {
        transactions.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
    }
    val totalGoods = totalGoodsProvided
    val totalPaid = remember(transactions) {
        transactions.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
    }
    val balance = totalGoods - totalPaid

    val runningBalances = remember(transactions) {
        val chronological = transactions.sortedWith(
            compareBy<TransactionEntity> { it.dateMillis }.thenBy { it.id }
        )
        val map = mutableMapOf<Long, Double>()
        var curBal = 0.0
        for (tx in chronological) {
            when (tx.type) {
                "GOODS_PROVIDED" -> curBal += tx.totalAmount
                "PAYMENT_DEPOSIT" -> curBal -= tx.totalAmount
                "GOODS_RETURNED" -> {
                    // Returned item does not change balance
                }
            }
            map[tx.id] = curBal
        }
        map
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Goods Provided & Deposits Ledger",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                PdfExportUtils.generateAndSharePdf(
                                    context = context,
                                    customer = customer,
                                    transactions = transactions,
                                    totalGoods = totalGoods,
                                    totalPaid = totalPaid,
                                    dues = balance
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A237E),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📄 PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                com.example.util.CsvBackupManager.exportCustomerLedgerCsv(
                                    context = context,
                                    customer = customer,
                                    transactions = transactions
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📊 CSV", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Ledger")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Financial Stats Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Goods", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format("₹%.2f", totalGoods),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format("₹%.2f", totalPaid),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format("₹%.2f", balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${transactions.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedFilter == "GOODS",
                        onClick = { selectedFilter = "GOODS" },
                        label = { Text("Goods Sold", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedFilter == "RETURNS",
                        onClick = { selectedFilter = "RETURNS" },
                        label = { Text("Returns", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedFilter == "PAYMENTS",
                        onClick = { selectedFilter = "PAYMENTS" },
                        label = { Text("Payments", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Record Goods or Deposit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History (${filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = { showRecordDialog = true },
                        modifier = Modifier.testTag("record_entry_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Goods / Money")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transaction History List
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (transactions.isEmpty()) "No goods or payment records yet"
                                else "No entries match selected filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            val isGoods = tx.type == "GOODS_PROVIDED"
                            val isReturn = tx.type == "GOODS_RETURNED"
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dateStr = dateFormat.format(Date(tx.dateMillis))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isGoods -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
                                        isReturn -> Color(0xFFFFF3E0)
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Row 1: Item icon, title, amount, and actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isGoods -> MaterialTheme.colorScheme.error
                                                            isReturn -> Color(0xFFE65100)
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isGoods -> Icons.Default.ShoppingBag
                                                        isReturn -> Icons.AutoMirrored.Filled.ArrowBack
                                                        else -> Icons.Default.Payments
                                                    },
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = (if (isReturn) "❌ " else "") + tx.itemDescription,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = if (isReturn) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReturn) Color(0xFFE65100) else Color.Unspecified
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                // Highlighted Date and Time Tag
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isGoods) Color(0xFFFFF8E1) else if (isReturn) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isGoods) Color(0xFFFFD54F) else if (isReturn) Color(0xFFFFB74D) else Color(0xFFA5D6A7)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CalendarToday,
                                                            contentDescription = "Date",
                                                            tint = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = dateStr,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isGoods -> "+₹${String.format("%.2f", tx.totalAmount)}"
                                                    isReturn -> "₹${String.format("%.2f", tx.totalAmount)}"
                                                    else -> "-₹${String.format("%.2f", tx.totalAmount)}"
                                                },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    textDecoration = if (isReturn) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isGoods -> MaterialTheme.colorScheme.error
                                                    isReturn -> Color(0xFFE65100)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )

                                            IconButton(
                                                onClick = { transactionToEdit = tx },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit entry",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { transactionToDelete = tx },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Quantity & rate breakdown + Running balance badge
                                    val rowBal = runningBalances[tx.id] ?: 0.0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isGoods || isReturn) {
                                            val unitLabel = when (tx.unitType) {
                                                "kg" -> "kg"
                                                "dz" -> "dz"
                                                else -> "pcs"
                                            }
                                            val qtyDisp = if (tx.quantityDouble > 0) String.format("%.2f", tx.quantityDouble).removeSuffix(".00").removeSuffix(".0") else "${tx.quantity}"
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = "📦 $qtyDisp $unitLabel  ×  ₹${String.format("%.2f", tx.unitPrice)}/$unitLabel",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (rowBal > 0) Color(0xFFFFEBEE) else if (rowBal < 0) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                                        ) {
                                            Text(
                                                text = if (rowBal > 0) "Bal: ₹${String.format("%,.2f", rowBal)}"
                                                else if (rowBal < 0) "Adv: ₹${String.format("%,.2f", -rowBal)}"
                                                else "Bal: ₹0.00",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rowBal > 0) Color(0xFFC62828) else if (rowBal < 0) Color(0xFF2E7D32) else Color(0xFF757575),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    if (tx.notes.isNotBlank()) {
                                        Text(
                                            text = "Note: ${tx.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        val customerFinancialsMap by viewModel.customerFinancialsMap.collectAsState()
        val customerFin = customerFinancialsMap[customer.id]
        val isCustBlocked = customer.status.contains("Blocked", ignoreCase = true) || (customerFin?.isBlocked == true)
        val suggestedProducts by viewModel.suggestedProductNames.collectAsState()
        RecordTransactionDialog(
            customerName = customer.name,
            customerPhone = customer.phone,
            currentDues = balance,
            smsNotificationsEnabled = customer.smsNotificationsEnabled,
            isCustomerBlocked = isCustBlocked,
            productSuggestions = suggestedProducts,
            onDismiss = { showRecordDialog = false },
            onSaveSingle = { type, desc, q, unitType, qDouble, p, total, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    return@RecordTransactionDialog
                }
                viewModel.recordTransaction(
                    customerId = customer.id,
                    folderId = customer.folderId,
                    type = type,
                    itemDescription = desc,
                    quantity = q,
                    unitType = unitType,
                    quantityDouble = qDouble,
                    unitPrice = p,
                    totalAmount = total,
                    notes = notes
                )
                showRecordDialog = false
            },
            onSaveMultiple = { type, items, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    return@RecordTransactionDialog
                }
                viewModel.recordMultipleTransactions(
                    customerId = customer.id,
                    folderId = customer.folderId,
                    transactions = items
                )
                showRecordDialog = false
            }
        )
    }

    transactionToEdit?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            customerName = customer.name,
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                transactionToEdit = null
            }
        )
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Confirm Deletion",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete this entry (${tx.itemDescription})?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { transactionToDelete = null }
                ) {
                    Text("No", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
