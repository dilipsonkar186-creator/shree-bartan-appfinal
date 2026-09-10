package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.scale
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantBottomSheet
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AiAssistantViewModelFactory
import com.example.util.NameMatcher
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import com.example.data.entity.CustomerEntity
import com.example.ui.components.CsvBackupRestoreDialog
import com.example.ui.components.GlobalSmsSettingsDialog
import com.example.ui.components.UpiQrGeneratorDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.FolderEntity
import com.example.data.model.FolderDeletionInfo
import com.example.data.model.FolderWithCount
import com.example.ui.components.AddEditFolderDialog
import com.example.ui.components.EditShopProfileDialog
import com.example.ui.components.PublishAnnouncementDialog
import com.example.ui.components.FolderCard
import androidx.compose.material.icons.filled.Campaign
import com.example.ui.components.GoogleDriveSyncStatusCard
import com.example.ui.components.GoogleSignInCard
import com.example.ui.components.ShopProfileBanner
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    viewModel: FolderViewModel,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onFolderClick: (Long) -> Unit,
    onCustomerClick: (Long) -> Unit = {},
    onOpenFinancialSummary: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onNavigateToProducts: () -> Unit = {},
    onNavigateToAdminCustomers: () -> Unit = {},
    onNavigateToCustomerAppHub: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onLockAppRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val folders by viewModel.filteredFolders.collectAsStateWithLifecycle()
    val allFoldersWithCount by viewModel.rawFolders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.folderSearchQuery.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedAreaFilter.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val driveSyncState by viewModel.driveSyncState.collectAsStateWithLifecycle()
    val pullDownSyncEnabled by viewModel.pullDownSyncEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isHindi = appLanguage == "hi"

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderCannotDeleteInfo by remember { mutableStateOf<FolderDeletionInfo?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showGlobalSmsDialog by remember { mutableStateOf(false) }
    var showCsvBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showEditShopDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showBroadcastReminderDialog by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }

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
                viewModel.syncAllDataToCloudAndDrive(context) { success, msg ->
                    Toast.makeText(context, "Google Drive Connected: $email ($msg)", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            val fallbackEmail = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            if (!fallbackEmail.isNullOrBlank()) {
                com.example.util.GoogleDriveManager.saveConnectedAccountEmail(context, fallbackEmail)
                viewModel.syncAllDataToCloudAndDrive(context) { success, msg ->
                    Toast.makeText(context, "Google Drive Connected: $fallbackEmail ($msg)", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Google Drive: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val aiViewModel: AiAssistantViewModel = viewModel(
        factory = AiAssistantViewModelFactory(viewModel.repository)
    )

    val aiSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull { it.isNotBlank() }
            if (!spokenText.isNullOrBlank()) {
                aiViewModel.onInputTextChange(spokenText)
                aiViewModel.submitQuery(spokenText)
                showAiAssistantBottomSheet = true
            }
        }
    }

    fun startAiVoiceSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "AI से बोलकर पूछें (जैसे: उमा सेन का खाता, कुल बकाया)...")
        }
        try {
            aiSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            showAiAssistantBottomSheet = true
            Toast.makeText(context, "Voice input not available, opening AI Chat", Toast.LENGTH_SHORT).show()
        }
    }

    // Available areas for filtering dynamically extracted from existing folders
    val availableAreas = remember(allFoldersWithCount) {
        val existingAreas = allFoldersWithCount.map { it.folder.areaTag }.distinct().filter { it.isNotBlank() }
        if (existingAreas.isNotEmpty()) listOf("All") + existingAreas else emptyList()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { startAiVoiceSpeech() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "AI Voice Assistant Mic",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = if (isHindi) "AI से बोलें (Ask AI)" else "Ask AI (Voice)",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("floating_ai_assistant_fab")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "श्री बर्तन भंडार",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "बड़ा पत्थर, रांझी",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "7879997777",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showUpiQrDialog = true },
                        modifier = Modifier.testTag("top_upi_qr_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "दुकान UPI QR कोड",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showAiAssistantBottomSheet = true },
                        modifier = Modifier.testTag("top_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onOpenRecycleBin,
                        modifier = Modifier.testTag("recycle_bin_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = "Recycle Bin",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Top-Right Menu Option
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("top_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(
                                            text = if (isHindi) "कस्टमर ऐप कंट्रोल सेंटर" else "Customer App Control Center",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isHindi) "शॉप, प्रोडक्ट, कैटलॉग, ऑफर व ग्राहक कंट्रोल" else "Shop, Products, Catalog, Offers & Customers",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onNavigateToCustomerAppHub()
                                },
                                modifier = Modifier.testTag("menu_customer_app_hub")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "वित्तीय रिपोर्ट (Reports)" else "Financial Reports") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onOpenFinancialSummary()
                                },
                                modifier = Modifier.testTag("menu_financial_summary")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "रीसायकल बिन (Recycle Bin)" else "Recycle Bin") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Recycling,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onOpenRecycleBin()
                                },
                                modifier = Modifier.testTag("menu_recycle_bin")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "CSV बैकअप एवं रिस्टोर" else "CSV Backup & Restore") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showCsvBackupDialog = true
                                },
                                modifier = Modifier.testTag("menu_csv_backup_restore")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "एसएमएस व संदेश सेटिंग" else "SMS & Reminders") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0)
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showGlobalSmsDialog = true
                                },
                                modifier = Modifier.testTag("menu_global_sms_settings")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "क्लाउड बैकअप (Save Cloud)" else "Save to Cloud") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    Toast.makeText(context, if (isHindi) "क्लाउड पर सेव हो रहा है..." else "Saving to Cloud...", Toast.LENGTH_SHORT).show()
                                    viewModel.saveToShreeFirestoreBartan { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.testTag("menu_save_firestore_shree_bartan")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "क्लाउड रिस्टोर (Restore)" else "Restore from Cloud") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showRestoreDialog = true
                                },
                                modifier = Modifier.testTag("menu_restore_firestore")
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isHindi) "पुल-डाउन सिंक" else "Pull-down Sync",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Switch(
                                            checked = pullDownSyncEnabled,
                                            onCheckedChange = { checked ->
                                                viewModel.updatePullDownSyncEnabled(checked)
                                                Toast.makeText(
                                                    context,
                                                    if (checked) (if (isHindi) "पुल-डाउन सिंक चालू (ON)" else "Pull-down Sync ON")
                                                    else (if (isHindi) "पुल-डाउन सिंक बंद (OFF)" else "Pull-down Sync OFF"),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = if (pullDownSyncEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    val newState = !pullDownSyncEnabled
                                    viewModel.updatePullDownSyncEnabled(newState)
                                    Toast.makeText(
                                        context,
                                        if (newState) (if (isHindi) "पुल-डाउन सिंक चालू (ON)" else "Pull-down Sync ON")
                                        else (if (isHindi) "पुल-डाउन सिंक बंद (OFF)" else "Pull-down Sync OFF"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("menu_pull_down_sync_toggle")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "सेटिंग्स व खाता (Settings)" else "Settings & Profile") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showAccountDialog = true
                                },
                                modifier = Modifier.testTag("menu_account_login")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "ऐप लॉक करें (Lock App)" else "Lock App") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    authViewModel.setAppUnlocked(false)
                                    onLockAppRequested()
                                },
                                modifier = Modifier.testTag("menu_lock_app")
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
        val folderListContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Google Drive Sync Status & Pending Operations Info Card
                GoogleDriveSyncStatusCard(
                    syncState = driveSyncState,
                    onSyncNow = {
                        val email = com.example.util.GoogleDriveManager.getConnectedAccountEmail(context)
                        if (email.isNullOrBlank()) {
                            try {
                                val client = com.example.util.GoogleDriveManager.getGoogleDriveSignInClient(context)
                                googleDriveAuthLauncher.launch(client.signInIntent)
                            } catch (e: Exception) {
                                viewModel.syncAllDataToCloudAndDrive(context) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            viewModel.syncAllDataToCloudAndDrive(context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (!success && msg.contains("Authorization", ignoreCase = true)) {
                                    try {
                                        val client = com.example.util.GoogleDriveManager.getGoogleDriveSignInClient(context)
                                        googleDriveAuthLauncher.launch(client.signInIntent)
                                    } catch (ign: Exception) {}
                                }
                            }
                        }
                    },
                    onOpenBackupDialog = {
                        showCsvBackupDialog = true
                    },
                    onToggleAutoSync = { enabled ->
                        viewModel.setDriveAutoSyncEnabled(enabled)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

            // 2. Customer Folders Section Title with Add Folder Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Customer Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Organized customer accounts & notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddFolderDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_folder_header_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Folder",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Folder",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Search Bar & Optional Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setFolderSearchQuery(it) },
                    placeholder = { Text("Search folders...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFolderSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_search_input")
                )


            }

            // Folder List or Empty State
            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = if (searchQuery.isNotBlank() || selectedArea != "All")
                                "No folders matching search"
                            else
                                "No Folders Added Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (searchQuery.isNotBlank() || selectedArea != "All")
                                "Try clearing your search query or area filters."
                            else
                                "Tap 'Add Folder' below or restore your previous data in 1-click from your 2-Day CSV backup.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (searchQuery.isBlank() && selectedArea == "All") {
                            Spacer(modifier = Modifier.height(14.dp))
                            FilledTonalButton(
                                onClick = { showCsvBackupDialog = true },
                                modifier = Modifier.testTag("btn_empty_state_restore_csv"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reinstalled App? 1-Click Restore from CSV")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("folder_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = folders,
                        key = { it.folder.id }
                    ) { item ->
                        FolderCard(
                            folderWithCount = item,
                            onClick = { onFolderClick(item.folder.id) },
                            onEdit = { folderToEdit = item.folder },
                            onDelete = {
                                coroutineScope.launch {
                                    val deleteInfo = viewModel.repository.getFolderDeletionValidation(item.folder)
                                    if (!deleteInfo.canDelete) {
                                        folderCannotDeleteInfo = deleteInfo
                                    } else {
                                        folderToDelete = item.folder
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

        if (pullDownSyncEnabled) {
            PullToRefreshBox(
                isRefreshing = isPullRefreshing || driveSyncState.isSyncing,
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
                    .testTag("pull_to_refresh_box")
            ) {
                folderListContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("pull_to_refresh_box_disabled")
            ) {
                folderListContent()
            }
        }
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        AddEditFolderDialog(
            initialFolder = null,
            onDismiss = { showAddFolderDialog = false },
            onSave = { name, description, areaTag, colorHex ->
                viewModel.addFolder(name, description, areaTag, colorHex)
                showAddFolderDialog = false
            }
        )
    }

    // Edit Folder Dialog
    folderToEdit?.let { folder ->
        AddEditFolderDialog(
            initialFolder = folder,
            onDismiss = { folderToEdit = null },
            onSave = { name, description, areaTag, colorHex ->
                viewModel.updateFolder(
                    folder.copy(
                        name = name,
                        description = description,
                        areaTag = areaTag,
                        colorHex = colorHex
                    )
                )
                folderToEdit = null
            }
        )
    }

    // Folder Cannot Be Deleted (Protection Warning) Dialog
    folderCannotDeleteInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { folderCannotDeleteInfo = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Folder Protected",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "फ़ोल्डर डिलीट नहीं किया जा सकता",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Folder name pill
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = info.folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (info.folder.areaTag.isNotBlank()) {
                                    Text(
                                        text = "एरिया: ${info.folder.areaTag}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Financial & Customer Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("कुल ग्राहक", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${info.customerCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = if (info.totalPendingDues > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("कुल बकाया", style = MaterialTheme.typography.labelSmall, color = if (info.totalPendingDues > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹ ${info.totalPendingDues.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (info.totalPendingDues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // List of customers with dues (if any)
                    if (info.customerDuesList.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "बकाया वाले ग्राहक (${info.customersWithDuesCount}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                info.customerDuesList.take(4).forEach { (custName, due) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• $custName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("₹ ${due.toInt()} बाकी", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (info.customerDuesList.size > 4) {
                                    Text(
                                        "+ अन्य ${info.customerDuesList.size - 4} ग्राहक",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Explanation Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ सुरक्षा नियम (Strict Data Protection):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "इस फ़ोल्डर में ग्राहकों का लेनदेन/बकाया बाकी है। रिकॉर्ड सुरक्षित रखने के लिए जब तक इस फ़ोल्डर के सभी ग्राहकों का हिसाब निल (₹0) नहीं हो जाता, यह फ़ोल्डर डिलीट नहीं किया जा सकता।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fid = info.folder.id
                        folderCannotDeleteInfo = null
                        onFolderClick(fid)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("open_protected_folder_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("फ़ोल्डर खोलें व हिसाब देखें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderCannotDeleteInfo = null }) {
                    Text("समझ गया (Dismiss)")
                }
            }
        )
    }

    // Delete Empty / Nil Dues Folder Confirmation Dialog
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder?") },
            text = {
                Text("Are you sure you want to delete folder '${folder.name}'? This folder will be moved to Recycle Bin.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder) { success, msg, validationInfo ->
                            if (!success && validationInfo != null && !validationInfo.canDelete) {
                                folderCannotDeleteInfo = validationInfo
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        folderToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_folder")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore from Firebase Confirmation Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore from Firebase (Firebase से डाटा रिस्टोर करें)") },
            text = {
                Text("क्या आप Firebase क्लाउड बैकअप से फ़ोल्डर, ग्राहक और लेन-देन का डाटा अपने ऐप में रिस्टोर करना चाहते हैं?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        Toast.makeText(context, "Firebase से डाटा रिस्टोर किया जा रहा है...", Toast.LENGTH_SHORT).show()
                        viewModel.restoreFromFirebase { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_firebase")
                ) {
                    Text("रिस्टोर करें (Restore)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("रद्द करें (Cancel)")
                }
            }
        )
    }

    // Account Profile & Settings Dialog
    if (showAccountDialog) {
        val autoCapitalizeNames by viewModel.autoCapitalizeCustomerNames.collectAsState()

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text(if (isHindi) "ऐप सेटिंग्स एवं खाता" else "App Settings & Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GoogleSignInCard(
                        authViewModel = authViewModel,
                        onNavigateToLogin = {
                            showAccountDialog = false
                            onNavigateToLogin()
                        }
                    )

                    HorizontalDivider()

                    // App Language Selector (Hindi / English)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isHindi) "ऐप की भाषा (App Language)" else "App Language",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isHindi) "नोट: ग्राहक व फोल्डर के नाम हमेशा इंग्लिश में ही रहेंगे।" else "Note: Customer and folder names remain in English.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = appLanguage == "en",
                                    onClick = {
                                        viewModel.updateAppLanguage("en")
                                        Toast.makeText(context, "Language set to English", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("English", fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = appLanguage == "hi",
                                    onClick = {
                                        viewModel.updateAppLanguage("hi")
                                        Toast.makeText(context, "भाषा हिंदी सेट की गई", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("हिंदी (Hindi)", fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // App Settings Section: Auto Capitalize Customer Names
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                                    text = if (isHindi) "ग्राहक नाम कैपिटल में (Auto Capitalize)" else "Auto Capitalize Customer Names",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "नया ग्राहक जोड़ते समय नाम हमेशा CAPITAL में रखें" else "Keep new customer names in UPPERCASE automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoCapitalizeNames,
                                onCheckedChange = { viewModel.updateAutoCapitalizeCustomerNames(it) },
                                modifier = Modifier.testTag("auto_capitalize_names_setting_switch")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text(if (isHindi) "बंद करें (Close)" else "Close")
                }
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
                Toast.makeText(context, "Shop Profile Updated!", Toast.LENGTH_SHORT).show()
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

    // AI Voice & Text Assistant BottomSheet
    if (showAiAssistantBottomSheet) {
        AiAssistantBottomSheet(
            viewModel = aiViewModel,
            onDismiss = { showAiAssistantBottomSheet = false },
            onCustomerClick = { customerId ->
                showAiAssistantBottomSheet = false
                onCustomerClick(customerId)
            }
        )
    }

    // CSV Auto-Backup & 1-Click Restore Dialog
    if (showCsvBackupDialog) {
        CsvBackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showCsvBackupDialog = false }
        )
    }

    // Global SMS & Message Settings Dialog
    if (showGlobalSmsDialog) {
        GlobalSmsSettingsDialog(
            onDismiss = { showGlobalSmsDialog = false }
        )
    }

    // Shop UPI QR Code Dialog (Open amount)
    if (showUpiQrDialog) {
        UpiQrGeneratorDialog(
            shopName = "श्री बर्तन भंडार",
            onDismiss = { showUpiQrDialog = false }
        )
    }

    // Broadcast Payment Reminder to all pending customers
    if (showBroadcastReminderDialog) {
        BroadcastPaymentReminderDialog(
            viewModel = viewModel,
            onDismiss = { showBroadcastReminderDialog = false }
        )
    }
}

/**
 * Static attractive header element designed exclusively for the home page (landing page).
 * Features metallic polished brass/copper style text, hardcoded address, and mobile number.
 * Static and uneditable.
 */
@Composable
fun StaticMetallicHomeHeader(
    modifier: Modifier = Modifier
) {
    val metallicBrassGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFF7D070), // Polished Brass Gold
            Color(0xFFE5AA70), // Copper Brass
            Color(0xFFB87333), // Metallic Copper
            Color(0xFFD4AF37), // Metallic Gold
            Color(0xFFF3E5AB)  // Polished Light Brass
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("static_home_header"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E232A)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            brush = metallicBrassGradient
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Metallic Title "श्री बर्तन भंडार"
            Text(
                text = "श्री बर्तन भंडार",
                style = TextStyle(
                    brush = metallicBrassGradient,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(130.dp)
                    .padding(vertical = 2.dp),
                thickness = 1.dp,
                color = Color(0xFFB87333).copy(alpha = 0.5f)
            )

            // Address "बड़ा पत्थर, रांझी "
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE5AA70),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "बड़ा पत्थर, रांझी ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp
                )
            }

            // Mobile number "7879997777"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "7879997777",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * 1-Click Broadcast Payment Reminder Dialog to notify all customers with pending dues
 */
@Composable
fun BroadcastPaymentReminderDialog(
    viewModel: FolderViewModel,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var pendingCustomers by remember { mutableStateOf<List<Pair<CustomerEntity, Double>>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var completionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val list = viewModel.getCustomersWithPendingDues()
        pendingCustomers = list
        isLoading = false
    }

    val totalDues = remember(pendingCustomers) { pendingCustomers.sumOf { it.second } }

    AlertDialog(
        onDismissRequest = {
            if (!isSending) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "🔔 सभी बकायेदारों को रिमाइंडर",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "बकायेदार ग्राहकों की सूची जांची जा रही है...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (completionMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = completionMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (pendingCustomers.isEmpty()) {
                    Text(
                        text = "वर्तमान में किसी भी ग्राहक का कोई उधार (बकाया) शेष नहीं है। सभी खाते चुकता हैं!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "कुल बकायेदार ग्राहक:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5D4037)
                                )
                                Text(
                                    text = "${pendingCustomers.size} ग्राहक",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "कुल बकाया राशि:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5D4037)
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalDues)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    Text(
                        text = "क्या आप इन सभी ${pendingCustomers.size} ग्राहकों को उनके कस्टमर ऐप पर एक साथ पेमेंट याददिहानी (Notification) भेजना चाहते हैं?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (isSending) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (completionMessage != null || pendingCustomers.isEmpty()) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ठीक है (OK)")
                }
            } else if (!isLoading) {
                Button(
                    onClick = {
                        isSending = true
                        progressText = "नोटिफिकेशन भेजे जा रहे हैं..."
                        viewModel.sendPaymentReminderToAllPending(
                            onProgress = { sent, total ->
                                progressText = "भेज रहे हैं... ($sent / $total)"
                            },
                            onComplete = { success, total, msg ->
                                isSending = false
                                completionMessage = msg
                            }
                        )
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isSending) "भेजा जा रहा है..." else "हाँ, सभी को भेजें",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            if (completionMessage == null && !isSending && pendingCustomers.isNotEmpty()) {
                OutlinedButton(onClick = onDismiss) {
                    Text("रद्द करें")
                }
            }
        }
    )
}
