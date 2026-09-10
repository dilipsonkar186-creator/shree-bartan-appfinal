package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.platform.LocalContext
import com.example.util.SmsUtils
import com.example.data.entity.TransactionEntity

val POPULAR_BRANDS_AND_PRODUCTS = listOf(
    "Hawkins Pressure Cooker 3L" to "pcs",
    "Hawkins Pressure Cooker 5L" to "pcs",
    "Prestige Induction Kadhai 24cm" to "pcs",
    "Futura Non-Stick Tawa" to "pcs",
    "Stainless Steel Kadhai Heavy" to "kg",
    "Milton Thermosteel Water Bottle" to "pcs",
    "Casserole Hot Pot Set (3 Pcs)" to "pcs",
    "Brass Pooja Thali Set" to "kg",
    "Copper Water Bottle 1000ml" to "pcs",
    "Stainless Steel Dinner Set (51 Pcs)" to "pcs",
    "Spoons & Forks Set (12 Pcs)" to "dz",
    "Handi Cooker With Lid" to "pcs",
    "Aluminium Bhagona / Topi" to "kg",
    "Steel Balti / Bucket" to "pcs",
    "Steel Tanki 50L" to "pcs",
    "Steel Tanki 100L" to "pcs",
    "Steel Dabbi / Dabba Set" to "pcs",
    "Non-Stick Fry Pan 22cm" to "pcs"
)

