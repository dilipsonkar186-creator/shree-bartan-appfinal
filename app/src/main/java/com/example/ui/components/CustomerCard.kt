package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.CustomerEntity
import com.example.ui.viewmodel.FolderViewModel

private fun openGoogleMaps(context: Context, customer: CustomerEntity) {
    val lat = customer.latitude
    val lng = customer.longitude
    val address = customer.address

    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
        com.example.util.LocationUtils.openGoogleMaps(context, lat, lng, customer.name.ifBlank { address })
    } else if (address.isNotBlank()) {
        try {
            val encodedAddress = Uri.encode(address)
            val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val genericIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (genericIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(genericIntent)
                } else {
                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                }
            }
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address))
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    } else {
        Toast.makeText(context, "Location or address not available", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    viewModel: FolderViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCustomerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showLedgerDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val avatarBgColor = MaterialTheme.colorScheme.primaryContainer
    val avatarContentColor = MaterialTheme.colorScheme.onPrimaryContainer

    val initialLetter = if (customer.name.isNotBlank()) {
        customer.name.trim().first().uppercaseChar().toString()
    } else "?"

    val transactions by viewModel.getTransactionsForCustomerFlow(customer.id)
        .collectAsState(initial = emptyList())

    val totalGoodsProvided = remember(transactions) {
        transactions.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
    }
    val totalGoods = totalGoodsProvided
    val totalPaid = remember(transactions) {
        transactions.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
    }
    val dues = totalGoods - totalPaid

    val customerFinancialsMap by viewModel.customerFinancialsMap.collectAsStateWithLifecycle()
    val filterMonth by viewModel.customerFilterMonth.collectAsStateWithLifecycle()
    val customerFin = customerFinancialsMap[customer.id]

    val monthNames = remember { arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }
    val mLabel = if (filterMonth < 0) "This Month" else monthNames.getOrElse(filterMonth) { "This Month" }

    val hasLocation = customer.address.isNotBlank() || (customer.latitude != null && customer.longitude != null)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCustomerClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Tapping Avatar or Customer Name opens the Customer Detail page
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCustomerClick() }
                        .padding(vertical = 2.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Profile Avatar / Picture
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(avatarBgColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                            .clickable(enabled = !customer.photoUri.isNullOrEmpty()) {
                                showPhotoPreview = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!customer.photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = customer.photoUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = initialLetter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = avatarContentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Customer Info Block
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Customer Name - fully visible, wraps across lines if long
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val hasBook = customer.bookNumber.isNotBlank()
                        val hasPage = customer.pageNumber.isNotBlank()
                        if (hasBook || hasPage || customer.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (hasBook || hasPage) {
                                    val badgeText = buildString {
                                        if (hasBook) append("Bk ${customer.bookNumber}")
                                        if (hasBook && hasPage) append(" • ")
                                        if (hasPage) append("Pg ${customer.pageNumber}")
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                if (customer.phone.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Financial Dues Summary Tag
                        Text(
                            text = if (dues > 0) "Dues: ₹${String.format("%.2f", dues)}"
                            else if (totalGoods > 0) "Paid in Full (₹${String.format("%.2f", totalGoods)})"
                            else "No transactions recorded",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )

                        // Blocked status badge or Monthly Payment Badge Chip
                        val isCustBlocked = customer.status.contains("Blocked", ignoreCase = true) || (customerFin?.isBlocked == true)
                        if (isCustBlocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = Color(0xFFC62828)
                                    )
                                    Text(
                                        text = if (customerFin?.isOverdue3Months == true) "🚫 BLOCKED (3+ M Overdue)" else "🚫 BLOCKED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        } else if (customerFin != null) {
                            if (customerFin.hasReceivedPaymentInMonth) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "$mLabel Paid: ₹${String.format("%.2f", customerFin.monthPaid)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            } else if (dues > 0.01) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "No Payment in $mLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3-dots Options Menu on Right
                Box(
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        val smsEnabled = customer.smsNotificationsEnabled
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (smsEnabled) "SMS/संदेश: ON" else "SMS/संदेश: OFF",
                                        fontWeight = FontWeight.Bold,
                                        color = if (smsEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                    Switch(
                                        checked = smsEnabled,
                                        onCheckedChange = { newStatus ->
                                            viewModel.updateCustomer(customer.copy(smsNotificationsEnabled = newStatus))
                                            Toast.makeText(
                                                context,
                                                if (newStatus) "${customer.name} के लिए SMS चालू किया गया" else "${customer.name} के लिए SMS बंद किया गया",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Message,
                                    contentDescription = null,
                                    tint = if (smsEnabled) Color(0xFF2E7D32) else Color.Gray
                                )
                            },
                            onClick = {
                                val newStatus = !customer.smsNotificationsEnabled
                                viewModel.updateCustomer(customer.copy(smsNotificationsEnabled = newStatus))
                                Toast.makeText(
                                    context,
                                    if (newStatus) "${customer.name} के लिए SMS चालू किया गया" else "${customer.name} के लिए SMS बंद किया गया",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Goods / Bill") },
                            leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF4A148C)) },
                            onClick = {
                                showMenu = false
                                showRecordDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Transaction History & Goods") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showLedgerDialog = true
                            }
                        )
                            DropdownMenuItem(
                                text = { Text("View Full Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    expanded = true
                                }
                            )
                            if (hasLocation) {
                                DropdownMenuItem(
                                    text = { Text("Open in Google Maps") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        openGoogleMaps(context, customer)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit Customer") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            val isCustomerBlocked = customer.status.contains("Blocked", ignoreCase = true) || (customerFin?.isBlocked == true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isCustomerBlocked) "🔓 Unblock Customer (अनब्लॉक करें)" else "🚫 Block Customer (ब्लॉक करें)",
                                        color = if (isCustomerBlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCustomerBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isCustomerBlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    val newBlockState = !isCustomerBlocked
                                    viewModel.setCustomerBlockStatus(customer, newBlockState)
                                    Toast.makeText(
                                        context,
                                        if (newBlockState) "${customer.name} ब्लॉक किया गया" else "${customer.name} अनब्लॉक/सक्रिय किया गया",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📁 Move to Folder (फोल्डर बदलें)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DriveFileMove,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showMoveDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Customer", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

            // Quick Chips Row below header
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tap to view history
                AssistChip(
                    onClick = { showLedgerDialog = true },
                    label = { Text("History & Goods (₹${String.format("%.0f", totalGoods)})", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )

                if (hasLocation) {
                    AssistChip(
                        onClick = { openGoogleMaps(context, customer) },
                        label = { Text("Google Maps", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Open location in Google Maps",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                if (customer.phone.isNotBlank()) {
                    AssistChip(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                            context.startActivity(intent)
                        },
                        label = { Text("Call", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Customer",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            // Expandable full customer details block (shown when tapping customer name)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Full Customer Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (customer.phone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (customer.bookNumber.isNotBlank() || customer.pageNumber.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val text = buildString {
                                        if (customer.bookNumber.isNotBlank()) append("Book No: ${customer.bookNumber}")
                                        if (customer.bookNumber.isNotBlank() && customer.pageNumber.isNotBlank()) append(" | ")
                                        if (customer.pageNumber.isNotBlank()) append("Page No: ${customer.pageNumber}")
                                    }
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (customer.address.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { openGoogleMaps(context, customer) }
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = customer.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to open location in Google Maps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (customer.latitude != null && customer.longitude != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { openGoogleMaps(context, customer) }
                                ) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GPS: ${String.format("%.4f", customer.latitude)}, ${String.format("%.4f", customer.longitude)} (Open Maps)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (customer.documentType.isNotBlank() || !customer.documentPhotoUri.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Column {
                                            Text(
                                                text = "${customer.documentType.ifBlank { "ID Verification" }} Verified ✓",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            if (customer.documentNumber.isNotBlank()) {
                                                Text(
                                                    text = "ID No: ${customer.documentNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (customer.notes.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = customer.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Direct Action Buttons inside expanded view
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasLocation) {
                            OutlinedButton(
                                onClick = { openGoogleMaps(context, customer) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Maps")
                            }
                        }

                        OutlinedButton(
                            onClick = { showLedgerDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transaction History")
                        }
                    }
                }
            }
        }
    }

    if (showLedgerDialog) {
        CustomerLedgerDialog(
            customer = customer,
            viewModel = viewModel,
            onDismiss = { showLedgerDialog = false }
        )
    }

    if (showRecordDialog) {
        val isCustBlocked = customer.status.contains("Blocked", ignoreCase = true) || (customerFin?.isBlocked == true)
        val suggestedProducts by viewModel.suggestedProductNames.collectAsStateWithLifecycle()
        RecordTransactionDialog(
            customerName = customer.name,
            customerPhone = customer.phone,
            currentDues = dues,
            smsNotificationsEnabled = customer.smsNotificationsEnabled,
            isCustomerBlocked = isCustBlocked,
            productSuggestions = suggestedProducts,
            onDismiss = { showRecordDialog = false },
            onSaveSingle = { type, itemDescription, quantity, unitType, quantityDouble, unitPrice, totalAmount, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    Toast.makeText(context, "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है।", Toast.LENGTH_LONG).show()
                    return@RecordTransactionDialog
                }
                viewModel.recordTransaction(
                    customerId = customer.id,
                    folderId = customer.folderId,
                    type = type,
                    itemDescription = itemDescription,
                    quantity = quantity,
                    unitType = unitType,
                    quantityDouble = quantityDouble,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    notes = notes
                )
                showRecordDialog = false
            },
            onSaveMultiple = { type, items, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    Toast.makeText(context, "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है।", Toast.LENGTH_LONG).show()
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

    if (showPhotoPreview && !customer.photoUri.isNullOrEmpty()) {
        PhotoPreviewDialog(
            photoUri = customer.photoUri,
            title = customer.name,
            subtitle = if (customer.phone.isNotBlank()) customer.phone else null,
            onDismiss = { showPhotoPreview = false }
        )
    }

    if (showMoveDialog) {
        val rawFoldersList by viewModel.rawFolders.collectAsStateWithLifecycle()
        MoveCustomerDialog(
            customer = customer,
            currentFolderId = customer.folderId,
            allFolders = rawFoldersList,
            onDismiss = { showMoveDialog = false },
            onMoveConfirmed = { targetFolder ->
                viewModel.moveCustomerToFolder(
                    customer = customer,
                    targetFolderId = targetFolder.id,
                    targetFolderName = targetFolder.name,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "✅ ${customer.name} को '${targetFolder.name}' में सफलतापूर्वक मूव कर दिया गया है",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                showMoveDialog = false
            }
        )
    }
}
