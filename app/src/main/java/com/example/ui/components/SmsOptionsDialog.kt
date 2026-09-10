package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import com.example.data.entity.CustomerEntity
import com.example.util.SmsUtils

@Composable
fun SmsOptionsDialog(
    customer: CustomerEntity,
    totalGoods: Double,
    totalPaid: Double,
    dues: Double,
    onDismiss: () -> Unit,
    onToggleSmsPreference: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isSmsEnabled by remember { mutableStateOf(customer.smsNotificationsEnabled) }

    // Message Types: 0 = Account Summary, 1 = Payment Reminder, 2 = Custom Message
    var selectedType by remember { mutableStateOf(0) }
    var isBriefFormat by remember { mutableStateOf(false) }

    var customMessageText by remember { mutableStateOf("") }
    var previewMessage by remember { mutableStateOf("") }

    fun generatePreview() {
        previewMessage = when (selectedType) {
            0 -> SmsUtils.buildAccountSummarySms(
                customerName = customer.name,
                totalGoods = totalGoods,
                totalPaid = totalPaid,
                dues = dues,
                isBrief = isBriefFormat
            )
            1 -> SmsUtils.buildPaymentReminderSms(
                customerName = customer.name,
                dues = dues,
                isBrief = isBriefFormat
            )
            else -> customMessageText.ifBlank {
                "Dear ${customer.name}, regarding your account at Shree Bartan Store: Current Dues Balance is Rs. ${String.format("%.2f", dues)}."
            }
        }
    }

    LaunchedEffect(selectedType, isBriefFormat, customMessageText, customer, dues, totalGoods, totalPaid) {
        generatePreview()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = Color(0xFF1565C0)
                )
                Column {
                    Text(
                        text = "SMS & Message Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customer: ${customer.name}",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer SMS Preference Setting Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val newValue = !isSmsEnabled
                            isSmsEnabled = newValue
                            onToggleSmsPreference(newValue)
                        },
                    color = if (isSmsEnabled) Color(0xFFE8EAF6) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
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
                                text = if (isSmsEnabled) "Customer Wants SMS: YES" else "Customer Wants SMS: NO",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSmsEnabled) Color(0xFF1A237E) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isSmsEnabled) "SMS receipt toggle will be ON by default for transactions" else "SMS notifications disabled in customer settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSmsEnabled,
                            onCheckedChange = { newValue ->
                                isSmsEnabled = newValue
                                onToggleSmsPreference(newValue)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF1A237E)
                            )
                        )
                    }
                }

                // Choose Message Template
                Text(
                    text = "Select Message Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == 0,
                        onClick = { selectedType = 0 },
                        label = { Text("Account Summary", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 },
                        label = { Text("Payment Reminder", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = selectedType == 2,
                        onClick = { selectedType = 2 },
                        label = { Text("Custom Message", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Format Style Selector (Detailed vs Brief)
                if (selectedType != 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Style:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        FilterChip(
                            selected = !isBriefFormat,
                            onClick = { isBriefFormat = false },
                            label = { Text("Detailed", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = isBriefFormat,
                            onClick = { isBriefFormat = true },
                            label = { Text("Brief SMS", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Custom Text Input if Custom Message selected
                if (selectedType == 2) {
                    OutlinedTextField(
                        value = customMessageText,
                        onValueChange = { customMessageText = it },
                        label = { Text("Custom SMS Content") },
                        placeholder = { Text("Type custom message for ${customer.name}...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                // Editable Message Preview Box
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Message Preview & Edit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = previewMessage,
                        onValueChange = { previewMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                // Quick Send Options
                Text(
                    text = "Send via:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SmsUtils.openSmsComposer(context, customer.phone, previewMessage)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("SMS App", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            SmsUtils.shareMessage(context, previewMessage)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share / WhatsApp", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    IconButton(
                        onClick = {
                            SmsUtils.copyToClipboard(context, previewMessage)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Message")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
