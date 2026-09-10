package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.GlobalSmsConfig
import com.example.util.SmsUtils

@Composable
fun GlobalSmsSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig = remember { SmsUtils.getGlobalSmsConfig(context) }

    var storeName by remember { mutableStateOf(currentConfig.storeName) }
    var storeFooter by remember { mutableStateOf(currentConfig.customStoreFooter) }
    var globalSmsEnabled by remember { mutableStateOf(currentConfig.globalSmsEnabled) }
    var defaultLanguage by remember { mutableStateOf(currentConfig.defaultLanguage) }
    var defaultStyle by remember { mutableStateOf(currentConfig.defaultStyle) }
    var customReminderNote by remember { mutableStateOf(currentConfig.customReminderNote) }

    // Preview message tabs: 0 = Payment Reminder, 1 = Account Summary, 2 = Sale Entry Bill
    var previewTab by remember { mutableStateOf(0) }
    var previewMessageText by remember { mutableStateOf("") }

    fun updatePreview() {
        val tempConfig = GlobalSmsConfig(
            storeName = storeName.ifBlank { "Shree Bartan Store" },
            globalSmsEnabled = globalSmsEnabled,
            defaultLanguage = defaultLanguage,
            defaultStyle = defaultStyle,
            customReminderNote = customReminderNote.ifBlank { "कृपया जल्द से जल्द बकाया राशि का भुगतान करें। धन्यवाद!" },
            customStoreFooter = storeFooter.ifBlank { storeName }
        )

        previewMessageText = when (previewTab) {
            0 -> SmsUtils.buildPaymentReminderSms(
                customerName = "राम कुमार (उदाहरण)",
                dues = 1500.0,
                isBrief = defaultStyle == "BRIEF",
                context = context
            )
            1 -> SmsUtils.buildAccountSummarySms(
                customerName = "राम कुमार (उदाहरण)",
                totalGoods = 2500.0,
                totalPaid = 1000.0,
                dues = 1500.0,
                isBrief = defaultStyle == "BRIEF",
                context = context
            )
            else -> SmsUtils.buildSingleTransactionSms(
                customerName = "राम कुमार (उदाहरण)",
                type = "GOODS_PROVIDED",
                itemDescription = "स्टील टंकी 50 लीटर",
                quantity = 1.0,
                unitType = "pcs",
                unitPrice = 1500.0,
                totalAmount = 1500.0,
                updatedDues = 1500.0,
                notes = "उधार बिल",
                context = context
            )
        }
    }

    LaunchedEffect(storeName, storeFooter, defaultLanguage, defaultStyle, customReminderNote, previewTab) {
        updatePreview()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8EAF6),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        tint = Color(0xFF1A237E),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "SMS & Message Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "सभी ग्राहकों के लिए डिफ़ॉल्ट संदेश सेटिंग",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 यहाँ की गई सेटिंग सभी कस्टमर के लिए एक समान लागू होगी। अलग-अलग कस्टमर के लिए मैसेज सेट करने की आवश्यकता नहीं है।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Global SMS Toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            globalSmsEnabled = !globalSmsEnabled
                        },
                    color = if (globalSmsEnabled) Color(0xFFE8EAF6) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (globalSmsEnabled) "सभी ग्राहकों को SMS/WhatsApp सुविधा: चालू" else "सभी ग्राहकों को SMS/WhatsApp सुविधा: बंद",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (globalSmsEnabled) Color(0xFF1A237E) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "बिल या पेमेंट दर्ज करते समय रसीद भेजने का विकल्प सक्रिय रहेगा",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = globalSmsEnabled,
                            onCheckedChange = { globalSmsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF1A237E)
                            )
                        )
                    }
                }

                // Language Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "मैसेज की भाषा (Message Language)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = defaultLanguage == "HINDI",
                            onClick = { defaultLanguage = "HINDI" },
                            label = { Text("🇮🇳 हिंदी (Hindi)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = defaultLanguage == "ENGLISH",
                            onClick = { defaultLanguage = "ENGLISH" },
                            label = { Text("🔤 English") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Message Style Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "मैसेज फॉर्मेट (Message Format)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = defaultStyle == "DETAILED",
                            onClick = { defaultStyle = "DETAILED" },
                            label = { Text("📋 विस्तृत (Detailed)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = defaultStyle == "BRIEF",
                            onClick = { defaultStyle = "BRIEF" },
                            label = { Text("⚡ संक्षिप्त (Brief SMS)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Store Name & Footer
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("दुकान / स्टोर का नाम (Store Name)") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customReminderNote,
                    onValueChange = { customReminderNote = it },
                    label = { Text("डिफ़ॉल्ट रिमाइंडर संदेश (Reminder Note)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                OutlinedTextField(
                    value = storeFooter,
                    onValueChange = { storeFooter = it },
                    label = { Text("मैसेज के अंत में हस्ताक्षर (Store Sign / Footer)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Message Live Preview Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "मैसेज का लाइव प्रीव्यू (Live Preview)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = previewTab == 0,
                            onClick = { previewTab = 0 },
                            label = { Text("उधार तकादा (Reminder)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = previewTab == 1,
                            onClick = { previewTab = 1 },
                            label = { Text("खाता विवरण (Summary)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = previewTab == 2,
                            onClick = { previewTab = 2 },
                            label = { Text("बिल (Bill Entry)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = previewMessageText,
                        onValueChange = { previewMessageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                SmsUtils.shareMessage(context, previewMessageText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Share / WhatsApp", style = MaterialTheme.typography.labelSmall)
                        }

                        IconButton(
                            onClick = {
                                SmsUtils.copyToClipboard(context, previewMessageText)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Preview")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedConfig = GlobalSmsConfig(
                        storeName = storeName.ifBlank { "Shree Bartan Store" },
                        globalSmsEnabled = globalSmsEnabled,
                        defaultLanguage = defaultLanguage,
                        defaultStyle = defaultStyle,
                        customReminderNote = customReminderNote.ifBlank { "कृपया जल्द से जल्द बकाया राशि का भुगतान करें। धन्यवाद!" },
                        customStoreFooter = storeFooter.ifBlank { storeName }
                    )
                    SmsUtils.saveGlobalSmsConfig(context, updatedConfig)
                    Toast.makeText(context, "मैसेज व SMS सेटिंग सफलतापूर्वक सेव हो गई!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A237E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("सेव करें (Save Settings)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें (Close)")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
