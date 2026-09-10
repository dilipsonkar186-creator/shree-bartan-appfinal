package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import com.example.util.GoogleDriveManager
import com.example.util.PhotoStorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import com.example.ui.components.LocationPickerMapDialog
import com.example.util.LocationUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.entity.CustomerEntity
import com.example.ui.components.DOCUMENT_TYPES
import com.example.ui.viewmodel.FolderViewModel
import com.example.util.PhoneNumberUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditCustomerScreen(
    folderId: Long,
    customerId: Long? = null,
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onCustomerCreated: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val folder by viewModel.getFolderFlow(folderId).collectAsState(initial = null)
    val autoCapitalizeSetting by viewModel.autoCapitalizeCustomerNames.collectAsState()
    val existingCustomersInFolder by viewModel.getCustomersForFolderFlow(folderId).collectAsState(initial = emptyList())
    val customerToEdit by if (customerId != null) {
        viewModel.getCustomerFlow(customerId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<CustomerEntity?>(null) }
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(PhoneNumberUtils.DEFAULT_PHONE_PREFIX) }
    var bookNumber by remember { mutableStateOf("") }
    var pageNumber by remember { mutableStateOf("") }
    var smsNotificationsEnabled by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var documentType by remember { mutableStateOf("Aadhaar Card") }
    var documentNumber by remember { mutableStateOf("") }
    var documentPhotoUri by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    // Real-time duplicate customer check and prefix matching within this folder
    val currentTrimmedName = name.trim()

    // Existing customers in this folder starting with the exact typed prefix (e.g. 'R' -> R..., 'RI' -> RI..., 'RIN' -> RIN...)
    val matchingCustomers = remember(currentTrimmedName, existingCustomersInFolder, customerId) {
        if (currentTrimmedName.isBlank()) emptyList()
        else existingCustomersInFolder.filter { existing ->
            (customerId == null || existing.id != customerId) &&
            existing.name.trim().startsWith(currentTrimmedName, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }
    }

    // Exact duplicate match check within this folder (case-insensitive)
    val duplicateCustomer = remember(currentTrimmedName, existingCustomersInFolder, customerId) {
        if (currentTrimmedName.isBlank()) null
        else existingCustomersInFolder.firstOrNull { existing ->
            (customerId == null || existing.id != customerId) &&
            existing.name.trim().equals(currentTrimmedName, ignoreCase = true)
        }
    }
    val isDuplicateName = duplicateCustomer != null

    // Collapsible Identity Verification Section State (starts collapsed unless populated)
    var isIdentitySectionExpanded by remember { mutableStateOf(false) }

    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(customerToEdit) {
        customerToEdit?.let { cust ->
            if (!isInitialized) {
                name = cust.name
                phone = if (cust.phone.isNotBlank()) cust.phone else PhoneNumberUtils.DEFAULT_PHONE_PREFIX
                bookNumber = cust.bookNumber
                pageNumber = cust.pageNumber
                smsNotificationsEnabled = cust.smsNotificationsEnabled
                address = cust.address
                notes = cust.notes
                photoUri = cust.photoUri
                documentType = if (cust.documentType.isBlank()) "Aadhaar Card" else cust.documentType
                documentNumber = cust.documentNumber
                documentPhotoUri = cust.documentPhotoUri
                latitude = cust.latitude
                longitude = cust.longitude
                if (cust.documentNumber.isNotBlank() || !cust.documentPhotoUri.isNullOrEmpty()) {
                    isIdentitySectionExpanded = true
                }
                isInitialized = true
            }
        }
    }

    // Launchers for Gallery Image Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            photoUri = it.toString()
        }
    }

    val docGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            documentPhotoUri = it.toString()
        }
    }

    // Camera Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                val file = File(context.cacheDir, "customer_photo_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                withContext(Dispatchers.Main) {
                    photoUri = Uri.fromFile(file).toString()
                }
            }
        }
    }

    val docCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                val file = File(context.cacheDir, "doc_photo_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                withContext(Dispatchers.Main) {
                    documentPhotoUri = Uri.fromFile(file).toString()
                }
            }
        }
    }

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch()
        else Toast.makeText(context, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show()
    }

    val docCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) docCameraLauncher.launch()
        else Toast.makeText(context, "Camera permission needed to scan documents", Toast.LENGTH_SHORT).show()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            isDetectingLocation = true
            LocationUtils.fetchHighAccuracyLocation(context) { lat, lng, addr, err ->
                isDetectingLocation = false
                if (lat != null && lng != null) {
                    latitude = lat
                    longitude = lng
                    if (!addr.isNullOrBlank()) address = addr
                    Toast.makeText(context, "Location set: ${addr ?: "GPS saved"}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, err ?: "Location unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSave() {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            nameError = true
            Toast.makeText(context, "Please enter customer name", Toast.LENGTH_SHORT).show()
        } else if (isDuplicateName) {
            showDuplicateDialog = true
            Toast.makeText(
                context,
                "Cannot save: Customer '${duplicateCustomer?.name}' already exists in this folder",
                Toast.LENGTH_LONG
            ).show()
        } else {
            scope.launch {
                val formattedName = if (autoCapitalizeSetting) trimmed.uppercase() else trimmed
                val normalizedPhone = PhoneNumberUtils.normalizeForStorage(phone)

                // Persist photos to permanent app internal storage
                val permanentPhotoUri = PhotoStorageManager.saveCustomerPhotoPermanently(context, photoUri, formattedName)
                val permanentDocUri = PhotoStorageManager.saveDocumentPhotoPermanently(context, documentPhotoUri, documentType)

                if (customerId == null) {
                    viewModel.addCustomer(
                        folderId = folderId,
                        name = formattedName,
                        phone = normalizedPhone,
                        bookNumber = bookNumber.trim(),
                        pageNumber = pageNumber.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        photoUri = permanentPhotoUri,
                        documentType = documentType,
                        documentNumber = documentNumber.trim(),
                        documentPhotoUri = permanentDocUri,
                        latitude = latitude,
                        longitude = longitude,
                        smsNotificationsEnabled = smsNotificationsEnabled,
                        onSuccess = { newId ->
                            Toast.makeText(context, "Customer Added Successfully", Toast.LENGTH_SHORT).show()
                            if (onCustomerCreated != null) {
                                onCustomerCreated(newId)
                            } else {
                                onBack()
                            }
                        }
                    )
                } else {
                    customerToEdit?.let { existing ->
                        viewModel.updateCustomer(
                            existing.copy(
                                name = formattedName,
                                phone = normalizedPhone,
                                bookNumber = bookNumber.trim(),
                                pageNumber = pageNumber.trim(),
                                address = address.trim(),
                                notes = notes.trim(),
                                photoUri = permanentPhotoUri,
                                documentType = documentType,
                                documentNumber = documentNumber.trim(),
                                documentPhotoUri = permanentDocUri,
                                latitude = latitude,
                                longitude = longitude,
                                smsNotificationsEnabled = smsNotificationsEnabled
                            )
                        )
                        Toast.makeText(context, "Customer Record Updated", Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (customerId == null) "Add New Customer" else "Edit Customer Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        folder?.let { f ->
                            Text(
                                text = "Folder: ${f.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    TextButton(
                        onClick = { handleSave() },
                        modifier = Modifier.testTag("save_customer_top_button")
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Photo Picker Area (Compact)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Customer Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    cameraLauncher.launch()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            label = { Text("Camera", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("camera_photo_button")
                        )

                        AssistChip(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            label = { Text("Gallery", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("gallery_photo_button")
                        )

                        if (!photoUri.isNullOrEmpty()) {
                            IconButton(
                                onClick = { photoUri = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Customer Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = if (autoCapitalizeSetting) it.uppercase() else it
                    if (it.isNotBlank()) nameError = false
                },
                label = { Text(if (autoCapitalizeSetting) "Customer Name (ALL CAPS) *" else "Customer Name *") },
                placeholder = { Text("e.g. Anju, Ramesh, Shree Bartan") },
                isError = nameError || isDuplicateName,
                supportingText = when {
                    isDuplicateName -> {
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Similar / Duplicate name '${duplicateCustomer?.name}' already exists in this folder",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    nameError -> { { Text("Customer name is required", color = MaterialTheme.colorScheme.error) } }
                    currentTrimmedName.isNotBlank() -> {
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "✓ Unique Name: '$currentTrimmedName' can be saved",
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> null
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isDuplicateName) Icons.Default.Warning else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = if (autoCapitalizeSetting) KeyboardCapitalization.Characters else KeyboardCapitalization.Words
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            // Showing all customers in this folder starting with the exact typed prefix (e.g. 'R', 'RI', 'RIN')
            if (currentTrimmedName.isNotBlank() && matchingCustomers.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDuplicateName) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (isDuplicateName) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prefix_matching_customers_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Existing Customers Starting with '$currentTrimmedName' (${matchingCustomers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = if (isDuplicateName)
                                "⚠️ An identical name is already registered. Similar names cannot be repeated. Please change or add a distinctive name (e.g. '${currentTrimmedName} 2' or '${currentTrimmedName} Sharma')."
                            else
                                "Here are existing customers in folder '${folder?.name ?: ""}' starting with '$currentTrimmedName'. Similar names should not be repeated:",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Flow of matching customers with prefix
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            matchingCustomers.forEach { cust ->
                                val isExactDuplicate = cust.name.trim().equals(currentTrimmedName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isExactDuplicate) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isExactDuplicate) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable {
                                        if (!isExactDuplicate) {
                                            // Suggest a unique alternative version
                                            name = if (autoCapitalizeSetting) "${cust.name} 2".uppercase() else "${cust.name} 2"
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExactDuplicate) Icons.Default.Warning else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isExactDuplicate) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = cust.name + if (cust.phone.isNotBlank()) " (${cust.phone})" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isExactDuplicate) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isExactDuplicate) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isExactDuplicate) {
                                            Text(
                                                text = "• Already Added",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Duplicate Customer Alert Banner during name entry
            AnimatedVisibility(
                visible = isDuplicateName,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duplicate_customer_alert_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Duplicate Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Similar / Duplicate Name Not Allowed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "A customer named '${duplicateCustomer?.name}' already exists in folder '${folder?.name ?: ""}'" +
                                        (if (!duplicateCustomer?.phone.isNullOrBlank()) " with phone ${duplicateCustomer?.phone}." else ".") +
                                        " Please save with a different name (e.g. '${duplicateCustomer?.name} 2', '${duplicateCustomer?.name} Sharma') to proceed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Auto-Capitalize Setting Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Capitalize Name (ALL CAPS / बड़े अक्षर)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoCapitalizeSetting,
                    onCheckedChange = { enabled ->
                        viewModel.updateAutoCapitalizeCustomerNames(enabled)
                        if (enabled && name.isNotBlank()) {
                            name = name.uppercase()
                        }
                    },
                    modifier = Modifier.testTag("auto_capitalize_screen_switch")
                )
            }

            // Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    phone = PhoneNumberUtils.formatInputPhone(input)
                },
                label = { Text("Mobile Number (India)") },
                placeholder = { Text("98765 43210") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    ) {
                        Text(text = "🇮🇳", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (phone.isNotBlank() && phone != PhoneNumberUtils.DEFAULT_PHONE_PREFIX) {
                        IconButton(onClick = { phone = PhoneNumberUtils.DEFAULT_PHONE_PREFIX }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear phone number",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        "Auto-includes country code +91 for India",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input")
            )

            // Book Number & Page Number Row (Compact & Side-by-Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Book Number / Bahi No.
                OutlinedTextField(
                    value = bookNumber,
                    onValueChange = { bookNumber = it },
                    label = { Text("Book No. (बही नं.)") },
                    placeholder = { Text("e.g. 1, A") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Book Number",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (bookNumber.isNotBlank()) {
                            IconButton(onClick = { bookNumber = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear book number",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_book_number_input")
                )

                // Page Number / Khata No.
                OutlinedTextField(
                    value = pageNumber,
                    onValueChange = { pageNumber = it },
                    label = { Text("Page No. (पेज नं.)") },
                    placeholder = { Text("e.g. 45, 12A") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Page Number",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (pageNumber.isNotBlank()) {
                            IconButton(onClick = { pageNumber = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear page number",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_page_number_input")
                )
            }

            // SMS Notification Switch
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "SMS Receipt Notifications",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Send automated SMS for new transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = smsNotificationsEnabled,
                        onCheckedChange = { smsNotificationsEnabled = it },
                        thumbContent = if (smsNotificationsEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            // Address & GPS Auto-Detect
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    placeholder = { Text("Enter address or pick on map") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val hasLoc = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasLoc) {
                                    isDetectingLocation = true
                                    LocationUtils.fetchHighAccuracyLocation(context) { lat, lng, addr, err ->
                                        isDetectingLocation = false
                                        if (lat != null && lng != null) {
                                            latitude = lat
                                            longitude = lng
                                            if (!addr.isNullOrBlank()) address = addr
                                            Toast.makeText(context, "Location set: ${addr ?: "GPS saved"}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, err ?: "Location unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("detect_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Detect Location",
                                tint = if (isDetectingLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val hasLoc = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasLoc) {
                                isDetectingLocation = true
                                LocationUtils.fetchHighAccuracyLocation(context) { lat, lng, addr, err ->
                                    isDetectingLocation = false
                                    if (lat != null && lng != null) {
                                        latitude = lat
                                        longitude = lng
                                        if (!addr.isNullOrBlank()) address = addr
                                        Toast.makeText(context, "Location set: ${addr ?: "GPS saved"}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, err ?: "Location unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        label = {
                            Text(if (isDetectingLocation) "Detecting..." else "GPS Auto-Detect")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_location_button")
                    )

                    AssistChip(
                        onClick = { showMapPicker = true },
                        label = { Text("Pick on Map (मैप)") },
                        leadingIcon = {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_on_map_button")
                    )
                }

                if (latitude != null && longitude != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { showMapPicker = true }
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "GPS: ${String.format(java.util.Locale.US, "%.4f", latitude)}, ${String.format(java.util.Locale.US, "%.4f", longitude)} (Tap to view on Map)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Identity Document Verification & Scanner Section (Collapsible Container Box)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // CLICKABLE HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isIdentitySectionExpanded = !isIdentitySectionExpanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Customer Identity Scan & Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isIdentitySectionExpanded) "Tap header to collapse section"
                                    else if (documentNumber.isNotBlank() || !documentPhotoUri.isNullOrEmpty()) "$documentType Attached • Tap to view"
                                    else "Aadhaar Card, PAN Card, Driver's License, Voter ID • Tap to expand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { isIdentitySectionExpanded = !isIdentitySectionExpanded }
                        ) {
                            Icon(
                                imageVector = if (isIdentitySectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isIdentitySectionExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // CONTENTS WITHIN CONTAINER BOX (Hidden when collapsed)
                    AnimatedVisibility(
                        visible = isIdentitySectionExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // Document Method Selection
                            Text(
                                text = "Verification Method / Document Type",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DOCUMENT_TYPES.forEach { doc ->
                                    val isSelected = documentType.equals(doc, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { documentType = doc },
                                        label = { Text(doc, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null,
                                        modifier = Modifier.testTag("doc_type_chip_$doc")
                                    )
                                }
                            }

                            // Document Number input field
                            OutlinedTextField(
                                value = documentNumber,
                                onValueChange = { documentNumber = it },
                                label = { Text("$documentType Number / ID") },
                                placeholder = { Text("e.g. 1234 5678 9012 or ABCDE1234F") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("document_number_input")
                            )

                            // Small Scan Bounding Box Camera Preview & Capture Area
                            Text(
                                text = "Scan / Capture $documentType",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (!documentPhotoUri.isNullOrEmpty())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!documentPhotoUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = documentPhotoUri,
                                        contentDescription = "Scanned $documentType",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Stylized Small Camera Scan Overlay
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 160.dp, height = 80.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "[ Align $documentType Here ]",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Keep card within the scan frame",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Action buttons to scan or upload document
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val hasCam = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasCam) {
                                            docCameraLauncher.launch()
                                        } else {
                                            docCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_document_camera_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan $documentType", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        docGalleryLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("upload_document_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                if (!documentPhotoUri.isNullOrEmpty()) {
                                    IconButton(onClick = {
                                        documentPhotoUri = null
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Scanned Document",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Additional Notes / Goods Specifications
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Goods Specifications") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("customer_notes_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Save Button
            Button(
                onClick = { handleSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_customer_main_button"),
                shape = RoundedCornerShape(12.dp),
                colors = if (isDuplicateName) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                Icon(
                    imageVector = if (isDuplicateName) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDuplicateName) "Customer Already Exists (Cannot Save)" else if (customerId == null) "Save Customer Record" else "Update Customer Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Duplicate Customer Alert Dialog
    if (showDuplicateDialog && duplicateCustomer != null) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Duplicate Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Customer Already Exists",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A customer named '${duplicateCustomer.name}' already exists in folder '${folder?.name ?: ""}'."
                    )
                    if (!duplicateCustomer.phone.isNullOrBlank()) {
                        Text(
                            text = "Existing Phone: ${duplicateCustomer.phone}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Duplicate customers cannot be added to the same folder to prevent accounting errors and confusion. Please use a unique name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDuplicateDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("OK, Change Name")
                }
            }
        )
    }

    if (showMapPicker) {
        LocationPickerMapDialog(
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialAddress = address,
            onDismiss = { showMapPicker = false },
            onLocationSelected = { lat, lng, addr ->
                latitude = lat
                longitude = lng
                address = addr
                showMapPicker = false
                Toast.makeText(context, "Location & Address applied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
