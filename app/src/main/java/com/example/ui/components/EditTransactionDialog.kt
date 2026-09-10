package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.TransactionEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    customerName: String,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val isGoods = transaction.type == "GOODS_PROVIDED" || transaction.type == "GOODS_RETURNED"
    val isReturn = transaction.type == "GOODS_RETURNED"

    var itemDescription by remember { mutableStateOf(transaction.itemDescription) }
    var unitType by remember { mutableStateOf(if (transaction.unitType.isNotBlank()) transaction.unitType else "pcs") }
    var quantityStr by remember {
        mutableStateOf(
            if (transaction.quantityDouble > 0) {
                String.format("%.2f", transaction.quantityDouble).removeSuffix(".00").removeSuffix("0")
            } else if (transaction.quantity > 0) {
                transaction.quantity.toString()
            } else {
                "1"
            }
        )
    }
    var unitPriceStr by remember {
        mutableStateOf(
            if (transaction.unitPrice > 0) {
                String.format("%.2f", transaction.unitPrice).removeSuffix(".00")
            } else {
                ""
            }
        )
    }
    var totalAmountStr by remember {
        mutableStateOf(
            if (transaction.totalAmount > 0) {
                String.format("%.2f", transaction.totalAmount).removeSuffix(".00")
            } else {
                ""
            }
        )
    }
    var notes by remember { mutableStateOf(transaction.notes) }
    var manualOverrideAmount by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    // Common standard units in utensil & retail businesses
    val standardUnits = listOf(
        "pcs" to "Pcs (नग)",
        "kg" to "Kg (किलो)",
        "gm" to "Gm (ग्राम)",
        "dz" to "Dozen (दर्जन)",
        "set" to "Set (सेट)",
        "box" to "Box (डिब्बा)",
        "pkt" to "Pkt (पैकेट)",
        "ltr" to "Ltr (लीटर)",
        "mtr" to "Mtr (मीटर)"
    )

    // Auto-calculate Total Amount when Qty or Unit Price changes (unless manually overridden)
    LaunchedEffect(quantityStr, unitPriceStr, manualOverrideAmount) {
        if (isGoods && !manualOverrideAmount) {
            val q = quantityStr.toDoubleOrNull() ?: 0.0
            val p = unitPriceStr.toDoubleOrNull() ?: 0.0
            if (q > 0 && p > 0) {
                val total = q * p
                totalAmountStr = String.format("%.2f", total).removeSuffix(".00")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGoods) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGoods) Icons.Default.ShoppingBag else Icons.Default.Payment,
                                contentDescription = null,
                                tint = if (isGoods) Color(0xFFC62828) else Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isGoods) "Edit Goods Entry" else "Edit Payment Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = customerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (validationError.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (isGoods) {
                    // Product / Goods Description
                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = { input ->
                            val formatted = if (input.isNotEmpty()) {
                                input.split(" ").joinToString(" ") { word ->
                                    if (word.isEmpty()) ""
                                    else word.replaceFirstChar { char -> char.uppercase() }
                                }
                            } else input
                            itemDescription = formatted
                            if (validationError.isNotBlank()) validationError = ""
                        },
                        label = { Text("Product / Goods Name *") },
                        placeholder = { Text("e.g. Tanki 50L, Hawkins Cooker 5L...") },
                        leadingIcon = {
                            Icon(Icons.Default.Inventory2, contentDescription = null)
                        },
                        trailingIcon = {
                            if (itemDescription.isNotBlank()) {
                                IconButton(onClick = { itemDescription = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tx_item_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Unit Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Unit Type (इकाई):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            standardUnits.forEach { (unitKey, unitLabel) ->
                                val isSelected = unitType.equals(unitKey, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { unitType = unitKey },
                                    label = { Text(unitLabel, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                )
                            }
                        }
                    }

                    // Quantity and Steppers
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Quantity (मात्रा):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Decrement button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        val current = quantityStr.toDoubleOrNull() ?: 1.0
                                        if (current > 1) {
                                            val next = current - 1
                                            quantityStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format("%.2f", next).removeSuffix(".00")
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = {
                                    quantityStr = it
                                    if (validationError.isNotBlank()) validationError = ""
                                },
                                label = { Text("Qty ($unitType)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_tx_qty"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Increment button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        val current = quantityStr.toDoubleOrNull() ?: 0.0
                                        val next = current + 1
                                        quantityStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format("%.2f", next).removeSuffix(".00")
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Rate / Price per unit
                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = {
                            unitPriceStr = it
                            if (validationError.isNotBlank()) validationError = ""
                        },
                        label = { Text("Rate / Price per $unitType (₹) *") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tx_rate"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Total Amount Preview & Override
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReturn) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)
                        ),
                        border = BorderStroke(1.dp, if (isReturn) Color(0xFFFFB74D) else Color(0xFFFFCDD2))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Amount (कुल राशि)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isReturn) Color(0xFFE65100) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                    val q = quantityStr.toDoubleOrNull() ?: 0.0
                                    val p = unitPriceStr.toDoubleOrNull() ?: 0.0
                                    Text(
                                        text = "$q $unitType × ₹$p",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }

                                Text(
                                    text = "₹${totalAmountStr.ifBlank { "0.00" }}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReturn) Color(0xFFE65100) else Color(0xFFC62828)
                                )
                            }

                            // Option to manually adjust total if needed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Direct amount edit (सीधा राशि बदलें)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = manualOverrideAmount,
                                    onCheckedChange = { manualOverrideAmount = it }
                                )
                            }

                            AnimatedVisibility(visible = manualOverrideAmount) {
                                OutlinedTextField(
                                    value = totalAmountStr,
                                    onValueChange = {
                                        totalAmountStr = it
                                        if (validationError.isNotBlank()) validationError = ""
                                    },
                                    label = { Text("Custom Total (₹)") },
                                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // PAYMENT RECEIVED EDIT
                    OutlinedTextField(
                        value = totalAmountStr,
                        onValueChange = {
                            totalAmountStr = it
                            if (validationError.isNotBlank()) validationError = ""
                        },
                        label = { Text("Payment Received Amount (₹) *") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tx_payment_amount"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = { itemDescription = it },
                        label = { Text("Payment Mode / Title") },
                        placeholder = { Text("e.g. Cash, UPI / QR, GPay, Bank Transfer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Notes / Remarks
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarks (टिप्पणी / विवरण)") },
                    placeholder = { Text("Optional bill number, details, warranty...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_notes"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDesc = itemDescription.trim().ifBlank {
                        if (isGoods) "Goods Item" else "Payment Received"
                    }
                    val finalQtyDouble = quantityStr.toDoubleOrNull() ?: 1.0
                    val finalQtyInt = if (finalQtyDouble > 0) finalQtyDouble.toInt() else 1
                    val finalRate = unitPriceStr.toDoubleOrNull() ?: 0.0
                    val finalTotal = totalAmountStr.toDoubleOrNull() ?: (finalQtyDouble * finalRate)

                    if (finalTotal <= 0) {
                        validationError = "Please enter a valid amount greater than 0"
                        return@Button
                    }

                    val updated = transaction.copy(
                        itemDescription = finalDesc,
                        quantity = finalQtyInt,
                        unitType = unitType.trim().lowercase(),
                        quantityDouble = finalQtyDouble,
                        unitPrice = finalRate,
                        totalAmount = finalTotal,
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGoods) Color(0xFFC62828) else Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_edit_tx_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
