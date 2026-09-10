package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import com.example.util.PdfExportUtils
import com.example.util.SmsUtils
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.EditShopProfileDialog
import com.example.ui.components.PublishAnnouncementDialog
import com.example.ui.components.MoveCustomerDialog
import com.example.ui.components.SendReminderDialog
import com.example.ui.components.ShopProfileBanner
import com.example.ui.components.SmsOptionsDialog
import com.example.ui.components.UpiQrGeneratorDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.ui.components.AddEditCustomerDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.RecordTransactionDialog
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.viewmodel.FolderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onEditCustomerClick: ((Long, Long) -> Unit)? = null
) {
    val context = LocalContext.current

    val customer by viewModel.getCustomerFlow(customerId).collectAsState(initial = null)
    val customerFinancialsMap by viewModel.customerFinancialsMap.collectAsStateWithLifecycle()
    val customerFin = customerFinancialsMap[customerId]
    val isCustBlocked = customer?.status?.contains("Blocked", ignoreCase = true) == true || (customerFin?.isBlocked == true)

    val transactions by viewModel.getTransactionsForCustomerFlow(customerId).collectAsState(initial = emptyList())
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val pullDownSyncEnabled by viewModel.pullDownSyncEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isHindi = appLanguage == "hi"

    var showProfileDialog by remember { mutableStateOf(false) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var recordDialogType by remember { mutableStateOf("GOODS_PROVIDED") } // or "PAYMENT_DEPOSIT"
    var showGoodsDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTxForAction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToReturnPrompt by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToReAddPrompt by remember { mutableStateOf<TransactionEntity?>(null) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showSendReminderDialog by remember { mutableStateOf(false) }
    var reminderMessageToSend by remember { mutableStateOf("") }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditShopDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var showMoveCustomerDialog by remember { mutableStateOf(false) }
    var showBlockedAlert by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }
    var enlargedPhotoUri by remember { mutableStateOf<String?>(null) }
    var enlargedPhotoTitle by remember { mutableStateOf("") }
    var enlargedPhotoSubtitle by remember { mutableStateOf<String?>(null) }

    // Financial totals
    val totalGoodsProvided = remember(transactions) {
        transactions.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
    }
    val totalGoods = totalGoodsProvided
    val totalPaid = remember(transactions) {
        transactions.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
    }
    val dues = totalGoods - totalPaid

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
                    // Returned item was cancelled/returned: does not increase balance and does not subtract from prior goods
                }
            }
            map[tx.id] = curBal
        }
        map
    }

    val goodsTransactions = remember(transactions) {
        transactions.filter { it.type == "GOODS_PROVIDED" }
    }
    val goodsItemCount = goodsTransactions.sumOf { it.quantity }

    val initialLetter = if (!customer?.name.isNullOrBlank()) {
        customer!!.name.trim().first().uppercaseChar().toString()
    } else "C"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showProfileDialog = true
                            }
                            .testTag("top_bar_customer_profile_click")
                    ) {
                        // Customer Avatar Circle with photo or initial letter
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C1338))
                                .clickable(enabled = !customer?.photoUri.isNullOrEmpty()) {
                                    enlargedPhotoUri = customer?.photoUri
                                    enlargedPhotoTitle = customer?.name ?: "Customer Photo"
                                    enlargedPhotoSubtitle = customer?.phone
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!customer?.photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = customer?.photoUri,
                                    contentDescription = "Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = initialLetter,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Customer Name + Book & Page badge
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = customer?.name ?: "Customer Detail",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.3.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val hasBook = !customer?.bookNumber.isNullOrBlank()
                            val hasPage = !customer?.pageNumber.isNullOrBlank()
                            if (hasBook || hasPage) {
                                val badgeText = buildString {
                                    if (hasBook) append("Bk ${customer?.bookNumber}")
                                    if (hasBook && hasPage) append(" • ")
                                    if (hasPage) append("Pg ${customer?.pageNumber}")
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("customer_detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showUpiQrDialog = true },
                        modifier = Modifier.testTag("customer_detail_upi_qr_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "दुकान UPI QR कोड",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("customer_menu_top_action")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Customer Menu")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            // Customer SMS Notifications ON / OFF Preference
                            val smsOn = customer?.smsNotificationsEnabled != false
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (smsOn) "🔔 SMS/संदेश: चालू (ON)" else "🔕 SMS/संदेश: बंद (OFF)",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (smsOn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = if (smsOn) "ग्राहक को संदेश/रसीद भेजी जाएगी" else "ग्राहक ने संदेश प्राप्त करने से मना किया है",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = smsOn,
                                            onCheckedChange = { newStatus ->
                                                customer?.let { c ->
                                                    viewModel.updateCustomer(c.copy(smsNotificationsEnabled = newStatus))
                                                    Toast.makeText(
                                                        context,
                                                        if (newStatus) "${c.name} के लिए SMS/संदेश चालू किया गया" else "${c.name} के लिए SMS/संदेश बंद किया गया",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Message,
                                        contentDescription = null,
                                        tint = if (smsOn) Color(0xFF2E7D32) else Color.Gray
                                    )
                                },
                                onClick = {
                                    customer?.let { c ->
                                        val newStatus = !c.smsNotificationsEnabled
                                        viewModel.updateCustomer(c.copy(smsNotificationsEnabled = newStatus))
                                        Toast.makeText(
                                            context,
                                            if (newStatus) "${c.name} के लिए SMS/संदेश चालू किया गया" else "${c.name} के लिए SMS/संदेश बंद किया गया",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("💬 Send Reminder (तकादा संदेश)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    customer?.let { c ->
                                        val reminderMsg = SmsUtils.buildPaymentReminderSms(
                                            customerName = c.name,
                                            dues = dues,
                                            context = context
                                        )
                                        reminderMessageToSend = reminderMsg
                                        showSendReminderDialog = true
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "✏️ ग्राहक एडिट करें (Edit)" else "✏️ Edit Customer Profile") },
                                onClick = {
                                    showOptionsMenu = false
                                    if (onEditCustomerClick != null && customer != null) {
                                        onEditCustomerClick(customer!!.folderId, customer!!.id)
                                    } else {
                                        showEditCustomerDialog = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "👤 ग्राहक जानकारी (Details)" else "👤 View Customer Details") },
                                onClick = {
                                    showOptionsMenu = false
                                    showProfileDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "📲 दुकान UPI QR कोड" else "📲 Shop UPI QR Code") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showUpiQrDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "🏪 शॉप प्रोफाइल व लाइव नोटिस" else "🏪 Shop Profile & Live Notice") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showEditShopDialog = true
                                },
                                modifier = Modifier.testTag("menu_customer_shop_profile_notice")
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "📢 ग्राहकों को बधाई / ऑफर भेजें" else "📢 Publish Announcement / Offer") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showAnnouncementDialog = true
                                },
                                modifier = Modifier.testTag("menu_customer_publish_announcement")
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "📄 पीडीएफ / लेजर शेयर (PDF)" else "📄 Share Ledger / Export PDF") },
                                onClick = {
                                    showOptionsMenu = false
                                    showShareOptionsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "📁 फोल्डर बदलें (Move Folder)" else "📁 Move to Folder") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DriveFileMove,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100)
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showMoveCustomerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isCustBlocked)
                                            (if (isHindi) "🔓 ग्राहक अनब्लॉक करें (Unblock)" else "🔓 Unblock Customer")
                                        else
                                            (if (isHindi) "🚫 ग्राहक ब्लॉक करें (Block)" else "🚫 Block Customer"),
                                        color = if (isCustBlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCustBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isCustBlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    customer?.let { c ->
                                        val nextState = !isCustBlocked
                                        viewModel.setCustomerBlockStatus(c, nextState)
                                        Toast.makeText(
                                            context,
                                            if (nextState)
                                                (if (isHindi) "${c.name} को ब्लॉक किया गया" else "${c.name} blocked")
                                            else
                                                (if (isHindi) "${c.name} को अनब्लॉक/सक्रिय किया गया" else "${c.name} unblocked & activated"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isHindi) "🗑️ खाता हटाएं (Delete Account)" else "🗑️ Delete Customer Account",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteCustomerDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sticky Bottom Action Bar (You Gave / You Got)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Red / Grey Button: YOU GAVE ₹ or GAVE (BLOCKED)
                    Button(
                        onClick = {
                            if (isCustBlocked) {
                                showBlockedAlert = true
                            } else {
                                recordDialogType = "GOODS_PROVIDED"
                                showRecordDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_gave_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCustBlocked) Color(0xFF757575) else Color(0xFFC62828), // Darker grey for clear contrast or Deep Red
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isCustBlocked) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "GAVE (BLOCKED)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            } else {
                                Text(
                                    text = "— GAVE ₹",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Green Button: YOU GOT ₹
                    Button(
                        onClick = {
                            recordDialogType = "PAYMENT_DEPOSIT"
                            showRecordDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_got_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32), // Deep Green
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "+ GOT ₹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val detailContent: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("customer_detail_lazy_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. GOODS PURCHASED CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4A148C), // Dark Purple
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Goods Purchased",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$goodsItemCount items/products given",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showGoodsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A148C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("view_goods_button")
                        ) {
                            Text("View Goods", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. NET ACCOUNT BALANCE DARK CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E0E2E) // Dark Purple / Navy Canvas
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NET ACCOUNT BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            if (dues > 0 && customer != null) {
                                IconButton(
                                    onClick = {
                                        viewModel.sendPaymentReminderPush(
                                            customer = customer!!,
                                            dues = dues,
                                            onResult = { success, msg ->
                                                Toast.makeText(
                                                    context,
                                                    if (success) "🔔 ${customer!!.name} को ₹${String.format("%,.0f", dues)} का पेमेंट रिमाइंडर भेज दिया गया!" else msg,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(32.dp)
                                        .background(Color(0xFFFFB74D).copy(alpha = 0.25f), CircleShape)
                                        .testTag("send_payment_reminder_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Send Payment Reminder Notification",
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (dues > 0) "BALANCE ₹${String.format("%,.0f", dues)}"
                            else if (dues < 0) "BALANCE -₹${String.format("%,.0f", -dues)}"
                            else "BALANCE ₹0",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (dues > 0) Color(0xFFFFB74D) else if (dues < 0) Color(0xFF81C784) else Color.White
                        )

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TOTAL GAVE (Credit)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", totalGoods)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TOTAL GOT (Jama)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", totalPaid)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784) // Light green
                                )
                            }
                        }


                    }
                }
            }

            // 4. TRANSACTION ENTRIES HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Entries (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

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
                                    dues = dues
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A237E),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("share_pdf_statement_button")
                        ) {
                            Text("📄 PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showShareOptionsDialog = true },
                            modifier = Modifier.testTag("share_statement_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Statement", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 5. TRANSACTION LIST
            if (transactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transaction entries recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val isGoods = tx.type == "GOODS_PROVIDED"
                    val isReturn = tx.type == "GOODS_RETURNED"
                    val dateOnlyFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    val timeOnlyFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    val dateOnlyStr = dateOnlyFormat.format(Date(tx.dateMillis))
                    val timeOnlyStr = timeOnlyFormat.format(Date(tx.dateMillis))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTxForAction = tx }
                            .testTag("tx_item_card_${tx.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReturn) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isReturn) BorderStroke(1.dp, Color(0xFFFFB74D)) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Type Tag
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            isGoods -> Color(0xFFFFEBEE)
                                            isReturn -> Color(0xFFE65100)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                isGoods -> "उधार दिया (Goods)"
                                                isReturn -> "❌ सामान वापसी"
                                                else -> "जमा मिला (Payment)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isGoods -> Color(0xFFC62828)
                                                isReturn -> Color.White
                                                else -> Color(0xFF2E7D32)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Mode / Unit Tag
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = if (isGoods || isReturn) tx.unitType.uppercase() else "CASH",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Amount and Line Running Balance
                                val rowBal = runningBalances[tx.id] ?: 0.0
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = when {
                                            isGoods -> "+₹${String.format("%,.2f", tx.totalAmount)}"
                                            isReturn -> "₹${String.format("%,.2f", tx.totalAmount)}"
                                            else -> "-₹${String.format("%,.2f", tx.totalAmount)}"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            textDecoration = if (isReturn) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isGoods -> Color(0xFFC62828)
                                            isReturn -> Color(0xFFE65100)
                                            else -> Color(0xFF2E7D32)
                                        }
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (rowBal > 0) Color(0xFFFFEBEE) else if (rowBal < 0) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = if (rowBal > 0) "Bal: ₹${String.format("%,.2f", rowBal)}"
                                            else if (rowBal < 0) "Adv: ₹${String.format("%,.2f", -rowBal)}"
                                            else "Bal: ₹0.00",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rowBal > 0) Color(0xFFC62828) else if (rowBal < 0) Color(0xFF2E7D32) else Color(0xFF757575),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tx.itemDescription,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (isReturn) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isReturn) Color(0xFFBF360C) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isGoods || isReturn) {
                                val qtyDisp = if (tx.quantityDouble > 0) String.format("%.1f", tx.quantityDouble).removeSuffix(".0") else "${tx.quantity}"
                                Text(
                                    text = "$qtyDisp ${tx.unitType} × ₹${String.format("%.2f", tx.unitPrice)} = ₹${String.format("%.2f", tx.totalAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isReturn) {
                                Text(
                                    text = "❌ Returned item — deducted from final bill",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (tx.notes.isNotBlank()) {
                                        Text(
                                            text = tx.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Prominently highlighted Date & Time badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isGoods) Color(0xFFFFF8E1) else if (isReturn) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isGoods) Color(0xFFFFD54F) else if (isReturn) Color(0xFFFFB74D) else Color(0xFFA5D6A7)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Date",
                                                tint = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = dateOnlyStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .background(
                                                        if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20),
                                                        CircleShape
                                                    )
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Time",
                                                tint = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = timeOnlyStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { transactionToEdit = tx },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("edit_tx_button_${tx.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { transactionToDelete = tx },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Statement Footer
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "End of Ledger Statement (${transactions.size} Entries Shown)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

        if (pullDownSyncEnabled) {
            PullToRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    isPullRefreshing = true
                    viewModel.syncAllDataToCloudAndDrive(context) { success, message ->
                        isPullRefreshing = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("customer_detail_pull_to_refresh")
            ) {
                detailContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("customer_detail_pull_to_refresh_disabled")
            ) {
                detailContent()
            }
        }
    }

    // CUSTOMER PROFILE & FULL DETAILS DIALOG
    if (showProfileDialog && customer != null) {
        val cust = customer!!
        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Customer Details (विवरण)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showProfileDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card with Avatar / Photo + Name + Book & Page badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C1338))
                                    .clickable(enabled = !cust.photoUri.isNullOrEmpty()) {
                                        enlargedPhotoUri = cust.photoUri
                                        enlargedPhotoTitle = cust.name
                                        enlargedPhotoSubtitle = if (cust.phone.isNotBlank()) cust.phone else null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!cust.photoUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = cust.photoUri,
                                        contentDescription = "Customer Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = cust.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cust.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                val hasBook = cust.bookNumber.isNotBlank()
                                val hasPage = cust.pageNumber.isNotBlank()
                                if (hasBook || hasPage) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = buildString {
                                                if (hasBook) append("Book: ${cust.bookNumber}")
                                                if (hasBook && hasPage) append(" | ")
                                                if (hasPage) append("Page: ${cust.pageNumber}")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Account Financial Summary Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Account Balance Summary (खाता स्थिति)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Purchases (उधारी)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("₹${String.format("%.2f", totalGoods)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                                Column {
                                    Text("Paid (जमा)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("₹${String.format("%.2f", totalPaid)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Balance", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    val (balColor, balLabel) = when {
                                        dues > 0.01 -> Color(0xFFC62828) to "₹${String.format("%.2f", dues)} Due"
                                        dues < -0.01 -> Color(0xFF2E7D32) to "₹${String.format("%.2f", -dues)} Adv"
                                        else -> Color.DarkGray to "₹0.00 Clear"
                                    }
                                    Text(balLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = balColor)
                                }
                            }
                        }
                    }

                    // Quick Action Buttons (Call, WhatsApp, Maps)
                    if (cust.phone.isNotBlank() || cust.address.isNotBlank() || (cust.latitude != null && cust.longitude != null)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cust.phone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (cust.phone.isBlank()) {
                                            Toast.makeText(context, "Phone number is missing", Toast.LENGTH_SHORT).show()
                                        } else {
                                            reminderMessageToSend = SmsUtils.buildPaymentReminderSms(
                                                customerName = cust.name,
                                                dues = dues,
                                                context = context
                                            )
                                            showSendReminderDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (cust.address.isNotBlank() || (cust.latitude != null && cust.longitude != null)) {
                                OutlinedButton(
                                    onClick = {
                                        if (cust.latitude != null && cust.longitude != null && cust.latitude != 0.0 && cust.longitude != 0.0) {
                                            com.example.util.LocationUtils.openGoogleMaps(context, cust.latitude!!, cust.longitude!!, cust.name)
                                        } else if (cust.address.isNotBlank()) {
                                            try {
                                                val encodedAddress = Uri.encode(cust.address)
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
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(cust.address))
                                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Contact Details
                    if (cust.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Mobile Number:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(cust.phone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Book & Page Number
                    if (cust.bookNumber.isNotBlank() || cust.pageNumber.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Book & Page (बही व पृष्ठ):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val text = buildString {
                                    if (cust.bookNumber.isNotBlank()) append("Book No: ${cust.bookNumber}")
                                    if (cust.bookNumber.isNotBlank() && cust.pageNumber.isNotBlank()) append(" | ")
                                    if (cust.pageNumber.isNotBlank()) append("Page No: ${cust.pageNumber}")
                                }
                                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Address & Location
                    if (cust.address.isNotBlank() || (cust.latitude != null && cust.longitude != null)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Address & GPS Location:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(cust.address.ifBlank { "GPS: ${cust.latitude}, ${cust.longitude}" }, style = MaterialTheme.typography.bodyMedium)
                                if (cust.latitude != null && cust.longitude != null && cust.address.isNotBlank()) {
                                    Text("GPS: ${cust.latitude}, ${cust.longitude}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Document / KYC Info (if provided)
                    if (cust.documentType.isNotBlank() || cust.documentNumber.isNotBlank() || !cust.documentPhotoUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text("KYC / ID Verification (दस्तावेज़)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                if (cust.documentType.isNotBlank() || cust.documentNumber.isNotBlank()) {
                                    Text(
                                        text = "${cust.documentType.ifBlank { "ID Proof" }}: ${cust.documentNumber.ifBlank { "Attached" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (!cust.documentPhotoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = cust.documentPhotoUri,
                                        contentDescription = "Document Photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray)
                                            .clickable {
                                                enlargedPhotoUri = cust.documentPhotoUri
                                                enlargedPhotoTitle = "${cust.name} - ${cust.documentType.ifBlank { "ID Proof" }}"
                                                enlargedPhotoSubtitle = cust.documentNumber.takeIf { it.isNotBlank() }
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    if (cust.notes.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Notes (टिप्पणी):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(cust.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    HorizontalDivider()

                    // SMS Preferences
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val newPref = !cust.smsNotificationsEnabled
                                viewModel.updateCustomer(cust.copy(smsNotificationsEnabled = newPref))
                                Toast.makeText(
                                    context,
                                    if (newPref) "${cust.name} के लिए SMS/संदेश चालू किया गया" else "${cust.name} के लिए SMS/संदेश बंद किया गया",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Message,
                                    contentDescription = null,
                                    tint = if (cust.smsNotificationsEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Column {
                                    Text(
                                        if (cust.smsNotificationsEnabled) "SMS व संदेश सेवा: चालू (ON)" else "SMS व संदेश सेवा: बंद (OFF)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cust.smsNotificationsEnabled) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                    )
                                    Text(
                                        if (cust.smsNotificationsEnabled) "ग्राहक को लेन-देन की पूरी जानकारी भेजी जाएगी" else "ग्राहक के मना करने पर संदेश बंद है",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = cust.smsNotificationsEnabled,
                                onCheckedChange = { newPref ->
                                    viewModel.updateCustomer(cust.copy(smsNotificationsEnabled = newPref))
                                    Toast.makeText(
                                        context,
                                        if (newPref) "${cust.name} के लिए SMS/संदेश चालू किया गया" else "${cust.name} के लिए SMS/संदेश बंद किया गया",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2E7D32),
                                    checkedTrackColor = Color(0xFFA5D6A7),
                                    uncheckedThumbColor = Color(0xFFC62828),
                                    uncheckedTrackColor = Color(0xFFEF9A9A)
                                )
                            )
                        }
                    }

                    // Prominent EDIT CUSTOMER DETAILS button
                    Button(
                        onClick = {
                            showProfileDialog = false
                            if (onEditCustomerClick != null && customer != null) {
                                onEditCustomerClick(customer!!.folderId, customer!!.id)
                            } else {
                                showEditCustomerDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_customer_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Customer Details (कस्टमर विवरण बदलें)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // MOVE CUSTOMER TO ANOTHER FOLDER button
                    OutlinedButton(
                        onClick = {
                            showProfileDialog = false
                            showMoveCustomerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE65100)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE65100))
                    ) {
                        Icon(
                            Icons.Default.DriveFileMove,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Move to Another Folder (दूसरे फोल्डर में भेजें)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProfileDialog = false },
                    modifier = Modifier.testTag("dialog_close_profile_button")
                ) {
                    Text("Close (बंद करें)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // VIEW GOODS PURCHASED DIALOG
    if (showGoodsDialog && customer != null) {
        AlertDialog(
            onDismissRequest = { showGoodsDialog = false },
            title = {
                Text("Goods Purchased (${goodsTransactions.size} Records)", fontWeight = FontWeight.Bold)
            },
            text = {
                if (goodsTransactions.isEmpty()) {
                    Text("No goods/items recorded for this customer yet.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(goodsTransactions) { tx ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.itemDescription, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${tx.quantity} ${tx.unitType.uppercase()} @ ₹${tx.unitPrice}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("₹${String.format("%.2f", tx.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoodsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // SEND REMINDER DIALOG (WhatsApp, WhatsApp Business, SMS, Share, Push Notification)
    if (showSendReminderDialog && customer != null) {
        SendReminderDialog(
            customerName = customer!!.name,
            customerPhone = customer!!.phone,
            dues = dues,
            initialMessage = reminderMessageToSend.ifBlank {
                SmsUtils.buildPaymentReminderSms(
                    customerName = customer!!.name,
                    dues = dues,
                    context = context
                )
            },
            onDismiss = { showSendReminderDialog = false },
            onSendPushReminder = {
                viewModel.sendPaymentReminderPush(
                    customer = customer!!,
                    dues = dues,
                    onResult = { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // BLOCKED CUSTOMER ALERT DIALOG
    if (showBlockedAlert) {
        AlertDialog(
            onDismissRequest = { showBlockedAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isHindi) "🚫 ग्राहक ब्लॉक है! (Gave Blocked)" else "🚫 Customer is Blocked!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (customerFin?.isOverdue3Months == true) {
                            if (isHindi)
                                "यह ग्राहक लगातार 3 महीने से पेमेंट न आने के कारण ब्लॉक है।"
                            else
                                "This customer is blocked due to 3+ continuous months with no payment."
                        } else {
                            if (isHindi)
                                "यह ग्राहक ब्लॉक सूची में है।"
                            else
                                "This customer is currently marked as Blocked."
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB71C1C)
                    )
                    Text(
                        text = if (isHindi)
                            "• ब्लॉक रहने पर आप इस ग्राहक को नया सामान (GAVE) नहीं दे सकते।\n• केवल 'YOU GOT (पेमेंट जमा)' की एंट्री हो सकती है।\n• सामान देने के लिए ऊपर 3-डॉट्स मेनू (⋮) से ग्राहक को अनब्लॉक करें।"
                        else
                            "• You cannot give new items on credit while the customer is blocked.\n• Only 'YOU GOT (Payment Deposit)' is allowed.\n• To give goods, unblock from the top 3-dots menu (⋮).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBlockedAlert = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isHindi) "समझ गया (OK)" else "OK")
                }
            }
        )
    }

    // RECORD TRANSACTION DIALOG (YOU GAVE / YOU GOT)
    val suggestedProducts by viewModel.suggestedProductNames.collectAsStateWithLifecycle()
    if (showRecordDialog && customer != null) {
        RecordTransactionDialog(
            customerName = customer!!.name,
            customerPhone = customer!!.phone,
            currentDues = dues,
            smsNotificationsEnabled = customer!!.smsNotificationsEnabled,
            initialType = recordDialogType,
            isCustomerBlocked = isCustBlocked,
            productSuggestions = suggestedProducts,
            onDismiss = { showRecordDialog = false },
            onSaveSingle = { type, itemDescription, quantity, unitType, quantityDouble, unitPrice, totalAmount, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    Toast.makeText(context, "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है।", Toast.LENGTH_LONG).show()
                    return@RecordTransactionDialog
                }
                viewModel.recordTransaction(
                    customerId = customer!!.id,
                    folderId = customer!!.folderId,
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
                Toast.makeText(context, "Transaction recorded!", Toast.LENGTH_SHORT).show()
            },
            onSaveMultiple = { type, itemsList, notes ->
                if (type == "GOODS_PROVIDED" && isCustBlocked) {
                    Toast.makeText(context, "🚫 ग्राहक ब्लॉक है! सामान देने की अनुमति नहीं है।", Toast.LENGTH_LONG).show()
                    return@RecordTransactionDialog
                }
                viewModel.recordMultipleTransactions(
                    customerId = customer!!.id,
                    folderId = customer!!.folderId,
                    transactions = itemsList
                )
                showRecordDialog = false
                Toast.makeText(context, "All items recorded!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // EDIT CUSTOMER DIALOG
    if (showEditCustomerDialog && customer != null) {
        AddEditCustomerDialog(
            folderName = "Folder",
            initialCustomer = customer,
            onDismiss = { showEditCustomerDialog = false },
            onSave = { name, phone, bookNumber, pageNumber, address, notes, photoUri, docType, docNum, docPhotoUri, lat, lng, smsEnabled, _ ->
                viewModel.updateCustomer(
                    customer!!.copy(
                        name = name,
                        phone = phone,
                        bookNumber = bookNumber,
                        pageNumber = pageNumber,
                        address = address,
                        notes = notes,
                        photoUri = photoUri,
                        documentType = docType,
                        documentNumber = docNum,
                        documentPhotoUri = docPhotoUri,
                        latitude = lat,
                        longitude = lng,
                        smsNotificationsEnabled = smsEnabled
                    )
                )
                showEditCustomerDialog = false
            }
        )
    }

    // CONFIRM DELETE TRANSACTION DIALOG
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
                Text("Are you sure you want to delete this item (${tx.itemDescription}) given to customer? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                        Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
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

    // ITEM TAP OPTIONS DIALOG
    selectedTxForAction?.let { tx ->
        val isGoods = tx.type == "GOODS_PROVIDED"
        val isReturn = tx.type == "GOODS_RETURNED"
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(tx.dateMillis))

        AlertDialog(
            onDismissRequest = { selectedTxForAction = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isGoods) "📦 Item Details & Actions" else if (isReturn) "❌ Returned Item Details" else "💳 Payment Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isReturn) Color(0xFFFFF3E0) else Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tx.itemDescription,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isReturn) Color(0xFFE65100) else Color.Unspecified,
                                textDecoration = if (isReturn) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (isGoods || isReturn) {
                                Text(
                                    text = "Quantity: ${if (tx.quantityDouble > 0) String.format("%.1f", tx.quantityDouble).removeSuffix(".0") else tx.quantity.toString()} ${tx.unitType} @ ₹${String.format("%.2f", tx.unitPrice)}/${tx.unitType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                            Text(
                                text = "Amount: ₹${String.format("%.2f", tx.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGoods) Color(0xFFC62828) else if (isReturn) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                            // Highlighted Date and Time Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGoods) Color(0xFFFFF8E1) else if (isReturn) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isGoods) Color(0xFFFFD54F) else if (isReturn) Color(0xFFFFB74D) else Color(0xFFA5D6A7)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Date",
                                        tint = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "दिनांक: $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGoods) Color(0xFFE65100) else if (isReturn) Color(0xFFBF360C) else Color(0xFF1B5E20)
                                    )
                                }
                            }
                            if (tx.notes.isNotBlank()) {
                                 Text(
                                    text = "Notes: ${tx.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val currentTx = selectedTxForAction
                            selectedTxForAction = null
                            transactionToEdit = currentTx
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_tx_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isGoods) "✏️ Edit Goods & Amount (सामान / राशि बदलें)" else "✏️ Edit Payment Amount (भुगतान बदलें)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val currentTx = selectedTxForAction
                            selectedTxForAction = null
                            currentTx?.let { targetTx ->
                                val smsMsg = SmsUtils.buildSingleTransactionSms(
                                    customerName = customer?.name ?: "Customer",
                                    type = targetTx.type,
                                    itemDescription = targetTx.itemDescription,
                                    quantity = targetTx.quantityDouble,
                                    unitType = targetTx.unitType,
                                    unitPrice = targetTx.unitPrice,
                                    totalAmount = targetTx.totalAmount,
                                    updatedDues = dues,
                                    notes = targetTx.notes
                                )
                                SmsUtils.openSmsComposer(context, customer?.phone ?: "", smsMsg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("📱 Send SMS Receipt", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isGoods) {
                        Button(
                            onClick = {
                                val currentTx = selectedTxForAction
                                selectedTxForAction = null
                                txToReturnPrompt = currentTx
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE65100),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("↩ Return This Item (Deduct from Bill)", fontWeight = FontWeight.Bold)
                        }
                    } else if (isReturn) {
                        Button(
                            onClick = {
                                val currentTx = selectedTxForAction
                                selectedTxForAction = null
                                txToReAddPrompt = currentTx
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Re-add to Active Bill", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentTx = selectedTxForAction
                        selectedTxForAction = null
                        transactionToDelete = currentTx
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForAction = null }) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // EDIT TRANSACTION DIALOG (GOODS / AMOUNT / DETAILS)
    transactionToEdit?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            customerName = customer?.name ?: "Customer",
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                transactionToEdit = null
                Toast.makeText(context, "Item details updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // CONFIRM ITEM RETURN PROMPT
    txToReturnPrompt?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToReturnPrompt = null },
            title = {
                Text(
                    text = "↩ Mark Item as Returned",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure the customer is returning '${tx.itemDescription}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• This exact item will be marked as ❌ RETURNED",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• No duplicate transaction line will be created",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ₹${String.format("%,.2f", tx.totalAmount)} will be deducted from the customer's total bill balance.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTransaction(
                            tx.copy(
                                type = "GOODS_RETURNED",
                                notes = if (tx.notes.isBlank()) "Returned by customer" else tx.notes
                            )
                        )
                        Toast.makeText(context, "'${tx.itemDescription}' marked as returned & deducted from bill!", Toast.LENGTH_SHORT).show()
                        txToReturnPrompt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Confirm Return", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToReturnPrompt = null }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // CONFIRM RE-ADD TO BILL PROMPT
    txToReAddPrompt?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToReAddPrompt = null },
            title = {
                Text(
                    text = "🔄 Confirm Re-add to Bill",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to re-add '${tx.itemDescription}' to the active bill?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• Item status will be restored as active",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Cross mark ❌ and strikethrough will be removed",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ₹${String.format("%,.2f", tx.totalAmount)} will be added back to the customer's total bill balance.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTransaction(tx.copy(type = "GOODS_PROVIDED"))
                        Toast.makeText(context, "'${tx.itemDescription}' re-added to active bill!", Toast.LENGTH_SHORT).show()
                        txToReAddPrompt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirm Re-add", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToReAddPrompt = null }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // SHARE STATEMENT OPTIONS DIALOG
    if (showShareOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showShareOptionsDialog = false },
            title = {
                Text(
                    text = "📄 Share Ledger Statement",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose format to share statement for ${customer?.name ?: "Customer"}:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Option 1: PDF Document
                    Button(
                        onClick = {
                            showShareOptionsDialog = false
                            PdfExportUtils.generateAndSharePdf(
                                context = context,
                                customer = customer,
                                transactions = transactions,
                                totalGoods = totalGoods,
                                totalPaid = totalPaid,
                                dues = dues
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A237E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📄", style = MaterialTheme.typography.titleMedium)
                            Text("Send as PDF Document", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Option 2: Text Message Summary
                    Button(
                        onClick = {
                            showShareOptionsDialog = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Ledger Statement for ${customer?.name}:\nTotal Goods: ₹${String.format("%,.2f", totalGoods)}\nTotal Paid: ₹${String.format("%,.2f", totalPaid)}\nNet Balance: ₹${String.format("%,.2f", dues)}\n\nThank you!\n- Shree Bartan Store"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Statement (Text)"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💬", style = MaterialTheme.typography.titleMedium)
                            Text("Send as Text Summary", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Option 3: Customer CSV Ledger (Saved with Customer Name)
                    Button(
                        onClick = {
                            showShareOptionsDialog = false
                            val currentCust = customer
                            if (currentCust != null) {
                                com.example.util.CsvBackupManager.exportCustomerLedgerCsv(
                                    context = context,
                                    customer = currentCust,
                                    transactions = transactions
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_customer_csv_ledger_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📊", style = MaterialTheme.typography.titleMedium)
                            Text("Export Customer CSV Ledger (खाता CSV)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareOptionsDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Customer Confirmation Dialog
    val currentCust = customer
    if (showDeleteCustomerDialog && currentCust != null) {
        val isBalanceZero = kotlin.math.abs(dues) <= 0.01

        if (!isBalanceZero) {
            AlertDialog(
                onDismissRequest = { showDeleteCustomerDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Cannot Delete Account",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Customer '${currentCust.name}' account cannot be deleted because the balance is not zero.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (dues > 0) "Pending Dues:" else "Advance Balance:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "₹${String.format("%.2f", kotlin.math.abs(dues))}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Text(
                            text = "⚠️ Accounting Rule: A customer's account balance must reach ₹0.00 before it can be deleted. Please settle all entries and payments first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDeleteCustomerDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Understood")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteCustomerDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete Customer Account?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("The balance for '${currentCust.name}' is ₹0.00 (All clear). Are you sure you want to move this customer account to the Recycle Bin?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCustomer(currentCust) { success, msg ->
                                Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                if (success) {
                                    onBack()
                                }
                            }
                            showDeleteCustomerDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteCustomerDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    // Move Customer to Another Folder Dialog
    if (showMoveCustomerDialog && customer != null) {
        val rawFoldersList by viewModel.rawFolders.collectAsStateWithLifecycle()
        MoveCustomerDialog(
            customer = customer!!,
            currentFolderId = customer!!.folderId,
            allFolders = rawFoldersList,
            onDismiss = { showMoveCustomerDialog = false },
            onMoveConfirmed = { targetFolder ->
                val cName = customer!!.name
                viewModel.moveCustomerToFolder(
                    customer = customer!!,
                    targetFolderId = targetFolder.id,
                    targetFolderName = targetFolder.name,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "✅ $cName को '${targetFolder.name}' में सफलतापूर्वक मूव कर दिया गया है",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                showMoveCustomerDialog = false
            }
        )
    }

    // Edit Shop Profile Dialog
    if (showEditShopDialog) {
        EditShopProfileDialog(
            currentProfile = shopProfile,
            onDismiss = { showEditShopDialog = false },
            onSave = { updated ->
                viewModel.updateShopProfile(updated)
                showEditShopDialog = false
            }
        )
    }

    // Publish Announcement / Greetings Dialog
    if (showAnnouncementDialog) {
        PublishAnnouncementDialog(
            onDismiss = { showAnnouncementDialog = false },
            viewModel = viewModel
        )
    }

    // Enlarged Photo Preview Popup
    if (!enlargedPhotoUri.isNullOrBlank()) {
        PhotoPreviewDialog(
            photoUri = enlargedPhotoUri,
            title = enlargedPhotoTitle,
            subtitle = enlargedPhotoSubtitle,
            onDismiss = { enlargedPhotoUri = null }
        )
    }

    // Shop UPI QR Code Dialog (Open amount)
    if (showUpiQrDialog) {
        UpiQrGeneratorDialog(
            shopName = "श्री बर्तन भंडार",
            onDismiss = { showUpiQrDialog = false }
        )
    }
}
