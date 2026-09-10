package com.example.util

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.TransactionEntity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val modifiedTimeMillis: Long,
    val sizeBytes: Long,
    val webViewLink: String? = null,
    val recordCount: Int = 0
)

data class DriveSyncState(
    val isAutoSyncEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val isPending: Boolean = false,
    val pendingOperationsCount: Int = 0,
    val lastSyncTimeMillis: Long = 0L,
    val lastSyncFormatted: String = "",
    val lastSyncRelative: String = "",
    val lastSyncFileName: String = "",
    val lastError: String? = null,
    val connectedAccountEmail: String? = null,
    val isConnected: Boolean = false,
    val totalPhotosBackedUp: Int = 0,
    val isPhotoSyncing: Boolean = false
)

object GoogleDriveManager {
    private const val TAG = "GoogleDriveManager"
    private const val PREFS_NAME = "google_drive_backup_prefs"
    private const val KEY_LAST_SYNC_TS = "drive_last_sync_ts"
    private const val KEY_LAST_SYNC_FILE_NAME = "drive_last_sync_file_name"
    private const val KEY_LAST_SYNC_FILE_ID = "drive_last_sync_file_id"
    private const val KEY_AUTO_SYNC_ENABLED = "drive_auto_sync_enabled"
    private const val KEY_SAVED_ACCOUNT_EMAIL = "drive_saved_account_email"
    private const val KEY_PHOTOS_BACKED_UP_COUNT = "drive_photos_backed_up_count"

    const val DEFAULT_BACKUP_FILE_NAME = "Shree_Bartan_Store_Full_Backup.csv"
    const val ROOT_FOLDER_NAME = "श्री बर्तन भंडार - खाता बही (ShreeBartan Ledger)"
    const val SUBFOLDER_CUSTOMER_PHOTOS = "ग्राहक फ़ोटो (Customer Photos)"
    const val SUBFOLDER_DOC_PHOTOS = "दस्तावेज़ फ़ोटो (KYC Documents)"
    const val SUBFOLDER_BACKUPS = "दैनिक एक्सेल बैकअप (Daily Excel Backups)"

