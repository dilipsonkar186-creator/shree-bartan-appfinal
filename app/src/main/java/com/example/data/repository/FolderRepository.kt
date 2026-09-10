package com.example.data.repository

import com.example.data.dao.CustomerDao
import com.example.data.dao.FolderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.TransactionEntity
import com.example.data.model.FolderDeletionInfo
import com.example.data.model.FolderWithCount
import com.example.data.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class FolderRepository(
    private val folderDao: FolderDao,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val context: android.content.Context? = null,
    val productDao: ProductDao? = null
) {
    val allProducts: Flow<List<ProductEntity>> = productDao?.getAllProductsFlow() ?: flowOf(emptyList())

    suspend fun addProduct(product: ProductEntity): Long = productDao?.insertProduct(product) ?: 0L

    suspend fun updateProduct(product: ProductEntity) {
        productDao?.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao?.deleteProduct(product)
    }

    suspend fun updateProductStock(id: Long, inStock: Boolean) {
        productDao?.updateStockStatus(id, inStock)
    }
    val foldersWithCount: Flow<List<FolderWithCount>> = folderDao.getFoldersWithCount()

    val deletedFolders: Flow<List<FolderWithCount>> = folderDao.getDeletedFolders()

    val deletedCustomers: Flow<List<CustomerEntity>> = customerDao.getDeletedCustomers()

    fun getFolder(folderId: Long): Flow<FolderEntity?> = folderDao.getFolderById(folderId)

    suspend fun addFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun getFolderDeletionValidation(folder: FolderEntity): FolderDeletionInfo {
        val customersInFolder = customerDao.getCustomersForFolderDirect(folder.id)
        val transactionsInFolder = transactionDao.getTransactionsForFolderList(folder.id)

        var totalGoods = 0.0
        var totalPaid = 0.0
        transactionsInFolder.forEach { tx ->
            when (tx.type) {
                "GOODS_PROVIDED" -> totalGoods += tx.totalAmount
                "PAYMENT_DEPOSIT" -> totalPaid += tx.totalAmount
            }
        }

        var totalPendingDues = 0.0
        var customersWithDuesCount = 0
        val customerDuesList = mutableListOf<Pair<String, Double>>()

        customersInFolder.forEach { cust ->
            val custTx = transactionsInFolder.filter { it.customerId == cust.id }
            val cGoods = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val cPaid = custTx.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
            val custBalance = cGoods - cPaid
            if (custBalance > 0.01) {
                customersWithDuesCount++
                totalPendingDues += custBalance
                customerDuesList.add(Pair(cust.name, custBalance))
            }
        }

        // Deletion rule: Folder can ONLY be deleted if there are ZERO customers with pending dues (> 0)
        // If even a single customer has even ₹1 or ₹50 or any amount due, canDelete is strictly FALSE.
        val canDelete = customersWithDuesCount == 0 && totalPendingDues <= 0.01

        return FolderDeletionInfo(
            canDelete = canDelete,
            folder = folder,
            customerCount = customersInFolder.size,
            totalPendingDues = totalPendingDues,
            totalGoods = totalGoods,
            totalPaid = totalPaid,
            customersWithDuesCount = customersWithDuesCount,
            customerDuesList = customerDuesList
        )
    }

    suspend fun deleteFolder(folder: FolderEntity): Result<Unit> {
        val validation = getFolderDeletionValidation(folder)
        if (!validation.canDelete) {
            return Result.failure(
                IllegalStateException("फ़ोल्डर '${folder.name}' में ₹${validation.totalPendingDues.toInt()} का बकाया बाकी है। यह फ़ोल्डर डिलीट नहीं किया जा सकता।")
            )
        }
        folderDao.softDeleteFolder(folder.id)
        return Result.success(Unit)
    }

    suspend fun restoreFolder(folderId: Long) = folderDao.restoreFolder(folderId)

    suspend fun permanentlyDeleteFolder(folderId: Long) = folderDao.permanentlyDeleteFolder(folderId)

    fun getCustomers(folderId: Long): Flow<List<CustomerEntity>> = customerDao.getCustomersForFolder(folderId)

    fun getCustomer(customerId: Long): Flow<CustomerEntity?> = customerDao.getCustomerById(customerId)

    fun searchCustomers(folderId: Long, query: String): Flow<List<CustomerEntity>> =
        customerDao.searchCustomers(folderId, query)

    suspend fun addCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)

    suspend fun moveCustomerToFolder(customerId: Long, newFolderId: Long) {
        customerDao.moveCustomerToFolder(customerId, newFolderId)
        transactionDao.updateTransactionsFolderForCustomer(customerId, newFolderId)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.softDeleteCustomer(customer.id)

    suspend fun deleteCustomerById(id: Long) = customerDao.softDeleteCustomer(id)

    suspend fun restoreCustomer(customer: CustomerEntity) {
        // Also ensure parent folder is restored if it was soft deleted
        folderDao.restoreFolder(customer.folderId)
        customerDao.restoreCustomer(customer.id)
    }

    suspend fun permanentlyDeleteCustomer(customerId: Long) = customerDao.permanentlyDeleteCustomer(customerId)

    suspend fun emptyRecycleBin() {
        customerDao.emptyDeletedCustomers()
        folderDao.emptyDeletedFolders()
    }

    // Transactions / Goods & Payments
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForCustomer(customerId)

    suspend fun getTransactionsForCustomerList(customerId: Long): List<TransactionEntity> =
        transactionDao.getTransactionsForCustomerList(customerId)

    suspend fun getCustomerBalance(customerId: Long): Double {
        val txList = transactionDao.getTransactionsForCustomerList(customerId)
        val goods = txList.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
        val paid = txList.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
        return goods - paid
    }

    fun getTransactionsForFolder(folderId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForFolder(folderId)

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun addTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    // Firestore Integration: 'shree' collection, 'bartan' field
    suspend fun saveToFirestoreShreeBartan(data: Any, docId: String? = null): Result<Unit> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val resolvedDocId = when {
            !docId.isNullOrBlank() -> docId
            currentUserId != "anonymous" -> "user_$currentUserId"
            else -> "bartan_data"
        }
        val payloadWithUserId = if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (data as Map<String, Any?>).toMutableMap().apply {
                put("userId", currentUserId)
            }
        } else {
            mapOf(
                "data" to data,
                "userId" to currentUserId
            )
        }
        return firestoreRepository.saveDataToShreeBartan(payloadWithUserId, docId = resolvedDocId)
    }

    suspend fun saveOfRestoreShreeBartan(data: Any, docId: String? = null): Result<Unit> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val resolvedDocId = when {
            !docId.isNullOrBlank() -> docId
            currentUserId != "anonymous" -> "user_$currentUserId"
            else -> "bartan_data"
        }
        val payloadWithUserId = if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (data as Map<String, Any?>).toMutableMap().apply {
                put("userId", currentUserId)
            }
        } else {
            mapOf(
                "data" to data,
                "userId" to currentUserId
            )
        }
        return firestoreRepository.saveDataToShreeBartan(payloadWithUserId, docId = resolvedDocId)
    }

    suspend fun saveFullBackupToFirestoreShreeBartan(
        folders: List<FolderEntity>,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        products: List<ProductEntity>? = null
    ): Result<Unit> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val actualProducts = products ?: try {
            productDao?.getAllProductsFlow()?.first() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return firestoreRepository.saveFullAppBackup(folders, customers, transactions, actualProducts, userId = currentUserId)
    }

    suspend fun syncProductsToFirestore(): Result<Unit> {
        val products = try {
            productDao?.getAllProductsFlow()?.first() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return firestoreRepository.saveProductsCatalog(products)
    }

    suspend fun fetchShreeBartanData(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null
    ): Result<Map<String, Any>?> {
        val currentUserId = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val resolvedDocId = when {
            !docId.isNullOrBlank() -> docId
            customerId != null -> "customer_$customerId"
            currentUserId != "anonymous" -> if (currentUserId.startsWith("user_")) currentUserId else "user_$currentUserId"
            else -> "bartan_data"
        }
        return firestoreRepository.fetchShreeBartanData(
            docId = resolvedDocId,
            customerId = customerId,
            userId = currentUserId
        )
    }

    suspend fun fetchShreeBartanDataFromFirestore(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null
    ): Result<Map<String, Any>?> {
        return fetchShreeBartanData(docId, customerId, userId)
    }

    suspend fun fetchSriprada(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null
    ): Result<Map<String, Any>?> {
        return fetchShreeBartanData(docId, customerId, userId)
    }

    suspend fun saveToFirestoreSriprada(data: Any, docId: String? = null): Result<Unit> {
        return saveToFirestoreShreeBartan(data, docId)
    }

    suspend fun restoreFullBackupFromFirestore(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null
    ): Result<String> {
        return try {
            var restoredFoldersCount = 0
            var restoredCustomersCount = 0
            var restoredTransactionsCount = 0

            // Helper to get existing customers to map customerId -> folderId
            val existingCustomersMap = mutableMapOf<Long, Long>()
            customerDao.getAllCustomers().first().forEach {
                existingCustomersMap[it.id] = it.folderId
            }

            // 1. Fetch primary target document
            val targetResult = fetchShreeBartanData(docId, customerId, userId)
            val primaryData = targetResult.getOrNull()

            // 2. Fetch all documents in 'shree' collection for maximum recovery coverage
            val allDocsResult = firestoreRepository.fetchAllShreeDocuments()
            val allDocsList = allDocsResult.getOrNull() ?: emptyList()

            val combinedDocs = mutableListOf<Map<String, Any>>()
            if (primaryData != null) combinedDocs.add(primaryData)
            allDocsList.forEach { doc ->
                if (!combinedDocs.any { it["_docId"] != null && it["_docId"] == doc["_docId"] }) {
                    combinedDocs.add(doc)
                }
            }

            if (combinedDocs.isEmpty()) {
                return Result.failure(Exception("Firebase document is empty or does not exist"))
            }

            // 1. Process Folders across documents
            for (doc in combinedDocs) {
                @Suppress("UNCHECKED_CAST")
                val container = (doc["bartan"] as? Map<String, Any?>) ?: doc
                @Suppress("UNCHECKED_CAST")
                val foldersList = container["folders"] as? List<Map<String, Any?>>
                foldersList?.forEach { item ->
                    val id = (item["id"] as? Number)?.toLong() ?: 0L
                    val name = item["name"] as? String ?: ""
                    if (name.isNotBlank()) {
                        val isDeleted = (item["isDeleted"] as? Boolean) == true || ((item["isDeleted"] as? Number)?.toInt() == 1)
                        val deletedAt = (item["deletedAt"] as? Number)?.toLong()?.takeIf { it > 0L }
                        val folder = FolderEntity(
                            id = id,
                            name = name,
                            description = item["description"] as? String ?: "",
                            areaTag = item["areaTag"] as? String ?: "General",
                            colorHex = item["colorHex"] as? String ?: "#4F46E5",
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            createdAt = (item["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        folderDao.insertFolder(folder)
                        restoredFoldersCount++
                    }
                }
            }

            // 2. Process Customers across documents
            for (doc in combinedDocs) {
                @Suppress("UNCHECKED_CAST")
                val container = (doc["bartan"] as? Map<String, Any?>) ?: doc
                @Suppress("UNCHECKED_CAST")
                val customersList = container["customers"] as? List<Map<String, Any?>>
                customersList?.forEach { item ->
                    val id = (item["id"] as? Number)?.toLong() ?: 0L
                    val folderId = (item["folderId"] as? Number)?.toLong() ?: 0L
                    val name = item["name"] as? String ?: ""
                    if (name.isNotBlank() && folderId > 0) {
                        val isDeleted = (item["isDeleted"] as? Boolean) == true || ((item["isDeleted"] as? Number)?.toInt() == 1)
                        val deletedAt = (item["deletedAt"] as? Number)?.toLong()?.takeIf { it > 0L }

                        // Reconstruct customer profile photo from Base64 if local file is missing
                        val rawPhotoUri = (item["photoUri"] as? String)?.takeIf { it.isNotBlank() }
                        val photoB64 = (item["photoBase64"] as? String)?.takeIf { it.isNotBlank() }
                        var resolvedPhotoUri = rawPhotoUri

                        if (context != null && !photoB64.isNullOrBlank()) {
                            val localFile = if (rawPhotoUri?.startsWith("file://") == true) {
                                java.io.File(android.net.Uri.parse(rawPhotoUri).path ?: "")
                            } else null

                            if (localFile == null || !localFile.exists() || localFile.length() == 0L) {
                                val cleanName = name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
                                val restored = com.example.util.PhotoStorageManager.restorePhotoFromBase64(
                                    context,
                                    photoB64,
                                    "photo_${cleanName}_$id.jpg",
                                    false
                                )
                                if (restored != null) resolvedPhotoUri = restored
                            }
                        }

                        // Reconstruct customer KYC document photo from Base64 if local file is missing
                        val rawDocPhotoUri = (item["documentPhotoUri"] as? String)?.takeIf { it.isNotBlank() }
                        val docPhotoB64 = (item["documentPhotoBase64"] as? String)?.takeIf { it.isNotBlank() }
                        var resolvedDocPhotoUri = rawDocPhotoUri

                        val docType = item["documentType"] as? String ?: ""
                        if (context != null && !docPhotoB64.isNullOrBlank()) {
                            val localDocFile = if (rawDocPhotoUri?.startsWith("file://") == true) {
                                java.io.File(android.net.Uri.parse(rawDocPhotoUri).path ?: "")
                            } else null

                            if (localDocFile == null || !localDocFile.exists() || localDocFile.length() == 0L) {
                                val cleanDocType = docType.replace(Regex("[^a-zA-Z0-9_]"), "_").take(15)
                                val restored = com.example.util.PhotoStorageManager.restorePhotoFromBase64(
                                    context,
                                    docPhotoB64,
                                    "doc_${cleanDocType}_$id.jpg",
                                    true
                                )
                                if (restored != null) resolvedDocPhotoUri = restored
                            }
                        }

                        val customer = CustomerEntity(
                            id = id,
                            folderId = folderId,
                            name = name,
                            phone = item["phone"] as? String ?: "",
                            bookNumber = item["bookNumber"] as? String ?: "",
                            pageNumber = item["pageNumber"] as? String ?: "",
                            email = item["email"] as? String ?: "",
                            address = item["address"] as? String ?: "",
                            notes = item["notes"] as? String ?: "",
                            status = item["status"] as? String ?: "Active",
                            photoUri = resolvedPhotoUri,
                            documentType = docType,
                            documentNumber = item["documentNumber"] as? String ?: "",
                            documentPhotoUri = resolvedDocPhotoUri,
                            latitude = (item["latitude"] as? Number)?.toDouble()?.takeIf { it != 0.0 },
                            longitude = (item["longitude"] as? Number)?.toDouble()?.takeIf { it != 0.0 },
                            smsNotificationsEnabled = (item["smsNotificationsEnabled"] as? Boolean) ?: true,
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            createdAt = (item["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        customerDao.insertCustomer(customer)
                        existingCustomersMap[id] = folderId
                        restoredCustomersCount++
                    }
                }
            }

            // 3. Process Transactions and Ledger Entries across all documents
            val existingTransactions: List<TransactionEntity> = transactionDao.getAllTransactions().first()
            val existingTxSignatures: MutableSet<String> = existingTransactions.map { tx ->
                "${tx.customerId}_${tx.dateMillis}_${tx.itemDescription}_${tx.totalAmount}"
            }.toMutableSet()

            for (doc in combinedDocs) {
                @Suppress("UNCHECKED_CAST")
                val container = (doc["bartan"] as? Map<String, Any?>) ?: doc
                
                // A. Explicit transactions array
                @Suppress("UNCHECKED_CAST")
                val txList = (container["transactions"] as? List<Map<String, Any?>>)
                    ?: (doc["transactions"] as? List<Map<String, Any?>>)

                txList?.forEach { item ->
                    val custId = (item["customerId"] as? Number)?.toLong() ?: 0L
                    var fldId = (item["folderId"] as? Number)?.toLong() ?: 0L
                    if (fldId <= 0L && custId > 0L) {
                        fldId = existingCustomersMap[custId] ?: 1L
                    }

                    if (custId > 0L) {
                        val desc = (item["itemDescription"] as? String) ?: (item["description"] as? String) ?: (item["name"] as? String) ?: ""
                        val date = (item["dateMillis"] as? Number)?.toLong() ?: (item["date"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        val total = (item["totalAmount"] as? Number)?.toDouble() ?: (item["amount"] as? Number)?.toDouble() ?: 0.0
                        val signature = "${custId}_${date}_${desc}_${total}"

                        if (!existingTxSignatures.contains(signature)) {
                            val tx = TransactionEntity(
                                customerId = custId,
                                folderId = fldId,
                                type = item["type"] as? String ?: "GOODS_PROVIDED",
                                itemDescription = desc,
                                quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                unitType = item["unitType"] as? String ?: "pcs",
                                quantityDouble = (item["quantityDouble"] as? Number)?.toDouble() ?: (item["quantity"] as? Number)?.toDouble() ?: 1.0,
                                unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: (item["price"] as? Number)?.toDouble() ?: 0.0,
                                totalAmount = total,
                                dateMillis = date,
                                notes = item["notes"] as? String ?: ""
                            )
                            transactionDao.insertTransaction(tx)
                            existingTxSignatures.add(signature)
                            restoredTransactionsCount++
                        }
                    }
                }

                // B. Customer-specific document bartan list e.g. customer_$id
                val docCustomerId = (doc["customerId"] as? Number)?.toLong()
                @Suppress("UNCHECKED_CAST")
                val bartanList = (doc["bartan"] as? List<Map<String, Any?>>)
                if (docCustomerId != null && docCustomerId > 0L && bartanList != null) {
                    val fldId = existingCustomersMap[docCustomerId] ?: 1L
                    bartanList.forEach { item ->
                        val desc = (item["itemDescription"] as? String) ?: (item["description"] as? String) ?: (item["name"] as? String) ?: ""
                        val date = (item["dateMillis"] as? Number)?.toLong() ?: (item["date"] as? Number)?.toLong() ?: (doc["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        val total = (item["totalAmount"] as? Number)?.toDouble() ?: (item["amount"] as? Number)?.toDouble() ?: 0.0
                        val signature = "${docCustomerId}_${date}_${desc}_${total}"

                        if (!existingTxSignatures.contains(signature)) {
                            val tx = TransactionEntity(
                                customerId = docCustomerId,
                                folderId = fldId,
                                type = item["type"] as? String ?: "GOODS_PROVIDED",
                                itemDescription = desc,
                                quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                unitType = item["unitType"] as? String ?: "pcs",
                                quantityDouble = (item["quantityDouble"] as? Number)?.toDouble() ?: (item["quantity"] as? Number)?.toDouble() ?: 1.0,
                                unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: (item["price"] as? Number)?.toDouble() ?: 0.0,
                                totalAmount = total,
                                dateMillis = date,
                                notes = item["notes"] as? String ?: ""
                            )
                            transactionDao.insertTransaction(tx)
                            existingTxSignatures.add(signature)
                            restoredTransactionsCount++
                        }
                    }
                }
            }

            val msg = "Firebase से $restoredFoldersCount फ़ोल्डर, $restoredCustomersCount ग्राहक और $restoredTransactionsCount लेन-देन रिस्टोर हुए!"
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs a 2-Day Rolling CSV Backup.
     * Automatically maintains exactly two rolling files (today and yesterday),
     * overwritten every 24 hours.
     */
    suspend fun performRollingCsvBackup(context: android.content.Context): Result<java.io.File> {
        val folders = folderDao.getAllFoldersDirect()
        val customers = customerDao.getAllCustomersDirect()
        val transactions = transactionDao.getAllTransactionsDirect()

        return com.example.util.CsvBackupManager.performRollingBackup(
            context = context,
            folders = folders,
            customers = customers,
            transactions = transactions
        )
    }

    /**
     * Restores full database in 1 click from a user-selected CSV URI.
     */
    suspend fun restoreFromCsvUri(context: android.content.Context, uri: android.net.Uri): com.example.util.RestoreSummary {
        val db = com.example.data.db.AppDatabase.getInstance(context)
        return com.example.util.CsvBackupManager.restoreDatabaseFromUri(context, uri, db)
    }

    /**
     * Restores full database in 1 click from one of the 2-day rolling backup slots (Today or Yesterday).
     */
    suspend fun restoreFromRollingSlot(context: android.content.Context, isTodaySlot: Boolean): com.example.util.RestoreSummary {
        val db = com.example.data.db.AppDatabase.getInstance(context)
        return com.example.util.CsvBackupManager.restoreFromRollingSlot(context, isTodaySlot, db)
    }

    suspend fun getFullDatabaseEntities(): Triple<List<FolderEntity>, List<CustomerEntity>, List<TransactionEntity>> {
        return Triple(
            folderDao.getAllFoldersDirect(),
            customerDao.getAllCustomersDirect(),
            transactionDao.getAllTransactionsDirect()
        )
    }
}


