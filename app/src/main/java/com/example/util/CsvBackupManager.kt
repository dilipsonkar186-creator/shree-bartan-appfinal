package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.AppDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RollingBackupInfo(
    val slotName: String,
    val title: String,
    val dateString: String,
    val timeString: String,
    val timestamp: Long,
    val file: File?,
    val exists: Boolean,
    val recordCount: Int,
    val fileSizeFormatted: String
)

data class RestoreSummary(
    val success: Boolean,
    val restoredFolders: Int = 0,
    val restoredCustomers: Int = 0,
    val restoredTransactions: Int = 0,
    val message: String = ""
)

data class SnapshotInfo(
    val id: String,
    val fileName: String,
    val title: String,
    val dateFormatted: String,
    val timeFormatted: String,
    val timestamp: Long,
    val file: File?,
    val folderCount: Int,
    val customerCount: Int,
    val transactionCount: Int,
    val totalRecords: Int,
    val fileSizeFormatted: String,
    val tag: String = "Manual",
    val isDriveSynced: Boolean = false
)

object CsvBackupManager {
    private const val TAG = "CsvBackupManager"
    private const val PREFS_NAME = "rolling_backup_prefs"
    private const val KEY_TODAY_DATE = "today_date"
    private const val KEY_TODAY_TIME = "today_time"
    private const val KEY_TODAY_COUNT = "today_count"
    private const val KEY_YESTERDAY_DATE = "yesterday_date"
    private const val KEY_YESTERDAY_TIME = "yesterday_time"
    private const val KEY_YESTERDAY_COUNT = "yesterday_count"
    private const val KEY_LAST_AUTO_BACKUP = "last_auto_backup_ts"

