package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShopProfile
import com.example.util.CloudinaryManager
import com.example.util.PhoneNumberUtils
import kotlinx.coroutines.launch

@Composable
fun EditShopProfileDialog(
    currentProfile: ShopProfile,
    onDismiss: () -> Unit,
    onSave: (ShopProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(currentProfile.name) }
    var hindiName by remember { mutableStateOf(currentProfile.hindiName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var altPhone by remember { mutableStateOf(currentProfile.altPhone) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var tagline by remember { mutableStateOf(currentProfile.tagline) }
    var gstNumber by remember { mutableStateOf(currentProfile.gstNumber) }
    var shopTimings by remember { mutableStateOf(currentProfile.shopTimings) }
    var notice by remember { mutableStateOf(currentProfile.notice) }
    var bannerUrl by remember { mutableStateOf(currentProfile.bannerUrl) }

    var isUploadingBanner by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf("") }

    // Banner image picker launcher
    val bannerPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            bannerUrl = uriString
            // Automatically upload to Cloudinary
            coroutineScope.launch {
                isUploadingBanner = true
                uploadStatusMessage = "बैनर इमेज Cloudinary पर अपलोड हो रही है... ☁️"
                val result = CloudinaryManager.uploadImage(context, uriString)
                isUploadingBanner = false
                result.fold(
                    onSuccess = { secureUrl ->
                        bannerUrl = secureUrl
                        uploadStatusMessage = "बैनर इमेज सफलतापूर्वक अपलोड हो गई! ✅"
                        Toast.makeText(context, "बैनर फोटो सफलतापूर्वक अपलोड हो गई! ✅", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        uploadStatusMessage = "अपलोड में त्रुटि: ${err.message ?: "अज्ञात त्रुटि"}"
                        Toast.makeText(context, "अपलोड विफल: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
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
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "शॉप प्रोफाइल व लाइव नोटिस",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shop Name English
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("दुकान का नाम (Shop Name)") },
                    placeholder = { Text("e.g. Shree Bartan Store") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_name_input")
                )

                // Shop Name Hindi
                OutlinedTextField(
                    value = hindiName,
                    onValueChange = { hindiName = it },
                    label = { Text("दुकान का हिंदी नाम") },
                    placeholder = { Text("e.g. श्री बर्तन स्टोर") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tagline / Specialty
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("टैगलाइन (Tagline / Description)") },
                    placeholder = { Text("e.g. Quality Utensils & Stainless Steel Kitchenware") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Primary Mobile / WhatsApp Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = PhoneNumberUtils.formatInputPhone(it) },
                    label = { Text("दुकान का फोन / WhatsApp नंबर") },
                    placeholder = { Text("+91 98765 43210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_phone_input")
                )

                // Alternate Mobile Number
                OutlinedTextField(
                    value = altPhone,
                    onValueChange = { altPhone = PhoneNumberUtils.formatInputPhone(it) },
                    label = { Text("अतिरिक्त फोन नंबर (Alternate Mobile)") },
                    placeholder = { Text("+91 98765 43211") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("दुकान का पता (Address)") },
                    placeholder = { Text("e.g. मेन मार्केट, गोला रोड, बर्तन बाजार") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Shop Timings
                OutlinedTextField(
                    value = shopTimings,
                    onValueChange = { shopTimings = it },
                    label = { Text("दुकान खुलने व बंद होने का समय (Timings)") },
                    placeholder = { Text("e.g. सुबह 9:00 AM से रात 9:00 PM (रविवार 2 बजे तक)") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Live Notice / Festival Wishes
                OutlinedTextField(
                    value = notice,
                    onValueChange = { notice = it },
                    label = { Text("📢 आज का मुख्य नोटिस / त्यौहार की बधाई") },
                    placeholder = { Text("e.g. धनतेरस पर हर खरीद पर उपहार या रविवार को दुकान 2 बजे तक") },
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFE65100)) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // -------------------------------------------------------------
                // LIVE BANNER / OFFER PHOTO UPLOAD & PREVIEW
                // -------------------------------------------------------------
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "लाइव बैनर / ऑफर फोटो (Shop Banner)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Size guideline chip/notice
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE3F2FD),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF90CAF9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "📐 अनुशंसित साइज (Recommended Size):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    text = "• साइज: 1200 × 400 पिक्सल (अनुपात 3:1 हॉरिजॉन्टल)\n• फॉर्मेट: JPG, PNG, WEBP (अधिकतम 5MB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }

                        // Image Preview Card (if bannerUrl is present)
                        if (bannerUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF263238))
                                    .border(1.dp, Color(0xFF90A4AE), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Banner Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                // Clear/Delete banner button
                                IconButton(
                                    onClick = { bannerUrl = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "हटाएं",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (bannerUrl.startsWith("http://") || bannerUrl.startsWith("https://")) {
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 6.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.9f),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Cloud Ready",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Upload status indicator
                        if (isUploadingBanner) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = uploadStatusMessage.ifBlank { "Cloudinary पर फोटो अपलोड हो रही है... ☁️" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Action Buttons: Pick Photo & manual URL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    bannerPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isUploadingBanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (bannerUrl.isBlank()) "फ़ोन से फोटो अपलोड करें" else "फोटो बदलें",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // URL Fallback TextField (editable if user has a custom external link)
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            label = { Text("या फोटो डायरेक्ट लिंक (Image URL)") },
                            placeholder = { Text("https://res.cloudinary.com/... (स्वचालित भरेगा)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // GST Number
                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = { gstNumber = it },
                    label = { Text("GSTIN (वैकल्पिक)") },
                    placeholder = { Text("e.g. 09ABCDE1234F1Z5") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isUploadingBanner) return@Button
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        // If bannerUrl is a local uri and wasn't uploaded yet, upload it before saving
                        if (bannerUrl.isNotBlank() && !bannerUrl.startsWith("http://") && !bannerUrl.startsWith("https://")) {
                            coroutineScope.launch {
                                isUploadingBanner = true
                                val uploadResult = CloudinaryManager.uploadImage(context, bannerUrl)
                                isUploadingBanner = false
                                val finalBannerUrl = uploadResult.getOrDefault(bannerUrl)
                                onSave(
                                    ShopProfile(
                                        name = name.trim(),
                                        hindiName = hindiName.trim(),
                                        phone = PhoneNumberUtils.normalizeForStorage(phone),
                                        altPhone = PhoneNumberUtils.normalizeForStorage(altPhone),
                                        address = address.trim(),
                                        tagline = tagline.trim(),
                                        gstNumber = gstNumber.trim(),
                                        shopTimings = shopTimings.trim(),
                                        notice = notice.trim(),
                                        bannerUrl = finalBannerUrl.trim()
                                    )
                                )
                            }
                        } else {
                            onSave(
                                ShopProfile(
                                    name = name.trim(),
                                    hindiName = hindiName.trim(),
                                    phone = PhoneNumberUtils.normalizeForStorage(phone),
                                    altPhone = PhoneNumberUtils.normalizeForStorage(altPhone),
                                    address = address.trim(),
                                    tagline = tagline.trim(),
                                    gstNumber = gstNumber.trim(),
                                    shopTimings = shopTimings.trim(),
                                    notice = notice.trim(),
                                    bannerUrl = bannerUrl.trim()
                                )
                            )
                        }
                    }
                },
                enabled = !isUploadingBanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                ),
                modifier = Modifier.testTag("save_shop_profile_cloud_btn")
            ) {
                if (isUploadingBanner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("अपलोड हो रहा है...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Cloud", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploadingBanner
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

