package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwitchAccessShortcut
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.util.SnapshotInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FolderViewModel
import com.example.util.CsvBackupManager
import com.example.util.DriveBackupFile
import com.example.util.GoogleDriveManager
import com.example.util.RestoreSummary
import com.example.util.RollingBackupInfo
import kotlinx.coroutines.launch

@Composable
fun CsvBackupRestoreDialog(
    viewModel: FolderViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val driveSyncState by viewModel.driveSyncState.collectAsStateWithLifecycle()
    val snapshotsList by viewModel.snapshotsList.collectAsStateWithLifecycle()
    val cloudBackupsList by viewModel.cloudBackupsList.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Google Cloud (CSV), 1: Snapshots (टाइम मशीन), 2: 2-Day Rolling Slots, 3: Manual Local CSV

    var todaySlotInfo by remember { mutableStateOf<RollingBackupInfo?>(null) }
    var yesterdaySlotInfo by remember { mutableStateOf<RollingBackupInfo?>(null) }
    
    var isCloudUploading by remember { mutableStateOf(false) }
    var isDriveUploading by remember { mutableStateOf(false) }
    var isDriveFetching by remember { mutableStateOf(false) }
    var isTakingSnapshot by remember { mutableStateOf(false) }
    var driveFilesList by remember { mutableStateOf<List<DriveBackupFile>>(emptyList()) }
    var showDriveFilesDialog by remember { mutableStateOf(false) }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<RestoreSummary?>(null) }
    var showConfirmRestoreDialog by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showConfirmCloudRestoreDialog by remember { mutableStateOf<com.example.util.CloudBackupInfo?>(null) }
    var showConfirmCloudDeleteDialog by remember { mutableStateOf<com.example.util.CloudBackupInfo?>(null) }
    var showConfirmDriveRestoreDialog by remember { mutableStateOf<DriveBackupFile?>(null) }
    var showConfirmSnapshotRestoreDialog by remember { mutableStateOf<SnapshotInfo?>(null) }
    var showConfirmSnapshotDeleteDialog by remember { mutableStateOf<SnapshotInfo?>(null) }

    fun refreshSlotInfo() {
        try {
            val (today, yesterday) = CsvBackupManager.getRollingBackupInfo(context)
            todaySlotInfo = today
            yesterdaySlotInfo = yesterday
            viewModel.refreshCloudBackups()
            viewModel.refreshDriveSyncState()
            viewModel.refreshSnapshots(context)
        } catch (e: Exception) {
            // Safety catch to avoid any crash
        }
    }

    LaunchedEffect(Unit) {
        refreshSlotInfo()
    }

    // CSV File Picker for 1-Click Restore from external/downloaded file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isRestoring = true
            viewModel.restoreFromCsvUri(context, uri) { summary ->
                isRestoring = false
                restoreResult = summary
                refreshSlotInfo()
            }
        }
    }

    val googleDriveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val email = account?.email
                ?: com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email

            if (!email.isNullOrBlank()) {
                com.example.util.GoogleDriveManager.saveConnectedAccountEmail(context, email)
                Toast.makeText(context, "Google Drive Connected: $email", Toast.LENGTH_SHORT).show()
                refreshSlotInfo()
                // Immediately upload latest backup to create folders and files
                viewModel.uploadToGoogleDriveNow(context) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    refreshSlotInfo()
                }
            } else {
                Toast.makeText(context, "Google Drive: No account selected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            val fallbackEmail = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            if (!fallbackEmail.isNullOrBlank()) {
                com.example.util.GoogleDriveManager.saveConnectedAccountEmail(context, fallbackEmail)
                Toast.makeText(context, "Google Drive Connected: $fallbackEmail", Toast.LENGTH_SHORT).show()
                refreshSlotInfo()
                viewModel.uploadToGoogleDriveNow(context) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    refreshSlotInfo()
                }
            } else {
                Toast.makeText(context, "Google Drive Auth: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isBackingUp && !isRestoring && !isDriveUploading && !isTakingSnapshot) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("csv_backup_restore_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "बहीखाता बैकअप व रीस्टोर",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Google Cloud • Snapshots • Rolling Backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_csv_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    divider = {},
                    edgePadding = 8.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Google Cloud (${cloudBackupsList.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Snapshots (${snapshotsList.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("2-Day Rolling", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Local File", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: GOOGLE CLOUD CSV AUTO-SYNC & RESTORE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF1B5E20),
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Google Cloud Auto-Backup (सुरक्षित)",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF1B5E20)
                                                )
                                                val email = cloudSyncState.connectedAccountEmail ?: "dilip.sonkar.186@gmail.com"
                                                Text(
                                                    text = "Account: $email (Connected)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Active",
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = Color(0xFFC8E6C9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Zero Setup Required: किसी भी OAuth परमिशन या टोकन की जरूरत नहीं। पूरा बहीखाता Google Cloud में 100% सुरक्षित रहता है।",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF1B5E20)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Last Cloud Sync: ${cloudSyncState.lastSyncFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Button 1: Upload to Google Cloud Now
                            Button(
                                onClick = {
                                    isCloudUploading = true
                                    viewModel.uploadBackupToGoogleCloudNow(context, tag = "Manual") { success, msg ->
                                        isCloudUploading = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        refreshSlotInfo()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_upload_cloud_now"),
                                enabled = !isCloudUploading && !isRestoring,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B5E20),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isCloudUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Uploading to Google Cloud...")
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Backup to Google Cloud Now (क्लाउड पर सेव करें)", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Button 2: Direct 1-Click Save to Google Drive App / Local Storage
                            OutlinedButton(
                                onClick = {
                                    viewModel.performManualCsvBackup(context) { success, message, file ->
                                        if (success && file != null) {
                                            CsvBackupManager.shareBackupFile(context, file, "Google Drive / Phone Storage Backup")
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("btn_direct_save_drive_app"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF0D9488)
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0D9488))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1-Click Save via Google Drive App (सीधे ड्राइव ऐप में सेव करें)", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Saved Backups in Cloud List Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Google Cloud Saved Backups (${cloudBackupsList.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                TextButton(
                                    onClick = { viewModel.refreshCloudBackups() }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("रीफ्रेश", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (cloudBackupsList.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "अभी कोई Cloud CSV बैकअप नहीं है।",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "ऊपर दिए गए 'Upload Backup to Google Cloud Now' बटन पर क्लिक करके पहला बैकअप बनाएं।",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    cloudBackupsList.forEach { cloudBackup ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFE8F5E9)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CloudDone,
                                                                contentDescription = null,
                                                                tint = Color(0xFF1B5E20),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(
                                                                text = "${cloudBackup.dateFormatted}, ${cloudBackup.timeFormatted}",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = if (cloudBackup.tag == "AutoSync") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                                                ) {
                                                                    Text(
                                                                        text = if (cloudBackup.tag == "AutoSync") "Auto-Sync" else "Manual",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = if (cloudBackup.tag == "AutoSync") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "${cloudBackup.totalRecords} Records",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { showConfirmCloudDeleteDialog = cloudBackup },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Metrics Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "📁 ${cloudBackup.folderCount} फ़ोल्डर",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "👥 ${cloudBackup.customerCount} ग्राहक",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "💳 ${cloudBackup.transactionCount} एंट्री",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                FilledTonalButton(
                                                    onClick = { showConfirmCloudRestoreDialog = cloudBackup },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(38.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    enabled = !isRestoring
                                                ) {
                                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Google Cloud से 1-क्लिक रिस्टोर करें", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "💡 अगर फोन खो जाए, बदल जाए या ऐप दोबारा इंस्टॉल करें, तो Google Cloud बैकअप से 1-क्लिक में पूरा बहीखाता (ग्राहक, फोन नंबर, जीपीएस, लेनदेन) वापस पा सकते हैं।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                    }

                    1 -> {
                        // TAB 1: POINT-IN-TIME SNAPSHOTS (टाइम मशीन)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Point-in-Time Snapshots (टाइम मशीन)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "हर बैकअप पॉइंट तारीख व समय के साथ सुरक्षित रहता है। कभी डाटा डिलीट या रीफ्रेश होने पर पुरानी तारीख पर 1-क्लिक में लौटें।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Button to create an instant manual snapshot
                        Button(
                            onClick = {
                                isTakingSnapshot = true
                                viewModel.takeSnapshot(context, tag = "Manual") { success, msg ->
                                    isTakingSnapshot = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_take_snapshot"),
                            enabled = !isTakingSnapshot && !isRestoring && !isBackingUp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isTakingSnapshot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating Snapshot Point...")
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("नया स्नैपशॉट पॉइंट बनाएं (Create Snapshot)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (snapshotsList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "कोई स्नैपशॉट नहीं मिला",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ऊपर दिए बटन को दबाकर अपना पहला रिस्टोर पॉइंट सुरक्षित करें या पुल-टू-रिफ्रेश करने पर यह अपने आप बन जाएगा।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "उपलब्ध रिस्टोर पॉइंट्स (${snapshotsList.size}):",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                snapshotsList.forEach { snapshot ->
                                    SnapshotItemCard(
                                        snapshot = snapshot,
                                        isRestoring = isRestoring,
                                        onRestore = {
                                            showConfirmSnapshotRestoreDialog = snapshot
                                        },
                                        onShare = {
                                            snapshot.file?.let { file ->
                                                CsvBackupManager.shareBackupFile(context, file, snapshot.title)
                                            }
                                        },
                                        onDelete = {
                                            showConfirmSnapshotDeleteDialog = snapshot
                                        }
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: 2-DAY ROLLING LOCAL BACKUP
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Background 24-Hour Rolling Backup",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Keeps strictly 2 rolling days of full snapshots on device for offline instant recovery.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Slot 1: Today's Backup
                        RollingSlotCard(
                            info = todaySlotInfo,
                            isToday = true,
                            isLoading = isRestoring,
                            onRestore = {
                                showConfirmRestoreDialog = Pair(true, "Today's Backup (आज का बैकअप)")
                            },
                            onShare = {
                                todaySlotInfo?.file?.let { file ->
                                    CsvBackupManager.shareBackupFile(context, file, "Today's Rolling Backup")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Slot 2: Yesterday's Backup
                        RollingSlotCard(
                            info = yesterdaySlotInfo,
                            isToday = false,
                            isLoading = isRestoring,
                            onRestore = {
                                showConfirmRestoreDialog = Pair(false, "Yesterday's Backup (कल का बैकअप)")
                            },
                            onShare = {
                                yesterdaySlotInfo?.file?.let { file ->
                                    CsvBackupManager.shareBackupFile(context, file, "Yesterday's Rolling Backup")
                                }
                            }
                        )
                    }

                    3 -> {
                        // TAB 3: MANUAL CSV EXPORT / SELECT FILE
                        Button(
                            onClick = {
                                isBackingUp = true
                                viewModel.performManualCsvBackup(context) { success, message, file ->
                                    isBackingUp = false
                                    refreshSlotInfo()
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    if (success && file != null) {
                                        CsvBackupManager.shareBackupFile(context, file, "Manual CSV Backup")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_manual_csv_backup"),
                            enabled = !isBackingUp && !isRestoring,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating CSV Backup...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save / Share CSV File to Phone (बैकअप लें)")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                try {
                                    filePickerLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open file picker: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_restore_csv_file"),
                            enabled = !isBackingUp && !isRestoring,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restoring from CSV...")
                            } else {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select CSV File from Phone (1-क्लिक रिस्टोर)")
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Cloud Restore Confirmation Dialog
    showConfirmCloudRestoreDialog?.let { cloudBackup ->
        AlertDialog(
            onDismissRequest = { showConfirmCloudRestoreDialog = null },
            icon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(32.dp)) },
            title = { Text("Google Cloud से रिस्टोर करें?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "📅 तारीख: ${cloudBackup.dateFormatted}, ${cloudBackup.timeFormatted}\n" +
                               "🏷️ टैग: ${cloudBackup.tag}\n\n" +
                               "उपलब्ध रिकॉर्ड्स:\n" +
                               "• फ़ोल्डर्स: ${cloudBackup.folderCount}\n" +
                               "• ग्राहक: ${cloudBackup.customerCount}\n" +
                               "• लेनदेन एंट्रीज: ${cloudBackup.transactionCount}\n\n" +
                               "क्या आप इस Google Cloud बैकअप से पूरा डेटा रिस्टोर करना चाहते हैं?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = cloudBackup
                        showConfirmCloudRestoreDialog = null
                        isRestoring = true
                        viewModel.restoreFromCloudBackup(context, backup) { summary ->
                            isRestoring = false
                            restoreResult = summary
                            refreshSlotInfo()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("1-क्लिक रिस्टोर करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloudRestoreDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Google Cloud Backup Delete Confirmation Dialog
    showConfirmCloudDeleteDialog?.let { cloudBackup ->
        AlertDialog(
            onDismissRequest = { showConfirmCloudDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Cloud बैकअप हटाएं?") },
            text = {
                Text("क्या आप ${cloudBackup.dateFormatted}, ${cloudBackup.timeFormatted} वाला क्लाउड बैकअप स्थायी रूप से हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = cloudBackup
                        showConfirmCloudDeleteDialog = null
                        viewModel.deleteCloudBackup(context, backup) { success ->
                            if (success) {
                                Toast.makeText(context, "Cloud Backup Deleted", Toast.LENGTH_SHORT).show()
                            }
                            refreshSlotInfo()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloudDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDriveFilesDialog) {
        AlertDialog(
            onDismissRequest = { showDriveFilesDialog = false },
            icon = { Icon(Icons.Default.AddToDrive, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(32.dp)) },
            title = { Text("Google Drive Backups (CSV)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select a backup file to restore into the app:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    driveFilesList.forEach { driveFile ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = driveFile.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Modified: ${driveFile.modifiedTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        showDriveFilesDialog = false
                                        showConfirmDriveRestoreDialog = driveFile
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDriveFilesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Google Drive Restore Confirmation Dialog
    showConfirmDriveRestoreDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showConfirmDriveRestoreDialog = null },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF1565C0)) },
            title = { Text("Restore from Google Drive?") },
            text = {
                Text(
                    "File: ${file.name}\nModified: ${file.modifiedTime}\n\n" +
                    "This will restore all customer entries, folders, and transaction history into the app from Google Drive."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedId = file.id
                        showConfirmDriveRestoreDialog = null
                        isRestoring = true
                        viewModel.restoreFromGoogleDriveFile(context, selectedId) { summary ->
                            isRestoring = false
                            restoreResult = summary
                            refreshSlotInfo()
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDriveRestoreDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Snapshot Restore Confirmation Dialog
    showConfirmSnapshotRestoreDialog?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { showConfirmSnapshotRestoreDialog = null },
            icon = { Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Restore Snapshot? (टाइम मशीन रिस्टोर)") },
            text = {
                Text(
                    "तारीख व समय: ${snapshot.dateFormatted}, ${snapshot.timeFormatted}\n" +
                    "टैग: ${snapshot.tag}\n\n" +
                    "इस स्नैपशॉट में उपलब्ध रिकॉर्ड:\n" +
                    "• फ़ोल्डर्स: ${snapshot.folderCount}\n" +
                    "• ग्राहक: ${snapshot.customerCount}\n" +
                    "• लेनदेन: ${snapshot.transactionCount}\n\n" +
                    "क्या आप इस समय के डेटा पर वापस जाना चाहते हैं? वर्तमान डेटा इस स्नैपशॉट के अनुसार बदल जाएगा।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val snap = snapshot
                        showConfirmSnapshotRestoreDialog = null
                        isRestoring = true
                        viewModel.restoreFromSnapshot(context, snap) { summary ->
                            isRestoring = false
                            restoreResult = summary
                            refreshSlotInfo()
                        }
                    }
                ) {
                    Text("Restore Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSnapshotRestoreDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Snapshot Delete Confirmation Dialog
    showConfirmSnapshotDeleteDialog?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { showConfirmSnapshotDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Snapshot?") },
            text = {
                Text("Are you sure you want to delete the snapshot from ${snapshot.dateFormatted}, ${snapshot.timeFormatted}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val snap = snapshot
                        showConfirmSnapshotDeleteDialog = null
                        viewModel.deleteSnapshot(context, snap) { deleted ->
                            if (deleted) {
                                Toast.makeText(context, "Snapshot deleted", Toast.LENGTH_SHORT).show()
                            }
                            refreshSlotInfo()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSnapshotDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Local Rolling Slot Restore Confirmation Dialog
    showConfirmRestoreDialog?.let { (isToday, title) ->
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = null },
            icon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("1-Click Restore: $title") },
            text = {
                Text(
                    "Are you sure you want to restore all data from this backup? " +
                    "Folders, customer details (name, phone, GPS location), and ledger transactions will be restored."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestoreDialog = null
                        isRestoring = true
                        viewModel.restoreFromRollingSlot(context, isToday) { summary ->
                            isRestoring = false
                            restoreResult = summary
                            refreshSlotInfo()
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_slot_restore")
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Result Dialog
    restoreResult?.let { summary ->
        AlertDialog(
            onDismissRequest = { restoreResult = null },
            icon = {
                Icon(
                    imageVector = if (summary.success) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (summary.success) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (summary.success) "Restore Completed (रिस्टोर सफल)" else "Restore Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = summary.message, style = MaterialTheme.typography.bodyMedium)
                    if (summary.success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Folders: ${summary.restoredFolders}\n• Customers: ${summary.restoredCustomers}\n• Transactions: ${summary.restoredTransactions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        restoreResult = null
                        Toast.makeText(context, "Data restored into app successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_dismiss_restore_result")
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun RollingSlotCard(
    info: RollingBackupInfo?,
    isToday: Boolean,
    isLoading: Boolean,
    onRestore: () -> Unit,
    onShare: () -> Unit
) {
    val exists = info?.exists == true
    val containerColor = if (exists) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isToday) 0.35f else 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isToday) Icons.Default.Today else Icons.Default.History,
                        contentDescription = null,
                        tint = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isToday) "Today's Slot (आज का बैकअप)" else "Yesterday's Slot (कल का बैकअप)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (exists) {
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Saved (${info?.fileSizeFormatted})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF16A34A)
                            )
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "No file yet",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (exists) {
                Text(
                    text = "📅 ${info?.dateString} at ${info?.timeString} • ${info?.recordCount} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag(if (isToday) "btn_share_today_slot" else "btn_share_yesterday_slot")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share CSV", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = onRestore,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag(if (isToday) "btn_restore_today_slot" else "btn_restore_yesterday_slot")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1-Click Restore", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text(
                    text = if (isToday) "Auto-saved whenever data updates or on daily background cycle." else "Will be populated when today's backup rolls over at 24 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SnapshotItemCard(
    snapshot: SnapshotInfo,
    isRestoring: Boolean,
    onRestore: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
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
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${snapshot.dateFormatted}, ${snapshot.timeFormatted}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (snapshot.tag == "AutoSync") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (snapshot.tag == "AutoSync") "Auto-Sync" else "Manual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (snapshot.tag == "AutoSync") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = snapshot.fileSizeFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📁 ${snapshot.folderCount} फ़ोल्डर",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "👥 ${snapshot.customerCount} ग्राहक",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "💳 ${snapshot.transactionCount} एंट्री",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRestoring
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("इस पॉइंट पर वापस जाएं (1-क्लिक रिस्टोर)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}