    private const val FILE_TODAY = "backup_day_today.csv"
    private const val FILE_YESTERDAY = "backup_day_yesterday.csv"

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val DISPLAY_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val DISPLAY_TIME_FORMAT = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun getBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, "rolling_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Performs a complete rolling backup (Folders, Customers, Transactions)
     * keeping strictly 2 days of rolling backups, overwritten every 24 hours.
     */
    suspend fun performRollingBackup(
        context: Context,
        folders: List<FolderEntity>,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val csvContent = buildFullAppCsv(folders, customers, transactions)
            val backupDir = getBackupDirectory(context)
            val todayFile = File(backupDir, FILE_TODAY)
            val yesterdayFile = File(backupDir, FILE_YESTERDAY)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentDateStr = DATE_FORMAT.format(Date())
            val savedTodayDate = prefs.getString(KEY_TODAY_DATE, "") ?: ""
            val totalRecords = folders.size + customers.size + transactions.size

            // If 24 hours / new calendar day has passed since previous today backup
            if (savedTodayDate.isNotBlank() && savedTodayDate != currentDateStr) {
                // Move todayFile to yesterdayFile (overwriting old 2nd day)
                if (todayFile.exists()) {
                    try {
                        todayFile.copyTo(yesterdayFile, overwrite = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rotating backup files", e)
                    }
                }
                val prevTime = prefs.getLong(KEY_TODAY_TIME, System.currentTimeMillis())
                val prevCount = prefs.getInt(KEY_TODAY_COUNT, 0)
                prefs.edit()
                    .putString(KEY_YESTERDAY_DATE, savedTodayDate)
                    .putLong(KEY_YESTERDAY_TIME, prevTime)
                    .putInt(KEY_YESTERDAY_COUNT, prevCount)
                    .apply()
            }

            // Write today's updated backup
            todayFile.writeText(csvContent, Charsets.UTF_8)

            // Also mirror to external files for user accessibility
            try {
                val extDir = context.getExternalFilesDir("backups")
                if (extDir != null && extDir.exists()) {
                    val extToday = File(extDir, FILE_TODAY)
                    todayFile.copyTo(extToday, overwrite = true)
                    if (yesterdayFile.exists()) {
                        val extYesterday = File(extDir, FILE_YESTERDAY)
                        yesterdayFile.copyTo(extYesterday, overwrite = true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "External backup copy warning: ${e.message}")
            }

            val now = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_TODAY_DATE, currentDateStr)
                .putLong(KEY_TODAY_TIME, now)
                .putInt(KEY_TODAY_COUNT, totalRecords)
                .putLong(KEY_LAST_AUTO_BACKUP, now)
                .apply()

            Log.i(TAG, "Rolling 2-Day CSV backup completed: $totalRecords records saved in $FILE_TODAY")
            Result.success(todayFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform rolling CSV backup", e)
            Result.failure(e)
        }
    }

    /**
     * Builds the unified full application CSV string containing Folders, Customers, and Transactions.
     */
    fun buildFullAppCsv(
        folders: List<FolderEntity>,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>
    ): String {
        val sb = StringBuilder()
        val generatedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.append("# SHREE BARTAN STORE FULL APPLICATION BACKUP\n")
        sb.append("# GENERATED_AT,$generatedTime\n")
        sb.append("# TOTAL_FOLDERS,${folders.size}\n")
        sb.append("# TOTAL_CUSTOMERS,${customers.size}\n")
        sb.append("# TOTAL_TRANSACTIONS,${transactions.size}\n\n")

        // 1. FOLDERS SECTION
        sb.append("# SECTION: FOLDERS\n")
        sb.append("id,name,description,areaTag,colorHex,isDeleted,deletedAt,createdAt\n")
        for (f in folders) {
            sb.append(escapeCsv(f.id.toString())).append(",")
            sb.append(escapeCsv(f.name)).append(",")
            sb.append(escapeCsv(f.description)).append(",")
            sb.append(escapeCsv(f.areaTag)).append(",")
            sb.append(escapeCsv(f.colorHex)).append(",")
            sb.append(escapeCsv(if (f.isDeleted) "1" else "0")).append(",")
            sb.append(escapeCsv(f.deletedAt?.toString() ?: "")).append(",")
            sb.append(escapeCsv(f.createdAt.toString())).append("\n")
        }
        sb.append("\n")

        // 2. CUSTOMERS SECTION
        sb.append("# SECTION: CUSTOMERS\n")
        sb.append("id,folderId,name,phone,bookNumber,pageNumber,email,address,notes,status,photoUri,documentType,documentNumber,documentPhotoUri,latitude,longitude,smsNotificationsEnabled,isDeleted,deletedAt,createdAt\n")
        for (c in customers) {
            sb.append(escapeCsv(c.id.toString())).append(",")
            sb.append(escapeCsv(c.folderId.toString())).append(",")
            sb.append(escapeCsv(c.name)).append(",")
            sb.append(escapeCsv(c.phone)).append(",")
            sb.append(escapeCsv(c.bookNumber)).append(",")
            sb.append(escapeCsv(c.pageNumber)).append(",")
            sb.append(escapeCsv(c.email)).append(",")
            sb.append(escapeCsv(c.address)).append(",")
            sb.append(escapeCsv(c.notes)).append(",")
            sb.append(escapeCsv(c.status)).append(",")
            sb.append(escapeCsv(c.photoUri ?: "")).append(",")
            sb.append(escapeCsv(c.documentType)).append(",")
            sb.append(escapeCsv(c.documentNumber)).append(",")
            sb.append(escapeCsv(c.documentPhotoUri ?: "")).append(",")
            sb.append(escapeCsv(c.latitude?.toString() ?: "")).append(",")
            sb.append(escapeCsv(c.longitude?.toString() ?: "")).append(",")
            sb.append(escapeCsv(if (c.smsNotificationsEnabled) "1" else "0")).append(",")
            sb.append(escapeCsv(if (c.isDeleted) "1" else "0")).append(",")
            sb.append(escapeCsv(c.deletedAt?.toString() ?: "")).append(",")
            sb.append(escapeCsv(c.createdAt.toString())).append("\n")
        }
        sb.append("\n")

        // 3. TRANSACTIONS SECTION
        sb.append("# SECTION: TRANSACTIONS\n")
        sb.append("id,customerId,folderId,type,itemDescription,quantity,unitType,quantityDouble,unitPrice,totalAmount,dateMillis,notes\n")
        for (t in transactions) {
            sb.append(escapeCsv(t.id.toString())).append(",")
            sb.append(escapeCsv(t.customerId.toString())).append(",")
            sb.append(escapeCsv(t.folderId.toString())).append(",")
            sb.append(escapeCsv(t.type)).append(",")
            sb.append(escapeCsv(t.itemDescription)).append(",")
            sb.append(escapeCsv(t.quantity.toString())).append(",")
            sb.append(escapeCsv(t.unitType)).append(",")
            sb.append(escapeCsv(t.quantityDouble.toString())).append(",")
            sb.append(escapeCsv(t.unitPrice.toString())).append(",")
            sb.append(escapeCsv(t.totalAmount.toString())).append(",")
            sb.append(escapeCsv(t.dateMillis.toString())).append(",")
            sb.append(escapeCsv(t.notes)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Builds and exports a single Customer / Patient Ledger CSV file with their name in the file name.
     */
    fun exportCustomerLedgerCsv(
        context: Context,
        customer: CustomerEntity,
        transactions: List<TransactionEntity>
    ) {
        try {
            val sb = StringBuilder()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
            val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateOnlyFormat.format(Date())

            val totalGoods = transactions.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val totalPayments = transactions.filter { it.type == "PAYMENT_DEPOSIT" }.sumOf { it.totalAmount }
            val totalReturns = transactions.filter { it.type == "GOODS_RETURNED" }.sumOf { it.totalAmount }
            val netDues = totalGoods - totalPayments - totalReturns

            sb.append("# CUSTOMER LEDGER STATEMENT / ग्राहक खाता विवरण\n")
            sb.append("Customer Name,${escapeCsv(customer.name)}\n")
            sb.append("Phone Number,${escapeCsv(customer.phone)}\n")
            sb.append("Page Number / Khata No.,${escapeCsv(customer.pageNumber)}\n")
            sb.append("Address,${escapeCsv(customer.address)}\n")
            sb.append("GPS Coordinates,${escapeCsv("${customer.latitude ?: ""}, ${customer.longitude ?: ""}")}\n")
            sb.append("Document,${escapeCsv("${customer.documentType} - ${customer.documentNumber}")}\n")
            sb.append("Customer Status,${escapeCsv(customer.status)}\n")
            sb.append("Customer Notes,${escapeCsv(customer.notes)}\n")
            sb.append("Total Goods Value,Rs. ${String.format(Locale.US, "%.2f", totalGoods)}\n")
            sb.append("Total Payment Received,Rs. ${String.format(Locale.US, "%.2f", totalPayments)}\n")
            sb.append("Total Goods Returned,Rs. ${String.format(Locale.US, "%.2f", totalReturns)}\n")
            sb.append("Net Balance Due,Rs. ${String.format(Locale.US, "%.2f", netDues)}\n")
            sb.append("Generated On,${escapeCsv(dateFormat.format(Date()))}\n\n")

            sb.append("# TRANSACTION DETAILS\n")
            sb.append("Transaction ID,Date & Time,Type,Item Description,Quantity,Unit,Unit Price,Total Amount,Notes\n")

            for (t in transactions) {
                val formattedDate = dateFormat.format(Date(t.dateMillis))
                val typeName = when (t.type) {
                    "GOODS_PROVIDED" -> "Goods Given (समान दिया)"
                    "PAYMENT_DEPOSIT" -> "Payment Received (जमा/भुगतान)"
                    "GOODS_RETURNED" -> "Goods Returned (वापस आया)"
                    else -> t.type
                }
                sb.append(escapeCsv(t.id.toString())).append(",")
                sb.append(escapeCsv(formattedDate)).append(",")
                sb.append(escapeCsv(typeName)).append(",")
                sb.append(escapeCsv(t.itemDescription)).append(",")
                sb.append(escapeCsv(t.quantityDouble.toString())).append(",")
                sb.append(escapeCsv(t.unitType)).append(",")
                sb.append(escapeCsv(t.unitPrice.toString())).append(",")
                sb.append(escapeCsv(t.totalAmount.toString())).append(",")
                sb.append(escapeCsv(t.notes)).append("\n")
            }

            val sanitizedName = customer.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "Customer_${sanitizedName}_Ledger_${currentDate}.csv"

            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val csvFile = File(exportDir, fileName)
            csvFile.writeText(sb.toString(), Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Customer Ledger Statement - ${customer.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Customer Ledger Statement for '${customer.name}' (Phone: ${customer.phone}, Balance Due: ₹${String.format(Locale.US, "%.2f", netDues)}).\nGenerated by Shree Bartan Store App."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Customer Ledger CSV")
            context.startActivity(chooser)
            Toast.makeText(context, "Ledger CSV exported for ${customer.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting customer ledger CSV", e)
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Reads and parses a CSV content string or stream, restoring Folders, Customers, and Transactions
     * into Room database in 1 click with resilient fallback for folder references.
     */
    suspend fun restoreDatabaseFromCsvContent(
        context: Context,
        csvContent: String,
        database: AppDatabase
    ): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val lines = csvContent.lines()
            var currentSection = ""
            var restoredFolders = 0
            var restoredCustomers = 0
            var restoredTransactions = 0

            val folderDao = database.folderDao()
            val customerDao = database.customerDao()
            val transactionDao = database.transactionDao()

            val existingFolders = folderDao.getAllFoldersDirect().associateBy { it.id }.toMutableMap()
            val parsedFolders = mutableListOf<FolderEntity>()
            val parsedCustomers = mutableListOf<CustomerEntity>()
            val parsedTransactions = mutableListOf<TransactionEntity>()
            val customerFolderMap = mutableMapOf<Long, Long>()
            var customerHeaderHasBookNumber = false
            var customerHeaderHasPageNumber = false

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("# SECTION:")) {
                    currentSection = line.substringAfter("# SECTION:").trim().uppercase(Locale.US)
                    continue
                }

                if (line.startsWith("#")) continue

                // Check for section headers
                if (line.startsWith("id,folderId,name")) {
                    customerHeaderHasBookNumber = line.contains("bookNumber")
                    customerHeaderHasPageNumber = line.contains("pageNumber")
                    continue
                }
                if (line.startsWith("id,name,description") || 
                    line.startsWith("id,customerId,folderId") ||
                    line.startsWith("Customer ID,Name,Phone") ||
                    line.startsWith("Transaction ID,Date")) {
                    continue
                }

                val tokens = parseCsvLine(rawLine)
                if (tokens.isEmpty()) continue

                when (currentSection) {
                    "FOLDERS" -> {
                        if (tokens.size >= 2) {
                            val id = tokens.getOrNull(0)?.toLongOrNull() ?: 0L
                            val name = tokens.getOrNull(1) ?: ""
                            val desc = tokens.getOrNull(2) ?: ""
                            val areaTag = tokens.getOrNull(3) ?: "General"
                            val colorHex = tokens.getOrNull(4) ?: "#4F46E5"
                            val isDeleted = (tokens.getOrNull(5) == "1" || tokens.getOrNull(5).equals("true", ignoreCase = true))
                            val deletedAt = tokens.getOrNull(6)?.toLongOrNull()?.takeIf { it > 0L }
                            val createdAt = tokens.getOrNull(7)?.toLongOrNull() ?: System.currentTimeMillis()

                            if (name.isNotBlank()) {
                                parsedFolders.add(
                                    FolderEntity(
                                        id = id,
                                        name = name,
                                        description = desc,
                                        areaTag = areaTag,
                                        colorHex = colorHex,
                                        isDeleted = isDeleted,
                                        deletedAt = deletedAt,
                                        createdAt = createdAt
                                    )
                                )
                            }
                        }
                    }

                    "CUSTOMERS" -> {
                        if (tokens.size >= 3) {
                            val id = tokens.getOrNull(0)?.toLongOrNull() ?: 0L
                            val rawFolderId = tokens.getOrNull(1)?.toLongOrNull() ?: 1L
                            val folderId = if (rawFolderId <= 0L) 1L else rawFolderId
                            val name = tokens.getOrNull(2) ?: ""
                            val phone = tokens.getOrNull(3) ?: ""
                            val bookNumber: String
                            val pageNumber: String
                            val email: String
                            val address: String
                            val notes: String
                            val status: String
                            val photoUri: String?
                            val docType: String
                            val docNum: String
                            val docPhoto: String?
                            val lat: Double?
                            val lng: Double?
                            val sms: Boolean
                            val isDeleted: Boolean
                            val deletedAt: Long?
                            val createdAt: Long

                            if (customerHeaderHasBookNumber || tokens.size >= 20) {
                                bookNumber = tokens.getOrNull(4) ?: ""
                                pageNumber = tokens.getOrNull(5) ?: ""
                                email = tokens.getOrNull(6) ?: ""
                                address = tokens.getOrNull(7) ?: ""
                                notes = tokens.getOrNull(8) ?: ""
                                status = tokens.getOrNull(9) ?: "Active"
                                photoUri = tokens.getOrNull(10)?.takeIf { it.isNotBlank() }
                                docType = tokens.getOrNull(11) ?: ""
                                docNum = tokens.getOrNull(12) ?: ""
                                docPhoto = tokens.getOrNull(13)?.takeIf { it.isNotBlank() }
                                lat = tokens.getOrNull(14)?.toDoubleOrNull()
                                lng = tokens.getOrNull(15)?.toDoubleOrNull()
                                sms = tokens.getOrNull(16) != "0"
                                isDeleted = (tokens.getOrNull(17) == "1" || tokens.getOrNull(17).equals("true", ignoreCase = true))
                                deletedAt = tokens.getOrNull(18)?.toLongOrNull()?.takeIf { it > 0L }
                                createdAt = tokens.getOrNull(19)?.toLongOrNull() ?: System.currentTimeMillis()
                            } else if (customerHeaderHasPageNumber || tokens.size >= 19) {
                                bookNumber = ""
                                pageNumber = tokens.getOrNull(4) ?: ""
                                email = tokens.getOrNull(5) ?: ""
                                address = tokens.getOrNull(6) ?: ""
                                notes = tokens.getOrNull(7) ?: ""
                                status = tokens.getOrNull(8) ?: "Active"
                                photoUri = tokens.getOrNull(9)?.takeIf { it.isNotBlank() }
                                docType = tokens.getOrNull(10) ?: ""
                                docNum = tokens.getOrNull(11) ?: ""
                                docPhoto = tokens.getOrNull(12)?.takeIf { it.isNotBlank() }
                                lat = tokens.getOrNull(13)?.toDoubleOrNull()
                                lng = tokens.getOrNull(14)?.toDoubleOrNull()
                                sms = tokens.getOrNull(15) != "0"
                                isDeleted = (tokens.getOrNull(16) == "1" || tokens.getOrNull(16).equals("true", ignoreCase = true))
                                deletedAt = tokens.getOrNull(17)?.toLongOrNull()?.takeIf { it > 0L }
                                createdAt = tokens.getOrNull(18)?.toLongOrNull() ?: System.currentTimeMillis()
                            } else {
                                bookNumber = ""
                                pageNumber = ""
                                email = tokens.getOrNull(4) ?: ""
                                address = tokens.getOrNull(5) ?: ""
                                notes = tokens.getOrNull(6) ?: ""
                                status = tokens.getOrNull(7) ?: "Active"
                                photoUri = tokens.getOrNull(8)?.takeIf { it.isNotBlank() }
                                docType = tokens.getOrNull(9) ?: ""
                                docNum = tokens.getOrNull(10) ?: ""
                                docPhoto = tokens.getOrNull(11)?.takeIf { it.isNotBlank() }
                                lat = tokens.getOrNull(12)?.toDoubleOrNull()
                                lng = tokens.getOrNull(13)?.toDoubleOrNull()
                                sms = tokens.getOrNull(14) != "0"
                                isDeleted = (tokens.getOrNull(15) == "1" || tokens.getOrNull(15).equals("true", ignoreCase = true))
                                deletedAt = tokens.getOrNull(16)?.toLongOrNull()?.takeIf { it > 0L }
                                createdAt = tokens.getOrNull(17)?.toLongOrNull() ?: System.currentTimeMillis()
                            }

                            if (name.isNotBlank()) {
                                val customer = CustomerEntity(
                                    id = id,
                                    folderId = folderId,
                                    name = name,
                                    phone = phone,
                                    bookNumber = bookNumber,
                                    pageNumber = pageNumber,
                                    email = email,
                                    address = address,
                                    notes = notes,
                                    status = status,
                                    photoUri = photoUri,
                                    documentType = docType,
                                    documentNumber = docNum,
                                    documentPhotoUri = docPhoto,
                                    latitude = lat,
                                    longitude = lng,
                                    smsNotificationsEnabled = sms,
                                    isDeleted = isDeleted,
                                    deletedAt = deletedAt,
                                    createdAt = createdAt
                                )
                                parsedCustomers.add(customer)
                                if (id > 0L) customerFolderMap[id] = folderId
                            }
                        }
                    }

                    "TRANSACTIONS" -> {
                        if (tokens.size >= 5) {
                            val id = tokens.getOrNull(0)?.toLongOrNull() ?: 0L
                            val custId = tokens.getOrNull(1)?.toLongOrNull() ?: 0L
                            var fldId = tokens.getOrNull(2)?.toLongOrNull() ?: 0L
                            if (fldId <= 0L && custId > 0L) {
                                fldId = customerFolderMap[custId] ?: 1L
                            }
                            if (fldId <= 0L) fldId = 1L

                            val type = tokens.getOrNull(3) ?: "GOODS_PROVIDED"
                            val desc = tokens.getOrNull(4) ?: ""
                            val qty = tokens.getOrNull(5)?.toIntOrNull() ?: 1
                            val unitType = tokens.getOrNull(6) ?: "pcs"
                            val qtyDouble = tokens.getOrNull(7)?.toDoubleOrNull() ?: qty.toDouble()
                            val price = tokens.getOrNull(8)?.toDoubleOrNull() ?: 0.0
                            val total = tokens.getOrNull(9)?.toDoubleOrNull() ?: 0.0
                            val dateMillis = tokens.getOrNull(10)?.toLongOrNull() ?: System.currentTimeMillis()
                            val notes = tokens.getOrNull(11) ?: ""

                            if (custId > 0L) {
                                parsedTransactions.add(
                                    TransactionEntity(
                                        id = id,
                                        customerId = custId,
                                        folderId = fldId,
                                        type = type,
                                        itemDescription = desc,
                                        quantity = qty,
                                        unitType = unitType,
                                        quantityDouble = qtyDouble,
                                        unitPrice = price,
                                        totalAmount = total,
                                        dateMillis = dateMillis,
                                        notes = notes
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        // Fallback for legacy format or unsectioned customer CSV
                        if (tokens.size >= 5 && tokens[0].toLongOrNull() != null) {
                            val id = tokens[0].toLongOrNull() ?: 0L
                            val name = tokens[1]
                            val phone = tokens[2]
                            val email = tokens[3]
                            val address = tokens[4]
                            if (name.isNotBlank()) {
                                parsedCustomers.add(
                                    CustomerEntity(
                                        id = id,
                                        folderId = 1L,
                                        name = name,
                                        phone = phone,
                                        email = email,
                                        address = address,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                if (id > 0L) customerFolderMap[id] = 1L
                            }
                        }
                    }
                }
            }

            // Step 1: Insert all parsed folders
            for (folder in parsedFolders) {
                try {
                    folderDao.insertFolder(folder)
                    existingFolders[folder.id] = folder
                    restoredFolders++
                } catch (e: Exception) {
                    Log.w(TAG, "Notice inserting folder ${folder.name}: ${e.message}")
                }
            }

            // Step 2: Ensure any folder referenced by customers or transactions exists in DB
            val referencedFolderIds = (parsedCustomers.map { it.folderId } + parsedTransactions.map { it.folderId }).toSet()
            for (fId in referencedFolderIds) {
                if (fId > 0L && !existingFolders.containsKey(fId)) {
                    val defaultFolder = FolderEntity(
                        id = fId,
                        name = if (fId == 1L) "General" else "Folder $fId",
                        description = "Auto-created during restore",
                        areaTag = "General",
                        colorHex = "#4F46E5"
                    )
                    try {
                        folderDao.insertFolder(defaultFolder)
                        existingFolders[fId] = defaultFolder
                        restoredFolders++
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice ensuring folder $fId: ${e.message}")
                    }
                }
            }

            // If still no folder exists, create default folder with id = 1
            if (existingFolders.isEmpty()) {
                val fallbackFolder = FolderEntity(
                    id = 1L,
                    name = "General",
                    description = "Default folder",
                    areaTag = "General",
                    colorHex = "#4F46E5"
                )
                try {
                    folderDao.insertFolder(fallbackFolder)
                    existingFolders[1L] = fallbackFolder
                    restoredFolders++
                } catch (e: Exception) {
                    Log.w(TAG, "Notice creating fallback folder: ${e.message}")
                }
            }

            // Step 3: Insert all customers
            for (customer in parsedCustomers) {
                try {
                    val targetFolderId = if (customer.folderId > 0L && existingFolders.containsKey(customer.folderId)) {
                        customer.folderId
                    } else {
                        existingFolders.keys.firstOrNull() ?: 1L
                    }
                    val finalCustomer = if (targetFolderId != customer.folderId) {
                        customer.copy(folderId = targetFolderId)
                    } else {
                        customer
                    }
                    customerDao.insertCustomer(finalCustomer)
                    restoredCustomers++
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring customer ${customer.name}", e)
                }
            }

            // Step 4: Insert all transactions
            for (tx in parsedTransactions) {
                try {
                    val targetFolderId = if (tx.folderId > 0L && existingFolders.containsKey(tx.folderId)) {
                        tx.folderId
                    } else {
                        customerFolderMap[tx.customerId] ?: existingFolders.keys.firstOrNull() ?: 1L
                    }
                    val finalTx = if (targetFolderId != tx.folderId) {
                        tx.copy(folderId = targetFolderId)
                    } else {
                        tx
                    }
                    transactionDao.insertTransaction(finalTx)
                    restoredTransactions++
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring transaction ${tx.itemDescription}", e)
                }
            }

            RestoreSummary(
                success = true,
                restoredFolders = restoredFolders,
                restoredCustomers = restoredCustomers,
                restoredTransactions = restoredTransactions,
                message = "Restored successfully: $restoredFolders folder(s), $restoredCustomers customer(s), $restoredTransactions transaction(s)."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring CSV content", e)
            RestoreSummary(
                success = false,
                message = "CSV Restore failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Restores data directly from an Android content URI (from file picker).
     */
    suspend fun restoreDatabaseFromUri(
        context: Context,
        uri: Uri,
        database: AppDatabase
    ): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext RestoreSummary(false, message = "Unable to open selected file")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val content = reader.readText()
            reader.close()
            inputStream.close()
            restoreDatabaseFromCsvContent(context, content, database)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV file from URI: $uri", e)
            RestoreSummary(false, message = "Could not read file: ${e.localizedMessage}")
        }
    }

    /**
     * Restores data from one of the rolling backup slots (Today or Yesterday).
     */
    suspend fun restoreFromRollingSlot(
        context: Context,
        isTodaySlot: Boolean,
        database: AppDatabase
    ): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val fileName = if (isTodaySlot) FILE_TODAY else FILE_YESTERDAY
            val backupDir = getBackupDirectory(context)
            val file = File(backupDir, fileName)

            if (!file.exists() || file.length() == 0L) {
                // Try external backup fallback
                val extDir = context.getExternalFilesDir("backups")
                val extFile = if (extDir != null) File(extDir, fileName) else null
                if (extFile != null && extFile.exists() && extFile.length() > 0L) {
                    val content = extFile.readText(Charsets.UTF_8)
                    return@withContext restoreDatabaseFromCsvContent(context, content, database)
                }
                return@withContext RestoreSummary(false, message = "Backup file not found for ${if (isTodaySlot) "Today" else "Yesterday"}")
            }

            val content = file.readText(Charsets.UTF_8)
            restoreDatabaseFromCsvContent(context, content, database)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from rolling slot", e)
            RestoreSummary(false, message = "Failed to restore slot: ${e.localizedMessage}")
        }
    }

    /**
     * Gets information about both rolling backup slots (Today and Yesterday).
     */
    fun getRollingBackupInfo(context: Context): Pair<RollingBackupInfo, RollingBackupInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backupDir = getBackupDirectory(context)

        val todayFile = File(backupDir, FILE_TODAY)
        val yesterdayFile = File(backupDir, FILE_YESTERDAY)

        val todayTs = prefs.getLong(KEY_TODAY_TIME, if (todayFile.exists()) todayFile.lastModified() else 0L)
        val todayCount = prefs.getInt(KEY_TODAY_COUNT, 0)
        val todayDateStr = if (todayTs > 0L) DISPLAY_DATE_FORMAT.format(Date(todayTs)) else "No Backup Yet"
        val todayTimeStr = if (todayTs > 0L) DISPLAY_TIME_FORMAT.format(Date(todayTs)) else "--:--"

        val yestTs = prefs.getLong(KEY_YESTERDAY_TIME, if (yesterdayFile.exists()) yesterdayFile.lastModified() else 0L)
        val yestCount = prefs.getInt(KEY_YESTERDAY_COUNT, 0)
        val yestDateStr = if (yestTs > 0L) DISPLAY_DATE_FORMAT.format(Date(yestTs)) else "No Backup Yet"
        val yestTimeStr = if (yestTs > 0L) DISPLAY_TIME_FORMAT.format(Date(yestTs)) else "--:--"

        val todayInfo = RollingBackupInfo(
            slotName = "Today (आज का बैकअप)",
            title = "Slot 1: Today's Auto-Backup",
            dateString = todayDateStr,
            timeString = todayTimeStr,
            timestamp = todayTs,
            file = if (todayFile.exists()) todayFile else null,
            exists = todayFile.exists() && todayFile.length() > 0L,
            recordCount = todayCount,
            fileSizeFormatted = if (todayFile.exists()) formatFileSize(todayFile.length()) else "0 KB"
        )

        val yesterdayInfo = RollingBackupInfo(
            slotName = "Yesterday (कल का बैकअप)",
            title = "Slot 2: Previous Day Auto-Backup",
            dateString = yestDateStr,
            timeString = yestTimeStr,
            timestamp = yestTs,
            file = if (yesterdayFile.exists()) yesterdayFile else null,
            exists = yesterdayFile.exists() && yesterdayFile.length() > 0L,
            recordCount = yestCount,
            fileSizeFormatted = if (yesterdayFile.exists()) formatFileSize(yesterdayFile.length()) else "0 KB"
        )

        return Pair(todayInfo, yesterdayInfo)
    }

    fun getSnapshotsDirectory(context: Context): File {
        val dir = File(context.filesDir, "point_in_time_snapshots")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Creates a new Point-in-Time immutable snapshot CSV file.
     * Keeps up to 30 snapshots, automatically pruning older ones.
     */
    suspend fun createSnapshot(
        context: Context,
        folders: List<FolderEntity>,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        tag: String = "Manual"
    ): Result<SnapshotInfo> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "Snapshot_${fileTimestampFormat.format(Date(now))}_${tag}.csv"

            val snapshotDir = getSnapshotsDirectory(context)
            val snapshotFile = File(snapshotDir, fileName)

            val csvContent = buildFullAppCsv(folders, customers, transactions)
            snapshotFile.writeText(csvContent, Charsets.UTF_8)

            // Mirror snapshot to external files dir if available for safe backup
            try {
                val extDocDir = context.getExternalFilesDir("snapshots")
                if (extDocDir != null && extDocDir.exists()) {
                    val extFile = File(extDocDir, fileName)
                    snapshotFile.copyTo(extFile, overwrite = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "External snapshot copy warning: ${e.message}")
            }

            // Prune old snapshots if more than 30 exist
            pruneOldSnapshots(snapshotDir, maxKeep = 30)

            val totalRecords = folders.size + customers.size + transactions.size
            val info = SnapshotInfo(
                id = snapshotFile.name,
                fileName = snapshotFile.name,
                title = "Snapshot ($tag)",
                dateFormatted = DISPLAY_DATE_FORMAT.format(Date(now)),
                timeFormatted = DISPLAY_TIME_FORMAT.format(Date(now)),
                timestamp = now,
                file = snapshotFile,
                folderCount = folders.size,
                customerCount = customers.size,
                transactionCount = transactions.size,
                totalRecords = totalRecords,
                fileSizeFormatted = formatFileSize(snapshotFile.length()),
                tag = tag,
                isDriveSynced = false
            )

            Log.i(TAG, "Created Point-in-Time snapshot: ${snapshotFile.name} ($totalRecords records)")
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create snapshot", e)
            Result.failure(e)
        }
    }

    /**
     * Lists all local point-in-time snapshots in chronological order (newest first).
     */
    fun listSnapshots(context: Context): List<SnapshotInfo> {
        val dir = getSnapshotsDirectory(context)
        val files = dir.listFiles { file -> file.isFile && file.name.startsWith("Snapshot_") && file.name.endsWith(".csv") }
            ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            parseSnapshotMetadata(file)
        }
    }

    private fun parseSnapshotMetadata(file: File): SnapshotInfo {
        var folderCount = 0
        var custCount = 0
        var txCount = 0
        val timestamp = file.lastModified()
        var tag = "Snapshot"

        // Extract tag from filename if present: Snapshot_yyyyMMdd_HHmmss_Tag.csv
        val nameParts = file.nameWithoutExtension.split("_")
        if (nameParts.size >= 4) {
            tag = nameParts.subList(3, nameParts.size).joinToString("_")
        }

        try {
            file.useLines { lines ->
                for (line in lines.take(30)) {
                    if (line.startsWith("# TOTAL_FOLDERS,")) {
                        folderCount = line.substringAfter("# TOTAL_FOLDERS,").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("# TOTAL_CUSTOMERS,")) {
                        custCount = line.substringAfter("# TOTAL_CUSTOMERS,").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("# TOTAL_TRANSACTIONS,")) {
                        txCount = line.substringAfter("# TOTAL_TRANSACTIONS,").trim().toIntOrNull() ?: 0
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val totalRecords = folderCount + custCount + txCount
        return SnapshotInfo(
            id = file.name,
            fileName = file.name,
            title = "Snapshot ($tag)",
            dateFormatted = DISPLAY_DATE_FORMAT.format(Date(timestamp)),
            timeFormatted = DISPLAY_TIME_FORMAT.format(Date(timestamp)),
            timestamp = timestamp,
            file = file,
            folderCount = folderCount,
            customerCount = custCount,
            transactionCount = txCount,
            totalRecords = totalRecords,
            fileSizeFormatted = formatFileSize(file.length()),
            tag = tag
        )
    }

    private fun pruneOldSnapshots(dir: File, maxKeep: Int) {
        try {
            val files = dir.listFiles { file -> file.isFile && file.name.startsWith("Snapshot_") && file.name.endsWith(".csv") }
                ?: return
            if (files.size > maxKeep) {
                val sorted = files.sortedBy { it.lastModified() }
                val toDelete = sorted.take(files.size - maxKeep)
                toDelete.forEach { it.delete() }
                Log.d(TAG, "Pruned ${toDelete.size} old snapshots")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pruning old snapshots", e)
        }
    }

    fun deleteSnapshot(context: Context, fileName: String): Boolean {
        return try {
            val dir = getSnapshotsDirectory(context)
            val file = File(dir, fileName)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shares a backup file via Android Share sheet.
     */
    fun shareBackupFile(context: Context, file: File, title: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "App CSV Backup - $title")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Shree Bartan Store App complete CSV backup ($title).\nKeep this file safe to restore your customer details and transactions in 1 click."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share / Save Backup CSV")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing backup file", e)
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun escapeCsv(field: String?): String {
        if (field.isNullOrEmpty()) return ""
        var escaped = field.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}
