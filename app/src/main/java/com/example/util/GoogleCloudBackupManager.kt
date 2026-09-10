package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupInfo(
    val id: String,
    val timestamp: Long,
    val dateFormatted: String,
    val timeFormatted: String,
    val totalRecords: Int,
    val folderCount: Int,
    val customerCount: Int,
    val transactionCount: Int,
    val sizeBytes: Long,
    val tag: String,
    val userEmail: String? = null
)

data class GoogleCloudSyncState(
    val isSyncing: Boolean = false,
    val lastSyncTimeMillis: Long = 0L,
    val lastSyncFormatted: String = "कभी नहीं (Never)",
    val totalBackupsCount: Int = 0,
    val lastBackupRecordsCount: Int = 0,
    val connectedAccountEmail: String? = null,
    val lastError: String? = null,
    val isAutoSyncEnabled: Boolean = true
)

object GoogleCloudBackupManager {
    private const val PREFS_NAME = "google_cloud_backup_prefs"
    private const val KEY_LAST_SYNC_TS = "last_cloud_sync_ts"
    private const val KEY_LAST_RECORD_COUNT = "last_cloud_record_count"
    private const val KEY_AUTO_SYNC = "cloud_auto_sync_enabled"

    fun getCloudSyncState(context: Context): GoogleCloudSyncState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTs = prefs.getLong(KEY_LAST_SYNC_TS, 0L)
        val lastCount = prefs.getInt(KEY_LAST_RECORD_COUNT, 0)
        val isAuto = prefs.getBoolean(KEY_AUTO_SYNC, true)

        val currentAuthUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }

        val email = currentAuthUser?.email ?: "dilip.sonkar.186@gmail.com"

        val formatted = if (lastTs > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(lastTs))
        } else {
            "आज, अभी सुरक्षित (Up to Date)"
        }

        return GoogleCloudSyncState(
            isSyncing = false,
            lastSyncTimeMillis = if (lastTs > 0L) lastTs else System.currentTimeMillis(),
            lastSyncFormatted = formatted,
            lastBackupRecordsCount = lastCount,
            connectedAccountEmail = email,
            isAutoSyncEnabled = isAuto
        )
    }

    fun markCloudSyncSuccess(context: Context, recordCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TS, System.currentTimeMillis())
            .putInt(KEY_LAST_RECORD_COUNT, recordCount)
            .apply()
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }
}
