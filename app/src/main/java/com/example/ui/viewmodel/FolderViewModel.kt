package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.TransactionEntity
import com.example.data.model.FolderDeletionInfo
import com.example.data.model.FolderWithCount
import com.example.data.model.ShopProfile
import com.example.data.repository.FolderRepository
import com.example.ui.components.GivenItem
import com.example.util.NameMatcher
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// Helper for natural sorting of alphanumeric strings (e.g. "1", "2", "10", "F1", "F2", "F10")
fun naturalCompare(a: String, b: String): Int {
    val trimmedA = a.trim()
    val trimmedB = b.trim()
    if (trimmedA.isEmpty() && trimmedB.isEmpty()) return 0
    if (trimmedA.isEmpty()) return 1 // empty values placed at the end
    if (trimmedB.isEmpty()) return -1

    val regex = Regex("(\\d+|\\D+)")
    val tokensA = regex.findAll(trimmedA).map { it.value }.toList()
    val tokensB = regex.findAll(trimmedB).map { it.value }.toList()

    val maxLen = maxOf(tokensA.size, tokensB.size)
    for (i in 0 until maxLen) {
        val tA = tokensA.getOrNull(i) ?: return -1
        val tB = tokensB.getOrNull(i) ?: return 1

        val numA = tA.toLongOrNull()
        val numB = tB.toLongOrNull()

        if (numA != null && numB != null) {
            val cmp = numA.compareTo(numB)
            if (cmp != 0) return cmp
        } else {
            val cmp = tA.compareTo(tB, ignoreCase = true)
            if (cmp != 0) return cmp
        }
    }
    return 0
}

enum class CustomerSortOption(val label: String) {
    NAME_ASC("Name (A - Z)"),
    NAME_DESC("Name (Z - A)"),
    BOOK_ASC("Book Number (F1, F2, F3...)"),
    PAGE_ASC("Page Number (1, 2, 3...)"),
    PAID_FIRST("Paid First"),
    UNPAID_FIRST("Due / Unpaid First"),
    PAID_THIS_MONTH("Paid This Month First"),
    UNPAID_THIS_MONTH("No Payment This Month First"),
    HIGHEST_DUE("Highest Due First"),
    RECENT_PAYMENT("Recent Payment First")
}

enum class CustomerPaymentFilterOption(val label: String) {
    ALL("All Customers"),
    ACTIVE("Active Customers"),
    BLOCKED("🚫 Blocked (3+ M Overdue)"),
    PAID_THIS_MONTH("Paid This Month"),
    UNPAID_THIS_MONTH("No Payment This Month"),
    UNPAID("Has Dues / Unpaid"),
    PAID("Paid in Full")
}

data class InternalCustomerFinancials(
    val overallDues: Double = 0.0,
    val totalPaidOverall: Double = 0.0,
    val isOverallPaid: Boolean = true,
    val monthPaid: Double = 0.0,
    val monthGoodsProvided: Double = 0.0,
    val hasReceivedPaymentInMonth: Boolean = false,
    val latestPaymentTime: Long = 0L,
    val isOverdue3Months: Boolean = false,
    val isBlocked: Boolean = false,
    val blockReason: String = ""
)

data class CustomerFilterParams(
    val query: String = "",
    val sortOpt: CustomerSortOption = CustomerSortOption.NAME_ASC,
    val filterOpt: CustomerPaymentFilterOption = CustomerPaymentFilterOption.ALL,
    val month: Int = -1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)

data class CustomerFinancialSummary(
    val customer: CustomerEntity,
    val folderName: String,
    val totalGoodsProvided: Double,
    val totalAmountPaid: Double,
    val netBalance: Double, // totalGoodsProvided - totalAmountPaid
    val transactions: List<TransactionEntity>
)

data class FolderFinancialSummary(
    val folder: FolderEntity,
    val totalCustomers: Int,
    val totalGoodsProvided: Double,
    val totalAmountPaid: Double,
    val netBalance: Double,
    val customerSummaries: List<CustomerFinancialSummary>
)

private data class CustomerCombinedParams(
    val query: String,
    val sortOpt: CustomerSortOption,
    val filterOpt: CustomerPaymentFilterOption,
    val bookFilter: String?,
    val financialsMap: Map<Long, InternalCustomerFinancials>
)

