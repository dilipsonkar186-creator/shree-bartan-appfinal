package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.TransactionEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(private val context: Context? = null) {

    private fun getFirestore(): FirebaseFirestore {
        if (context != null && FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "FirebaseApp.initializeApp default error", e)
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCN9xZ4Uo3kSnIABCcFwEV-bcT6VhYYUiA")
                        .setProjectId("clientfolder-manager")
                        .setApplicationId("1:583177365912:android:1a9ad390f097f5764ca3ed")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                } catch (ex: Exception) {
                    Log.e("FirestoreRepository", "FirebaseApp options init error", ex)
                }
            }
        }
        return FirebaseFirestore.getInstance()
    }

    /**
     * Saves app data into Firestore collection 'shree', using a dynamic document ID
     * (e.g. user_$currentUserId, docId, or fallback 'bartan_data').
     */
    suspend fun saveDataToShreeBartan(data: Any, docId: String? = null): Result<Unit> {
        return try {
            val currentAuthUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
            val currentUserId = currentAuthUser?.uid ?: "anonymous"
            if (currentAuthUser == null && docId == null) {
                Log.d("FirestoreRepository", "Skipping auto Firestore save because user is not authenticated")
                return Result.success(Unit)
            }
            val targetDocId = when {
                !docId.isNullOrBlank() -> docId
                currentUserId != "anonymous" -> "user_$currentUserId"
                else -> "bartan_data"
            }
            val docRef = getFirestore().collection("shree").document(targetDocId)
            val payload = hashMapOf<String, Any>(
                "bartan" to data,
                "userId" to currentUserId,
                "lastUpdated" to System.currentTimeMillis()
            )
            docRef.set(payload, SetOptions.merge()).await()
            Log.d("FirestoreRepository", "Successfully saved data to shree/$targetDocId -> field: bartan with userId: $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Warning: Could not save data to shree: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Reads data from Firestore collection 'shree', using dynamic document ID
     * (e.g. customer_$customerId, user_$userId, or provided docId).
     */
    suspend fun fetchShreeBartanData(
        docId: String? = null,
        customerId: Long? = null,
        userId: String? = null
    ): Result<Map<String, Any>?> {
        return try {
            val currentUserId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
            val targetDocId = when {
                !docId.isNullOrBlank() -> docId
                customerId != null -> "customer_$customerId"
                !userId.isNullOrBlank() -> if (userId.startsWith("user_")) userId else "user_$userId"
                !currentUserId.isNullOrBlank() -> "user_$currentUserId"
                else -> "bartan_data"
            }

            val doc = getFirestore().collection("shree").document(targetDocId).get().await()
            if (doc.exists()) {
                Log.d("FirestoreRepository", "Successfully fetched shree/$targetDocId document")
                Result.success(doc.data)
            } else {
                // Fallback check for legacy 'bartan_data' document
                if (targetDocId != "bartan_data") {
                    val fallbackDoc = getFirestore().collection("shree").document("bartan_data").get().await()
                    if (fallbackDoc.exists()) {
                        Log.d("FirestoreRepository", "Successfully fetched fallback shree/bartan_data document")
                        return Result.success(fallbackDoc.data)
                    }
                }
                Log.d("FirestoreRepository", "Document shree/$targetDocId does not exist yet")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching shree document", e)
            Result.failure(e)
        }
    }

    /**
     * Saves specific customer/transaction bartan records to collection 'shree'
     * with document ID as customer_$customerId and field 'bartan'.
     */
    suspend fun saveCustomerBartanData(
        customerId: Long,
        customerName: String,
        bartanItems: List<Map<String, Any>>,
        phone: String? = null
    ): Result<Unit> {
        return try {
            val docRef = getFirestore().collection("shree").document("customer_$customerId")
            val payload = hashMapOf<String, Any>(
                "customerId" to customerId,
                "customerName" to customerName,
                "phone" to (phone ?: ""),
                "bartan" to bartanItems,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads customer bartan records from Firestore collection 'shree' matching the logged-in user's mobile number.
     * Uses whereEqualTo to filter by customer mobile number so customers only see their own data.
     */
    suspend fun fetchCustomerBartanData(phoneNumber: String? = null): Result<Map<String, Any>?> {
        return try {
            val currentAuthUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
            val rawPhone = when {
                !phoneNumber.isNullOrBlank() -> phoneNumber.trim()
                !currentAuthUser?.phoneNumber.isNullOrBlank() -> currentAuthUser?.phoneNumber!!.trim()
                else -> ""
            }

            if (rawPhone.isBlank()) {
                Log.w("FirestoreRepository", "fetchCustomerBartanData: No phone number found for current user")
                return Result.success(null)
            }

            val collection = getFirestore().collection("shree")

            // Primary query using whereEqualTo with the user's mobile number
            var snapshot = collection.whereEqualTo("phone", rawPhone).get().await()

            // If not found, try common format variations with whereEqualTo
            if (snapshot.isEmpty) {
                val digitsOnly = rawPhone.filter { it.isDigit() }
                val variants = mutableListOf<String>()

                // Standard +91 format with space e.g. "+91 98765 43210" or "+91 9876543210"
                if (digitsOnly.length == 10) {
                    variants.add("+91 $digitsOnly")
                    variants.add("+91${digitsOnly}")
                    variants.add("+91 ${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}")
                    variants.add(digitsOnly)
                } else if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
                    val tenDigits = digitsOnly.substring(2)
                    variants.add("+91 $tenDigits")
                    variants.add("+91${tenDigits}")
                    variants.add("+91 ${tenDigits.substring(0, 5)} ${tenDigits.substring(5)}")
                    variants.add(tenDigits)
                }

                for (variant in variants) {
                    if (variant != rawPhone) {
                        val variantSnapshot = collection.whereEqualTo("phone", variant).get().await()
                        if (!variantSnapshot.isEmpty) {
                            snapshot = variantSnapshot
                            break
                        }
                    }
                }
            }

            // Also check alternate field name customerPhone if not found
            if (snapshot.isEmpty) {
                val mobileSnapshot = collection.whereEqualTo("customerPhone", rawPhone).get().await()
                if (!mobileSnapshot.isEmpty) {
                    snapshot = mobileSnapshot
                }
            }

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                Log.d("FirestoreRepository", "Successfully fetched bartan data for customer phone: $rawPhone (docId: ${doc.id})")
                Result.success(doc.data)
            } else {
                Log.d("FirestoreRepository", "No bartan data found matching phone: $rawPhone")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error querying customer bartan data by phone", e)
            Result.failure(e)
        }
    }

    /**
     * Saves complete app backup (Folders, Customers, Transactions) in 'shree' collection, 'bartan' field.
     * Safeguards against accidental ledger loss: never overwrites existing cloud transactions with an empty list
     * if customers exist.
     */
    suspend fun saveFullAppBackup(
        folders: List<FolderEntity>,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        products: List<ProductEntity> = emptyList(),
        userId: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: (try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }) ?: "anonymous"
            
            var finalTransactions: List<Map<String, Any>> = transactions.map { tx ->
                mapOf<String, Any>(
                    "id" to tx.id,
                    "customerId" to tx.customerId,
                    "folderId" to tx.folderId,
                    "type" to tx.type,
                    "itemDescription" to tx.itemDescription,
                    "quantity" to tx.quantity,
                    "unitType" to tx.unitType,
                    "quantityDouble" to tx.quantityDouble,
                    "unitPrice" to tx.unitPrice,
                    "totalAmount" to tx.totalAmount,
                    "dateMillis" to tx.dateMillis,
                    "notes" to tx.notes
                )
            }

            // CRITICAL ANTI-DATA-LOSS SAFEGUARD:
            // If local transactions list is empty but local customers exist, check if cloud already has transactions.
            // If cloud has transactions, preserve them so local temporary emptiness does not wipe cloud history.
            if (finalTransactions.isEmpty() && customers.isNotEmpty()) {
                val existingCloudResult = fetchShreeBartanData(userId = currentUserId)
                val existingCloudData = existingCloudResult.getOrNull()
                if (existingCloudData != null) {
                    @Suppress("UNCHECKED_CAST")
                    val existingContainer = (existingCloudData["bartan"] as? Map<String, Any?>) ?: existingCloudData
                    @Suppress("UNCHECKED_CAST")
                    val existingTxList = existingContainer["transactions"] as? List<Map<String, Any?>>
                    if (!existingTxList.isNullOrEmpty()) {
                        Log.w("FirestoreRepository", "Preserving ${existingTxList.size} existing cloud transactions during backup")
                        finalTransactions = existingTxList.mapNotNull { itemMap ->
                            val nonNullMap = mutableMapOf<String, Any>()
                            itemMap.forEach { (k, v) ->
                                if (v != null) nonNullMap[k] = v
                            }
                            if (nonNullMap.isNotEmpty()) nonNullMap else null
                        }
                    }
                }
            }

            val productsList = products.map { prod ->
                mapOf<String, Any>(
                    "id" to prod.id,
                    "name" to prod.name,
                    "category" to prod.category,
                    "metalType" to prod.metalType,
                    "sizeSpec" to prod.sizeSpec,
                    "mrp" to prod.mrp,
                    "sellingPrice" to prod.sellingPrice,
                    "description" to prod.description,
                    "imageUris" to prod.imageUris,
                    "inStock" to prod.inStock,
                    "createdAt" to prod.createdAt,
                    "updatedAt" to prod.updatedAt
                )
            }

            val backupMap = hashMapOf<String, Any>(
                "userId" to currentUserId,
                "folders" to folders.map { folder ->
                    mapOf(
                        "id" to folder.id,
                        "name" to folder.name,
                        "description" to folder.description,
                        "areaTag" to folder.areaTag,
                        "colorHex" to folder.colorHex,
                        "isDeleted" to folder.isDeleted,
                        "deletedAt" to (folder.deletedAt ?: 0L),
                        "createdAt" to folder.createdAt
                    )
                },
                "customers" to customers.map { cust ->
                    var photoB64: String? = null
                    var docPhotoB64: String? = null
                    if (context != null) {
                        if (!cust.photoUri.isNullOrBlank()) {
                            photoB64 = kotlinx.coroutines.runBlocking {
                                com.example.util.PhotoStorageManager.getPhotoAsBase64(context, cust.photoUri, 600, 75)
                            }
                        }
                        if (!cust.documentPhotoUri.isNullOrBlank()) {
                            docPhotoB64 = kotlinx.coroutines.runBlocking {
                                com.example.util.PhotoStorageManager.getPhotoAsBase64(context, cust.documentPhotoUri, 800, 75)
                            }
                        }
                    }
                    mapOf(
                        "id" to cust.id,
                        "folderId" to cust.folderId,
                        "name" to cust.name,
                        "phone" to cust.phone,
                        "bookNumber" to cust.bookNumber,
                        "pageNumber" to cust.pageNumber,
                        "email" to cust.email,
                        "address" to cust.address,
                        "notes" to cust.notes,
                        "status" to cust.status,
                        "photoUri" to (cust.photoUri ?: ""),
                        "photoBase64" to (photoB64 ?: ""),
                        "documentType" to cust.documentType,
                        "documentNumber" to cust.documentNumber,
                        "documentPhotoUri" to (cust.documentPhotoUri ?: ""),
                        "documentPhotoBase64" to (docPhotoB64 ?: ""),
                        "latitude" to (cust.latitude ?: 0.0),
                        "longitude" to (cust.longitude ?: 0.0),
                        "smsNotificationsEnabled" to cust.smsNotificationsEnabled,
                        "isDeleted" to cust.isDeleted,
                        "deletedAt" to (cust.deletedAt ?: 0L),
                        "createdAt" to cust.createdAt
                    )
                },
                "transactions" to finalTransactions,
                "products" to productsList
            )

            // Also sync dedicated product collections if products are present
            if (products.isNotEmpty()) {
                try {
                    saveProductsCatalog(products)
                } catch (_: Exception) {}
            }

            saveDataToShreeBartan(backupMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves products/shop catalog to Firestore.
     * Uploads across multiple standard locations so any customer app can read them:
     * 1) 'shree' collection, document 'products'
     * 2) 'shree' collection, document 'bartan_data' and 'user_$uid' under field 'products' & 'bartan.products'
     * 3) 'products' collection, document 'all' and individual product documents
     */
    suspend fun saveProductsCatalog(products: List<ProductEntity>): Result<Unit> {
        return try {
            val currentUserId = (try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }) ?: "anonymous"
            val productsList = products.map { prod ->
                mapOf<String, Any>(
                    "id" to prod.id,
                    "name" to prod.name,
                    "category" to prod.category,
                    "metalType" to prod.metalType,
                    "sizeSpec" to prod.sizeSpec,
                    "mrp" to prod.mrp,
                    "sellingPrice" to prod.sellingPrice,
                    "description" to prod.description,
                    "imageUris" to prod.imageUris,
                    "inStock" to prod.inStock,
                    "createdAt" to prod.createdAt,
                    "updatedAt" to prod.updatedAt
                )
            }

            val payload = hashMapOf<String, Any>(
                "products" to productsList,
                "userId" to currentUserId,
                "lastUpdated" to System.currentTimeMillis()
            )

            val firestore = getFirestore()

            // 1. Save in 'shree' collection, document 'products'
            try {
                firestore.collection("shree").document("products").set(payload, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed to save shree/products: ${e.message}")
            }

            // 2. Save in 'shree' collection, document 'bartan_data'
            try {
                val bartanDataPayload = hashMapOf<String, Any>(
                    "products" to productsList,
                    "bartan" to mapOf("products" to productsList),
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("shree").document("bartan_data").set(bartanDataPayload, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed to save shree/bartan_data products: ${e.message}")
            }

            // 3. If user is authenticated, save in 'shree/user_$uid'
            if (currentUserId != "anonymous") {
                try {
                    val userDocPayload = hashMapOf<String, Any>(
                        "products" to productsList,
                        "bartan" to mapOf("products" to productsList),
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    firestore.collection("shree").document("user_$currentUserId").set(userDocPayload, SetOptions.merge()).await()
                } catch (e: Exception) {
                    Log.w("FirestoreRepository", "Failed to save shree/user_$currentUserId products: ${e.message}")
                }
            }

            // 4. Save in 'products' collection, document 'all'
            try {
                firestore.collection("products").document("all").set(payload, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed to save products/all: ${e.message}")
            }

            // 5. Also save each individual product in 'products' collection
            try {
                for (prod in productsList) {
                    val prodId = prod["id"]?.toString() ?: continue
                    firestore.collection("products").document(prodId).set(prod, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed to save individual products in collection: ${e.message}")
            }

            Log.d("FirestoreRepository", "Successfully synced ${products.size} products to Firestore across multiple paths!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error syncing products to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all documents from the 'shree' collection to enable thorough data and ledger recovery.
     */
    suspend fun fetchAllShreeDocuments(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = getFirestore().collection("shree").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["_docId"] = doc.id
                data
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching all shree documents", e)
            Result.failure(e)
        }
    }

    /**
     * Saves or updates logged-in user profile details in Firestore
     * under collection 'users', document [uid], and also in 'shree' collection.
     */
    suspend fun saveUserData(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val userMap: Map<String, Any> = mapOf(
                "uid" to uid,
                "name" to (name ?: ""),
                "email" to (email ?: ""),
                "photoUrl" to (photoUrl ?: ""),
                "lastLogin" to System.currentTimeMillis()
            )

            // Save in 'users' collection
            try {
                getFirestore().collection("users").document(uid)
                    .set(userMap, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Could not save user profile to users/$uid: ${e.message}")
            }

            // Also save in 'shree' collection under user document
            try {
                getFirestore().collection("shree").document("user_$uid")
                    .set(mapOf("lastLoggedInUser" to userMap), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Could not save user profile to shree/user_$uid: ${e.message}")
            }

            Log.d("FirestoreRepository", "Successfully saved user $uid data to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Warning: Could not save user data to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Saves a complete CSV backup file to Google Cloud Firestore (collection 'cloud_csv_backups').
     * Accessible by user UID or default account.
     */
    suspend fun saveCsvBackupToCloud(
        csvContent: String,
        totalRecords: Int,
        folderCount: Int,
        customerCount: Int,
        transactionCount: Int,
        tag: String = "Manual"
    ): Result<Map<String, Any>> {
        return try {
            val currentAuthUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
            val currentUserId = currentAuthUser?.uid ?: "default_user"
            val userEmail = currentAuthUser?.email ?: "dilip.sonkar.186@gmail.com"
            val now = System.currentTimeMillis()
            val docId = "backup_${now}"

            val sdfDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val sdfTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val dateFormatted = sdfDate.format(java.util.Date(now))
            val timeFormatted = sdfTime.format(java.util.Date(now))

            val backupPayload = hashMapOf<String, Any>(
                "id" to docId,
                "timestamp" to now,
                "dateFormatted" to dateFormatted,
                "timeFormatted" to timeFormatted,
                "totalRecords" to totalRecords,
                "folderCount" to folderCount,
                "customerCount" to customerCount,
                "transactionCount" to transactionCount,
                "sizeBytes" to csvContent.toByteArray(Charsets.UTF_8).size.toLong(),
                "tag" to tag,
                "userId" to currentUserId,
                "userEmail" to userEmail,
                "csvContent" to csvContent
            )

            getFirestore().collection("cloud_csv_backups").document(docId)
                .set(backupPayload, SetOptions.merge())
                .await()

            // Also update the 'latest_backup' pointer
            getFirestore().collection("cloud_csv_backups").document("latest_${currentUserId}")
                .set(backupPayload, SetOptions.merge())
                .await()

            Log.d("FirestoreRepository", "Saved Cloud CSV backup: $docId ($totalRecords records)")
            Result.success(backupPayload)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error saving CSV backup to cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all saved Cloud CSV backups from Firestore.
     */
    suspend fun fetchCloudCsvBackups(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = getFirestore().collection("cloud_csv_backups")
                .whereNotEqualTo("id", null)
                .get()
                .await()

            val backups = snapshot.documents.mapNotNull { doc ->
                if (doc.id.startsWith("latest_")) return@mapNotNull null
                val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                data["id"] = doc.id
                data
            }.sortedByDescending { (it["timestamp"] as? Long) ?: 0L }

            Result.success(backups)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching Cloud CSV backups", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches CSV content of a specific backup from Google Cloud.
     */
    suspend fun fetchCloudCsvContent(backupId: String): Result<String> {
        return try {
            val doc = getFirestore().collection("cloud_csv_backups").document(backupId).get().await()
            val content = doc.getString("csvContent")
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Cloud backup content is empty or not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific Cloud CSV backup from Firestore.
     */
    suspend fun deleteCloudCsvBackup(backupId: String): Result<Unit> {
        return try {
            getFirestore().collection("cloud_csv_backups").document(backupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches user profile document from Firestore 'users' collection.
     */
    suspend fun fetchUserData(uid: String): Result<Map<String, Any>?> {
        return try {
            val doc = getFirestore().collection("users").document(uid).get().await()
            Result.success(if (doc.exists()) doc.data else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches registered customer accounts and locations from Firestore.
     * Looks in 'registered_customers', 'customer_locations', and 'users' collections.
     */
    suspend fun fetchRegisteredCustomers(): Result<List<com.example.data.model.RegisteredCustomer>> {
        return try {
            val firestore = getFirestore()
            val list = mutableListOf<com.example.data.model.RegisteredCustomer>()
            val processedIds = mutableSetOf<String>()

            // 1. Try 'registered_customers' collection
            try {
                val snapshot = firestore.collection("registered_customers").get().await()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    val data = doc.data ?: continue
                    val name = data["name"] as? String ?: data["customerName"] as? String ?: "ग्राहक"
                    val phone = data["phone"] as? String ?: data["phoneNumber"] as? String ?: data["mobile"] as? String ?: ""
                    val address = data["address"] as? String ?: data["locationAddress"] as? String ?: ""
                    val city = data["city"] as? String ?: "जबलपुर"
                    val lat = (data["latitude"] as? Number)?.toDouble() ?: (data["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (data["longitude"] as? Number)?.toDouble() ?: (data["lng"] as? Number)?.toDouble() ?: 0.0
                    val registeredAt = (data["registeredAt"] as? Number)?.toLong() ?: (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val accountType = data["accountType"] as? String ?: "नया खाता"
                    val lastOrderNote = data["lastOrderNote"] as? String ?: ""

                    processedIds.add(id)
                    list.add(
                        com.example.data.model.RegisteredCustomer(
                            id = id,
                            name = name,
                            phone = phone,
                            address = address,
                            city = city,
                            latitude = lat,
                            longitude = lng,
                            registeredAt = registeredAt,
                            accountType = accountType,
                            lastOrderNote = lastOrderNote
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "registered_customers collection not found or query error: ${e.message}")
            }

            // 2. Try 'customer_locations' collection
            try {
                val snapshot = firestore.collection("customer_locations").get().await()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    if (processedIds.contains(id)) continue
                    val data = doc.data ?: continue
                    val name = data["name"] as? String ?: data["customerName"] as? String ?: "ग्राहक"
                    val phone = data["phone"] as? String ?: data["phoneNumber"] as? String ?: data["mobile"] as? String ?: ""
                    val address = data["address"] as? String ?: data["locationAddress"] as? String ?: ""
                    val city = data["city"] as? String ?: "जबलपुर"
                    val lat = (data["latitude"] as? Number)?.toDouble() ?: (data["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (data["longitude"] as? Number)?.toDouble() ?: (data["lng"] as? Number)?.toDouble() ?: 0.0
                    val registeredAt = (data["registeredAt"] as? Number)?.toLong() ?: (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val accountType = data["accountType"] as? String ?: "नया खाता"
                    val lastOrderNote = data["lastOrderNote"] as? String ?: ""

                    processedIds.add(id)
                    list.add(
                        com.example.data.model.RegisteredCustomer(
                            id = id,
                            name = name,
                            phone = phone,
                            address = address,
                            city = city,
                            latitude = lat,
                            longitude = lng,
                            registeredAt = registeredAt,
                            accountType = accountType,
                            lastOrderNote = lastOrderNote
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "customer_locations query error: ${e.message}")
            }

            // 3. Try 'users' collection (Customer profiles who registered via app)
            try {
                val snapshot = firestore.collection("users").get().await()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    if (processedIds.contains(id)) continue
                    val data = doc.data ?: continue
                    val name = data["displayName"] as? String ?: data["name"] as? String ?: "ग्राहक"
                    val phone = data["phoneNumber"] as? String ?: data["phone"] as? String ?: ""
                    val address = data["address"] as? String ?: ""
                    val city = data["city"] as? String ?: "जबलपुर"
                    val lat = (data["latitude"] as? Number)?.toDouble() ?: (data["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (data["longitude"] as? Number)?.toDouble() ?: (data["lng"] as? Number)?.toDouble() ?: 0.0
                    val registeredAt = (data["createdAt"] as? Number)?.toLong() ?: (data["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val accountType = data["role"] as? String ?: "कस्टमर खाता"
                    val lastOrderNote = data["notes"] as? String ?: ""

                    // Add if has phone or location or non-empty customer name
                    if (phone.isNotBlank() || address.isNotBlank() || (lat != 0.0 && lng != 0.0)) {
                        processedIds.add(id)
                        list.add(
                            com.example.data.model.RegisteredCustomer(
                                id = id,
                                name = name,
                                phone = phone,
                                address = address,
                                city = city,
                                latitude = lat,
                                longitude = lng,
                                registeredAt = registeredAt,
                                accountType = accountType,
                                lastOrderNote = lastOrderNote
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "users collection query error: ${e.message}")
            }

            // 4. Also check if shree/registered_customers doc has array
            try {
                val doc = firestore.collection("shree").document("registered_customers").get().await()
                if (doc.exists()) {
                    val rawList = doc.get("customers") as? List<Map<String, Any>>
                    if (rawList != null) {
                        for (item in rawList) {
                            val id = item["id"] as? String ?: UUID.randomUUID().toString()
                            if (processedIds.contains(id)) continue
                            val name = item["name"] as? String ?: "ग्राहक"
                            val phone = item["phone"] as? String ?: ""
                            val address = item["address"] as? String ?: ""
                            val city = item["city"] as? String ?: "जबलपुर"
                            val lat = (item["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lng = (item["longitude"] as? Number)?.toDouble() ?: 0.0
                            val registeredAt = (item["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            val accountType = item["accountType"] as? String ?: "नया खाता"
                            val lastOrderNote = item["lastOrderNote"] as? String ?: ""

                            processedIds.add(id)
                            list.add(
                                com.example.data.model.RegisteredCustomer(
                                    id = id,
                                    name = name,
                                    phone = phone,
                                    address = address,
                                    city = city,
                                    latitude = lat,
                                    longitude = lng,
                                    registeredAt = registeredAt,
                                    accountType = accountType,
                                    lastOrderNote = lastOrderNote
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "shree/registered_customers query error: ${e.message}")
            }

            // Sort by registration time descending (newest first)
            list.sortByDescending { it.registeredAt }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves shop settings and live notice to Firestore across standard collections
     * (e.g. `shree/shop_settings`, `shop_settings/current`, `shree/bartan_data`, `shop/profile`)
     * so that any customer application or web view automatically reflects the banner, notice, and details.
     */
    suspend fun saveShopSettings(shopProfile: com.example.data.model.ShopProfile): Result<Unit> {
        return try {
            val firestore = getFirestore()
            val payload = hashMapOf<String, Any>(
                "name" to shopProfile.name,
                "hindiName" to shopProfile.hindiName,
                "phone" to shopProfile.phone,
                "altPhone" to shopProfile.altPhone,
                "address" to shopProfile.address,
                "tagline" to shopProfile.tagline,
                "gstNumber" to shopProfile.gstNumber,
                "shopTimings" to shopProfile.shopTimings,
                "notice" to shopProfile.notice,
                "bannerUrl" to shopProfile.bannerUrl,
                "banner_url" to shopProfile.bannerUrl,
                "shopBanner" to shopProfile.bannerUrl,
                "lastUpdated" to System.currentTimeMillis()
            )

            // 1. Primary path: shree/shop_settings
            try {
                firestore.collection("shree").document("shop_settings")
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed saving shree/shop_settings: ${e.message}")
            }

            // 2. Fallback path for customer apps: shop_settings/current
            try {
                firestore.collection("shop_settings").document("current")
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed saving shop_settings/current: ${e.message}")
            }

            // 3. Fallback path: shree/bartan_data (embed in shop_settings field)
            try {
                val bartanPayload = hashMapOf<String, Any>(
                    "shop_settings" to payload,
                    "shopSettings" to payload,
                    "shop_profile" to payload,
                    "bannerUrl" to shopProfile.bannerUrl,
                    "banner_url" to shopProfile.bannerUrl,
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("shree").document("bartan_data")
                    .set(bartanPayload, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed saving shree/bartan_data shop_settings: ${e.message}")
            }

            // 4. Fallback path: shop/profile
            try {
                firestore.collection("shop").document("profile")
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreRepository", "Failed saving shop/profile: ${e.message}")
            }

            Log.d("FirestoreRepository", "Successfully saved shop settings & banner across cloud Firestore paths")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error saving shop settings: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches shop settings and live notice from Firestore at `shree/shop_settings`.
     */
    suspend fun fetchShopSettings(): Result<com.example.data.model.ShopProfile?> {
        return try {
            val firestore = getFirestore()
            val doc = firestore.collection("shree").document("shop_settings").get().await()
            if (doc.exists()) {
                val profile = com.example.data.model.ShopProfile(
                    name = doc.getString("name") ?: "Shree Bartan Store",
                    hindiName = doc.getString("hindiName") ?: "श्री बर्तन स्टोर",
                    phone = doc.getString("phone") ?: "+91 98765 43210",
                    altPhone = doc.getString("altPhone") ?: "+91 98765 43211",
                    address = doc.getString("address") ?: "Main Market, Gola Road, Utensils Market",
                    tagline = doc.getString("tagline") ?: "Quality Utensils & Stainless Steel Kitchenware",
                    gstNumber = doc.getString("gstNumber") ?: "09ABCDE1234F1Z5",
                    shopTimings = doc.getString("shopTimings") ?: "सुबह 9:00 AM से रात 9:00 PM",
                    notice = doc.getString("notice") ?: "",
                    bannerUrl = doc.getString("bannerUrl") ?: ""
                )
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Error fetching shop settings: ${e.message}")
            Result.failure(e)
        }
    }
}