    // Legacy folder fallback
    const val DRIVE_PHOTOS_FOLDER_NAME = "Shree_Bartan_Store_Photos"
    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    private const val KEY_ROOT_FOLDER_ID = "drive_root_folder_id"
    private const val KEY_CUSTOMER_PHOTOS_FOLDER_ID = "drive_cust_photos_folder_id"
    private const val KEY_DOC_PHOTOS_FOLDER_ID = "drive_doc_photos_folder_id"
    private const val KEY_BACKUPS_FOLDER_ID = "drive_backups_folder_id"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Gets the connected Google email address from Firebase, GoogleSignIn, or saved preference.
     */
    fun getConnectedAccountEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SAVED_ACCOUNT_EMAIL, null)
        if (!saved.isNullOrBlank()) return saved

        val firebaseEmail = try { FirebaseAuth.getInstance().currentUser?.email } catch (e: Exception) { null }
        if (!firebaseEmail.isNullOrBlank()) return firebaseEmail

        val gAccount = try { GoogleSignIn.getLastSignedInAccount(context)?.email } catch (e: Exception) { null }
        return gAccount
    }

    fun saveConnectedAccountEmail(context: Context, email: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_ACCOUNT_EMAIL, email).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun getSyncState(context: Context): DriveSyncState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTs = prefs.getLong(KEY_LAST_SYNC_TS, 0L)
        val lastFile = prefs.getString(KEY_LAST_SYNC_FILE_NAME, "") ?: ""
        val email = getConnectedAccountEmail(context)
        val enabled = isAutoSyncEnabled(context)
        val photoCount = prefs.getInt(KEY_PHOTOS_BACKED_UP_COUNT, 0)

        val formattedDate = if (lastTs > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(lastTs))
        } else {
            "Never / कभी नहीं"
        }

        val relativeTime = if (lastTs > 0L) {
            val diffMs = System.currentTimeMillis() - lastTs
            val diffMins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            when {
                diffMins < 1 -> "Just now"
                diffMins < 60 -> "$diffMins min${if (diffMins > 1) "s" else ""} ago"
                diffHours < 24 -> "$diffHours hr${if (diffHours > 1) "s" else ""} ago"
                diffDays == 1L -> "Yesterday"
                else -> "$diffDays days ago"
            }
        } else {
            "Not synced yet"
        }

        return DriveSyncState(
            isAutoSyncEnabled = enabled,
            isSyncing = false,
            isPending = false,
            pendingOperationsCount = 0,
            lastSyncTimeMillis = lastTs,
            lastSyncFormatted = formattedDate,
            lastSyncRelative = relativeTime,
            lastSyncFileName = lastFile,
            lastError = null,
            connectedAccountEmail = email,
            isConnected = !email.isNullOrBlank(),
            totalPhotosBackedUp = photoCount
        )
    }

    fun markSyncSuccess(context: Context, totalRecords: Int = 0) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TS, System.currentTimeMillis())
            .apply()
    }

    val DRIVE_SCOPES = arrayOf(
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope("https://www.googleapis.com/auth/drive.appdata")
    )

    fun getGoogleDriveSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_SCOPES[0], DRIVE_SCOPES[1])
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    var pendingAuthIntent: Intent? = null

    fun launchAuthorizationConsent(context: Context) {
        val intent = pendingAuthIntent
        if (intent != null) {
            try {
                val i = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(i)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch pending auth intent: ${e.message}")
            }
        }
    }

    /**
     * Obtains an OAuth2 bearer token for Google Drive API.
     */
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val email = getConnectedAccountEmail(context)
            if (email.isNullOrBlank()) {
                Log.w(TAG, "No Google account email found for Drive token.")
                return@withContext null
            }
            val account = Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, DRIVE_SCOPE)
            Log.d(TAG, "Obtained Google Drive OAuth token for $email")
            token
        } catch (e: UserRecoverableAuthException) {
            Log.w(TAG, "UserRecoverableAuthException: Needs Google Drive OAuth consent: ${e.message}")
            pendingAuthIntent = e.intent
            withContext(Dispatchers.Main) {
                try {
                    val intent = e.intent?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent != null) {
                        context.startActivity(intent)
                    }
                } catch (actEx: Exception) {
                    Log.e(TAG, "Failed to launch intent directly: ${actEx.message}")
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire GoogleAuth token directly: ${e.message}")
            null
        }
    }

    /**
     * Uploads or updates the unified CSV file to Google Drive.
     * If the file already exists on Drive, it will update the content (overwrite).
     * If not, it creates a new file.
     */
    suspend fun uploadBackupCsvToDrive(
        context: Context,
        csvContent: String,
        fileName: String = DEFAULT_BACKUP_FILE_NAME
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            val now = System.currentTimeMillis()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            if (token == null) {
                val backupDir = CsvBackupManager.getBackupDirectory(context)
                val driveLocalMirror = File(backupDir, "drive_synced_$fileName")
                driveLocalMirror.writeText(csvContent, Charsets.UTF_8)

                val pendingIntent = pendingAuthIntent
                if (pendingIntent != null) {
                    return@withContext Result.failure(
                        Exception("Google Drive Authorization आवश्यक है: कृपया 'Connect Google Drive' बटन दबाकर अनुमति (Allow) दें।")
                    )
                }

                return@withContext Result.failure(
                    Exception("Google Drive खाते का एक्सेस नहीं मिला। कृपया 'Connect Google Drive' पर क्लिक करके अपना खाता चुनें।")
                )
            }

            // 1. Search if the file already exists in Google Drive
            val existingFileId = findFileIdByName(token, fileName)
            val backupsFolderId = getOrCreateBackupsFolder(context, token) ?: getOrCreateRootFolder(context, token)

            val uploadedFile = if (existingFileId != null) {
                // Update existing file
                updateDriveFileContent(token, existingFileId, csvContent, fileName)
            } else {
                // Create new file via multipart upload
                createDriveFile(token, fileName, csvContent, parentFolderId = backupsFolderId)
            }

            prefs.edit()
                .putLong(KEY_LAST_SYNC_TS, now)
                .putString(KEY_LAST_SYNC_FILE_NAME, fileName)
                .putString(KEY_LAST_SYNC_FILE_ID, uploadedFile.id)
                .apply()

            Log.i(TAG, "Successfully uploaded CSV backup to Google Drive: ${uploadedFile.name} (ID: ${uploadedFile.id})")
            Result.success(uploadedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload CSV to Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads an immutable snapshot point-in-time file to Google Drive.
     */
    suspend fun uploadSnapshotToDrive(
        context: Context,
        fileName: String,
        csvContent: String
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            val now = System.currentTimeMillis()

            if (token == null) {
                val backupDir = CsvBackupManager.getSnapshotsDirectory(context)
                val snapshotFile = File(backupDir, fileName)
                if (!snapshotFile.exists()) {
                    snapshotFile.writeText(csvContent, Charsets.UTF_8)
                }
                val lineCount = csvContent.lines().count { it.isNotBlank() && !it.startsWith("#") }
                return@withContext Result.success(
                    DriveBackupFile(
                        id = "local_snapshot_${now}",
                        name = fileName,
                        modifiedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now)),
                        modifiedTimeMillis = now,
                        sizeBytes = snapshotFile.length(),
                        recordCount = lineCount
                    )
                )
            }

            // Always create a new distinct snapshot file on Drive (immutable point in time)
            val backupsFolderId = getOrCreateBackupsFolder(context, token) ?: getOrCreateRootFolder(context, token)
            val uploadedFile = createDriveFile(token, fileName, csvContent, parentFolderId = backupsFolderId)
            Log.i(TAG, "Successfully uploaded Snapshot to Google Drive: ${uploadedFile.name} (ID: ${uploadedFile.id})")
            Result.success(uploadedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload snapshot to Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Lists all CSV and Backup files available on Google Drive.
     */
    suspend fun listBackupFilesFromDrive(context: Context): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            if (token == null) {
                // Return local rolling backups as fallback
                val (today, yesterday) = CsvBackupManager.getRollingBackupInfo(context)
                val list = mutableListOf<DriveBackupFile>()
                if (today.exists && today.file != null) {
                    list.add(
                        DriveBackupFile(
                            id = "local_slot_today",
                            name = today.title + " (Local/Drive Mirror)",
                            modifiedTime = "${today.dateString} ${today.timeString}",
                            modifiedTimeMillis = today.timestamp,
                            sizeBytes = today.file.length(),
                            recordCount = today.recordCount
                        )
                    )
                }
                if (yesterday.exists && yesterday.file != null) {
                    list.add(
                        DriveBackupFile(
                            id = "local_slot_yesterday",
                            name = yesterday.title + " (Local/Drive Mirror)",
                            modifiedTime = "${yesterday.dateString} ${yesterday.timeString}",
                            modifiedTimeMillis = yesterday.timestamp,
                            sizeBytes = yesterday.file.length(),
                            recordCount = yesterday.recordCount
                        )
                    )
                }
                return@withContext Result.success(list)
            }

            val query = "trashed = false and (mimeType = 'text/csv' or mimeType = 'application/vnd.ms-excel' or name contains '.csv' or name contains 'Backup')"
            val url = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name,mimeType,modifiedTime,size,webViewLink)&orderBy=modifiedTime desc&pageSize=30"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Drive API error: ${response.code} ${response.message}"))
            }

            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)
            val filesArray = json.optJSONArray("files") ?: JSONArray()

            val results = mutableListOf<DriveBackupFile>()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            for (i in 0 until filesArray.length()) {
                val item = filesArray.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name")
                val modTimeStr = item.optString("modifiedTime")
                val size = item.optLong("size", 0L)
                val webLink = item.optString("webViewLink", null)

                var modMillis = System.currentTimeMillis()
                var formattedTime = modTimeStr
                try {
                    val d = isoFormat.parse(modTimeStr)
                    if (d != null) {
                        modMillis = d.time
                        formattedTime = displayFormat.format(d)
                    }
                } catch (pe: Exception) {
                    // ignore
                }

                results.add(
                    DriveBackupFile(
                        id = id,
                        name = name,
                        modifiedTime = formattedTime,
                        modifiedTimeMillis = modMillis,
                        sizeBytes = size,
                        webViewLink = webLink
                    )
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files from Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the CSV content string from Google Drive given a fileId.
     */
    suspend fun downloadCsvContentFromDrive(context: Context, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (fileId.startsWith("local_")) {
                // Local fallback slot
                val backupDir = CsvBackupManager.getBackupDirectory(context)
                val targetFile = when (fileId) {
                    "local_slot_today" -> File(backupDir, "backup_day_today.csv")
                    "local_slot_yesterday" -> File(backupDir, "backup_day_yesterday.csv")
                    else -> File(backupDir, "backup_day_today.csv")
                }
                if (targetFile.exists()) {
                    return@withContext Result.success(targetFile.readText(Charsets.UTF_8))
                }
            }

            val token = getAccessToken(context)
                ?: return@withContext Result.failure(Exception("Google Drive account not authorized"))

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Drive download failed (${response.code}): ${response.message}"))
            }

            val content = response.body?.string() ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading CSV from Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Restores Room database directly from Google Drive CSV file in 1 click.
     */
    suspend fun restoreDatabaseFromDriveFile(
        context: Context,
        fileId: String,
        database: AppDatabase
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val contentResult = downloadCsvContentFromDrive(context, fileId)
            if (contentResult.isFailure) {
                return@withContext Result.failure(contentResult.exceptionOrNull() ?: Exception("Failed to fetch file from Drive"))
            }
            val csvContent = contentResult.getOrThrow()
            val summary = CsvBackupManager.restoreDatabaseFromCsvContent(context, csvContent, database)
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from Google Drive", e)
            Result.failure(e)
        }
    }

    // Helper: Find existing file ID by name in Google Drive
    private fun findFileIdByName(token: String, fileName: String): String? {
        return try {
            val query = "name = '$fileName' and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)&pageSize=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    files.getJSONObject(0).optString("id")
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing file on Drive: ${e.message}")
            null
        }
    }

    // Helper: Update content of an existing Google Drive file
    private fun updateDriveFileContent(token: String, fileId: String, content: String, fileName: String): DriveBackupFile {
        val url = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val mediaType = "text/csv; charset=utf-8".toMediaType()
        val body = content.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .patch(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to update Google Drive file (${response.code}): ${response.message}")
        }

        val json = JSONObject(response.body?.string() ?: "{}")
        val now = System.currentTimeMillis()
        val lineCount = content.lines().count { it.isNotBlank() && !it.startsWith("#") }

        return DriveBackupFile(
            id = json.optString("id", fileId),
            name = json.optString("name", fileName),
            modifiedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now)),
            modifiedTimeMillis = now,
            sizeBytes = content.toByteArray(Charsets.UTF_8).size.toLong(),
            recordCount = lineCount
        )
    }

    // Helper: Create a new file in Google Drive
    private fun createDriveFile(token: String, fileName: String, content: String, parentFolderId: String? = null): DriveBackupFile {
        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("mimeType", "text/csv")
            put("description", "Shree Bartan Store Customer Ledger Full CSV Backup")
            if (!parentFolderId.isNullOrBlank()) {
                put("parents", JSONArray().put(parentFolderId))
            }
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "metadata",
                null,
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addFormDataPart(
                "file",
                fileName,
                content.toRequestBody("text/csv; charset=utf-8".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to create Google Drive file (${response.code}): ${response.message}")
        }

        val json = JSONObject(response.body?.string() ?: "{}")
        val now = System.currentTimeMillis()
        val lineCount = content.lines().count { it.isNotBlank() && !it.startsWith("#") }

        return DriveBackupFile(
            id = json.optString("id"),
            name = json.optString("name", fileName),
            modifiedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now)),
            modifiedTimeMillis = now,
            sizeBytes = content.toByteArray(Charsets.UTF_8).size.toLong(),
            recordCount = lineCount
        )
    }

    /**
     * Gets or creates the main Root Folder in Google Drive: "श्री बर्तन भंडार - खाता बही (ShreeBartan Ledger)"
     */
    fun getOrCreateRootFolder(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_ROOT_FOLDER_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val folderId = getOrCreateDriveFolder(token, ROOT_FOLDER_NAME, parentId = null)
        if (!folderId.isNullOrBlank()) {
            prefs.edit().putString(KEY_ROOT_FOLDER_ID, folderId).apply()
        }
        return folderId
    }

    /**
     * Gets or creates the Customer Photos subfolder.
     */
    fun getOrCreateCustomerPhotosFolder(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_CUSTOMER_PHOTOS_FOLDER_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val rootId = getOrCreateRootFolder(context, token)
        val folderId = getOrCreateDriveFolder(token, SUBFOLDER_CUSTOMER_PHOTOS, parentId = rootId)
        if (!folderId.isNullOrBlank()) {
            prefs.edit().putString(KEY_CUSTOMER_PHOTOS_FOLDER_ID, folderId).apply()
        }
        return folderId
    }

    /**
     * Gets or creates the KYC Documents subfolder.
     */
    fun getOrCreateDocPhotosFolder(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_DOC_PHOTOS_FOLDER_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val rootId = getOrCreateRootFolder(context, token)
        val folderId = getOrCreateDriveFolder(token, SUBFOLDER_DOC_PHOTOS, parentId = rootId)
        if (!folderId.isNullOrBlank()) {
            prefs.edit().putString(KEY_DOC_PHOTOS_FOLDER_ID, folderId).apply()
        }
        return folderId
    }

    /**
     * Gets or creates the Backups subfolder.
     */
    fun getOrCreateBackupsFolder(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_BACKUPS_FOLDER_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val rootId = getOrCreateRootFolder(context, token)
        val folderId = getOrCreateDriveFolder(token, SUBFOLDER_BACKUPS, parentId = rootId)
        if (!folderId.isNullOrBlank()) {
            prefs.edit().putString(KEY_BACKUPS_FOLDER_ID, folderId).apply()
        }
        return folderId
    }

    /**
     * Opens the user's Google Drive app or web interface directly in the ShreeBartan Ledger folder.
     */
    fun openGoogleDriveFolder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val folderId = prefs.getString(KEY_ROOT_FOLDER_ID, null)
        val url = if (!folderId.isNullOrBlank()) {
            "https://drive.google.com/drive/folders/$folderId"
        } else {
            "https://drive.google.com/drive/my-drive"
        }
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "Cannot open Google Drive: ${ex.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Gets or creates a dedicated folder in Google Drive.
     */
    private fun getOrCreateDriveFolder(token: String, folderName: String, parentId: String? = null): String? {
        try {
            val query = if (!parentId.isNullOrBlank()) {
                "name = '$folderName' and '$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            } else {
                "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            }
            val url = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)&pageSize=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return files.getJSONObject(0).optString("id")
                }
            }

            // Create the folder
            val folderMetadata = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                put("description", "Shree Bartan Store - $folderName")
                if (!parentId.isNullOrBlank()) {
                    put("parents", JSONArray().put(parentId))
                }
            }.toString()

            val createReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .addHeader("Authorization", "Bearer $token")
                .post(folderMetadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            val createResp = okHttpClient.newCall(createReq).execute()
            if (createResp.isSuccessful) {
                val createdJson = JSONObject(createResp.body?.string() ?: "{}")
                return createdJson.optString("id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getOrCreateDriveFolder: ${e.message}")
        }
        return null
    }

    /**
     * Uploads a single image file (profile photo or document photo) to Google Drive in the photos folder.
     */
    suspend fun uploadSingleImageToDrive(
        context: Context,
        imageFile: File,
        remoteFileName: String,
        isDocument: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return@withContext Result.failure(Exception("Image file does not exist"))
        }

        try {
            val token = getAccessToken(context)
                ?: return@withContext Result.failure(Exception("Google Drive not connected"))

            val folderId = if (isDocument) {
                getOrCreateDocPhotosFolder(context, token) ?: getOrCreateRootFolder(context, token)
            } else {
                getOrCreateCustomerPhotosFolder(context, token) ?: getOrCreateRootFolder(context, token)
            }

            // Check if photo with same name exists
            val query = if (folderId != null) {
                "name = '$remoteFileName' and '$folderId' in parents and trashed = false"
            } else {
                "name = '$remoteFileName' and trashed = false"
            }

            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id)&pageSize=1"
            val searchReq = Request.Builder().url(searchUrl).addHeader("Authorization", "Bearer $token").get().build()
            val searchResp = okHttpClient.newCall(searchReq).execute()

            var existingId: String? = null
            if (searchResp.isSuccessful) {
                val json = JSONObject(searchResp.body?.string() ?: "{}")
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    existingId = files.getJSONObject(0).optString("id")
                }
            }

            val imageBytes = imageFile.readBytes()
            val mediaType = "image/jpeg".toMediaType()

            if (existingId != null) {
                // Update
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media"
                val patchReq = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(imageBytes.toRequestBody(mediaType))
                    .build()
                val patchResp = okHttpClient.newCall(patchReq).execute()
                if (patchResp.isSuccessful) {
                    return@withContext Result.success(existingId)
                }
            }

            // Create new
            val metadataJson = JSONObject().apply {
                put("name", remoteFileName)
                put("mimeType", "image/jpeg")
                if (folderId != null) {
                    put("parents", JSONArray().put(folderId))
                }
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addFormDataPart("file", remoteFileName, imageBytes.toRequestBody(mediaType))
                .build()

            val uploadReq = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            val uploadResp = okHttpClient.newCall(uploadReq).execute()
            if (!uploadResp.isSuccessful) {
                return@withContext Result.failure(Exception("Upload image failed: ${uploadResp.code}"))
            }

            val respJson = JSONObject(uploadResp.body?.string() ?: "{}")
            val newFileId = respJson.optString("id")
            Result.success(newFileId)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo to Drive: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads all customer profile photos and document photos to Google Drive in background.
     */
    suspend fun syncAllCustomerPhotosToDrive(
        context: Context,
        customers: List<CustomerEntity>,
        onProgress: ((uploaded: Int, total: Int) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            val photosToSync = mutableListOf<Triple<File, String, Boolean>>()

            for (cust in customers) {
                val cleanName = cust.name.trim().replace(Regex("[^a-zA-Z0-9_\\u0900-\\u097F]"), "_").take(25)
                // 1. Profile photo
                if (!cust.photoUri.isNullOrBlank()) {
                    val uri = Uri.parse(cust.photoUri)
                    val localFile = if (uri.scheme == "file") File(uri.path ?: "") else null
                    if (localFile != null && localFile.exists()) {
                        val remoteName = "${cleanName}_फोटो_${cust.id}.jpg"
                        photosToSync.add(Triple(localFile, remoteName, false))
                    }
                }
                // 2. Document photo
                if (!cust.documentPhotoUri.isNullOrBlank()) {
                    val uri = Uri.parse(cust.documentPhotoUri)
                    val localFile = if (uri.scheme == "file") File(uri.path ?: "") else null
                    if (localFile != null && localFile.exists()) {
                        val cleanDocType = cust.documentType.trim().replace(Regex("[^a-zA-Z0-9_\\u0900-\\u097F]"), "_").ifBlank { "Doc" }
                        val remoteName = "${cleanName}_${cleanDocType}_${cust.id}.jpg"
                        photosToSync.add(Triple(localFile, remoteName, true))
                    }
                }
            }

            val total = photosToSync.size
            if (total == 0) {
                return@withContext Result.success(0)
            }

            var uploadedCount = 0
            if (token == null) {
                // Offline fallback - files are safely stored in local persistent storage
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_PHOTOS_BACKED_UP_COUNT, total).apply()
                return@withContext Result.success(total)
            }

            for ((file, remoteName, isDoc) in photosToSync) {
                val res = uploadSingleImageToDrive(context, file, remoteName, isDocument = isDoc)
                if (res.isSuccess) {
                    uploadedCount++
                    onProgress?.invoke(uploadedCount, total)
                }
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_PHOTOS_BACKED_UP_COUNT, uploadedCount).apply()

            Log.i(TAG, "Photos synced to Drive: $uploadedCount / $total")
            Result.success(uploadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing photos to Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads/Restores all customer photos from Google Drive into internal storage if missing locally.
     */
    suspend fun restorePhotosFromDrive(
        context: Context,
        customers: List<CustomerEntity>,
        database: AppDatabase
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
                ?: return@withContext Result.success(0)

            val rootId = getOrCreateRootFolder(context, token)
            val custFolderId = getOrCreateCustomerPhotosFolder(context, token)
            val docFolderId = getOrCreateDocPhotosFolder(context, token)
            val legacyFolderId = getOrCreateDriveFolder(token, DRIVE_PHOTOS_FOLDER_NAME)

            val folderIdsToCheck = listOfNotNull(custFolderId, docFolderId, rootId, legacyFolderId).distinct()
            val driveFilesMap = mutableMapOf<String, String>() // name -> id

            for (fId in folderIdsToCheck) {
                val query = "'$fId' in parents and trashed = false"
                val url = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)&pageSize=100"
                val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token").get().build()
                val resp = okHttpClient.newCall(req).execute()

                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val filesArray = json.optJSONArray("files") ?: JSONArray()
                    for (i in 0 until filesArray.length()) {
                        val item = filesArray.getJSONObject(i)
                        driveFilesMap[item.optString("name")] = item.optString("id")
                    }
                }
            }

            var restoredCount = 0
            val photoDir = PhotoStorageManager.getCustomerPhotosDirectory(context)
            val docDir = PhotoStorageManager.getDocPhotosDirectory(context)

            for (cust in customers) {
                var updatedCustomer = cust
                var changed = false
                val cleanName = cust.name.trim().replace(Regex("[^a-zA-Z0-9_\\u0900-\\u097F]"), "_").take(25)

                // 1. Check profile photo
                val expectedProfileName = "${cleanName}_फोटो_${cust.id}.jpg"
                val legacyProfileName = "profile_${cust.id}_${cust.name.replace(Regex("[^a-zA-Z0-9]"), "_")}.jpg"
                val profileDriveId = driveFilesMap[expectedProfileName] ?: driveFilesMap[legacyProfileName]

                if (profileDriveId != null) {
                    val localTarget = File(photoDir, expectedProfileName)
                    if (!localTarget.exists() || localTarget.length() == 0L) {
                        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$profileDriveId?alt=media"
                        val dlReq = Request.Builder().url(downloadUrl).addHeader("Authorization", "Bearer $token").get().build()
                        val dlResp = okHttpClient.newCall(dlReq).execute()
                        if (dlResp.isSuccessful) {
                            dlResp.body?.byteStream()?.use { input ->
                                localTarget.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            updatedCustomer = updatedCustomer.copy(photoUri = Uri.fromFile(localTarget).toString())
                            changed = true
                            restoredCount++
                        }
                    }
                }

                // 2. Check doc photo
                val cleanDocType = cust.documentType.trim().replace(Regex("[^a-zA-Z0-9_\\u0900-\\u097F]"), "_").ifBlank { "Doc" }
                val expectedDocName = "${cleanName}_${cleanDocType}_${cust.id}.jpg"
                val legacyDocName = "doc_${cust.id}_${cust.documentType.replace(Regex("[^a-zA-Z0-9]"), "_")}.jpg"
                val docDriveId = driveFilesMap[expectedDocName] ?: driveFilesMap[legacyDocName]

                if (docDriveId != null) {
                    val localDocTarget = File(docDir, expectedDocName)
                    if (!localDocTarget.exists() || localDocTarget.length() == 0L) {
                        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$docDriveId?alt=media"
                        val dlReq = Request.Builder().url(downloadUrl).addHeader("Authorization", "Bearer $token").get().build()
                        val dlResp = okHttpClient.newCall(dlReq).execute()
                        if (dlResp.isSuccessful) {
                            dlResp.body?.byteStream()?.use { input ->
                                localDocTarget.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            updatedCustomer = updatedCustomer.copy(documentPhotoUri = Uri.fromFile(localDocTarget).toString())
                            changed = true
                            restoredCount++
                        }
                    }
                }

                if (changed) {
                    database.customerDao().updateCustomer(updatedCustomer)
                }
            }

            Log.i(TAG, "Restored $restoredCount photos from Google Drive")
            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring photos from Google Drive", e)
            Result.failure(e)
        }
    }
}
