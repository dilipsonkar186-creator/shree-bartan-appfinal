package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages local persistent storage and optimization for customer profile photos
 * and document photos so they survive app restarts, cache wipes, and can be synced.
 */
object PhotoStorageManager {
    private const val TAG = "PhotoStorageManager"
    private const val CUSTOMER_PHOTOS_DIR = "customer_photos"
    private const val DOC_PHOTOS_DIR = "customer_documents"

    fun getCustomerPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, CUSTOMER_PHOTOS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDocPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, DOC_PHOTOS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Saves a customer profile bitmap or Uri permanently into internal storage and returns the persistent file Uri string.
     */
    suspend fun saveCustomerPhotoPermanently(
        context: Context,
        sourceUriString: String?,
        customerName: String = "customer"
    ): String? = withContext(Dispatchers.IO) {
        if (sourceUriString.isNullOrBlank()) return@withContext null
        try {
            val dir = getCustomerPhotosDirectory(context)
            val cleanName = customerName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetFile = File(dir, "photo_${cleanName}_$timeStamp.jpg")

            if (sourceUriString.startsWith("file://")) {
                val srcFile = File(Uri.parse(sourceUriString).path ?: "")
                if (srcFile.exists() && srcFile.absolutePath != targetFile.absolutePath) {
                    srcFile.copyTo(targetFile, overwrite = true)
                    return@withContext Uri.fromFile(targetFile).toString()
                }
            }

            val uri = Uri.parse(sourceUriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Decode and optimize (compress to max 1200px width/height and 85% quality to save space and Drive bandwidth)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val scaled = scaleBitmapDown(bitmap, 1200)
                    FileOutputStream(targetFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    if (scaled != bitmap) scaled.recycle()
                    bitmap.recycle()
                    return@withContext Uri.fromFile(targetFile).toString()
                }
            }

            sourceUriString
        } catch (e: Exception) {
            Log.e(TAG, "Error saving customer photo permanently", e)
            sourceUriString
        }
    }

    /**
     * Saves a document photo (Aadhaar, PAN, etc.) permanently.
     */
    suspend fun saveDocumentPhotoPermanently(
        context: Context,
        sourceUriString: String?,
        docType: String = "doc"
    ): String? = withContext(Dispatchers.IO) {
        if (sourceUriString.isNullOrBlank()) return@withContext null
        try {
            val dir = getDocPhotosDirectory(context)
            val cleanType = docType.replace(Regex("[^a-zA-Z0-9_]"), "_").take(15)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetFile = File(dir, "doc_${cleanType}_$timeStamp.jpg")

            if (sourceUriString.startsWith("file://")) {
                val srcFile = File(Uri.parse(sourceUriString).path ?: "")
                if (srcFile.exists() && srcFile.absolutePath != targetFile.absolutePath) {
                    srcFile.copyTo(targetFile, overwrite = true)
                    return@withContext Uri.fromFile(targetFile).toString()
                }
            }

            val uri = Uri.parse(sourceUriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val scaled = scaleBitmapDown(bitmap, 1400)
                    FileOutputStream(targetFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    if (scaled != bitmap) scaled.recycle()
                    bitmap.recycle()
                    return@withContext Uri.fromFile(targetFile).toString()
                }
            }

            sourceUriString
        } catch (e: Exception) {
            Log.e(TAG, "Error saving doc photo permanently", e)
            sourceUriString
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = originalWidth
        var resizedHeight = originalHeight

        if (originalHeight > maxDimension || originalWidth > maxDimension) {
            if (originalWidth > originalHeight) {
                resizedWidth = maxDimension
                resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
            } else {
                resizedHeight = maxDimension
                resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
        }
        return bitmap
    }

    /**
     * Converts a photo Uri or file into a compact Base64 JPEG string (approx 15-35KB) for seamless cloud backup.
     */
    suspend fun getPhotoAsBase64(
        context: Context,
        uriString: String?,
        maxDimension: Int = 600,
        quality: Int = 75
    ): String? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        try {
            val bitmap: Bitmap? = if (uriString.startsWith("file://")) {
                val file = File(Uri.parse(uriString).path ?: "")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            } else {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }

            if (bitmap == null) return@withContext null

            val scaled = scaleBitmapDown(bitmap, maxDimension)
            val stream = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val byteArray = stream.toByteArray()
            if (scaled != bitmap) scaled.recycle()
            bitmap.recycle()

            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding photo to base64: ${e.message}")
            null
        }
    }

    /**
     * Recreates a local image file from Base64 data during cloud restore.
     */
    suspend fun restorePhotoFromBase64(
        context: Context,
        base64Str: String?,
        fileName: String,
        isDocument: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (base64Str.isNullOrBlank()) return@withContext null
        try {
            val targetDir = if (isDocument) getDocPhotosDirectory(context) else getCustomerPhotosDirectory(context)
            val targetFile = File(targetDir, fileName)
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)

            FileOutputStream(targetFile).use { out ->
                out.write(decodedBytes)
            }
            Log.d(TAG, "Successfully restored photo from base64: ${targetFile.absolutePath}")
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring photo from base64: ${e.message}")
            null
        }
    }
}