data class PaymentScrapItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val material: String,
    val weightKg: Double,
    val ratePerKg: Double
) {
    val totalAmount: Double get() = weightKg * ratePerKg
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordTransactionDialog(
    customerName: String,
    customerPhone: String = "",
    currentDues: Double = 0.0,
    smsNotificationsEnabled: Boolean = true,
    initialType: String = "GOODS_PROVIDED",
    isCustomerBlocked: Boolean = false,
    productSuggestions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSaveSingle: (
        type: String,
        itemDescription: String,
        quantity: Int,
        unitType: String,
        quantityDouble: Double,
        unitPrice: Double,
        totalAmount: Double,
        notes: String
    ) -> Unit,
    onSaveMultiple: ((type: String, items: List<TransactionEntity>, notes: String) -> Unit)? = null
) {
    val context = LocalContext.current
    // Receipt delivery channel selection: "WHATSAPP_BUSINESS", "SMS", "NONE"
    var selectedReceiptChannel by remember {
        mutableStateOf(
            if (customerPhone.isNotBlank() && smsNotificationsEnabled) "WHATSAPP_BUSINESS"
            else "NONE"
        )
    }

    fun dispatchReceipt(smsMsg: String) {
        when (selectedReceiptChannel) {
            "WHATSAPP_BUSINESS" -> {
                if (customerPhone.isNotBlank()) {
                    SmsUtils.openWhatsAppBusiness(context, customerPhone, smsMsg)
                } else {
                    SmsUtils.shareMessage(context, smsMsg)
                }
            }
            "SMS" -> {
                SmsUtils.openSmsComposer(context, customerPhone, smsMsg)
            }
            else -> {
                // Do not send message ("NONE")
            }
        }
    }

    var activeMode by remember {
        mutableStateOf(
            if (isCustomerBlocked) {
                "PAYMENT_DEPOSIT"
            } else {
                when (initialType) {
                    "GOODS_RETURNED" -> "GOODS_RETURNED"
                    "PAYMENT_DEPOSIT" -> "PAYMENT_DEPOSIT"
                    else -> "GOODS_PROVIDED"
                }
            }
        )
    }

    // YOU GAVE (Sale) Fields
    var productName by remember { mutableStateOf("") }
    var sellingUnitMethod by remember { mutableStateOf("pcs") } // "pcs", "kg", "dz"
    var qtyStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var selectedGst by remember { mutableStateOf("No GST") } // "No GST", "5%", "12%", "18%", "28%", "Custom"
    var customGstStr by remember { mutableStateOf("") }
    var manualFinalAmount by remember { mutableStateOf("") }
    var salePaymentMode by remember { mutableStateOf("Credit (Udhar)") } // "Credit (Udhar)", "Cash", "UPI / QR", "Bank Transfer", "Cheque", "Scrap / Metal"
    var billNo by remember { mutableStateOf("") }

    // ITEM RETURN Fields
    var returnProductName by remember { mutableStateOf("") }
    var returnUnitType by remember { mutableStateOf("pcs") } // "pcs", "kg", "dz", "set", "box"
    var returnQtyStr by remember { mutableStateOf("") }
    var returnRateStr by remember { mutableStateOf("") }
    var returnAmountStr by remember { mutableStateOf("") }
    var returnReason by remember { mutableStateOf("Customer Returned Item") }

    // YOU GOT (Payment) Fields
    var paymentAmountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") } // "Cash", "UPI / QR", "Bank Transfer", "Cheque", "Scrap / Metal", "Credit Adjustment"
    var receiptNo by remember { mutableStateOf("") }

    // Scrap / Metal fields
    var scrapMaterialType by remember { mutableStateOf("Aluminum Scrap") }
    var scrapWeightStr by remember { mutableStateOf("") }
    var scrapRateStr by remember { mutableStateOf("") }
    var paymentScrapItems by remember { mutableStateOf<List<PaymentScrapItem>>(emptyList()) }

    // Common Field
    var notesRemark by remember { mutableStateOf("") }
    var dialogGivenItems by remember { mutableStateOf<List<GivenItem>>(emptyList()) }
    var utensilsRemarks by remember { mutableStateOf("") }

    var showBrandSelectorDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }

    // Calculation logic for Sale
    val calculatedSaleAmount = remember(qtyStr, rateStr, selectedGst, customGstStr, dialogGivenItems, scrapWeightStr, scrapRateStr, salePaymentMode) {
        val qty = qtyStr.toDoubleOrNull() ?: 1.0
        val rate = rateStr.toDoubleOrNull() ?: 0.0

        val itemsGoodsTotal = dialogGivenItems.filter { it.totalAmount > 0 }.sumOf { it.totalAmount }
        val itemsScrapDeductions = dialogGivenItems.filter { it.totalAmount < 0 }.sumOf { -it.totalAmount }

        val currentInputBase = if (rate > 0) qty * rate else 0.0
        val grossGoodsBase = if (itemsGoodsTotal > 0) itemsGoodsTotal + currentInputBase else currentInputBase

        val directScrapValue = if (scrapWeightStr.isNotBlank()) {
            (scrapWeightStr.toDoubleOrNull() ?: 0.0) * (scrapRateStr.toDoubleOrNull() ?: 0.0)
        } else 0.0

        val totalScrapDeduction = itemsScrapDeductions + (if (itemsScrapDeductions == 0.0) directScrapValue else 0.0)

        val netBase = maxOf(0.0, grossGoodsBase - totalScrapDeduction)

        val gstPercent = when (selectedGst) {
            "5%" -> 5.0
            "12%" -> 12.0
            "18%" -> 18.0
            "28%" -> 28.0
            "Custom" -> customGstStr.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        netBase + (netBase * (gstPercent / 100.0))
    }

    val finalSaleAmountDouble = remember(manualFinalAmount, calculatedSaleAmount) {
        manualFinalAmount.toDoubleOrNull() ?: calculatedSaleAmount
    }

    // Calculation logic for Return
    val calculatedReturnAmount = remember(returnQtyStr, returnRateStr) {
        val qty = returnQtyStr.toDoubleOrNull() ?: 1.0
        val rate = returnRateStr.toDoubleOrNull() ?: 0.0
        qty * rate
    }

    val finalReturnAmountDouble = remember(returnAmountStr, calculatedReturnAmount) {
        returnAmountStr.toDoubleOrNull() ?: calculatedReturnAmount
    }

    val finalPaymentAmountDouble = remember(paymentAmountStr) {
        paymentAmountStr.toDoubleOrNull() ?: 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (activeMode) {
                                    "GOODS_PROVIDED" -> "New Credit / Sale Entry"
                                    "GOODS_RETURNED" -> "Item Return Entry (Deduction)"
                                    else -> "Payment Received (Jama)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (activeMode) {
                                    "GOODS_PROVIDED" -> Color(0xFFC62828)
                                    "GOODS_RETURNED" -> Color(0xFFE65100)
                                    else -> Color(0xFF2E7D32)
                                }
                            )
                            Text(
                                text = "Customer: $customerName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_sale_entry_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_sale_entry_button")) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Bottom Save Action Bar with Delivery Channel Selector (WhatsApp / SMS / None)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Receipt Channel Selector Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "रसीद व संदेश भेजें (Send Receipt):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (!smsNotificationsEnabled) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFEBEE),
                                            border = BorderStroke(0.6.dp, Color(0xFFEF9A9A))
                                        ) {
                                            Text(
                                                text = "🔕 ग्राहक का SMS बंद है",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC62828),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // 2 Channel Choices: WhatsApp Business and Text Message (SMS), with Off toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // WhatsApp Business Option
                                    FilterChip(
                                        selected = selectedReceiptChannel == "WHATSAPP_BUSINESS",
                                        onClick = {
                                            selectedReceiptChannel = if (selectedReceiptChannel == "WHATSAPP_BUSINESS") "NONE" else "WHATSAPP_BUSINESS"
                                        },
                                        label = { 
                                            Text(
                                                text = "WhatsApp Business",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ) 
                                        },
                                        leadingIcon = { Text("💼", fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE0F2F1),
                                            selectedLabelColor = Color(0xFF004D40)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedReceiptChannel == "WHATSAPP_BUSINESS",
                                            selectedBorderColor = Color(0xFF00796B),
                                            borderWidth = 1.2.dp
                                        ),
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    // Text Message / SMS Option
                                    FilterChip(
                                        selected = selectedReceiptChannel == "SMS",
                                        onClick = {
                                            selectedReceiptChannel = if (selectedReceiptChannel == "SMS") "NONE" else "SMS"
                                        },
                                        label = { 
                                            Text(
                                                text = "टेक्स्ट मैसेज (SMS)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ) 
                                        },
                                        leadingIcon = { Text("💬", fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE3F2FD),
                                            selectedLabelColor = Color(0xFF0D47A1)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedReceiptChannel == "SMS",
                                            selectedBorderColor = Color(0xFF1976D2),
                                            borderWidth = 1.2.dp
                                        ),
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // Off / None Option
                                    FilterChip(
                                        selected = selectedReceiptChannel == "NONE",
                                        onClick = { selectedReceiptChannel = "NONE" },
                                        label = { 
                                            Text(
                                                text = "बंद (Off)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ) 
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFEBEE),
                                            selectedLabelColor = Color(0xFFC62828)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedReceiptChannel == "NONE",
                                            selectedBorderColor = Color(0xFFEF9A9A),
                                            borderWidth = 1.dp
                                        ),
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                        }

                        // 2. Action Save Button based on activeMode
                        if (activeMode == "GOODS_PROVIDED") {
                            val cQty = qtyStr.toDoubleOrNull() ?: 1.0
                            val cRate = rateStr.toDoubleOrNull() ?: 0.0
                            val pendingItem = if (productName.isNotBlank() || cRate > 0) {
                                GivenItem(
                                    name = productName.ifBlank { "Goods / Cookware Item" },
                                    qty = cQty,
                                    unit = sellingUnitMethod,
                                    rate = cRate,
                                    totalAmount = cQty * cRate
                                )
                            } else null

                            val sWeight = scrapWeightStr.toDoubleOrNull() ?: 0.0
                            val sRate = scrapRateStr.toDoubleOrNull() ?: 0.0
                            val pendingScrapItem = if (sWeight > 0 && sRate > 0 && dialogGivenItems.none { it.totalAmount < 0 }) {
                                GivenItem(
                                    name = "Scrap Exchange ($scrapMaterialType)",
                                    qty = sWeight,
                                    unit = "kg",
                                    rate = sRate,
                                    totalAmount = -(sWeight * sRate)
                                )
                            } else null

                            val allItemsToSave = buildList {
                                addAll(dialogGivenItems)
                                if (pendingItem != null) add(pendingItem)
                                if (pendingScrapItem != null) add(pendingScrapItem)
                            }

                            val itemsTotal = if (manualFinalAmount.isNotBlank()) manualFinalAmount.toDoubleOrNull() ?: finalSaleAmountDouble
                            else finalSaleAmountDouble

                            Button(
                                onClick = {
                                    if (isCustomerBlocked) {
                                        Toast.makeText(
                                            context,
                                            "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है। केवल पेमेंट जमा (YOU GOT) कर सकते हैं।",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }
                                    val fullNotes = buildString {
                                        if (utensilsRemarks.isNotBlank()) append("Remarks: $utensilsRemarks | ")
                                        if (billNo.isNotBlank()) append("Bill No: $billNo | ")
                                        append("Mode: $salePaymentMode")
                                        if (selectedGst != "No GST") append(" | GST: $selectedGst")
                                        if (notesRemark.isNotBlank()) append(" | $notesRemark")
                                    }

                                    if (allItemsToSave.isNotEmpty()) {
                                        val entities = allItemsToSave.map { item ->
                                            val isDeduction = item.totalAmount < 0
                                            TransactionEntity(
                                                customerId = 0,
                                                folderId = 0,
                                                type = if (isDeduction) "GOODS_RETURNED" else "GOODS_PROVIDED",
                                                itemDescription = item.name,
                                                quantity = item.qty.toInt().coerceAtLeast(1),
                                                unitType = item.unit,
                                                quantityDouble = item.qty,
                                                unitPrice = item.rate,
                                                totalAmount = kotlin.math.abs(item.totalAmount),
                                                notes = fullNotes
                                            )
                                        }
                                        if (onSaveMultiple != null) {
                                            onSaveMultiple("GOODS_PROVIDED", entities, fullNotes)
                                        } else {
                                            entities.forEach { item ->
                                                onSaveSingle(
                                                    item.type,
                                                    item.itemDescription,
                                                    item.quantity,
                                                    item.unitType,
                                                    item.quantityDouble,
                                                    item.unitPrice,
                                                    item.totalAmount,
                                                    fullNotes
                                                )
                                            }
                                        }
                                        val smsMsg = SmsUtils.buildMultipleItemsTransactionSms(
                                            customerName = customerName,
                                            itemsList = entities,
                                            updatedDues = currentDues + itemsTotal,
                                            notes = fullNotes
                                        )
                                        dispatchReceipt(smsMsg)
                                    } else {
                                        val itemDesc = productName.ifBlank { "Goods / Cookware Item" }
                                        val qty = qtyStr.toDoubleOrNull() ?: 1.0
                                        val rate = rateStr.toDoubleOrNull() ?: 0.0
                                        val total = finalSaleAmountDouble

                                        if (total <= 0 && rate <= 0) {
                                            validationError = true
                                            return@Button
                                        }

                                        onSaveSingle(
                                            "GOODS_PROVIDED",
                                            itemDesc,
                                            qty.toInt().coerceAtLeast(1),
                                            sellingUnitMethod,
                                            qty,
                                            rate,
                                            total,
                                            fullNotes
                                        )

                                        val smsMsg = SmsUtils.buildSingleTransactionSms(
                                            customerName = customerName,
                                            type = "GOODS_PROVIDED",
                                            itemDescription = itemDesc,
                                            quantity = qty,
                                            unitType = sellingUnitMethod,
                                            unitPrice = rate,
                                            totalAmount = total,
                                            updatedDues = currentDues + total,
                                            notes = fullNotes
                                        )
                                        dispatchReceipt(smsMsg)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("save_credit_sale_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828), // Red
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Credit Sale (₹${String.format("%.2f", itemsTotal)})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (activeMode == "GOODS_RETURNED") {
                            Button(
                                onClick = {
                                    val itemDesc = returnProductName.ifBlank { "Returned Item" }
                                    val qty = returnQtyStr.toDoubleOrNull() ?: 1.0
                                    val rate = returnRateStr.toDoubleOrNull() ?: 0.0
                                    val total = finalReturnAmountDouble

                                    if (total <= 0 && rate <= 0) {
                                        validationError = true
                                        return@Button
                                    }

                                    val fullNotes = buildString {
                                        append("Reason: $returnReason")
                                        if (notesRemark.isNotBlank()) append(" | $notesRemark")
                                    }

                                    onSaveSingle(
                                        "GOODS_RETURNED",
                                        "RETURN: $itemDesc",
                                        qty.toInt().coerceAtLeast(1),
                                        returnUnitType,
                                        qty,
                                        rate,
                                        total,
                                        fullNotes
                                    )

                                    val smsMsg = SmsUtils.buildSingleTransactionSms(
                                        customerName = customerName,
                                        type = "GOODS_RETURNED",
                                        itemDescription = "RETURN: $itemDesc",
                                        quantity = qty,
                                        unitType = returnUnitType,
                                        unitPrice = rate,
                                        totalAmount = total,
                                        updatedDues = currentDues - total,
                                        notes = fullNotes
                                    )
                                    dispatchReceipt(smsMsg)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("save_return_item_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE65100), // Orange / Amber
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Item Return (-₹${String.format("%,.2f", finalReturnAmountDouble)})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    val total = finalPaymentAmountDouble
                                    if (total <= 0) {
                                        validationError = true
                                        return@Button
                                    }

                                    val finalScrapList = buildList {
                                        addAll(paymentScrapItems)
                                        val curW = scrapWeightStr.toDoubleOrNull() ?: 0.0
                                        val curR = scrapRateStr.toDoubleOrNull() ?: 0.0
                                        if (curW > 0 && curR > 0) {
                                            add(PaymentScrapItem(material = scrapMaterialType, weightKg = curW, ratePerKg = curR))
                                        }
                                    }

                                    val fullNotes = buildString {
                                        if (paymentMode == "Scrap / Metal") {
                                            if (finalScrapList.isNotEmpty()) {
                                                val scrapDetails = finalScrapList.joinToString("; ") {
                                                    "${it.material}: ${it.weightKg}kg @ ₹${it.ratePerKg}/kg = ₹${String.format("%.2f", it.totalAmount)}"
                                                }
                                                append("Scrap Items: $scrapDetails | ")
                                            } else {
                                                append("Scrap: $scrapMaterialType | ")
                                            }
                                        }
                                        if (receiptNo.isNotBlank()) append("Receipt No: $receiptNo | ")
                                        append("Mode: $paymentMode")
                                        if (notesRemark.isNotBlank()) append(" | $notesRemark")
                                    }

                                    val itemDesc = if (paymentMode == "Scrap / Metal") {
                                        if (finalScrapList.size > 1) {
                                            "Scrap Metal (${finalScrapList.joinToString(", ") { "${it.material} ${it.weightKg}kg" }})"
                                        } else if (finalScrapList.size == 1) {
                                            "Scrap Metal (${finalScrapList.first().material} ${finalScrapList.first().weightKg}kg)"
                                        } else {
                                            "Scrap Metal ($scrapMaterialType)"
                                        }
                                    } else {
                                        "Payment Received ($paymentMode)"
                                    }

                                    onSaveSingle(
                                        "PAYMENT_DEPOSIT",
                                        itemDesc,
                                        1,
                                        "pcs",
                                        1.0,
                                        total,
                                        total,
                                        fullNotes
                                    )

                                    val smsMsg = SmsUtils.buildSingleTransactionSms(
                                        customerName = customerName,
                                        type = "PAYMENT_DEPOSIT",
                                        itemDescription = itemDesc,
                                        quantity = 1.0,
                                        unitType = "pcs",
                                        unitPrice = total,
                                        totalAmount = total,
                                        updatedDues = currentDues - total,
                                        notes = fullNotes
                                    )
                                    dispatchReceipt(smsMsg)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("save_payment_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32), // Green
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Payment Received (₹${String.format("%.2f", finalPaymentAmountDouble)})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF9F9FC))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP TOGGLE TABS (YOU GAVE / RETURN / YOU GOT)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEDE7F6), // Light Lavender
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // YOU GAVE (Sale) Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        activeMode == "GOODS_PROVIDED" -> Color(0xFFC62828)
                                        isCustomerBlocked -> Color(0xFFEEEEEE)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (isCustomerBlocked) {
                                        Toast.makeText(
                                            context,
                                            "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है। केवल पेमेंट जमा (YOU GOT) कर सकते हैं।",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        activeMode = "GOODS_PROVIDED"
                                    }
                                }
                                .testTag("you_gave_sale_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCustomerBlocked) "🚫 GAVE (Blocked)" else "YOU GAVE (Sale)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    activeMode == "GOODS_PROVIDED" -> Color.White
                                    isCustomerBlocked -> Color(0xFF9E9E9E)
                                    else -> Color(0xFF4A148C)
                                }
                            )
                        }

                        // YOU GOT (Payment) Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeMode == "PAYMENT_DEPOSIT") Color(0xFF2E7D32) else Color.Transparent)
                                .clickable { activeMode = "PAYMENT_DEPOSIT" }
                                .testTag("you_got_payment_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "YOU GOT (Payment)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeMode == "PAYMENT_DEPOSIT") Color.White else Color(0xFF4A148C)
                            )
                        }
                    }
                }

                if (activeMode == "GOODS_PROVIDED") {
                    // 1. PRODUCT / ITEM NAME
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Product / Item Name",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C1338)
                            )

                            Row(
                                modifier = Modifier.clickable { showBrandSelectorDialog = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🛍️ Select Product / Brand",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A148C)
                                )
                            }
                        }

                        // Product Name Field with Auto-Capitalization & Instant History Suggestions
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = productName,
                                onValueChange = { input ->
                                    // Auto-capitalize first letter of each word
                                    val formatted = if (input.isNotEmpty()) {
                                        input.split(" ").joinToString(" ") { word ->
                                            if (word.isEmpty()) ""
                                            else word.replaceFirstChar { char -> char.uppercase() }
                                        }
                                    } else input
                                    productName = formatted
                                    if (validationError) validationError = false
                                },
                                placeholder = { Text("Enter Product Name (e.g., Tanki, Cooker)") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEDE7F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color(0xFF4A148C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (productName.isNotBlank()) {
                                        IconButton(onClick = { productName = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF4A148C)
                                )
                            )

                            // Dynamic auto-complete chips from history & saved items
                            val matchingSuggestions = remember(productName, productSuggestions) {
                                val query = productName.trim()
                                if (query.isEmpty()) {
                                    productSuggestions.take(4)
                                } else {
                                    productSuggestions.filter {
                                        it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true)
                                    }.take(6)
                                }
                            }

                            if (matchingSuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    matchingSuggestions.forEach { suggestion ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3E5F5),
                                            border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    productName = suggestion
                                                    if (validationError) validationError = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "✨ $suggestion",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF4A148C)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. SELLING UNIT METHOD
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Selling Unit Method",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C1338)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val units = listOf(
                                "pcs" to "📦 By Piece (Pcs)",
                                "kg" to "⚖️ By Weight (Kg)",
                                "dz" to "🔢 By Dozen (Dz)"
                            )

                            units.forEach { (code, label) ->
                                val selected = sellingUnitMethod == code
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFF4A148C) else Color(0xFFEDE7F6),
                                    shadowElevation = if (selected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { sellingUnitMethod = code }
                                        .testTag("unit_method_$code")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else Color(0xFF2C1338)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. QUANTITY & RATE INPUTS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Qty Input
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = {
                                Text(
                                    when (sellingUnitMethod) {
                                        "kg" -> "Qty (Kg)"
                                        "dz" -> "Qty (Dz)"
                                        else -> "Qty (Pcs)"
                                    }
                                )
                            },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qty_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Rate Input
                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = {
                                rateStr = it
                                if (validationError) validationError = false
                            },
                            label = {
                                Text(
                                    when (sellingUnitMethod) {
                                        "kg" -> "Rate (₹/Kg)"
                                        "dz" -> "Rate (₹/Dz)"
                                        else -> "Rate (₹/Piece)"
                                    }
                                )
                            },
                            placeholder = { Text("Rate (₹/$sellingUnitMethod)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rate_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // 2.1 CURRENT LINE TOTAL DISPLAY (Directly on this line's inputs)
                    val activeInputRate = rateStr.toDoubleOrNull() ?: 0.0
                    val activeInputQty = qtyStr.toDoubleOrNull() ?: 1.0
                    val activeLineTotal = activeInputQty * activeInputRate
                    if (activeInputRate > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEDE7F6),
                            border = BorderStroke(1.dp, Color(0xFFD1C4E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Line Total (${qtyStr.ifBlank { "1" }} $sellingUnitMethod × ₹${rateStr.ifBlank { "0" }}):",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4A148C)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", activeLineTotal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A148C)
                                )
                            }
                        }
                    }

                    // 3.1 ADD ITEM TO BILL BUTTON
                    OutlinedButton(
                        onClick = {
                            val qty = qtyStr.toDoubleOrNull() ?: 1.0
                            val rate = rateStr.toDoubleOrNull() ?: 0.0
                            if (productName.isNotBlank() || rate > 0) {
                                val name = productName.ifBlank { "Item ${dialogGivenItems.size + 1}" }
                                val newItem = GivenItem(
                                    name = name,
                                    qty = qty,
                                    unit = sellingUnitMethod,
                                    rate = rate,
                                    totalAmount = qty * rate
                                )
                                dialogGivenItems = dialogGivenItems + newItem
                                productName = ""
                                rateStr = ""
                                qtyStr = ""
                                validationError = false
                            } else {
                                validationError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("add_item_to_bill_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF4A148C)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4A148C)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dialogGivenItems.isEmpty()) "➕ Add Item to Bill" else "➕ Add More Items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3.2 ADDED ITEMS LIST WITH LINE-BY-LINE TOTALS
                    if (dialogGivenItems.isNotEmpty()) {
                        var cumulativeRunningTotal = 0.0
                        val itemsWithRunningTotal = dialogGivenItems.map { item ->
                            cumulativeRunningTotal += item.totalAmount
                            Pair(item, cumulativeRunningTotal)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3EDF7))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛒 Added Items (${dialogGivenItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A148C)
                            )

                            itemsWithRunningTotal.forEachIndexed { index, (item, runningTotalSoFar) ->
                                val isScrapDeduction = item.totalAmount < 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isScrapDeduction) Color(0xFFFFEBEE) else Color.White,
                                    border = BorderStroke(1.dp, if (isScrapDeduction) Color(0xFFEF9A9A) else Color(0xFFE0E0E0)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}. ${item.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isScrapDeduction) Color(0xFFB71C1C) else Color(0xFF2C1338)
                                                )
                                                if (isScrapDeduction) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFC62828)
                                                    ) {
                                                        Text(
                                                            text = "DEDUCTION",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Line calculation
                                            Text(
                                                text = "${item.qty} ${item.unit} × ₹${item.rate} = ${if (isScrapDeduction) "-" else ""}₹${String.format("%.2f", kotlin.math.abs(item.totalAmount))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isScrapDeduction) Color(0xFFC62828) else Color.DarkGray,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            // Line-by-line running total on this exact line
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (runningTotalSoFar >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Line Running Total: ₹${String.format("%.2f", runningTotalSoFar)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (runningTotalSoFar >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                dialogGivenItems = dialogGivenItems.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove item",
                                                tint = Color(0xFFC62828),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. GST TAX OPTION
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "GST Tax Option",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C1338)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("No GST", "5%", "12%", "18%", "28%", "Custom").forEach { gstLabel ->
                                val selected = selectedGst == gstLabel
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) Color(0xFF4A148C) else Color(0xFFEDE7F6),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedGst = gstLabel }
                                        .testTag("gst_chip_$gstLabel")
                                ) {
                                    Text(
                                        text = gstLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color(0xFF2C1338),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        if (selectedGst == "Custom") {
                            OutlinedTextField(
                                value = customGstStr,
                                onValueChange = { customGstStr = it },
                                label = { Text("Custom GST Percent (%)") },
                                placeholder = { Text("15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    // 5. FINAL CREDIT SALE AMOUNT (₹) *
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = if (manualFinalAmount.isNotBlank()) manualFinalAmount else if (calculatedSaleAmount > 0) String.format("%.2f", calculatedSaleAmount) else "",
                            onValueChange = { manualFinalAmount = it },
                            label = { Text("Final Credit Sale Amount (₹) *") },
                            placeholder = { Text("Final Credit Sale Amount (₹) *") },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF4A148C),
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("final_sale_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF4A148C)
                            )
                        )
                    }

                    // 6. SALE PAYMENT MODE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Sale Payment Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C1338)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf(
                                "Credit (Udhar)" to "💳 Credit (Udhar)",
                                "Cash" to "💵 Cash",
                                "UPI / QR" to "📱 UPI / QR",
                                "Bank Transfer" to "🏦 Bank Transfer",
                                "Cheque" to "📝 Cheque"
                            )

                            modes.forEach { (code, label) ->
                                val selected = salePaymentMode == code
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) Color(0xFFC62828) else Color(0xFFEDE7F6),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { salePaymentMode = code }
                                        .testTag("payment_mode_$code")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color(0xFF2C1338),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 7. NOTES / REMARK & BILL NO.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = notesRemark,
                            onValueChange = { notesRemark = it },
                            label = { Text("Notes / Remark") },
                            placeholder = { Text("Optional notes...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = billNo,
                            onValueChange = { billNo = it },
                            label = { Text("Bill No.") },
                            placeholder = { Text("e.g. INV-102") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_no_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                } else if (activeMode == "GOODS_RETURNED") {
                    // ITEM RETURN FORM (DEDUCTION FROM TRANSACTION)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Info Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Item Return Entry (Deduction)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "The returned item value will be deducted from total goods and net customer dues balance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFBF360C)
                                    )
                                }
                            }
                        }

                        // Product Name Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Returned Product / Item Name *",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C1338)
                                )

                                TextButton(
                                    onClick = { showBrandSelectorDialog = true }
                                ) {
                                    Text(
                                        text = "🛍️ Select Product",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = returnProductName,
                                onValueChange = { input ->
                                    val formatted = if (input.isNotEmpty()) {
                                        input.split(" ").joinToString(" ") { word ->
                                            if (word.isEmpty()) ""
                                            else word.replaceFirstChar { char -> char.uppercase() }
                                        }
                                    } else input
                                    returnProductName = formatted
                                },
                                placeholder = { Text("e.g. Tanki 50L, Hawkins Cooker 3L") },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("return_product_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFFE65100)
                                )
                            )
                        }

                        // Return Quantity & Unit Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Return Quantity & Unit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C1338)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val returnUnits = listOf("kg" to "Per Kg", "pcs" to "Per Piece", "dz" to "Dozen")
                                returnUnits.forEach { (code, label) ->
                                    val selected = returnUnitType == code
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) Color(0xFFE65100) else Color(0xFFEDE7F6),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { returnUnitType = code }
                                            .testTag("return_unit_$code")
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else Color(0xFF2C1338),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = returnQtyStr,
                                    onValueChange = { returnQtyStr = it },
                                    label = { Text("Return Qty") },
                                    placeholder = { Text("1") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("return_qty_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = returnRateStr,
                                    onValueChange = { returnRateStr = it },
                                    label = { Text("Rate / Unit (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("return_rate_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }

                        // Return Amount Field
                        OutlinedTextField(
                            value = if (returnAmountStr.isNotBlank()) returnAmountStr else if (calculatedReturnAmount > 0) String.format("%.2f", calculatedReturnAmount) else "",
                            onValueChange = { returnAmountStr = it },
                            label = { Text("Total Return Amount (₹) *") },
                            placeholder = { Text("Return value to deduct...") },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("return_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFE65100)
                            )
                        )

                        // Quick Return Reason Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Return Reason / Condition",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C1338)
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val reasons = listOf("Defective Item", "Wrong Size / Spec", "Exchanged Item", "Customer Returned", "Damaged Goods")
                                reasons.forEach { reason ->
                                    val selected = returnReason == reason
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) Color(0xFFE65100) else Color(0xFFEDE7F6),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { returnReason = reason }
                                            .testTag("reason_chip_$reason")
                                    ) {
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else Color(0xFF2C1338),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notes / Remarks
                        OutlinedTextField(
                            value = notesRemark,
                            onValueChange = { notesRemark = it },
                            label = { Text("Notes / Additional Remarks") },
                            placeholder = { Text("Optional return notes...") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("return_notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                } else {
                    // PAYMENT MODE FORM (YOU GOT PAYMENT)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Payment Mode Received",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C1338)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf(
                                "Cash" to "💵 Cash",
                                "UPI / QR" to "📱 UPI / QR",
                                "Scrap / Metal" to "♻️ Scrap / Metal",
                                "Bank Transfer" to "🏦 Bank Transfer",
                                "Cheque" to "📝 Cheque",
                                "Credit Adjustment" to "💳 Credit Adjustment"
                            )

                            modes.forEach { (code, label) ->
                                val selected = paymentMode == code
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) Color(0xFF2E7D32) else Color(0xFFEDE7F6),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { paymentMode = code }
                                        .testTag("payment_recv_mode_$code")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color(0xFF2C1338),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        if (paymentMode == "Scrap / Metal") {
                            val updatePaymentFromScrap = { items: List<PaymentScrapItem>, wStr: String, rStr: String ->
                                val addedSum = items.sumOf { it.totalAmount }
                                val w = wStr.toDoubleOrNull() ?: 0.0
                                val r = rStr.toDoubleOrNull() ?: 0.0
                                val totalVal = addedSum + (if (w > 0 && r > 0) w * r else 0.0)
                                if (totalVal > 0) {
                                    paymentAmountStr = String.format("%.2f", totalVal)
                                } else if (items.isEmpty()) {
                                    paymentAmountStr = ""
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "♻️",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Scrap / Metal Received as Payment",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20)
                                        )
                                    }

                                    Text(
                                        text = "Select Scrap Material:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF49454F)
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val scrapMaterials = listOf(
                                            "Aluminum Scrap",
                                            "Copper Scrap",
                                            "Stainless Steel Scrap",
                                            "Brass Scrap",
                                            "Lead / Zinc Scrap",
                                            "Other Scrap"
                                        )

                                        scrapMaterials.forEach { material ->
                                            val isSelected = scrapMaterialType == material
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Color(0xFFE8DEF8) else Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF6750A4) else Color(0xFFE0E0E0)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { scrapMaterialType = material }
                                                    .testTag("scrap_material_$material")
                                            ) {
                                                Text(
                                                    text = material,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = scrapWeightStr,
                                            onValueChange = {
                                                scrapWeightStr = it
                                                updatePaymentFromScrap(paymentScrapItems, it, scrapRateStr)
                                            },
                                            label = { Text("Weight (Kg)") },
                                            placeholder = { Text("0.0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("scrap_weight_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )

                                        OutlinedTextField(
                                            value = scrapRateStr,
                                            onValueChange = {
                                                scrapRateStr = it
                                                updatePaymentFromScrap(paymentScrapItems, scrapWeightStr, it)
                                            },
                                            label = { Text("Rate (₹/Kg)") },
                                            placeholder = { Text("0.0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("scrap_rate_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )
                                    }

                                    // Button to Add Scrap Item to List ("Add More Items")
                                    val canAddCurrentScrap = (scrapWeightStr.toDoubleOrNull() ?: 0.0) > 0 && (scrapRateStr.toDoubleOrNull() ?: 0.0) > 0

                                    Button(
                                        onClick = {
                                            val w = scrapWeightStr.toDoubleOrNull() ?: 0.0
                                            val r = scrapRateStr.toDoubleOrNull() ?: 0.0
                                            if (w > 0 && r > 0) {
                                                val newItem = PaymentScrapItem(
                                                    material = scrapMaterialType,
                                                    weightKg = w,
                                                    ratePerKg = r
                                                )
                                                val newList = paymentScrapItems + newItem
                                                paymentScrapItems = newList
                                                scrapWeightStr = ""
                                                scrapRateStr = ""
                                                updatePaymentFromScrap(newList, "", "")
                                            }
                                        },
                                        enabled = canAddCurrentScrap,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_scrap_more_items_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6750A4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (paymentScrapItems.isEmpty()) "➕ Add Scrap Item" else "➕ Add Scrap / More Items",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // List of Added Scrap Items
                                    if (paymentScrapItems.isNotEmpty()) {
                                        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                                        Text(
                                            text = "Added Scrap Items (${paymentScrapItems.size}):",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF21005D)
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            paymentScrapItems.forEachIndexed { idx, item ->
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${idx + 1}. ${item.material}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1D1B20)
                                                            )
                                                            Text(
                                                                text = "${item.weightKg} kg × ₹${item.ratePerKg}/kg = ₹${String.format("%.2f", item.totalAmount)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF49454F)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                val newList = paymentScrapItems.filter { it.id != item.id }
                                                                paymentScrapItems = newList
                                                                updatePaymentFromScrap(newList, scrapWeightStr, scrapRateStr)
                                                            },
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .testTag("remove_scrap_item_${item.id}")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Remove item",
                                                                tint = Color(0xFFB3261E),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFE8DEF8)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Total Scrap Value:",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF21005D)
                                                )
                                                Text(
                                                    text = "₹${String.format("%.2f", paymentScrapItems.sumOf { it.totalAmount })}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = paymentAmountStr,
                            onValueChange = {
                                paymentAmountStr = it
                                if (validationError) validationError = false
                            },
                            label = { Text("Payment Amount Received (₹) *") },
                            placeholder = { Text("Payment Amount Received (₹) *") },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2E7D32)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = notesRemark,
                                onValueChange = { notesRemark = it },
                                label = { Text("Notes / Remark") },
                                placeholder = { Text("e.g. Jama payment...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_notes_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = receiptNo,
                                onValueChange = { receiptNo = it },
                                label = { Text("Ref / Receipt No.") },
                                placeholder = { Text("e.g. REC-801") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("receipt_no_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (validationError) {
                    Text(
                        text = "Please enter valid item/product name and amount or rate.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // SELECT PRODUCT / BRAND PRESET DIALOG
    if (showBrandSelectorDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val allAvailableProducts = remember(productSuggestions) {
            val presetMap = POPULAR_BRANDS_AND_PRODUCTS.toMap()
            val customItems = productSuggestions.map { it to (presetMap[it] ?: "pcs") }
            val presets = POPULAR_BRANDS_AND_PRODUCTS
            (customItems + presets).distinctBy { it.first.lowercase() }
        }
        val filteredProducts = remember(searchQuery, allAvailableProducts) {
            if (searchQuery.isBlank()) allAvailableProducts
            else allAvailableProducts.filter {
                it.first.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showBrandSelectorDialog = false },
            title = {
                Text("Select Product / Brand", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search products / brands...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredProducts) { (pName, defaultUnit) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        productName = pName
                                        returnProductName = pName
                                        sellingUnitMethod = defaultUnit
                                        showBrandSelectorDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF4A148C)
                                    ) {
                                        Text(
                                            text = defaultUnit.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrandSelectorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
