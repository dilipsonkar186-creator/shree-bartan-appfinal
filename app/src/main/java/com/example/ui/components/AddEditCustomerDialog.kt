package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardOptions
import com.example.util.LocationUtils
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.entity.CustomerEntity
import com.example.util.PhoneNumberUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

val DOCUMENT_TYPES = listOf("Aadhaar Card", "PAN Card", "Driver's License", "Voter ID", "Other Document")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCustomerDialog(
    folderName: String,
    initialCustomer: CustomerEntity? = null,
    existingCustomers: List<CustomerEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        bookNumber: String,
        pageNumber: String,
        address: String,
        notes: String,
        photoUri: String?,
        documentType: String,
        documentNumber: String,
        documentPhotoUri: String?,
        latitude: Double?,
        longitude: Double?,
        smsNotificationsEnabled: Boolean,
        givenItems: List<GivenItem>
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("shop_profile_prefs", android.content.Context.MODE_PRIVATE) }
    var isAutoCapitalizeEnabled by remember { mutableStateOf(prefs.getBoolean("auto_capitalize_customer_names", true)) }

    var name by remember {
        val rawName = initialCustomer?.name ?: ""
        mutableStateOf(if (isAutoCapitalizeEnabled && rawName.isNotBlank()) rawName.uppercase() else rawName)
    }
    var phone by remember {
        val initialPhone = initialCustomer?.phone ?: ""
        mutableStateOf(if (initialPhone.isNotBlank()) initialPhone else PhoneNumberUtils.DEFAULT_PHONE_PREFIX)
    }
    var bookNumber by remember { mutableStateOf(initialCustomer?.bookNumber ?: "") }
    var pageNumber by remember { mutableStateOf(initialCustomer?.pageNumber ?: "") }
    var smsNotificationsEnabled by remember { mutableStateOf(initialCustomer?.smsNotificationsEnabled ?: true) }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var photoUri by remember { mutableStateOf(initialCustomer?.photoUri) }
    var documentType by remember { mutableStateOf(if (initialCustomer?.documentType.isNullOrBlank()) "Aadhaar Card" else initialCustomer!!.documentType) }
    var documentNumber by remember { mutableStateOf(initialCustomer?.documentNumber ?: "") }
    var documentPhotoUri by remember { mutableStateOf(initialCustomer?.documentPhotoUri) }
    var latitude by remember { mutableStateOf(initialCustomer?.latitude) }
    var longitude by remember { mutableStateOf(initialCustomer?.longitude) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var previewPhotoTitle by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedDocBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var nameError by remember { mutableStateOf(false) }

    // Real-time duplicate customer check and prefix matching within this folder
    val currentTrimmedName = name.trim()
    val matchingCustomers = remember(currentTrimmedName, existingCustomers, initialCustomer) {
        if (currentTrimmedName.isBlank()) emptyList()
        else existingCustomers.filter { existing ->
            (initialCustomer == null || existing.id != initialCustomer.id) &&
            existing.name.trim().startsWith(currentTrimmedName, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }
    }
    val duplicateCustomer = remember(currentTrimmedName, existingCustomers, initialCustomer) {
        if (currentTrimmedName.isBlank()) null
        else existingCustomers.firstOrNull { existing ->
            (initialCustomer == null || existing.id != initialCustomer.id) &&
            existing.name.trim().equals(currentTrimmedName, ignoreCase = true)
        }
    }
    val isDuplicateName = duplicateCustomer != null

    // Collapsible Identity Verification Section State (starts collapsed or expanded based on initialCustomer)
    var isIdentitySectionExpanded by remember {
        mutableStateOf(!initialCustomer?.documentNumber.isNullOrBlank() || !initialCustomer?.documentPhotoUri.isNullOrBlank())
    }

    // Launcher for Gallery Image Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            photoUri = it.toString()
            capturedBitmap = null
        }
    }

    // Launcher for Profile Photo Capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            capturedBitmap = it
            try {
                val file = File(context.cacheDir, "customer_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                photoUri = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Launcher for Document Scanner Photo Capture
    val docCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            capturedDocBitmap = it
            try {
                val file = File(context.cacheDir, "doc_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                documentPhotoUri = Uri.fromFile(file).toString()
                Toast.makeText(context, "$documentType photo captured & verified", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gallery launcher for document
    val docGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            documentPhotoUri = it.toString()
            capturedDocBitmap = null
            Toast.makeText(context, "$documentType uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission required to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Document Scanner
    val docCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            docCameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission required for scanning document", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to trigger High Accuracy Auto Location Detection (enableHighAccuracy: true, maximumAge: 0)
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        isDetectingLocation = true
        LocationUtils.fetchHighAccuracyLocation(context) { lat, lng, addr, errorMsg ->
            isDetectingLocation = false
            if (lat != null && lng != null) {
                latitude = lat
                longitude = lng
                if (!addr.isNullOrBlank()) {
                    address = addr
                    Toast.makeText(context, "Location set: $addr", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "GPS Coordinates saved (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, errorMsg ?: "Unable to detect GPS location. Please ensure location services are enabled.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission required for automatic location setting", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (initialCustomer == null) "Add Customer Record" else "Edit Customer Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Folder: $folderName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Photo Section (Compact)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(enabled = !photoUri.isNullOrEmpty()) {
                                previewPhotoUri = photoUri
                                previewPhotoTitle = if (name.isNotBlank()) name else "Customer Photo"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Camera / Gallery Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCamPerm) {
                                    cameraLauncher.launch()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.testTag("camera_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.testTag("gallery_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery", style = MaterialTheme.typography.labelSmall)
                        }

                        if (!photoUri.isNullOrEmpty() || capturedBitmap != null) {
                            IconButton(
                                onClick = {
                                    photoUri = null
                                    capturedBitmap = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = if (isAutoCapitalizeEnabled) it.uppercase() else it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = { Text(if (isAutoCapitalizeEnabled) "Customer Name (ALL CAPS) *" else "Customer Name *") },
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
                        capitalization = if (isAutoCapitalizeEnabled) KeyboardCapitalization.Characters else KeyboardCapitalization.Words
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
                            .testTag("dialog_prefix_matching_customers_card")
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
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
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
                                    "⚠️ An identical name is already registered. Similar names cannot be repeated. Please use a distinct name."
                                else
                                    "Customers in '$folderName' starting with '$currentTrimmedName':",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                matchingCustomers.forEach { cust ->
                                    val isExactDuplicate = cust.name.trim().equals(currentTrimmedName, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isExactDuplicate) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isExactDuplicate) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable {
                                            if (!isExactDuplicate) {
                                                name = if (isAutoCapitalizeEnabled) "${cust.name} 2".uppercase() else "${cust.name} 2"
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExactDuplicate) Icons.Default.Warning else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isExactDuplicate) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = cust.name,
                                                style = MaterialTheme.typography.labelMedium,
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
                        checked = isAutoCapitalizeEnabled,
                        onCheckedChange = { enabled ->
                            isAutoCapitalizeEnabled = enabled
                            prefs.edit().putBoolean("auto_capitalize_customer_names", enabled).apply()
                            if (enabled && name.isNotBlank()) {
                                name = name.uppercase()
                            }
                        },
                        modifier = Modifier.testTag("auto_capitalize_dialog_switch")
                    )
                }

                // Phone
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

                // Customer SMS Receipts Preference Switch
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { smsNotificationsEnabled = !smsNotificationsEnabled },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = null,
                                tint = if (smsNotificationsEnabled) Color(0xFF1565C0) else Color.Gray
                            )
                            Column {
                                Text(
                                    text = "Send SMS Receipts / Reminders",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (smsNotificationsEnabled) "SMS enabled for transactions" else "SMS disabled for this customer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = smsNotificationsEnabled,
                            onCheckedChange = { smsNotificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF1565C0)
                            ),
                            modifier = Modifier.testTag("sms_preference_switch")
                        )
                    }
                }

                // Address + Auto Location Setting Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Location") },
                        placeholder = { Text("123 Industrial Way, Sector 4") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        singleLine = true,
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
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine || hasCoarse) {
                                    fetchCurrentLocation()
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
                                text = "GPS: ${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)} (Tap to view on Map)",
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
                        // CLICKABLE HEADER (Clicking toggles collapse/expand of all document items)
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
                                            color = if (!documentPhotoUri.isNullOrEmpty() || capturedDocBitmap != null)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !documentPhotoUri.isNullOrEmpty()) {
                                            previewPhotoUri = documentPhotoUri
                                            previewPhotoTitle = "$documentType - ${if (name.isNotBlank()) name else "Document"}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedDocBitmap != null) {
                                        Image(
                                            bitmap = capturedDocBitmap!!.asImageBitmap(),
                                            contentDescription = "Scanned $documentType",
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (!documentPhotoUri.isNullOrEmpty()) {
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

                                    if (!documentPhotoUri.isNullOrEmpty() || capturedDocBitmap != null) {
                                        IconButton(onClick = {
                                            documentPhotoUri = null
                                            capturedDocBitmap = null
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

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Goods Specifications") },
                    placeholder = { Text("Delivery preferences, item sizes, special terms") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_notes_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else if (isDuplicateName) {
                        android.widget.Toast.makeText(
                            context,
                            "Cannot save: Similar/Duplicate name '${duplicateCustomer?.name}' already exists in this folder. Please use a distinct name.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val finalName = if (isAutoCapitalizeEnabled) name.trim().uppercase() else name.trim()
                        val normalizedPhone = PhoneNumberUtils.normalizeForStorage(phone)
                        onSave(
                            finalName,
                            normalizedPhone,
                            bookNumber.trim(),
                            pageNumber.trim(),
                            address.trim(),
                            notes.trim(),
                            photoUri,
                            documentType,
                            documentNumber.trim(),
                            documentPhotoUri,
                            latitude,
                            longitude,
                            smsNotificationsEnabled,
                            emptyList()
                        )
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(
                    text = if (initialCustomer == null) "Add Customer" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    color = if (isDuplicateName) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_customer_button")) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )

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

    if (!previewPhotoUri.isNullOrBlank()) {
        PhotoPreviewDialog(
            photoUri = previewPhotoUri,
            title = previewPhotoTitle,
            onDismiss = { previewPhotoUri = null }
        )
    }
}