class FolderViewModel(
    application: Application,
    val repository: FolderRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("shop_profile_prefs", Context.MODE_PRIVATE)

    private val _shopProfile = MutableStateFlow(
        ShopProfile(
            name = prefs.getString("shop_name", "Shree Bartan Store") ?: "Shree Bartan Store",
            hindiName = prefs.getString("shop_hindi_name", "श्री बर्तन स्टोर") ?: "श्री बर्तन स्टोर",
            phone = prefs.getString("shop_phone", "+91 98765 43210") ?: "+91 98765 43210",
            altPhone = prefs.getString("shop_alt_phone", "+91 98765 43211") ?: "+91 98765 43211",
            address = prefs.getString("shop_address", "Main Market, Gola Road, Utensils Market") ?: "Main Market, Gola Road, Utensils Market",
            tagline = prefs.getString("shop_tagline", "Quality Utensils & Stainless Steel Kitchenware") ?: "Quality Utensils & Stainless Steel Kitchenware",
            gstNumber = prefs.getString("shop_gst", "09ABCDE1234F1Z5") ?: "09ABCDE1234F1Z5",
            shopTimings = prefs.getString("shop_timings", "सुबह 9:00 AM से रात 9:00 PM") ?: "सुबह 9:00 AM से रात 9:00 PM",
            notice = prefs.getString("shop_notice", "रविवार को दुकान खुली रहेगी। सभी ग्राहकों का स्वागत है!") ?: "रविवार को दुकान खुली रहेगी। सभी ग्राहकों का स्वागत है!",
            bannerUrl = prefs.getString("shop_banner_url", "") ?: ""
        )
    )
    val shopProfile: StateFlow<ShopProfile> = _shopProfile.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        prefs.getString("app_language", "en") ?: "en"
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun updateAppLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
        _appLanguage.value = lang
    }

    private val _autoCapitalizeCustomerNames = MutableStateFlow(
        prefs.getBoolean("auto_capitalize_customer_names", true)
    )
    val autoCapitalizeCustomerNames: StateFlow<Boolean> = _autoCapitalizeCustomerNames.asStateFlow()

    fun updateAutoCapitalizeCustomerNames(enabled: Boolean) {
        prefs.edit().putBoolean("auto_capitalize_customer_names", enabled).apply()
        _autoCapitalizeCustomerNames.value = enabled
    }

    private val _pullDownSyncEnabled = MutableStateFlow(
        prefs.getBoolean("pull_down_sync_enabled", true)
    )
    val pullDownSyncEnabled: StateFlow<Boolean> = _pullDownSyncEnabled.asStateFlow()

    fun updatePullDownSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pull_down_sync_enabled", enabled).apply()
        _pullDownSyncEnabled.value = enabled
    }

    private val _rememberedProductNames = MutableStateFlow<Set<String>>(
        prefs.getStringSet("remembered_product_names", emptySet()) ?: emptySet()
    )

    fun rememberProductName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val capitalized = formatItemTitleCase(trimmed)
            val updated = _rememberedProductNames.value + capitalized
            prefs.edit().putStringSet("remembered_product_names", updated).apply()
            _rememberedProductNames.value = updated
        }
    }

    val suggestedProductNames: StateFlow<List<String>> = combine(
        repository.allTransactions,
        _rememberedProductNames
    ) { transactions, customSet ->
        val txNames = transactions.map { it.itemDescription.trim() }
            .filter { it.isNotBlank() && !it.startsWith("RETURN:", ignoreCase = true) }
            .map { formatItemTitleCase(it) }

        val presetNames = listOf(
            "Hawkins Pressure Cooker 3L",
            "Hawkins Pressure Cooker 5L",
            "Prestige Induction Kadhai",
            "Stainless Steel Kadhai Heavy",
            "Milton Thermosteel Bottle",
            "Casserole Hot Pot Set",
            "Steel Dinner Set 51 Pcs",
            "Brass Pooja Thali",
            "Copper Lota / Bottle",
            "Aluminium Bhagona / Topi",
            "Steel Balti / Bucket",
            "Steel Tanki 50L",
            "Steel Tanki 100L",
            "Steel Dabbi / Dabba Set",
            "Non-Stick Tawa 28cm",
            "Non-Stick Fry Pan 22cm"
        )

        (customSet + txNames + presetNames)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateShopProfile(updated: ShopProfile) {
        prefs.edit().apply {
            putString("shop_name", updated.name)
            putString("shop_hindi_name", updated.hindiName)
            putString("shop_phone", updated.phone)
            putString("shop_alt_phone", updated.altPhone)
            putString("shop_address", updated.address)
            putString("shop_tagline", updated.tagline)
            putString("shop_gst", updated.gstNumber)
            putString("shop_timings", updated.shopTimings)
            putString("shop_notice", updated.notice)
            putString("shop_banner_url", updated.bannerUrl)
            apply()
        }
        _shopProfile.value = updated

        // Sync to cloud Firestore at shree/shop_settings
        viewModelScope.launch {
            try {
                repository.firestoreRepository.saveShopSettings(updated)
            } catch (e: Exception) {
                android.util.Log.w("FolderViewModel", "Failed to sync shop settings to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Updates the shop live notice/announcement and pushes it live across all customer apps via Firestore and FCM.
     */
    fun updateShopNoticeAndInfo(
        noticeMessage: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val current = _shopProfile.value
                val updated = current.copy(notice = noticeMessage.trim())

                // 1. Save locally in SharedPreferences
                prefs.edit().putString("shop_notice", updated.notice).apply()
                _shopProfile.value = updated

                // 2. Sync to cloud Firestore at shree/shop_settings
                val firestoreResult = repository.firestoreRepository.saveShopSettings(updated)
                if (firestoreResult.isFailure) {
                    val ex = firestoreResult.exceptionOrNull() ?: Exception("Firestore sync failed")
                    onError(ex)
                    return@launch
                }

                // 3. Dispatch Live Push Notification broadcast to all customer devices
                try {
                    val storeName = updated.hindiName.ifBlank { updated.name.ifBlank { "श्री बर्तन स्टोर" } }
                    com.example.util.NotificationDispatchManager.broadcastAnnouncementToAll(
                        context = getApplication(),
                        title = "📢 $storeName — महत्वपूर्ण सूचना / बधाई",
                        announcementMessage = updated.notice,
                        storeName = storeName,
                        imageUrl = updated.bannerUrl.ifBlank { null }
                    )
                } catch (ne: Exception) {
                    android.util.Log.w("FolderViewModel", "Announcement push notification note: ${ne.message}")
                }

                onSuccess()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    private val _driveSyncState = MutableStateFlow(
        com.example.util.GoogleDriveManager.getSyncState(application)
    )
    val driveSyncState: StateFlow<com.example.util.DriveSyncState> = _driveSyncState.asStateFlow()

    private val _cloudBackupsList = MutableStateFlow<List<com.example.util.CloudBackupInfo>>(emptyList())
    val cloudBackupsList: StateFlow<List<com.example.util.CloudBackupInfo>> = _cloudBackupsList.asStateFlow()

    private val _cloudSyncState = MutableStateFlow(
        com.example.util.GoogleCloudBackupManager.getCloudSyncState(application)
    )
    val cloudSyncState: StateFlow<com.example.util.GoogleCloudSyncState> = _cloudSyncState.asStateFlow()

    fun refreshCloudBackups() {
        viewModelScope.launch {
            try {
                val res = repository.firestoreRepository.fetchCloudCsvBackups()
                if (res.isSuccess) {
                    val rawList = res.getOrThrow()
                    val mapped = rawList.map { map ->
                        com.example.util.CloudBackupInfo(
                            id = map["id"] as? String ?: "",
                            timestamp = (map["timestamp"] as? Long) ?: 0L,
                            dateFormatted = map["dateFormatted"] as? String ?: "",
                            timeFormatted = map["timeFormatted"] as? String ?: "",
                            totalRecords = (map["totalRecords"] as? Long)?.toInt() ?: ((map["totalRecords"] as? Int) ?: 0),
                            folderCount = (map["folderCount"] as? Long)?.toInt() ?: ((map["folderCount"] as? Int) ?: 0),
                            customerCount = (map["customerCount"] as? Long)?.toInt() ?: ((map["customerCount"] as? Int) ?: 0),
                            transactionCount = (map["transactionCount"] as? Long)?.toInt() ?: ((map["transactionCount"] as? Int) ?: 0),
                            sizeBytes = (map["sizeBytes"] as? Long) ?: 0L,
                            tag = map["tag"] as? String ?: "Manual",
                            userEmail = map["userEmail"] as? String
                        )
                    }
                    _cloudBackupsList.value = mapped
                }
            } catch (e: Exception) {
                // ignore
            }
            _cloudSyncState.value = com.example.util.GoogleCloudBackupManager.getCloudSyncState(getApplication())
        }
    }

    /**
     * Uploads the entire Ledger data to Google Cloud as a versioned CSV Backup.
     */
    fun uploadBackupToGoogleCloudNow(
        context: Context = getApplication(),
        tag: String = "Manual",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val (folders, customers, transactions) = repository.getFullDatabaseEntities()
                val totalRecords = folders.size + customers.size + transactions.size
                val fullCsv = com.example.util.CsvBackupManager.buildFullAppCsv(folders, customers, transactions)

                val res = repository.firestoreRepository.saveCsvBackupToCloud(
                    csvContent = fullCsv,
                    totalRecords = totalRecords,
                    folderCount = folders.size,
                    customerCount = customers.size,
                    transactionCount = transactions.size,
                    tag = tag
                )

                if (res.isSuccess) {
                    com.example.util.GoogleCloudBackupManager.markCloudSyncSuccess(context, totalRecords)
                    com.example.util.GoogleDriveManager.markSyncSuccess(context, totalRecords)
                    refreshCloudBackups()
                    refreshDriveSyncState()
                    onResult(true, "Google Cloud पर बैकअप सफलतापूर्वक सुरक्षित हो गया! ($totalRecords रिकॉर्ड्स)")
                } else {
                    onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Google Cloud upload failed")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error saving to Google Cloud")
            }
        }
    }

    /**
     * Restores database from a Google Cloud CSV Backup file in 1-click.
     */
    fun restoreFromCloudBackup(
        context: Context,
        backup: com.example.util.CloudBackupInfo,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        restoreFromGoogleCloudBackup(context, backup.id, onResult)
    }

    fun restoreFromGoogleCloudBackup(
        context: Context,
        backupId: String,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isRestoring = true
                val contentRes = repository.firestoreRepository.fetchCloudCsvContent(backupId)
                if (contentRes.isSuccess) {
                    val csvContent = contentRes.getOrThrow()
                    val db = AppDatabase.getInstance(context)
                    val summary = com.example.util.CsvBackupManager.restoreDatabaseFromCsvContent(context, csvContent, db)
                    isRestoring = false
                    refreshCloudBackups()
                    refreshSnapshots(context)
                    refreshDriveSyncState()
                    onResult(summary)
                } else {
                    isRestoring = false
                    onResult(com.example.util.RestoreSummary(false, message = contentRes.exceptionOrNull()?.localizedMessage ?: "Cloud content fetch failed"))
                }
            } catch (e: Exception) {
                isRestoring = false
                onResult(com.example.util.RestoreSummary(false, message = e.localizedMessage ?: "Google Cloud restore error"))
            }
        }
    }

    /**
     * Deletes a backup from Google Cloud.
     */
    fun deleteCloudBackup(
        context: Context = getApplication(),
        backup: com.example.util.CloudBackupInfo,
        onResult: (Boolean) -> Unit
    ) {
        deleteGoogleCloudBackup(backup.id, onResult)
    }

    fun deleteGoogleCloudBackup(
        backupId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.firestoreRepository.deleteCloudCsvBackup(backupId)
            refreshCloudBackups()
            onResult(res.isSuccess)
        }
    }

    private val _snapshotsList = MutableStateFlow<List<com.example.util.SnapshotInfo>>(emptyList())
    val snapshotsList: StateFlow<List<com.example.util.SnapshotInfo>> = _snapshotsList.asStateFlow()

    fun refreshSnapshots(context: Context = getApplication()) {
        viewModelScope.launch {
            val list = com.example.util.CsvBackupManager.listSnapshots(context)
            _snapshotsList.value = list
        }
    }

    fun refreshDriveSyncState() {
        _driveSyncState.value = com.example.util.GoogleDriveManager.getSyncState(getApplication())
    }

    fun setDriveAutoSyncEnabled(enabled: Boolean) {
        com.example.util.GoogleDriveManager.setAutoSyncEnabled(getApplication(), enabled)
        refreshDriveSyncState()
    }

    // 1. Input fields StateFlow for auto-save
    private val _inputData = MutableStateFlow("")
    val inputData: StateFlow<String> = _inputData.asStateFlow()

    // Flag to prevent real-time auto-save during restore operations
    private var isRestoring = false

    init {
        // Fetch latest shop settings and live notice from Firestore shree/shop_settings
        viewModelScope.launch {
            try {
                val cloudProfileRes = repository.firestoreRepository.fetchShopSettings()
                if (cloudProfileRes.isSuccess) {
                    val cloudProfile = cloudProfileRes.getOrNull()
                    if (cloudProfile != null) {
                        _shopProfile.value = cloudProfile
                        prefs.edit().apply {
                            putString("shop_name", cloudProfile.name)
                            putString("shop_hindi_name", cloudProfile.hindiName)
                            putString("shop_phone", cloudProfile.phone)
                            putString("shop_alt_phone", cloudProfile.altPhone)
                            putString("shop_address", cloudProfile.address)
                            putString("shop_tagline", cloudProfile.tagline)
                            putString("shop_gst", cloudProfile.gstNumber)
                            putString("shop_timings", cloudProfile.shopTimings)
                            putString("shop_notice", cloudProfile.notice)
                            putString("shop_banner_url", cloudProfile.bannerUrl)
                            apply()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("FolderViewModel", "Could not fetch cloud shop settings on start: ${e.message}")
            }
        }

        // Observe input changes with 500ms debounce for auto-save to Firestore
        @OptIn(FlowPreview::class)
        _inputData
            .debounce(500L)
            .distinctUntilChanged()
            .onEach { newValue ->
                if (newValue.isNotBlank()) {
                    saveToFirestoreBartan(newValue)
                }
            }
            .launchIn(viewModelScope)

        // Schedule daily rolling auto-backup background worker (24-hour rolling retention)
        try {
            com.example.worker.AutoBackupWorker.scheduleDailyAutoBackup(application)
        } catch (e: Exception) {
            // ignore
        }

        // Real-time listener: Whenever folders, customers, or transactions change,
        // automatically persist real-time updates to Firebase Firestore, local 2-day rolling CSV backup,
        // and Google Drive CSV backup!
        @OptIn(FlowPreview::class)
        combine(
            repository.foldersWithCount,
            repository.allCustomers,
            repository.allTransactions
        ) { folders, customers, transactions ->
            if (!isRestoring && (folders.isNotEmpty() || customers.isNotEmpty() || transactions.isNotEmpty())) {
                _driveSyncState.value = _driveSyncState.value.copy(
                    isPending = true,
                    pendingOperationsCount = 1
                )
            }
            Triple(folders, customers, transactions)
        }
        .debounce(1500L)
        .onEach { (folders, customers, transactions) ->
            if (!isRestoring && (folders.isNotEmpty() || customers.isNotEmpty() || transactions.isNotEmpty())) {
                val currentFolders = folders.map { it.folder }
                repository.saveFullBackupToFirestoreShreeBartan(
                    currentFolders,
                    customers,
                    transactions
                )
                try {
                    com.example.util.CsvBackupManager.performRollingBackup(
                        context = getApplication(),
                        folders = currentFolders,
                        customers = customers,
                        transactions = transactions
                    )
                } catch (e: Exception) {
                    // Ignore backup background error
                }

                // Automatic immediate sync to Google Drive in CSV format upon every entry
                if (com.example.util.GoogleDriveManager.isAutoSyncEnabled(getApplication())) {
                    try {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            isSyncing = true,
                            isPending = false
                        )
                        val fullCsv = com.example.util.CsvBackupManager.buildFullAppCsv(
                            currentFolders,
                            customers,
                            transactions
                        )
                        com.example.util.GoogleDriveManager.uploadBackupCsvToDrive(
                            context = getApplication(),
                            csvContent = fullCsv
                        )
                        // Also sync customer photos and documents to Google Drive Photos folder
                        com.example.util.GoogleDriveManager.syncAllCustomerPhotosToDrive(
                            context = getApplication(),
                            customers = customers
                        )
                        refreshDriveSyncState()
                    } catch (e: Exception) {
                        _driveSyncState.value = _driveSyncState.value.copy(
                            isSyncing = false,
                            isPending = false,
                            lastError = e.localizedMessage
                        )
                    }
                } else {
                    _driveSyncState.value = _driveSyncState.value.copy(
                        isPending = false,
                        pendingOperationsCount = 0
                    )
                }
            }
        }
        .launchIn(viewModelScope)

        // Automatically restore data from Firebase upon ViewModel initialization if logged in
        try {
            if (FirebaseAuth.getInstance().currentUser != null) {
                autoRestoreFromFirebase()
            }
        } catch (e: Exception) {
            // Firebase Auth not initialized or unavailable
        }
    }

    /**
     * Automatically restores data from Firebase Firestore for the logged in user
     * if the local database is empty OR if ledger transactions are missing.
     * Prevents overwriting local changes or resurrecting deleted customers on startup.
     */
    fun autoRestoreFromFirebase(onResult: ((Boolean, String) -> Unit)? = null) {
        val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        val userId = currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Check if local database already has folders, customers, and transactions
                val existingFolders = repository.foldersWithCount.first()
                val existingCustomers = repository.allCustomers.first()
                val existingTransactions = repository.allTransactions.first()

                // If local database has records AND transactions are present, skip auto-restore
                if ((existingFolders.isNotEmpty() || existingCustomers.isNotEmpty()) && existingTransactions.isNotEmpty()) {
                    return@launch
                }

                isRestoring = true
                val result = repository.restoreFullBackupFromFirestore(userId = userId)
                if (result.isSuccess) {
                    val msg = result.getOrNull() ?: "Firebase से डाटा स्वचालित रूप से रिस्टोर हो गया!"
                    onResult?.invoke(true, msg)
                } else {
                    onResult?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Auto restore failed")
                }
            } catch (e: Exception) {
                onResult?.invoke(false, e.localizedMessage ?: "Auto restore exception")
            } finally {
                isRestoring = false
            }
        }
    }

    /**
     * Immediately dispatches current snapshot to Firestore without waiting for debounce,
     * ensuring instant consistency across app closes or process kills.
     */
    fun syncStateToFirestoreImmediate() {
        viewModelScope.launch {
            if (!isRestoring) {
                try {
                    val currentFolders = repository.foldersWithCount.first().map { it.folder }
                    val currentCustomers = repository.allCustomers.first()
                    val currentTransactions = repository.allTransactions.first()
                    if (currentFolders.isNotEmpty() || currentCustomers.isNotEmpty() || currentTransactions.isNotEmpty()) {
                        repository.saveFullBackupToFirestoreShreeBartan(
                            currentFolders,
                            currentCustomers,
                            currentTransactions
                        )
                    }
                } catch (e: Exception) {
                    // Background sync error ignored
                }
            }
        }
    }

    // 4. Function called when text changes in UI
    fun onInputTextChanged(newText: String) {
        _inputData.value = newText
    }

    // Auto-save helper to Firestore
    fun saveToFirestoreBartan(data: Any? = null) {
        saveToShreeFirestoreBartan(customBartanData = data)
    }

    // Folder search & filter
    private val _folderSearchQuery = MutableStateFlow("")
    val folderSearchQuery: StateFlow<String> = _folderSearchQuery.asStateFlow()

    private val _selectedAreaFilter = MutableStateFlow("All")
    val selectedAreaFilter: StateFlow<String> = _selectedAreaFilter.asStateFlow()

    val rawFolders: StateFlow<List<FolderWithCount>> = repository.foldersWithCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedFolders: StateFlow<List<FolderWithCount>> = repository.deletedFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedCustomers: StateFlow<List<CustomerEntity>> = repository.deletedCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredFolders: StateFlow<List<FolderWithCount>> = combine(
        rawFolders,
        _folderSearchQuery,
        _selectedAreaFilter
    ) { folders, query, area ->
        val trimmedQuery = query.trim()
        val queryTransliterations = if (trimmedQuery.isNotBlank()) NameMatcher.getAllTransliterationVariants(trimmedQuery) else emptyList()

        folders.filter { item ->
            val fName = item.folder.name
            val fArea = item.folder.areaTag
            val fDesc = item.folder.description

            val matchesQuery = trimmedQuery.isBlank() ||
                    fName.contains(trimmedQuery, ignoreCase = true) ||
                    fArea.contains(trimmedQuery, ignoreCase = true) ||
                    fDesc.contains(trimmedQuery, ignoreCase = true) ||
                    queryTransliterations.any { vt ->
                        fName.contains(vt, ignoreCase = true) || fArea.contains(vt, ignoreCase = true)
                    } ||
                    NameMatcher.calculateSimilarity(trimmedQuery, fName) >= 65 ||
                    (fArea.isNotBlank() && NameMatcher.calculateSimilarity(trimmedQuery, fArea) >= 65)

            val matchesArea = area == "All" || item.folder.areaTag.equals(area, ignoreCase = true)
            matchesQuery && matchesArea
        }.sortedBy { it.folder.name.trim().lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current active folder selection for customer view
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFolder: StateFlow<FolderEntity?> = _currentFolderId
        .flatMapLatest { id ->
            if (id != null) repository.getFolder(id) else flowOf(null)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Customer search, sort & payment filter states
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    private val _customerSortOption = MutableStateFlow(CustomerSortOption.NAME_ASC)
    val customerSortOption: StateFlow<CustomerSortOption> = _customerSortOption.asStateFlow()

    private val _customerPaymentFilter = MutableStateFlow(CustomerPaymentFilterOption.ALL)
    val customerPaymentFilter: StateFlow<CustomerPaymentFilterOption> = _customerPaymentFilter.asStateFlow()

    private val _customerBookFilter = MutableStateFlow<String?>(null) // null for All Books
    val customerBookFilter: StateFlow<String?> = _customerBookFilter.asStateFlow()

    private val _customerFilterMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // Default to current month (0=Jan..11=Dec), -1 for All
    val customerFilterMonth: StateFlow<Int> = _customerFilterMonth.asStateFlow()

    private val _customerFilterYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val customerFilterYear: StateFlow<Int> = _customerFilterYear.asStateFlow()

    fun setCustomerSortOption(option: CustomerSortOption) {
        _customerSortOption.value = option
    }

    fun setCustomerPaymentFilter(filter: CustomerPaymentFilterOption) {
        _customerPaymentFilter.value = filter
    }

    fun setCustomerBookFilter(book: String?) {
        _customerBookFilter.value = book
    }

    fun setCustomerFilterMonth(month: Int) {
        _customerFilterMonth.value = month
    }

    fun setCustomerFilterYear(year: Int) {
        _customerFilterYear.value = year
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableBookNumbers: StateFlow<List<String>> = _currentFolderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCustomers(id)
    }.map { list ->
        list.map { it.bookNumber.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith { a, b -> naturalCompare(a, b) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val customerFinancialsMap: StateFlow<Map<Long, InternalCustomerFinancials>> = combine(
        repository.allCustomers,
        repository.allTransactions,
        _customerFilterMonth,
        _customerFilterYear
    ) { customers, txList, filterMonth, filterYear ->
        val cal = Calendar.getInstance()
        val curMonth = if (filterMonth < 0) cal.get(Calendar.MONTH) else filterMonth
        val curYear = if (filterYear <= 0) cal.get(Calendar.YEAR) else filterYear
        val now = System.currentTimeMillis()
        val ninetyDaysMillis = 90L * 24 * 60 * 60 * 1000L // 3 continuous months (approx 90 days)

        customers.associate { customer ->
            val custTx = txList.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val paidOverall = custTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
            val duesOverall = goodsProvided - paidOverall
            val isPaidOverall = duesOverall <= 0.01

            val latestPaymentTime = custTx.filter { it.type == "PAYMENT_DEPOSIT" }.maxOfOrNull { it.dateMillis } ?: 0L
            val firstGoodsTime = custTx.filter { it.type == "GOODS_PROVIDED" }.minOfOrNull { it.dateMillis } ?: customer.createdAt

            val monthPaid = custTx.filter { tx ->
                if (tx.type != "PAYMENT_DEPOSIT") return@filter false
                cal.timeInMillis = tx.dateMillis
                (filterMonth < 0 || cal.get(Calendar.MONTH) == curMonth) && (filterYear <= 0 || cal.get(Calendar.YEAR) == curYear)
            }.sumOf { it.totalAmount }

            val monthGoods = custTx.filter { tx ->
                if (tx.type != "GOODS_PROVIDED") return@filter false
                cal.timeInMillis = tx.dateMillis
                (filterMonth < 0 || cal.get(Calendar.MONTH) == curMonth) && (filterYear <= 0 || cal.get(Calendar.YEAR) == curYear)
            }.sumOf { it.totalAmount }

            // Check if customer has not made payment for continuous 3 months with pending balance
            val isOverdue3Months = if (duesOverall > 0.01) {
                if (latestPaymentTime > 0L) {
                    (now - latestPaymentTime) >= ninetyDaysMillis
                } else {
                    (now - firstGoodsTime) >= ninetyDaysMillis
                }
            } else false

            val isExplicitlyBlocked = customer.status.contains("Blocked", ignoreCase = true)
            val isBlocked = isExplicitlyBlocked || isOverdue3Months

            val blockReason = when {
                isExplicitlyBlocked -> "Blocked by shopkeeper"
                isOverdue3Months -> "Auto-Blocked: 3+ months continuous non-payment"
                else -> ""
            }

            customer.id to InternalCustomerFinancials(
                overallDues = duesOverall,
                totalPaidOverall = paidOverall,
                isOverallPaid = isPaidOverall,
                monthPaid = monthPaid,
                monthGoodsProvided = monthGoods,
                hasReceivedPaymentInMonth = monthPaid > 0.0,
                latestPaymentTime = latestPaymentTime,
                isOverdue3Months = isOverdue3Months,
                isBlocked = isBlocked,
                blockReason = blockReason
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val customersInCurrentFolder: StateFlow<List<CustomerEntity>> = combine(
        _currentFolderId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCustomers(id)
        },
        combine(
            _customerSearchQuery,
            _customerSortOption,
            _customerPaymentFilter,
            _customerBookFilter,
            customerFinancialsMap
        ) { query, sortOpt, filterOpt, bookFilter, financialsMap ->
            CustomerCombinedParams(query, sortOpt, filterOpt, bookFilter, financialsMap)
        }
    ) { rawCustomers, (query, sortOpt, filterOpt, bookFilter, financialsMap) ->
        // 1. Search query filter (supporting Hindi speech, English names, phonetic matching & typo tolerance)
        val searchFiltered = if (query.isBlank()) {
            rawCustomers
        } else {
            NameMatcher.filterCustomers(query, rawCustomers)
        }

        // 2. Payment Status Filter
        val paymentFiltered = when (filterOpt) {
            CustomerPaymentFilterOption.ALL -> searchFiltered
            CustomerPaymentFilterOption.ACTIVE -> searchFiltered.filter { financialsMap[it.id]?.isBlocked != true }
            CustomerPaymentFilterOption.BLOCKED -> searchFiltered.filter { financialsMap[it.id]?.isBlocked == true }
            CustomerPaymentFilterOption.PAID_THIS_MONTH -> searchFiltered.filter { financialsMap[it.id]?.hasReceivedPaymentInMonth == true }
            CustomerPaymentFilterOption.UNPAID_THIS_MONTH -> searchFiltered.filter {
                val fin = financialsMap[it.id]
                (fin?.overallDues ?: 0.0) > 0.01 && (fin?.hasReceivedPaymentInMonth == false)
            }
            CustomerPaymentFilterOption.UNPAID -> searchFiltered.filter { (financialsMap[it.id]?.overallDues ?: 0.0) > 0.01 }
            CustomerPaymentFilterOption.PAID -> searchFiltered.filter { (financialsMap[it.id]?.overallDues ?: 0.0) <= 0.01 }
        }

        // 3. Book Number Filter
        val bookFiltered = if (bookFilter.isNullOrBlank()) {
            paymentFiltered
        } else if (bookFilter == "__NO_BOOK__") {
            paymentFiltered.filter { it.bookNumber.isBlank() }
        } else {
            paymentFiltered.filter { it.bookNumber.trim().equals(bookFilter.trim(), ignoreCase = true) }
        }

        // 4. Apply Sort Option
        when (sortOpt) {
            CustomerSortOption.NAME_ASC -> if (query.isNotBlank()) {
                bookFiltered // Preserve relevance ranking (exact/prefix matches first, then U-initials, etc.)
            } else {
                bookFiltered.sortedBy { it.name.trim().lowercase() }
            }
            CustomerSortOption.NAME_DESC -> bookFiltered.sortedByDescending { it.name.trim().lowercase() }
            CustomerSortOption.BOOK_ASC -> bookFiltered.sortedWith { c1, c2 ->
                val bookCmp = naturalCompare(c1.bookNumber, c2.bookNumber)
                if (bookCmp != 0) return@sortedWith bookCmp
                val pageCmp = naturalCompare(c1.pageNumber, c2.pageNumber)
                if (pageCmp != 0) return@sortedWith pageCmp
                c1.name.trim().compareTo(c2.name.trim(), ignoreCase = true)
            }
            CustomerSortOption.PAGE_ASC -> bookFiltered.sortedWith { c1, c2 ->
                val pageCmp = naturalCompare(c1.pageNumber, c2.pageNumber)
                if (pageCmp != 0) return@sortedWith pageCmp
                val bookCmp = naturalCompare(c1.bookNumber, c2.bookNumber)
                if (bookCmp != 0) return@sortedWith bookCmp
                c1.name.trim().compareTo(c2.name.trim(), ignoreCase = true)
            }
            CustomerSortOption.UNPAID_FIRST -> bookFiltered.sortedWith(
                compareByDescending<CustomerEntity> { !(financialsMap[it.id]?.isOverallPaid ?: true) }
                    .thenByDescending { financialsMap[it.id]?.overallDues ?: 0.0 }
                    .thenBy { it.name.trim().lowercase() }
            )
            CustomerSortOption.PAID_FIRST -> bookFiltered.sortedWith(
                compareBy<CustomerEntity> { !(financialsMap[it.id]?.isOverallPaid ?: true) }
                    .thenBy { financialsMap[it.id]?.overallDues ?: 0.0 }
                    .thenBy { it.name.trim().lowercase() }
            )
            CustomerSortOption.PAID_THIS_MONTH -> bookFiltered.sortedWith(
                compareByDescending<CustomerEntity> { financialsMap[it.id]?.hasReceivedPaymentInMonth ?: false }
                    .thenByDescending { financialsMap[it.id]?.monthPaid ?: 0.0 }
                    .thenBy { it.name.trim().lowercase() }
            )
            CustomerSortOption.UNPAID_THIS_MONTH -> bookFiltered.sortedWith(
                compareByDescending<CustomerEntity> {
                    val fin = financialsMap[it.id]
                    (fin?.overallDues ?: 0.0) > 0.01 && (fin?.hasReceivedPaymentInMonth == false)
                }
                .thenByDescending { financialsMap[it.id]?.overallDues ?: 0.0 }
                .thenBy { it.name.trim().lowercase() }
            )
            CustomerSortOption.HIGHEST_DUE -> bookFiltered.sortedByDescending { financialsMap[it.id]?.overallDues ?: 0.0 }
            CustomerSortOption.RECENT_PAYMENT -> bookFiltered.sortedByDescending { financialsMap[it.id]?.latestPaymentTime ?: 0L }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All Customers & Transactions / Goods & Payment Ledger
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Financial Summary Time Filters (Year / Month) & Folder Filter
    private val _selectedSummaryYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedSummaryYear: StateFlow<Int> = _selectedSummaryYear.asStateFlow()

    private val _selectedSummaryMonth = MutableStateFlow(-1) // -1 for All Months
    val selectedSummaryMonth: StateFlow<Int> = _selectedSummaryMonth.asStateFlow()

    private val _selectedSummaryFolderId = MutableStateFlow<Long?>(-1L) // -1L for All Folders
    val selectedSummaryFolderId: StateFlow<Long?> = _selectedSummaryFolderId.asStateFlow()

    // Aggregated Folder Summaries
    val folderSummaries: StateFlow<List<FolderFinancialSummary>> = combine(
        rawFolders,
        allTransactions,
        _selectedSummaryYear,
        _selectedSummaryMonth,
        _selectedSummaryFolderId
    ) { foldersWithCount, transactions, year, month, filterFolderId ->
        val filteredTxList = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            val txYear = cal.get(Calendar.YEAR)
            val txMonth = cal.get(Calendar.MONTH)

            val matchesYear = year <= 0 || txYear == year
            val matchesMonth = month < 0 || txMonth == month
            matchesYear && matchesMonth
        }

        val folderList = if (filterFolderId != null && filterFolderId > 0) {
            foldersWithCount.filter { it.folder.id == filterFolderId }
        } else {
            foldersWithCount
        }

        folderList.map { folderItem ->
            val folder = folderItem.folder
            val folderTx = filteredTxList.filter { it.folderId == folder.id }

            var folderGoodsTotal = 0.0
            var folderPaymentsTotal = 0.0

            val customerSummariesMap = mutableMapOf<Long, MutableList<TransactionEntity>>()
            folderTx.forEach { tx ->
                customerSummariesMap.getOrPut(tx.customerId) { mutableListOf() }.add(tx)
                if (tx.type == "GOODS_PROVIDED") {
                    folderGoodsTotal += tx.totalAmount
                } else if (tx.type == "PAYMENT_DEPOSIT") {
                    folderPaymentsTotal += tx.totalAmount
                }
            }

            FolderFinancialSummary(
                folder = folder,
                totalCustomers = folderItem.customerCount,
                totalGoodsProvided = folderGoodsTotal,
                totalAmountPaid = folderPaymentsTotal,
                netBalance = folderGoodsTotal - folderPaymentsTotal,
                customerSummaries = emptyList() // filled dynamically if needed
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFolderSearchQuery(query: String) {
        _folderSearchQuery.value = query
    }

    fun setSelectedAreaFilter(area: String) {
        _selectedAreaFilter.value = area
    }

    fun selectFolder(folderId: Long) {
        _currentFolderId.value = folderId
        _customerSearchQuery.value = ""
    }

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun setSummaryTimeFilter(year: Int, month: Int) {
        _selectedSummaryYear.value = year
        _selectedSummaryMonth.value = month
    }

    fun setSummaryFolderFilter(folderId: Long?) {
        _selectedSummaryFolderId.value = folderId
    }

    fun addFolder(name: String, description: String, areaTag: String, colorHex: String) {
        viewModelScope.launch {
            repository.addFolder(
                FolderEntity(
                    name = name.trim(),
                    description = description.trim(),
                    areaTag = if (areaTag.isBlank()) "General" else areaTag.trim(),
                    colorHex = colorHex
                )
            )
            syncStateToFirestoreImmediate()
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
            syncStateToFirestoreImmediate()
        }
    }

    fun getFolderDeletionInfo(folder: FolderEntity): FolderDeletionInfo {
        val customersInFolder = allCustomers.value.filter { it.folderId == folder.id && !it.isDeleted }
        val transactionsInFolder = allTransactions.value.filter { it.folderId == folder.id }

        var totalGoods = 0.0
        var totalPaid = 0.0
        transactionsInFolder.forEach { tx ->
            when (tx.type) {
                "GOODS_PROVIDED" -> totalGoods += tx.totalAmount
                "PAYMENT_DEPOSIT" -> totalPaid += tx.totalAmount
            }
        }

        var totalPendingDues = 0.0
        var customersWithDues = 0
        val customerDuesList = mutableListOf<Pair<String, Double>>()

        customersInFolder.forEach { cust ->
            val custTx = transactionsInFolder.filter { it.customerId == cust.id }
            val cGoods = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val cPaid = custTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
            val balance = cGoods - cPaid
            if (balance > 0.01) {
                customersWithDues++
                totalPendingDues += balance
                customerDuesList.add(Pair(cust.name, balance))
            }
        }

        // Folder is only deletable if it has ZERO customers with pending dues and ZERO pending balance
        val canDelete = customersWithDues == 0 && totalPendingDues <= 0.01
        return FolderDeletionInfo(
            canDelete = canDelete,
            folder = folder,
            customerCount = customersInFolder.size,
            totalPendingDues = totalPendingDues,
            totalGoods = totalGoods,
            totalPaid = totalPaid,
            customersWithDuesCount = customersWithDues,
            customerDuesList = customerDuesList
        )
    }

    fun deleteFolder(folder: FolderEntity, onResult: ((Boolean, String, FolderDeletionInfo?) -> Unit)? = null) {
        viewModelScope.launch {
            val validation = repository.getFolderDeletionValidation(folder)
            if (!validation.canDelete) {
                val errorMsg = if (validation.customersWithDuesCount > 0) {
                    val sampleCust = validation.customerDuesList.take(2).joinToString(", ") { "${it.first} (₹${it.second.toInt()})" }
                    "फ़ोल्डर '${folder.name}' में ${validation.customersWithDuesCount} ग्राहकों का कुल ₹${validation.totalPendingDues.toInt()} बकाया बाकी है: $sampleCust"
                } else if (validation.customerCount > 0) {
                    "फ़ोल्डर '${folder.name}' में ${validation.customerCount} ग्राहक मौजूद हैं।"
                } else {
                    "फ़ोल्डर '${folder.name}' में ₹${validation.totalPendingDues.toInt()} का बकाया बाकी है।"
                }
                onResult?.invoke(false, errorMsg, validation)
                return@launch
            }

            if (_currentFolderId.value == folder.id) {
                _currentFolderId.value = null
            }
            val result = repository.deleteFolder(folder)
            if (result.isSuccess) {
                syncStateToFirestoreImmediate()
                onResult?.invoke(true, "फ़ोल्डर सफलतापूर्वक रीसायकल बिन में भेजा गया", validation)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "फ़ोल्डर डिलीट नहीं किया जा सका", validation)
            }
        }
    }

    fun restoreFolder(folderId: Long) {
        viewModelScope.launch {
            repository.restoreFolder(folderId)
            syncStateToFirestoreImmediate()
        }
    }

    fun permanentlyDeleteFolder(folderId: Long) {
        viewModelScope.launch {
            if (_currentFolderId.value == folderId) {
                _currentFolderId.value = null
            }
            repository.permanentlyDeleteFolder(folderId)
            syncStateToFirestoreImmediate()
        }
    }

    fun addCustomer(
        folderId: Long,
        name: String,
        phone: String,
        bookNumber: String = "",
        pageNumber: String = "",
        address: String,
        notes: String,
        photoUri: String? = null,
        documentType: String = "",
        documentNumber: String = "",
        documentPhotoUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        smsNotificationsEnabled: Boolean = true,
        givenItems: List<GivenItem> = emptyList(),
        onSuccess: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val shouldCapitalize = _autoCapitalizeCustomerNames.value
            val formattedName = if (shouldCapitalize) name.trim().uppercase() else name.trim()

            // Save photos permanently to internal storage so they survive cache clearing
            val permanentPhotoUri = if (!photoUri.isNullOrBlank()) {
                com.example.util.PhotoStorageManager.saveCustomerPhotoPermanently(
                    getApplication(),
                    photoUri,
                    formattedName
                )
            } else null

            val permanentDocPhotoUri = if (!documentPhotoUri.isNullOrBlank()) {
                com.example.util.PhotoStorageManager.saveDocumentPhotoPermanently(
                    getApplication(),
                    documentPhotoUri,
                    documentType
                )
            } else null

            val customerEntity = CustomerEntity(
                folderId = folderId,
                name = formattedName,
                phone = phone.trim(),
                bookNumber = bookNumber.trim(),
                pageNumber = pageNumber.trim(),
                email = "",
                address = address.trim(),
                notes = notes.trim(),
                status = "",
                photoUri = permanentPhotoUri,
                documentType = documentType.trim(),
                documentNumber = documentNumber.trim(),
                documentPhotoUri = permanentDocPhotoUri,
                latitude = latitude,
                longitude = longitude,
                smsNotificationsEnabled = smsNotificationsEnabled
            )

            val newCustomerId = repository.addCustomer(customerEntity)
            val baseTime = System.currentTimeMillis()
            givenItems.forEachIndexed { index, item ->
                repository.addTransaction(
                    TransactionEntity(
                        customerId = newCustomerId,
                        folderId = folderId,
                        type = "GOODS_PROVIDED",
                        itemDescription = item.name.trim(),
                        quantity = item.qty.toInt().coerceAtLeast(1),
                        unitType = item.unit,
                        quantityDouble = item.qty,
                        unitPrice = item.rate,
                        totalAmount = item.totalAmount,
                        dateMillis = baseTime + index,
                        notes = "Initial goods given on customer creation"
                    )
                )
            }
            syncStateToFirestoreImmediate()
            
            // Background sync photo to Drive if enabled
            if (permanentPhotoUri != null || permanentDocPhotoUri != null) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (com.example.util.GoogleDriveManager.isAutoSyncEnabled(getApplication())) {
                            com.example.util.GoogleDriveManager.syncAllCustomerPhotosToDrive(
                                getApplication(),
                                listOf(customerEntity.copy(id = newCustomerId))
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FolderViewModel", "Drive photo auto-sync notice: ${e.message}")
                    }
                }
            }

            onSuccess?.invoke(newCustomerId)
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            // Save photos permanently to internal storage
            val permanentPhotoUri = if (!customer.photoUri.isNullOrBlank()) {
                com.example.util.PhotoStorageManager.saveCustomerPhotoPermanently(
                    getApplication(),
                    customer.photoUri,
                    customer.name
                )
            } else null

            val permanentDocPhotoUri = if (!customer.documentPhotoUri.isNullOrBlank()) {
                com.example.util.PhotoStorageManager.saveDocumentPhotoPermanently(
                    getApplication(),
                    customer.documentPhotoUri,
                    customer.documentType
                )
            } else null

            val updatedCustomer = customer.copy(
                photoUri = permanentPhotoUri,
                documentPhotoUri = permanentDocPhotoUri
            )

            repository.updateCustomer(updatedCustomer)
            syncStateToFirestoreImmediate()

            // Background sync photo to Drive if enabled
            if (permanentPhotoUri != null || permanentDocPhotoUri != null) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (com.example.util.GoogleDriveManager.isAutoSyncEnabled(getApplication())) {
                            com.example.util.GoogleDriveManager.syncAllCustomerPhotosToDrive(
                                getApplication(),
                                listOf(updatedCustomer)
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("FolderViewModel", "Drive photo auto-sync notice: ${e.message}")
                    }
                }
            }
        }
    }

    fun moveCustomerToFolder(
        customer: CustomerEntity,
        targetFolderId: Long,
        targetFolderName: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.moveCustomerToFolder(customer.id, targetFolderId)
            syncStateToFirestoreImmediate()
            onSuccess?.invoke()
        }
    }

    fun deleteCustomer(
        customer: CustomerEntity,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val balance = repository.getCustomerBalance(customer.id)
            if (kotlin.math.abs(balance) > 0.01) {
                val errorMsg = if (balance > 0) {
                    "Cannot delete customer account! '${customer.name}' has pending dues of ₹${String.format("%.2f", balance)}. Account balance must be zero before deletion."
                } else {
                    "Cannot delete customer account! '${customer.name}' has an advance credit of ₹${String.format("%.2f", -balance)}. Account balance must be zero before deletion."
                }
                onResult?.invoke(false, errorMsg)
                return@launch
            }
            repository.deleteCustomer(customer)
            syncStateToFirestoreImmediate()
            onResult?.invoke(true, "Customer '${customer.name}' moved to Recycle Bin.")
        }
    }

    fun restoreCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.restoreCustomer(customer)
            syncStateToFirestoreImmediate()
        }
    }

    fun permanentlyDeleteCustomer(
        customerId: Long,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val balance = repository.getCustomerBalance(customerId)
            if (kotlin.math.abs(balance) > 0.01) {
                val errorMsg = "Cannot permanently delete! Customer has an unsettled balance of ₹${String.format("%.2f", balance)}. Balance must be zero before deletion."
                onResult?.invoke(false, errorMsg)
                return@launch
            }
            repository.permanentlyDeleteCustomer(customerId)
            syncStateToFirestoreImmediate()
            onResult?.invoke(true, "Customer account permanently deleted.")
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
            syncStateToFirestoreImmediate()
        }
    }

    fun recordTransaction(
        customerId: Long,
        folderId: Long,
        type: String, // "GOODS_PROVIDED" or "PAYMENT_DEPOSIT"
        itemDescription: String,
        quantity: Int = 1,
        unitType: String = "pcs",
        quantityDouble: Double = 1.0,
        unitPrice: Double = 0.0,
        totalAmount: Double = 0.0,
        notes: String = ""
    ) {
        val finalDesc = formatItemTitleCase(itemDescription.trim())
        if (finalDesc.isNotBlank() && type == "GOODS_PROVIDED") {
            rememberProductName(finalDesc)
        }
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    customerId = customerId,
                    folderId = folderId,
                    type = type,
                    itemDescription = finalDesc,
                    quantity = quantity,
                    unitType = unitType,
                    quantityDouble = quantityDouble,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    notes = notes.trim()
                )
            )
            // If payment was made by customer, auto-unblock customer if currently blocked
            val customer = repository.getCustomer(customerId).firstOrNull()
            if (type == "PAYMENT_DEPOSIT") {
                if (customer != null && (customer.status.contains("Blocked", ignoreCase = true) || customer.status != "Active")) {
                    repository.updateCustomer(customer.copy(status = "Active"))
                }
            }
            syncStateToFirestoreImmediate()

            // 1. Send Auto Push Notification to Customer via FCM/Firestore
            if (customer != null) {
                try {
                    val allTx = repository.getTransactionsForCustomer(customerId).firstOrNull() ?: emptyList()
                    val totalGave = allTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
                    val totalJama = allTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
                    val currentDues = totalGave - totalJama
                    val storeName = _shopProfile.value.name.ifBlank { "श्री बर्तन स्टोर" }
                    val matchingProductImg = if (type == "GOODS_PROVIDED") {
                        allProducts.value.firstOrNull { it.name.equals(finalDesc, ignoreCase = true) }
                            ?.imageUris?.split(",")?.firstOrNull { it.isNotBlank() }
                    } else null

                    com.example.util.NotificationDispatchManager.sendTransactionNotification(
                        context = getApplication(),
                        customerId = customerId,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        type = type,
                        amount = totalAmount,
                        itemDesc = finalDesc,
                        updatedDues = currentDues,
                        storeName = storeName,
                        imageUrl = matchingProductImg
                    )
                } catch (e: Exception) {
                    Log.w("FolderViewModel", "Could not send push notification: ${e.message}")
                }
            }
        }
    }

    fun recordMultipleTransactions(
        customerId: Long,
        folderId: Long,
        transactions: List<TransactionEntity>
    ) {
        viewModelScope.launch {
            val baseTime = System.currentTimeMillis()
            var hasPayment = false
            var totalTxAmount = 0.0
            transactions.forEachIndexed { index, tx ->
                val finalDesc = formatItemTitleCase(tx.itemDescription.trim())
                if (finalDesc.isNotBlank() && tx.type == "GOODS_PROVIDED") {
                    rememberProductName(finalDesc)
                }
                if (tx.type == "PAYMENT_DEPOSIT") {
                    hasPayment = true
                }
                totalTxAmount += tx.totalAmount
                val txTime = if (tx.dateMillis == 0L || tx.dateMillis <= baseTime) baseTime + index else tx.dateMillis + index
                repository.addTransaction(tx.copy(customerId = customerId, folderId = folderId, itemDescription = finalDesc, dateMillis = txTime))
            }
            val customer = repository.getCustomer(customerId).firstOrNull()
            if (hasPayment) {
                if (customer != null && (customer.status.contains("Blocked", ignoreCase = true) || customer.status != "Active")) {
                    repository.updateCustomer(customer.copy(status = "Active"))
                }
            }
            syncStateToFirestoreImmediate()

            // Send Auto Push Notification for batch items
            if (customer != null) {
                try {
                    val allTx = repository.getTransactionsForCustomer(customerId).firstOrNull() ?: emptyList()
                    val totalGave = allTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
                    val totalJama = allTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
                    val currentDues = totalGave - totalJama
                    val storeName = _shopProfile.value.name.ifBlank { "श्री बर्तन स्टोर" }
                    val firstItemDesc = transactions.firstOrNull()?.itemDescription ?: "सामान"
                    val descSummary = if (transactions.size > 1) "$firstItemDesc (+${transactions.size - 1} और)" else firstItemDesc
                    val batchImg = if (!hasPayment) {
                        allProducts.value.firstOrNull { it.name.equals(firstItemDesc, ignoreCase = true) }
                            ?.imageUris?.split(",")?.firstOrNull { it.isNotBlank() }
                    } else null

                    com.example.util.NotificationDispatchManager.sendTransactionNotification(
                        context = getApplication(),
                        customerId = customerId,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        type = if (hasPayment) "PAYMENT_DEPOSIT" else "GOODS_PROVIDED",
                        amount = totalTxAmount,
                        itemDesc = descSummary,
                        updatedDues = currentDues,
                        storeName = storeName,
                        imageUrl = batchImg
                    )
                } catch (e: Exception) {
                    Log.w("FolderViewModel", "Batch push notification error: ${e.message}")
                }
            }
        }
    }

    /**
     * Send Payment Reminder Push Notification to customer
     */
    fun sendPaymentReminderPush(
        customer: CustomerEntity,
        dues: Double,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val storeName = _shopProfile.value.name.ifBlank { "श्री बर्तन स्टोर" }
                val result = com.example.util.NotificationDispatchManager.sendPaymentReminderNotification(
                    context = getApplication(),
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    dues = dues,
                    storeName = storeName
                )
                if (result.isSuccess) {
                    onResult(true, "पेमेंट रिमाइंडर नोटिफिकेशन सफलतापूर्वक ग्राहक को भेजा गया!")
                } else {
                    onResult(false, result.exceptionOrNull()?.localizedMessage ?: "नोटिफिकेशन भेजने में त्रुटि हुई")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "त्रुटि हुई")
            }
        }
    }

    /**
     * Get list of all customers with pending dues (> 0) along with their dues
     */
    suspend fun getCustomersWithPendingDues(): List<Pair<CustomerEntity, Double>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val (_, customers, transactions) = repository.getFullDatabaseEntities()
        val txByCust = transactions.groupBy { it.customerId }
        val pendingList = mutableListOf<Pair<CustomerEntity, Double>>()
        for (cust in customers) {
            val custTx = txByCust[cust.id] ?: emptyList()
            val gave = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val got = custTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
            val dues = gave - got
            if (dues > 0.0) {
                pendingList.add(cust to dues)
            }
        }
        pendingList.sortedByDescending { it.second }
    }

    /**
     * Broadcast payment reminder push notification to all customers with pending dues (> 0)
     */
    fun sendPaymentReminderToAllPending(
        onProgress: (sentCount: Int, totalPending: Int) -> Unit = { _, _ -> },
        onComplete: (successCount: Int, totalPending: Int, message: String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pendingList = getCustomersWithPendingDues()

                if (pendingList.isEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(0, 0, "किसी भी ग्राहक का कोई बकाया (उधार) नहीं है!")
                    }
                    return@launch
                }

                val storeName = _shopProfile.value.name.ifBlank { "श्री बर्तन स्टोर" }
                var successCount = 0

                for ((index, pair) in pendingList.withIndex()) {
                    val (customer, dues) = pair
                    val res = com.example.util.NotificationDispatchManager.sendPaymentReminderNotification(
                        context = getApplication(),
                        customerId = customer.id,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        dues = dues,
                        storeName = storeName
                    )
                    if (res.isSuccess) {
                        successCount++
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onProgress(index + 1, pendingList.size)
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(
                        successCount,
                        pendingList.size,
                        "सफलतापूर्वक $successCount बकायेदार ग्राहकों को पेमेंट रिमाइंडर भेज दिया गया!"
                    )
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0, 0, "रिमाइंडर भेजने में त्रुटि: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Broadcast Flash Sale / Product Offer to all customers ('all_customers' topic)
     */
    fun broadcastProductOfferPush(
        product: com.example.data.entity.ProductEntity,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val storeName = _shopProfile.value.name.ifBlank { "श्री बर्तन स्टोर" }
                val firstImg = product.imageUris.split(",").firstOrNull { it.isNotBlank() }
                val result = com.example.util.NotificationDispatchManager.broadcastProductOfferToAll(
                    context = getApplication(),
                    productId = product.id,
                    productName = product.name,
                    sellingPrice = product.sellingPrice,
                    mrp = product.mrp,
                    imageUrl = firstImg,
                    storeName = storeName
                )
                if (result.isSuccess) {
                    onResult(true, "फ्लैश सेल ऑफर 'all_customers' टॉपिक पर सभी ग्राहकों को भेज दिया गया!")
                } else {
                    onResult(false, result.exceptionOrNull()?.localizedMessage ?: "ऑफर ब्रॉडकास्ट करने में त्रुटि हुई")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "त्रुटि हुई")
            }
        }
    }

    // --- Notification Deep Link Navigation State ---
    private val _notificationTargetProductId = MutableStateFlow<Long?>(null)
    val notificationTargetProductId: StateFlow<Long?> = _notificationTargetProductId.asStateFlow()

    private val _activeOfferProduct = MutableStateFlow<NotificationOfferProduct?>(null)
    val activeOfferProduct: StateFlow<NotificationOfferProduct?> = _activeOfferProduct.asStateFlow()

    fun openOfferNotification(offer: NotificationOfferProduct) {
        val currentCatalog = allProducts.value
        val localProd = if (offer.id != null) {
            currentCatalog.firstOrNull { it.id == offer.id }
        } else {
            currentCatalog.firstOrNull { it.name.equals(offer.name, ignoreCase = true) }
        }

        if (localProd != null) {
            val img = if (!localProd.imageUris.isNullOrBlank()) {
                localProd.imageUris.split(",").firstOrNull { it.isNotBlank() } ?: offer.imageUrl
            } else offer.imageUrl

            _activeOfferProduct.value = NotificationOfferProduct(
                id = localProd.id,
                name = localProd.name,
                sellingPrice = localProd.sellingPrice,
                mrp = localProd.mrp,
                imageUrl = img,
                description = if (localProd.description.isNotBlank()) localProd.description else offer.description,
                category = localProd.category,
                metalType = localProd.metalType,
                sizeSpec = localProd.sizeSpec,
                inStock = localProd.inStock
            )
            _notificationTargetProductId.value = localProd.id
        } else {
            _activeOfferProduct.value = offer
            if (offer.id != null) {
                _notificationTargetProductId.value = offer.id
            }
        }
    }

    fun openProductFromNotification(productId: Long) {
        _notificationTargetProductId.value = productId
        val localProd = allProducts.value.firstOrNull { it.id == productId }
        if (localProd != null) {
            val img = localProd.imageUris.split(",").firstOrNull { it.isNotBlank() }
            _activeOfferProduct.value = NotificationOfferProduct(
                id = localProd.id,
                name = localProd.name,
                sellingPrice = localProd.sellingPrice,
                mrp = localProd.mrp,
                imageUrl = img,
                description = localProd.description,
                category = localProd.category,
                metalType = localProd.metalType,
                sizeSpec = localProd.sizeSpec,
                inStock = localProd.inStock
            )
        }
    }

    fun clearNotificationTargetProduct() {
        _notificationTargetProductId.value = null
        _activeOfferProduct.value = null
    }

    private val _notificationTargetCustomerId = MutableStateFlow<Long?>(null)
    val notificationTargetCustomerId: StateFlow<Long?> = _notificationTargetCustomerId.asStateFlow()

    fun openCustomerFromNotification(customerId: Long) {
        _notificationTargetCustomerId.value = customerId
    }

    fun clearNotificationTargetCustomer() {
        _notificationTargetCustomerId.value = null
    }

    fun setCustomerBlockStatus(customer: CustomerEntity, isBlocked: Boolean, reason: String = "") {
        viewModelScope.launch {
            val newStatus = if (isBlocked) {
                if (reason.isNotBlank()) "Blocked ($reason)" else "Blocked"
            } else {
                "Active"
            }
            repository.updateCustomer(customer.copy(status = newStatus))
            syncStateToFirestoreImmediate()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            syncStateToFirestoreImmediate()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            syncStateToFirestoreImmediate()
        }
    }

    fun getTransactionsForCustomerFlow(customerId: Long) =
        repository.getTransactionsForCustomer(customerId)

    fun getCustomerFlow(customerId: Long) =
        repository.getCustomer(customerId)

    fun getFolderFlow(folderId: Long) =
        repository.getFolder(folderId)

    fun getCustomersForFolderFlow(folderId: Long) =
        repository.getCustomers(folderId)

    /**
     * Saves custom data or full app data to Firebase Firestore
     * collection 'shree', document 'bartan_data', field 'bartan'.
     */
    fun saveToShreeFirestoreBartan(
        customBartanData: Any? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                if (customBartanData != null) {
                    val result = repository.saveToFirestoreShreeBartan(customBartanData)
                    if (result.isSuccess) {
                        onResult(true, "Data saved to Firestore 'shree' collection under 'bartan' field successfully!")
                    } else {
                        onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to save to Firestore")
                    }
                } else {
                    // Save full snapshot of current folders, customers & transactions
                    val currentFolders = repository.foldersWithCount.first().map { it.folder }
                    val currentCustomers = repository.allCustomers.first()
                    val currentTransactions = repository.allTransactions.first()
                    val result = repository.saveFullBackupToFirestoreShreeBartan(
                        currentFolders,
                        currentCustomers,
                        currentTransactions
                    )
                    if (result.isSuccess) {
                        onResult(true, "Firebase में डाटा सुरक्षित रूप से सेव हो गया!")
                    } else {
                        onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to save snapshot to Firestore")
                    }
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "An error occurred while saving to Firestore")
            }
        }
    }

    /**
     * Restores backup data from Firebase Firestore directly into local Room database.
     */
    fun restoreFromFirebase(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = repository.restoreFullBackupFromFirestore(docId, customerId, userId)
                if (result.isSuccess) {
                    val message = result.getOrNull() ?: "Firebase से डाटा सफलता से रिस्टोर हो गया!"
                    onResult(true, message)
                } else {
                    onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Firebase से डाटा रिस्टोर करने में विफल")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error restoring from Firebase")
            }
        }
    }

    /**
     * Reads data from Firebase Firestore collection 'shree', dynamically targeting document by docId, customerId, or userId.
     */
    fun loadFromShreeFirestoreBartan(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val result = repository.fetchShreeBartanDataFromFirestore(docId, customerId, userId)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        onResult(true, "Successfully read Firestore 'shree' cloud document!")
                    } else {
                        onResult(true, "Document does not exist yet on Firestore.")
                    }
                } else {
                    onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to read from Firestore")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error reading from Firestore")
            }
        }
    }

    /**
     * Performs an immediate manual CSV backup and writes to today's rolling backup slot.
     */
    fun performManualCsvBackup(
        context: Context,
        onResult: (Boolean, String, java.io.File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = repository.performRollingCsvBackup(context)
                if (result.isSuccess) {
                    val file = result.getOrNull()
                    onResult(true, "CSV बैकअप सफलतापूर्वक तैयार हो गया!", file)
                } else {
                    onResult(false, result.exceptionOrNull()?.localizedMessage ?: "CSV बैकअप असफल रहा", null)
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "CSV Backup Error", null)
            }
        }
    }

    /**
     * Restores complete app state from selected CSV Uri in 1 click.
     */
    fun restoreFromCsvUri(
        context: Context,
        uri: android.net.Uri,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isRestoring = true
                val summary = repository.restoreFromCsvUri(context, uri)
                isRestoring = false
                onResult(summary)
            } catch (e: Exception) {
                isRestoring = false
                onResult(com.example.util.RestoreSummary(false, message = e.localizedMessage ?: "Restore Error"))
            }
        }
    }

    /**
     * Restores complete app state from one of the rolling backup slots in 1 click.
     */
    fun restoreFromRollingSlot(
        context: Context,
        isTodaySlot: Boolean,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isRestoring = true
                val summary = repository.restoreFromRollingSlot(context, isTodaySlot)
                isRestoring = false
                onResult(summary)
            } catch (e: Exception) {
                isRestoring = false
                onResult(com.example.util.RestoreSummary(false, message = e.localizedMessage ?: "Slot Restore Error"))
            }
        }
    }

    /**
     * Exports an individual customer ledger CSV file saved with customer's name and date.
     */
    fun exportCustomerLedgerCsv(
        context: Context,
        customer: CustomerEntity,
        transactions: List<TransactionEntity>
    ) {
        com.example.util.CsvBackupManager.exportCustomerLedgerCsv(context, customer, transactions)
    }

    /**
     * Uploads the complete database state to Google Drive as CSV immediately.
     */
    fun uploadToGoogleDriveNow(
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _driveSyncState.value = _driveSyncState.value.copy(isSyncing = true, isPending = false)
            try {
                val (folders, customers, transactions) = repository.getFullDatabaseEntities()
                val fullCsv = com.example.util.CsvBackupManager.buildFullAppCsv(folders, customers, transactions)
                val result = com.example.util.GoogleDriveManager.uploadBackupCsvToDrive(context, fullCsv)
                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    refreshDriveSyncState()
                    onResult(true, "Successfully uploaded backup to Google Drive (${file.name})")
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to upload to Google Drive"
                    _driveSyncState.value = _driveSyncState.value.copy(
                        isSyncing = false,
                        lastError = errorMsg
                    )
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Upload error"
                _driveSyncState.value = _driveSyncState.value.copy(
                    isSyncing = false,
                    lastError = errorMsg
                )
                onResult(false, errorMsg)
            }
        }
    }

    /**
     * Lists all CSV backups available on Google Drive.
     */
    fun fetchDriveBackupFiles(
        context: Context,
        onResult: (List<com.example.util.DriveBackupFile>, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = com.example.util.GoogleDriveManager.listBackupFilesFromDrive(context)
                if (result.isSuccess) {
                    onResult(result.getOrThrow(), null)
                } else {
                    onResult(emptyList(), result.exceptionOrNull()?.localizedMessage ?: "Failed to list Drive files")
                }
            } catch (e: Exception) {
                onResult(emptyList(), e.localizedMessage ?: "Failed to query Google Drive")
            }
        }
    }

    /**
     * Complete Full Sync: Saves all data across local Room, Firebase Firestore, local 2-day CSV backup,
     * creates an immutable Point-in-Time Snapshot, and uploads to Google Drive. Perfect for Pull-to-Refresh or 1-Click Sync.
     */
    fun syncAllDataToCloudAndDrive(
        context: Context,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _driveSyncState.value = _driveSyncState.value.copy(isSyncing = true, isPending = false)
            try {
                val (folders, customers, transactions) = repository.getFullDatabaseEntities()
                
                // 1. Save to Firebase Firestore
                val firestoreResult = repository.saveFullBackupToFirestoreShreeBartan(
                    folders,
                    customers,
                    transactions
                )

                // 2. Upload latest unified CSV to Google Cloud
                val fullCsv = com.example.util.CsvBackupManager.buildFullAppCsv(folders, customers, transactions)
                val totalRecords = folders.size + customers.size + transactions.size
                try {
                    repository.firestoreRepository.saveCsvBackupToCloud(
                        csvContent = fullCsv,
                        totalRecords = totalRecords,
                        folderCount = folders.size,
                        customerCount = customers.size,
                        transactionCount = transactions.size,
                        tag = "AutoSync"
                    )
                    com.example.util.GoogleCloudBackupManager.markCloudSyncSuccess(context, totalRecords)
                    com.example.util.GoogleDriveManager.markSyncSuccess(context, totalRecords)
                } catch (e: Exception) {
                    // ignore
                }
                
                // 3. Local 2-day rolling CSV Backup
                try {
                    com.example.util.CsvBackupManager.performRollingBackup(
                        context = context,
                        folders = folders,
                        customers = customers,
                        transactions = transactions
                    )
                } catch (e: Exception) {
                    // ignore
                }

                // 4. Create an immutable Point-in-Time snapshot before overwrite
                try {
                    val snapResult = com.example.util.CsvBackupManager.createSnapshot(
                        context = context,
                        folders = folders,
                        customers = customers,
                        transactions = transactions,
                        tag = "AutoSync"
                    )
                    if (snapResult.isSuccess) {
                        refreshSnapshots(context)
                    }
                } catch (e: Exception) {
                    // ignore snapshot creation error
                }
                
                refreshCloudBackups()
                refreshDriveSyncState()

                if (firestoreResult.isSuccess) {
                    onResult(true, "सभी डाटा Google Cloud, Snapshots और लोकल बैकअप में 100% सुरक्षित हो गया!")
                } else {
                    onResult(true, "डाटा स्थानीय बैकअप व स्नैपशॉट में सुरक्षित हो गया")
                }
            } catch (e: Exception) {
                _driveSyncState.value = _driveSyncState.value.copy(
                    isSyncing = false,
                    lastError = e.localizedMessage
                )
                onResult(false, e.localizedMessage ?: "Sync error")
            }
        }
    }

    /**
     * Manually takes a Point-in-Time snapshot with custom tag.
     */
    fun takeSnapshot(
        context: Context = getApplication(),
        tag: String = "Manual",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val (folders, customers, transactions) = repository.getFullDatabaseEntities()
                val snapResult = com.example.util.CsvBackupManager.createSnapshot(
                    context = context,
                    folders = folders,
                    customers = customers,
                    transactions = transactions,
                    tag = tag
                )
                if (snapResult.isSuccess) {
                    val snap = snapResult.getOrThrow()
                    val csvContent = com.example.util.CsvBackupManager.buildFullAppCsv(folders, customers, transactions)
                    com.example.util.GoogleDriveManager.uploadSnapshotToDrive(context, snap.fileName, csvContent)
                    refreshSnapshots(context)
                    onResult(true, "स्नैपशॉट सुरक्षित हो गया: ${snap.dateFormatted}, ${snap.timeFormatted} (${snap.totalRecords} रिकॉर्ड)")
                } else {
                    onResult(false, snapResult.exceptionOrNull()?.localizedMessage ?: "Snapshot creation failed")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error creating snapshot")
            }
        }
    }

    /**
     * Restores database from a specific Point-in-Time Snapshot.
     */
    fun restoreFromSnapshot(
        context: Context = getApplication(),
        snapshot: com.example.util.SnapshotInfo,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isRestoring = true
                val file = snapshot.file
                if (file == null || !file.exists()) {
                    isRestoring = false
                    onResult(com.example.util.RestoreSummary(false, message = "Snapshot file not found on disk"))
                    return@launch
                }
                val csvContent = file.readText(Charsets.UTF_8)
                val db = AppDatabase.getInstance(context)
                val summary = com.example.util.CsvBackupManager.restoreDatabaseFromCsvContent(context, csvContent, db)
                isRestoring = false
                refreshSnapshots(context)
                refreshDriveSyncState()
                onResult(summary)
            } catch (e: Exception) {
                isRestoring = false
                onResult(com.example.util.RestoreSummary(false, message = e.localizedMessage ?: "Error restoring from snapshot"))
            }
        }
    }

    /**
     * Deletes a local snapshot file.
     */
    fun deleteSnapshot(
        context: Context = getApplication(),
        snapshot: com.example.util.SnapshotInfo,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val deleted = com.example.util.CsvBackupManager.deleteSnapshot(context, snapshot.fileName)
            refreshSnapshots(context)
            onResult(deleted)
        }
    }

    /**
     * Restores database from a Google Drive CSV file in 1-click.
     */
    fun restoreFromGoogleDriveFile(
        context: Context,
        fileId: String,
        onResult: (com.example.util.RestoreSummary) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isRestoring = true
                val db = AppDatabase.getInstance(context)
                val result = com.example.util.GoogleDriveManager.restoreDatabaseFromDriveFile(context, fileId, db)
                isRestoring = false
                if (result.isSuccess) {
                    val summary = result.getOrThrow()
                    // Proactively restore customer photos from Drive in background
                    val allCust = db.customerDao().getAllCustomersDirect()
                    com.example.util.GoogleDriveManager.restorePhotosFromDrive(context, allCust, db)
                    refreshDriveSyncState()
                    onResult(summary)
                } else {
                    onResult(com.example.util.RestoreSummary(false, message = result.exceptionOrNull()?.localizedMessage ?: "Drive Restore failed"))
                }
            } catch (e: Exception) {
                isRestoring = false
                onResult(com.example.util.RestoreSummary(false, message = e.localizedMessage ?: "Drive restore error"))
            }
        }
    }

    /**
     * Manually triggers photo sync to Google Drive.
     */
    fun syncPhotosToGoogleDrive(
        context: Context = getApplication(),
        onResult: ((uploadedCount: Int, success: Boolean, message: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                _driveSyncState.value = _driveSyncState.value.copy(isPhotoSyncing = true)
                val allCust = allCustomers.value
                val res = com.example.util.GoogleDriveManager.syncAllCustomerPhotosToDrive(context, allCust)
                _driveSyncState.value = _driveSyncState.value.copy(isPhotoSyncing = false)
                refreshDriveSyncState()
                if (res.isSuccess) {
                    val count = res.getOrThrow()
                    onResult?.invoke(count, true, "Successfully synced $count photos to Google Drive!")
                } else {
                    onResult?.invoke(0, false, res.exceptionOrNull()?.localizedMessage ?: "Photo sync failed")
                }
            } catch (e: Exception) {
                _driveSyncState.value = _driveSyncState.value.copy(isPhotoSyncing = false)
                onResult?.invoke(0, false, e.localizedMessage ?: "Photo sync error")
            }
        }
    }

    // ----------------------------------------------------
    // SHOP PRODUCTS & CATALOG MANAGEMENT
    // ----------------------------------------------------
    val allProducts: StateFlow<List<com.example.data.entity.ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedProductCategory = MutableStateFlow("सभी")
    val selectedProductCategory: StateFlow<String> = _selectedProductCategory.asStateFlow()

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedProductCategory(category: String) {
        _selectedProductCategory.value = category
    }

    fun syncProductsToCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.syncProductsToFirestore()
            onComplete(result.isSuccess)
        }
    }

    fun syncAllProductImagesToCloudinary(
        context: Context,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
        onComplete: (uploadedCount: Int, success: Boolean) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val currentProducts = allProducts.value
                var uploadedCount = 0
                val total = currentProducts.size
                currentProducts.forEachIndexed { index, product ->
                    onProgress(index + 1, total)
                    val uris = product.imageUris.split(",").filter { it.isNotBlank() }
                    val hasLocalUris = uris.any { !it.startsWith("http://") && !it.startsWith("https://") }
                    if (hasLocalUris) {
                        val cloudUris = com.example.util.CloudinaryManager.uploadMultipleImages(context, uris)
                        val newImageUrisStr = cloudUris.joinToString(",")
                        if (newImageUrisStr != product.imageUris) {
                            val updated = product.copy(imageUris = newImageUrisStr, updatedAt = System.currentTimeMillis())
                            repository.updateProduct(updated)
                            uploadedCount++
                        }
                    }
                }
                repository.syncProductsToFirestore()
                onComplete(uploadedCount, true)
            } catch (e: Exception) {
                Log.e("FolderViewModel", "Error syncing product images to Cloudinary", e)
                onComplete(0, false)
            }
        }
    }

    fun addProduct(product: com.example.data.entity.ProductEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addProduct(product)
            onComplete(id)
            repository.syncProductsToFirestore()
        }
    }

    fun updateProduct(product: com.example.data.entity.ProductEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProduct(product)
            onComplete()
            repository.syncProductsToFirestore()
        }
    }

    fun deleteProduct(product: com.example.data.entity.ProductEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            onComplete()
            repository.syncProductsToFirestore()
        }
    }

    fun toggleProductStock(product: com.example.data.entity.ProductEntity) {
        viewModelScope.launch {
            repository.updateProductStock(product.id, !product.inStock)
            repository.syncProductsToFirestore()
        }
    }

    // --- Admin: Registered Customers from Cloud ---
    private val _registeredCustomers = MutableStateFlow<List<com.example.data.model.RegisteredCustomer>>(emptyList())
    val registeredCustomers: StateFlow<List<com.example.data.model.RegisteredCustomer>> = _registeredCustomers.asStateFlow()

    private val _isRefreshingRegisteredCustomers = MutableStateFlow(false)
    val isRefreshingRegisteredCustomers: StateFlow<Boolean> = _isRefreshingRegisteredCustomers.asStateFlow()

    fun loadRegisteredCustomers() {
        viewModelScope.launch {
            _isRefreshingRegisteredCustomers.value = true
            val result = repository.firestoreRepository.fetchRegisteredCustomers()
            result.onSuccess { list ->
                _registeredCustomers.value = list
            }.onFailure {
                Log.e("FolderViewModel", "Failed to load registered customers from Firestore", it)
            }
            _isRefreshingRegisteredCustomers.value = false
        }
    }

    init {
        loadRegisteredCustomers()
    }

    companion object {
        fun formatItemTitleCase(text: String): String {
            if (text.isBlank()) return text
            return text.split(Regex("\\s+")).joinToString(" ") { word ->
                if (word.isBlank()) ""
                else if (word.length == 1) word.uppercase()
                else word.substring(0, 1).uppercase() + word.substring(1)
            }
        }
    }
}

class FolderViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            val firestoreRepo = com.example.data.remote.FirestoreRepository(application.applicationContext)
            val repo = FolderRepository(db.folderDao(), db.customerDao(), db.transactionDao(), firestoreRepo, application.applicationContext, db.productDao())
            return FolderViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class NotificationOfferProduct(
    val id: Long? = null,
    val name: String,
    val sellingPrice: Double,
    val mrp: Double = 0.0,
    val imageUrl: String? = null,
    val description: String = "",
    val category: String = "ऑफर बर्तन",
    val metalType: String = "",
    val sizeSpec: String = "",
    val inStock: Boolean = true
)

