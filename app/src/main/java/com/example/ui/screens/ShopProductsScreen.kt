package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ProductEntity
import com.example.ui.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val PRODUCT_CATEGORIES = listOf(
    "सभी",
    "प्रेशर कुकर",
    "कड़ाही व पैन",
    "थाली व डिनर सेट",
    "भगोने व पतीले",
    "नॉन-स्टिक बर्तन",
    "पीतल व तांबा",
    "पूजा बर्तन",
    "अन्य"
)

private val METAL_TYPES = listOf(
    "स्टेनलेस स्टील",
    "पीतल",
    "तांबा",
    "एल्युमिनियम",
    "नॉन-स्टिक",
    "कांस्य/कांसा",
    "अन्य"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProductsScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedProductCategory.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var productToBroadcastOffer by remember { mutableStateOf<ProductEntity?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchesCategory = selectedCategory == "सभी" || product.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.metalType.contains(searchQuery, ignoreCase = true) ||
                    product.sizeSpec.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val inStockCount = remember(allProducts) { allProducts.count { it.inStock } }
    val outOfStockCount = remember(allProducts) { allProducts.count { !it.inStock } }

    val productsListState = rememberLazyListState()

    // If target product from notification arrives, auto-reset search/filter to ensure it's visible and scroll to it
    val targetProductId by viewModel.notificationTargetProductId.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(targetProductId, allProducts) {
        if (targetProductId != null) {
            val targetIdx = filteredProducts.indexOfFirst { it.id == targetProductId }
            if (targetIdx >= 0) {
                productsListState.animateScrollToItem(targetIdx)
            } else {
                // If filtered out, reset category and search query
                if (selectedCategory != "सभी") {
                    viewModel.setSelectedProductCategory("सभी")
                }
                if (searchQuery.isNotBlank()) {
                    viewModel.setProductSearchQuery("")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "शॉप प्रोडक्ट एंड कैटलॉग",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allProducts.size} प्रोडक्ट्स लिस्टेड",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("नया सामान जोड़ें") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_product")
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isSyncingCloud,
            onRefresh = {
                isSyncingCloud = true
                viewModel.syncAllProductImagesToCloudinary(
                    context = context,
                    onComplete = { count, success ->
                        isSyncingCloud = false
                        if (success) {
                            if (count > 0) {
                                Toast.makeText(context, "$count फ़ोटो Cloudinary पर अपलोड हुईं और डेटा सिंक हो गया! ☁️", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "सारे प्रोडक्ट्स और फ़ोटो Cloudinary पर अप-टू-डेट हैं! ☁️", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "क्लाउड सिंक में समस्या आई। इंटरनेट चेक करें।", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("pull_to_refresh_products")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Search Bar
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setProductSearchQuery(it) },
                    placeholder = { Text("बर्तन खोजें (कुकर, कड़ाही, थाली...)", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setProductSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Quick Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("कुल आइटम", style = MaterialTheme.typography.labelSmall)
                        Text("${allProducts.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("स्टॉक में", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        Text("$inStockCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)))
                    }
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("आउट ऑफ स्टॉक", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                        Text("$outOfStockCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFC62828)))
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRODUCT_CATEGORIES) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedProductCategory(category) },
                        label = { Text(category, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "सभी")
                                "कोई सामान नहीं मिला"
                            else
                                "अभी तक कोई सामान नहीं जोड़ा गया है",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "नया बर्तन जोड़ने के लिए नीचे (+) बटन दबाएं",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                productToEdit = null
                                showAddEditSheet = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सामान जोड़ें")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = productsListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            shopPhone = shopProfile.phone,
                            shopName = shopProfile.hindiName.ifBlank { shopProfile.name.ifBlank { "श्री बर्तन भंडार" } },
                            onToggleStock = { viewModel.toggleProductStock(product) },
                            onEdit = {
                                productToEdit = product
                                showAddEditSheet = true
                            },
                            onDelete = {
                                productToDelete = product
                            },
                            onImageClick = { uri ->
                                previewImageUri = uri
                            },
                            onBroadcastOffer = {
                                productToBroadcastOffer = product
                            }
                        )
                    }
                }
            }
        }
    }
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
                    text = "क्या आप \"${prod.name}\" (कीमत: ₹${String.format("%,.0f", prod.sellingPrice)}) का स्पेशल ऑफर नोटिफिकेशन सभी ग्राहकों (all_customers) को भेजना चाहते हैं?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prod
                        productToBroadcastOffer = null
                        viewModel.broadcastProductOfferPush(p) { success, msg ->
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
    if (showAddEditSheet) {
        AddEditProductBottomSheet(
            product = productToEdit,
            onDismiss = { showAddEditSheet = false },
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
                showAddEditSheet = false
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
internal fun ProductItemCard(
    product: ProductEntity,
    shopPhone: String,
    shopName: String,
    onToggleStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    onBroadcastOffer: () -> Unit
) {
    val context = LocalContext.current
    val imageList = remember(product.imageUris) {
        product.imageUris.split(",").filter { it.isNotBlank() }
    }
    val firstImage = imageList.firstOrNull()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val discountPercent = remember(product.mrp, product.sellingPrice) {
        if (product.mrp > product.sellingPrice && product.mrp > 0) {
            (((product.mrp - product.sellingPrice) / product.mrp) * 100).toInt()
        } else {
            0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(enabled = firstImage != null) {
                            if (firstImage != null) onImageClick(firstImage)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (firstImage != null) {
                        AsyncImage(
                            model = firstImage,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (imageList.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(topStart = 6.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "+${imageList.size - 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product Details
                Column(modifier = Modifier.weight(1f)) {
                    // Category & Metal tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (product.metalType.isNotBlank() && product.metalType != "अन्य") {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = product.metalType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (product.sizeSpec.isNotBlank()) {
                        Text(
                            text = "साइज / विवरण: ${product.sizeSpec}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pricing Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currencyFormat.format(product.sellingPrice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        )

                        if (product.mrp > product.sellingPrice && product.mrp > 0) {
                            Text(
                                text = currencyFormat.format(product.mrp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )

                            if (discountPercent > 0) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "$discountPercent% छूट",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action & Stock row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // In Stock Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleStock() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Switch(
                        checked = product.inStock,
                        onCheckedChange = { onToggleStock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.inStock) "स्टॉक में है" else "आउट ऑफ स्टॉक",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.inStock) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                // Edit, Delete and WhatsApp Share
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Broadcast Offer Push Button
                    Button(
                        onClick = onBroadcastOffer,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("product_offer_push_${product.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEDE7F6),
                            contentColor = Color(0xFF6200EA)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Broadcast Offer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ऑफर", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // WhatsApp Share
                    OutlinedButton(
                        onClick = {
                            val priceStr = currencyFormat.format(product.sellingPrice)
                            val shareMessage = "🛍️ *$shopName*\n\n" +
                                    "उत्पाद: *${product.name}*\n" +
                                    (if (product.sizeSpec.isNotBlank()) "विवरण: ${product.sizeSpec}\n" else "") +
                                    "कीमत: *$priceStr*\n\n" +
                                    "दुकान से लेने या ऑर्डर करने के लिए संपर्क करें।"

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(intent, "प्रोडक्ट शेयर करें"))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("शेयर", fontSize = 12.sp)
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddEditProductBottomSheet(
    product: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "प्रेशर कुकर") }
    var metalType by remember { mutableStateOf(product?.metalType ?: "स्टेनलेस स्टील") }
    var sizeSpec by remember { mutableStateOf(product?.sizeSpec ?: "") }
    var mrpText by remember { mutableStateOf(if (product != null && product.mrp > 0) product.mrp.toString() else "") }
    var sellingPriceText by remember { mutableStateOf(if (product != null && product.sellingPrice > 0) product.sellingPrice.toString() else "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var inStock by remember { mutableStateOf(product?.inStock ?: true) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploadingImages by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("") }

    val imageUris = remember {
        mutableStateListOf<String>().apply {
            if (product != null && product.imageUris.isNotBlank()) {
                addAll(product.imageUris.split(",").filter { it.isNotBlank() })
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val uriStr = uri.toString()
            if (!imageUris.contains(uriStr) && imageUris.size < 4) {
                imageUris.add(uriStr)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (product == null) "नया सामान जोड़ें (Add Product)" else "सामान एडिट करें (Edit Product)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Multiple Photo Picker & Thumbnails
            Column {
                Text(
                    text = "प्रोडक्ट फ़ोटो (अधिकतम 4 फ़ोटो):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selected photos preview
                    imageUris.forEachIndexed { index, uriStr ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uriStr,
                                contentDescription = "Product image $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUris.removeAt(index) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Add photo button
                    if (imageUris.size < 4) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "फ़ोटो जोड़ें",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("बर्तन का नाम *") },
                placeholder = { Text("जैसे: 5L प्रेस्टीज कुकर, पीतल की थाली") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Chips
            Column {
                Text(
                    text = "कैटेगरी (Category):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PRODUCT_CATEGORIES.filter { it != "सभी" }.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Metal Type
            Column {
                Text(
                    text = "धातु का प्रकार (Metal):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    METAL_TYPES.forEach { metal ->
                        FilterChip(
                            selected = metalType == metal,
                            onClick = { metalType = metal },
                            label = { Text(metal, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Size / Specs
            OutlinedTextField(
                value = sizeSpec,
                onValueChange = { sizeSpec = it },
                label = { Text("साइज / क्षमता / वजन") },
                placeholder = { Text("जैसे: 3 Litre, 5 Litre, 24cm, 1.5 Kg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Pricing Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = mrpText,
                    onValueChange = { mrpText = it },
                    label = { Text("MRP (₹)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    label = { Text("दुकान का रेट (₹) *") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Description / Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("विवरण / नोट (वैकल्पिक)") },
                placeholder = { Text("जैसे: इंडक्शन बेस, 5 साल वारंटी, हैवी बॉटम") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // In Stock Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "स्टॉक में उपलब्ध है (In Stock):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Switch(
                    checked = inStock,
                    onCheckedChange = { inStock = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF2E7D32)
                    )
                )
            }

            // Cloudinary Upload Status Banner
            if (isUploadingImages) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = uploadStatusText.ifBlank { "Cloudinary पर फ़ोटो सुरक्षित अपलोड हो रही है... ☁️" },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank() || isUploadingImages) return@Button
                    val mrp = mrpText.toDoubleOrNull() ?: 0.0
                    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: mrp

                    coroutineScope.launch {
                        val currentImages = imageUris.toList()
                        val hasLocalImages = currentImages.any { !it.startsWith("http://") && !it.startsWith("https://") }

                        val finalImageUris = if (hasLocalImages) {
                            isUploadingImages = true
                            uploadStatusText = "Cloudinary पर फ़ोटो अपलोड हो रही है... ☁️"
                            val uploaded = com.example.util.CloudinaryManager.uploadMultipleImages(
                                context = context,
                                uris = currentImages,
                                onProgress = { current, total ->
                                    uploadStatusText = "फ़ोटो $current/$total Cloudinary पर अपलोड हो रही है... ☁️"
                                }
                            )
                            isUploadingImages = false
                            uploaded
                        } else {
                            currentImages
                        }

                        val updatedProduct = (product ?: ProductEntity(
                            name = name.trim(),
                            category = category,
                            metalType = metalType,
                            sizeSpec = sizeSpec.trim(),
                            mrp = mrp,
                            sellingPrice = sellingPrice,
                            description = description.trim(),
                            imageUris = finalImageUris.joinToString(","),
                            inStock = inStock
                        )).copy(
                            name = name.trim(),
                            category = category,
                            metalType = metalType,
                            sizeSpec = sizeSpec.trim(),
                            mrp = mrp,
                            sellingPrice = sellingPrice,
                            description = description.trim(),
                            imageUris = finalImageUris.joinToString(","),
                            inStock = inStock,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updatedProduct)
                    }
                },
                enabled = name.isNotBlank() && !isUploadingImages,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploadingImages) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloudinary पर अपलोड हो रहा है...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product == null) "सामान सेव करें" else "अपडेट सेव करें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
