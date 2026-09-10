package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FolderViewModel

@Composable
fun PublishAnnouncementDialog(
    onDismiss: () -> Unit,
    viewModel: FolderViewModel? = null,
    onUpdateNotice: ((String, () -> Unit, (Throwable) -> Unit) -> Unit)? = null
) {
    val liveNotice = viewModel?.shopProfile?.collectAsStateWithLifecycle()?.value?.notice
    var noticeText by remember(liveNotice) { 
        mutableStateOf(
            if (!liveNotice.isNullOrBlank()) liveNotice
            else "शुभ धनतेरस एवं दीपावली! श्री बर्तन भण्डार की ओर से सभी ग्राहकों को हार्दिक शुभकामनाएं 🪔✨"
        ) 
    }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    fun updateShopNoticeAndInfo(
        noticeMessage: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (onUpdateNotice != null) {
            onUpdateNotice(noticeMessage, onSuccess, onError)
        } else if (viewModel != null) {
            viewModel.updateShopNoticeAndInfo(noticeMessage, onSuccess, onError)
        } else {
            onError(IllegalStateException("ViewModel not available"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ग्राहकों को बधाई / ऑफर भेजें") },
        text = {
            Column {
                Text(
                    text = "यहाँ लिखा संदेश सभी ग्राहकों के ऐप में होम स्क्रीन पर तुरंत लाइव दिखेगा:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noticeText,
                    onValueChange = { noticeText = it },
                    label = { Text("संदेश / बधाई / ऑफर") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = statusMessage, color = Color(0xFF2E7D32))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSending = true
                    updateShopNoticeAndInfo(
                        noticeMessage = noticeText,
                        onSuccess = {
                            isSending = false
                            statusMessage = "सफलतापूर्वक सभी ग्राहकों के ऐप में अपडेट हो गया!"
                        },
                        onError = {
                            isSending = false
                            statusMessage = "त्रुटि: ${it.message}"
                        }
                    )
                },
                enabled = !isSending && noticeText.isNotBlank()
            ) {
                Text(if (isSending) "अपडेट हो रहा है..." else "लाइव पब्लिश करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}
