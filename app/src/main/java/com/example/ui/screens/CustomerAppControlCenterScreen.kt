package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditShopProfileDialog
import com.example.ui.components.PublishAnnouncementDialog
import com.example.ui.components.UpiQrGeneratorDialog
import com.example.ui.screens.BroadcastPaymentReminderDialog
import com.example.ui.screens.AddEditProductBottomSheet
import com.example.ui.screens.ProductItemCard
import com.example.data.entity.ProductEntity
import com.example.ui.viewmodel.FolderViewModel

/**
 * Dedicated Customer App Management Center / Control Hub.
 * Brings together all customer-facing controls into a single organized screen:
 * 1. Product Catalog & Add/Edit Products
 * 2. Announcements, Festival Wishes & Offers
 * 3. Registered Customers & GPS Locations
 * 4. Shop Profile, Timings & Live Notice
 * 5. Shop UPI QR Code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAppControlCenterScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToAdminCustomers: () -> Unit
) {
    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val registeredCustomers by viewModel.registeredCustomers.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    var showEditShopDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showBroadcastReminderDialog by remember { mutableStateOf(false) }
    var showControlCenterMenu by remember { mutableStateOf(false) }

    // Full catalog management states directly on main screen
    var showAddEditProductSheet by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var productToBroadcastOffer by remember { mutableStateOf<ProductEntity?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Categories calculation
    val allCategories = remember(allProducts) {
        allProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Filtered products list
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.metalType.contains(searchQuery, ignoreCase = true) ||
                product.sizeSpec.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true) ||
                product.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || selectedCategory == "सभी" || product.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "कस्टमर ऐप कंट्रोल सेंटर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Customer App Management Hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएं"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showControlCenterMenu = true },
                            modifier = Modifier.testTag("control_center_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "कंट्रोल मेन्यू (Menu Options)"
                            )
                        }

                        DropdownMenu(
                            expanded = showControlCenterMenu,
                            onDismissRequest = { showControlCenterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🔔 सभी बकायेदारों को रिमाइंडर (1-क्लिक)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800)
                                    )
                                },
                                onClick = {
                                    showControlCenterMenu = false
                                    showBroadcastReminderDialog = true
                                },
                                modifier = Modifier.testTag("menu_control_broadcast_reminder")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("दुकानदार सहायता एवं जानकारी") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showControlCenterMenu = false
                                    Toast.makeText(
                                        context,
                                        "कस्टमर ऐप के सभी फीचर्स (प्रोडक्ट्स, ग्राहक GPS, दुकान प्रोफाइल, QR) मुख्य स्क्रीन पर उपलब्ध हैं।",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.testTag("menu_control_help_info")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("customer_app_control_center_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. TOP-MOST: त्वरित कंट्रोल मेन्यू (Quick Menu)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚡ त्वरित कंट्रोल मेन्यू (Quick Menu)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "शीर्ष शॉर्टकट",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBroadcastReminderDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("रिमाइंडर", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onNavigateToAdminCustomers,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ग्राहक/GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showUpiQrDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B1FA2))
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UPI QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. Header Hero Banner: Shop Details & Notice
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E232A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFF7D070), Color(0xFFD4AF37))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color(0xFF1E232A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shopProfile.hindiName.ifBlank { "श्री बर्तन भंडार" },
                                    color = Color(0xFFF3E5AB),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "कस्टमर ऐप की सभी सेटिंग्स और डेटा यहीं से नियंत्रित करें",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Live Notice Pill Preview
                        val notice = shopProfile.notice
                        if (!notice.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2A313C),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = Color(0xFFF7D070),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "लाइव नोटिस: $notice",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Live Banner Image Preview in Header Card
                        val bannerUrl = shopProfile.bannerUrl
                        if (bannerUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showEditShopDialog = true }
                            ) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Active Banner Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(topStart = 6.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = "लाइव बैनर (बदलने के लिए टैप करें)",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. FULL CATALOG DIRECTLY ON MAIN SCREEN
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("control_center_full_catalog_header"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row with Add Product action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1976D2).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "दुकान प्रोडक्ट कैटलॉग (पूरा कैटलॉग)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "यहीं से प्रोडक्ट जोड़ें, एडिट करें या स्टॉक बदलें",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    productToEdit = null
                                    showAddEditProductSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("नया जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Statistics mini-chips row
                        val inStockCount = remember(allProducts) { allProducts.count { it.inStock } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${allProducts.size}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1976D2)
                                    )
                                    Text("कुल प्रोडक्ट", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$inStockCount",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text("स्टॉक में उपलब्ध", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${allCategories.size}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Text("कैटेगरी", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("प्रोडक्ट नाम या ब्रांड खोजें...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("control_center_catalog_search"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Category filter chips
                        if (allCategories.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("सभी (${allProducts.size})", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF1976D2),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                items(allCategories) { cat ->
                                    val catCount = remember(allProducts, cat) { allProducts.count { it.category == cat } }
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = {
                                            selectedCategory = if (selectedCategory == cat) null else cat
                                        },
                                        label = { Text("$cat ($catCount)", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF1976D2),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Products list or empty state
            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "कोई प्रोडक्ट नहीं मिला" else "अभी तक कोई प्रोडक्ट नहीं जोड़ा गया",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    productToEdit = null
                                    showAddEditProductSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("पहला प्रोडक्ट जोड़ें")
                            }
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        shopPhone = shopProfile.phone,
                        shopName = shopProfile.name.ifBlank { shopProfile.hindiName },
                        onToggleStock = {
                            viewModel.toggleProductStock(product)
                        },
                        onEdit = {
                            productToEdit = product
                            showAddEditProductSheet = true
                        },
                        onDelete = {
                            productToDelete = product
                        },
                        onImageClick = { imgUri ->
                            previewImageUri = imgUri
                        },
                        onBroadcastOffer = {
                            productToBroadcastOffer = product
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 2: Marketing, Wishes & Announcements
            item {
                Text(
                    text = "ऑफर, बधाई संदेश व बैनर (Marketing & Banner)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                ControlCenterActionCard(
                    title = "लाइव बैनर व ऑफर फोटो (1200×400)",
                    subtitle = if (shopProfile.bannerUrl.isNotBlank()) "फ़ोटो अपलोड है • बदलने या हटाने के लिए क्लिक करें (1200×400 px)" else "ग्राहकों के होम पेज पर टॉप बैनर या ऑफर फोटो अपलोड करें",
                    badge = if (shopProfile.bannerUrl.isNotBlank()) "लाइव सक्रिय" else "अपलोड करें",
                    badgeColor = if (shopProfile.bannerUrl.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    badgeTextColor = if (shopProfile.bannerUrl.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFE65100),
                    icon = Icons.Default.Image,
                    iconBgColor = Color(0xFF0288D1),
                    onClick = { showEditShopDialog = true },
                    testTag = "action_upload_shop_banner"
                )
            }

            item {
                ControlCenterActionCard(
                    title = "ग्राहकों को बधाई व ऑफर संदेश भेजें",
                    subtitle = "त्योहारों की बधाई, स्पेशल डिस्काउंट या सूचना भेजें जो ग्राहक ऐप के होम स्क्रीन पर दिखेगी",
                    badge = "लाइव ब्रॉडकास्ट",
                    badgeColor = Color(0xFFFFECB3),
                    badgeTextColor = Color(0xFFB78103),
                    icon = Icons.Default.Campaign,
                    iconBgColor = Color(0xFFE65100),
                    onClick = { showAnnouncementDialog = true },
                    testTag = "action_broadcast_announcement"
                )
            }

            // Section 3: Customers & Locations
            item {
                Text(
                    text = "ग्राहक और लोकेशन (Customers & GPS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                ControlCenterActionCard(
                    title = "रजिस्टर्ड ग्राहक व GPS लोकेशन",
                    subtitle = "कुल ${registeredCustomers.size} कस्टमर रजिस्टर्ड • पते, फ़ोन नंबर और मैप पर लाइव लोकेशन देखें",
                    badge = "${registeredCustomers.size} रजिस्टर्ड",
                    badgeColor = Color(0xFFFFEBEE),
                    badgeTextColor = Color(0xFFC62828),
                    icon = Icons.Default.LocationOn,
                    iconBgColor = Color(0xFFD32F2F),
                    onClick = onNavigateToAdminCustomers,
                    testTag = "action_registered_customers"
                )
            }

            // Section 4: Shop Profile & Payment Settings
            item {
                Text(
                    text = "दुकान प्रोफाइल व पेमेंट (Shop & QR)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                ControlCenterActionCard(
                    title = "शॉप प्रोफाइल व लाइव नोटिस",
                    subtitle = "दुकान का नाम, पता, फ़ोन नंबर, खुलने का समय और लाइव नोटिस बदलें",
                    badge = "प्रोफाइल",
                    badgeColor = Color(0xFFE8F5E9),
                    badgeTextColor = Color(0xFF2E7D32),
                    icon = Icons.Default.AddBusiness,
                    iconBgColor = Color(0xFF388E3C),
                    onClick = { showEditShopDialog = true },
                    testTag = "action_edit_shop_profile"
                )
            }

            item {
                ControlCenterActionCard(
                    title = "दुकान UPI QR कोड",
                    subtitle = "दुकान का UPI पेमेंट QR कोड देखें व ग्राहकों के साथ शेयर करें",
                    badge = "UPI QR",
                    badgeColor = Color(0xFFEDE7F6),
                    badgeTextColor = Color(0xFF512DA8),
                    icon = Icons.Default.QrCode,
                    iconBgColor = Color(0xFF7B1FA2),
                    onClick = { showUpiQrDialog = true },
                    testTag = "action_shop_upi_qr"
                )
            }

            item {
                ControlCenterActionCard(
                    title = "🔔 सभी बकायेदारों को रिमाइंडर भेजें (1-क्लिक)",
                    subtitle = "जिन ग्राहकों का उधार बकाया है, उन सभी को एक साथ उनके ग्राहक ऐप पर पेमेंट रिमाइंडर भेजें",
                    badge = "ऑटो रिमाइंडर",
                    badgeColor = Color(0xFFFFF3E0),
                    badgeTextColor = Color(0xFFE65100),
                    icon = Icons.Default.NotificationsActive,
                    iconBgColor = Color(0xFFFF9800),
                    onClick = { showBroadcastReminderDialog = true },
                    testTag = "action_broadcast_payment_reminder"
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Shop Profile Dialog
    if (showEditShopDialog) {
        EditShopProfileDialog(
            currentProfile = shopProfile,
            onDismiss = { showEditShopDialog = false },
            onSave = { updated ->
                viewModel.updateShopProfile(updated)
                showEditShopDialog = false
                Toast.makeText(context, "दुकान प्रोफाइल अपडेट हो गई! ✅", Toast.LENGTH_SHORT).show()
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

    // Shop UPI QR Code Dialog
    if (showUpiQrDialog) {
        UpiQrGeneratorDialog(
            shopName = shopProfile.hindiName.ifBlank { "श्री बर्तन भंडार" },
            onDismiss = { showUpiQrDialog = false }
        )
    }

    // Broadcast Payment Reminder Dialog to notify all pending customers
    if (showBroadcastReminderDialog) {
        BroadcastPaymentReminderDialog(
            viewModel = viewModel,
            onDismiss = { showBroadcastReminderDialog = false }
        )
    }

    // Broadcast Offer Confirmation Dialog
    if (productToBroadcastOffer != null) {
        val prod = productToBroadcastOffer!!
        AlertDialog(
            onDismissRequest = { productToBroadcastOffer = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color(0xFF6200EA),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "धमाकेदार ऑफर नोटिफिकेशन भेजें",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "क्या आप \"${prod.name}\" (कीमत: ₹${String.format(java.util.Locale.ENGLISH, "%,.0f", prod.sellingPrice)}) का स्पेशल ऑफर नोटिफिकेशन सभी ग्राहकों को भेजना चाहते हैं?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prod
                        productToBroadcastOffer = null
                        viewModel.broadcastProductOfferPush(p) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
                ) {
                    Text("हाँ, भेजें (Send Offer)")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToBroadcastOffer = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Add / Edit Product Bottom Sheet
    if (showAddEditProductSheet) {
        AddEditProductBottomSheet(
            product = productToEdit,
            onDismiss = {
                showAddEditProductSheet = false
                productToEdit = null
            },
            onSave = { savedProduct ->
                if (productToEdit == null) {
                    viewModel.addProduct(savedProduct) {
                        Toast.makeText(context, "नया सामान सफलतापूर्वक जोड़ा गया!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.updateProduct(savedProduct) {
                        Toast.makeText(context, "सामान अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                    }
                }
                showAddEditProductSheet = false
                productToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val target = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("सामान हटाएं?") },
            text = { Text("क्या आप \"${target.name}\" को अपनी लिस्ट से हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(target) {
                            Toast.makeText(context, "सामान हटा दिया गया", Toast.LENGTH_SHORT).show()
                        }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Full Screen Image Preview Dialog
    if (previewImageUri != null) {
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = previewImageUri,
                    contentDescription = "Product preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )
                IconButton(
                    onClick = { previewImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ControlCenterActionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badge,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
