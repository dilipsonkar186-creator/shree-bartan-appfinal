package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class GivenItem(
    val name: String,
    val qty: Double,
    val unit: String,
    val rate: Double,
    val totalAmount: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GiveToCustomerSection(
    givenItems: List<GivenItem>,
    onItemsChange: (List<GivenItem>) -> Unit,
    remarks: String,
    onRemarksChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Input state
    var itemName by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var rateStr by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("pcs") } // pcs, kg, gm, mt
    var itemError by remember { mutableStateOf(false) }
    var itemToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val currentQty = qtyStr.toDoubleOrNull() ?: 1.0
    val currentRate = rateStr.toDoubleOrNull() ?: 0.0
    val currentSubtotal = currentQty * currentRate

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFAFAFA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: 🛒 Give to Customer (दी जा रही सामग्री का विवरण)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Give to Customer (दी जा रही सामग्री का विवरण)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }

            // 1. Item Name / सामान Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        if (itemError && it.isNotBlank()) itemError = false
                    },
                    placeholder = { Text("Item Name / सामान (e.g. Cooker, Wire, Thali)") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4A148C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    isError = itemError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("given_item_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFC62828)
                    )
                )

                // Quick item chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val quickItems = listOf("Steel Bartan Set", "Pressure Cooker", "Copper Wire", "Aluminium Utensils")
                    quickItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1C4E9)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { itemName = item }
                                .testTag("quick_item_$item")
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4A148C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 2. Qty / मात्रा & Rate / दर (₹)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Qty / मात्रा") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("given_qty_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Rate / दर (₹)") },
                    placeholder = { Text("Rate (₹)") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("given_rate_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // 3. Item Subtotal Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Item Subtotal:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "₹${String.format("%,.0f", currentSubtotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                }
            }

            // 5. + Add Item to Given List (सामान जोड़ें) Button
            Button(
                onClick = {
                    val nameToUse = itemName.ifBlank { "Goods / Materials" }
                    val rate = currentRate
                    val qty = currentQty
                    val total = currentSubtotal
                    if (nameToUse.isNotBlank() && (rate > 0 || total > 0)) {
                        val newItem = GivenItem(
                            name = nameToUse,
                            qty = qty,
                            unit = selectedUnit,
                            rate = rate,
                            totalAmount = if (total > 0) total else rate * qty
                        )
                        onItemsChange(givenItems + newItem)
                        itemName = ""
                        rateStr = ""
                        qtyStr = "1"
                        itemError = false
                    } else {
                        itemError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("add_item_to_given_list_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDE7F6),
                    contentColor = Color(0xFF4A148C)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Add Item to Given List (सामान जोड़ें)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Display List of Added Items with Line-by-Line Totals
            if (givenItems.isNotEmpty()) {
                var cumulativeRunningTotal = 0.0
                val itemsWithRunningTotal = givenItems.map { item ->
                    cumulativeRunningTotal += item.totalAmount
                    Pair(item, cumulativeRunningTotal)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Added Given Items (${givenItems.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C1338)
                    )
                    itemsWithRunningTotal.forEachIndexed { idx, (item, runningTotalSoFar) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${item.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2C1338)
                                    )
                                    Text(
                                        text = "${item.qty} ${item.unit} × ₹${item.rate} = ₹${String.format("%.2f", item.totalAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF757575),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFEBEE),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "Line Running Total: ₹${String.format("%.2f", runningTotalSoFar)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        itemToDeleteIndex = idx
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove item",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Total Dues Created / कुल उधारी Summary Box
            val displayedTotalDues = if (givenItems.isNotEmpty()) {
                givenItems.sumOf { it.totalAmount }
            } else {
                currentSubtotal
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFEBEE),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Dues Created / कुल उधारी:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = "Customer Owes Store",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB71C1C)
                        )
                    }

                    Text(
                        text = "₹${String.format("%,.0f", displayedTotalDues)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                }
            }

            // 7. Utensils / Metal Preferences & Remarks Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = null,
                            tint = Color(0xFF4A148C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Utensils / Metal Preferences & Remarks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C)
                        )
                    }

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = onRemarksChange,
                        placeholder = { Text("Enter metal preferences, weight details or special instructions...") },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("utensils_remarks_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            }
        }
    }

    itemToDeleteIndex?.let { idx ->
        val item = givenItems.getOrNull(idx)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { itemToDeleteIndex = null },
            title = {
                Text(
                    text = "Confirm Deletion",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${item?.name ?: "this item"}' from the given list?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onItemsChange(givenItems.filterIndexed { i, _ -> i != idx })
                        itemToDeleteIndex = null
                    },
                    modifier = Modifier.testTag("confirm_delete_item_yes")
                ) {
                    Text("Yes", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { itemToDeleteIndex = null },
                    modifier = Modifier.testTag("confirm_delete_item_no")
                ) {
                    Text("No", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
