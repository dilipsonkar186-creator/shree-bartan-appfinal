package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.remote.FirestoreRepository
import com.example.util.CsvBackupManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting 24-Hour Rolling Auto-Backup in background...")
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val folders = database.folderDao().getAllFoldersDirect()
            val customers = database.customerDao().getAllCustomersDirect()
            val transactions = database.transactionDao().getAllTransactionsDirect()

            // 1. Perform 2-Day Rolling CSV Backup (Overwrites older day every 24 hours)
            val csvResult = CsvBackupManager.performRollingBackup(
                applicationContext,
                folders,
                customers,
                transactions
            )

            // 2. Also sync to cloud backup if user is logged in
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUid = auth.currentUser?.uid
                if (currentUid != null) {
                    val firestoreRepo = FirestoreRepository()
                    firestoreRepo.saveFullAppBackup(
                        folders = folders,
                        customers = customers,
                        transactions = transactions,
                        userId = currentUid
                    )
                }
            } catch (cloudErr: Exception) {
                Log.w(TAG, "Background cloud backup sync skipped: ${cloudErr.message}")
            }

            // 3. Upload Full CSV Backup to Google Drive if auto-sync enabled
            try {
                if (com.example.util.GoogleDriveManager.isAutoSyncEnabled(applicationContext)) {
                    val fullCsv = com.example.util.CsvBackupManager.buildFullAppCsv(folders, customers, transactions)
                    com.example.util.GoogleDriveManager.uploadBackupCsvToDrive(applicationContext, fullCsv)
                    Log.i(TAG, "Background Google Drive CSV backup completed.")
                }
            } catch (driveErr: Exception) {
                Log.w(TAG, "Background Google Drive backup skipped: ${driveErr.message}")
            }

            if (csvResult.isSuccess) {
                Log.i(TAG, "Background 2-Day Rolling Auto-Backup completed successfully.")
                Result.success()
            } else {
                Log.e(TAG, "CSV rolling backup failed: ${csvResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running AutoBackupWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val PERIODIC_WORK_TAG = "auto_rolling_backup_work"

        /**
         * Schedules the daily background rolling backup task using WorkManager.
         * Runs automatically every 24 hours even if the app is closed.
         */
        fun scheduleDailyAutoBackup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                    24, TimeUnit.HOURS,
                    2, TimeUnit.HOURS // 2-hour flex window around daily schedule
                )
                    .setConstraints(constraints)
                    .addTag(PERIODIC_WORK_TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Log.i(TAG, "Scheduled 24-hour rolling background auto-backup successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling background backup work", e)
            }
        }
    }
}
