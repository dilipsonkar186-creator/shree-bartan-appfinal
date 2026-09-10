package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Cloudinary Manager for Shree Bartan Store.
 * Provides zero-cost, high-speed, permanent image hosting for product catalogs
 * and documents so customer apps can load images directly via HTTPS.
 */
object CloudinaryManager {
    private const val TAG = "CloudinaryManager"

    // Cloudinary Credentials configured for shop catalog
    const val CLOUD_NAME = "mxtn5eek"
    const val API_KEY = "545269335528999"
    private const val API_SECRET = "JsX0vb9OQ8z3afTYzsI81jURflo"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Uploads an image from a local Uri to Cloudinary and returns the permanent HTTPS secure_url.
     * If the uriString already starts with "http://" or "https://", it returns it as is.
     */
    suspend fun uploadImage(
        context: Context,
        uriString: String,
        folder: String = "shree_bartan_products"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (uriString.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Image URI is empty"))
        }
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return@withContext Result.success(uriString)
        }

        try {
            val uri = Uri.parse(uriString)
            // 1. Compress image to clean, lightweight JPEG (~80-150 KB)
            val compressedBytes = compressImage(context, uri, maxDimension = 1200, quality = 82)
                ?: return@withContext Result.failure(Exception("Failed to process image data"))

            // 2. Prepare timestamp and SHA-1 signature
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val paramsToSign = mapOf(
                "folder" to folder,
                "timestamp" to timestamp
            )
            val signature = generateSignature(paramsToSign, API_SECRET)

            // 3. Build multipart form request
            val imageRequestBody = compressedBytes.toRequestBody("image/jpeg".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "product_${System.currentTimeMillis()}.jpg", imageRequestBody)
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("folder", folder)
                .addFormDataPart("signature", signature)
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Cloudinary upload failed: HTTP ${response.code} -> $responseBody")
                return@withContext Result.failure(Exception("Cloudinary upload failed: HTTP ${response.code}"))
            }

            val json = JSONObject(responseBody)
            val secureUrl = json.optString("secure_url").ifBlank { json.optString("url") }
            if (secureUrl.isNotBlank()) {
                Log.d(TAG, "Successfully uploaded to Cloudinary: $secureUrl")
                Result.success(secureUrl)
            } else {
                Result.failure(Exception("Cloudinary returned empty secure_url"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Cloudinary upload", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads multiple image URIs to Cloudinary sequentially.
     * Skips images that are already web URLs.
     */
    suspend fun uploadMultipleImages(
        context: Context,
        uris: List<String>,
        folder: String = "shree_bartan_products",
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        val total = uris.size
        uris.forEachIndexed { index, uriStr ->
            onProgress(index + 1, total)
            if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                results.add(uriStr)
            } else {
                val uploadRes = uploadImage(context, uriStr, folder)
                if (uploadRes.isSuccess) {
                    results.add(uploadRes.getOrThrow())
                } else {
                    Log.w(TAG, "Could not upload $uriStr to Cloudinary, keeping local fallback")
                    results.add(uriStr)
                }
            }
        }
        results
    }

    private fun compressImage(context: Context, uri: Uri, maxDimension: Int, quality: Int): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW = if (ratio >= 1f) maxDimension else (maxDimension * ratio).toInt()
                val targetH = if (ratio >= 1f) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)

            if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
            originalBitmap.recycle()

            baos.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image for Cloudinary", e)
            null
        }
    }

    private fun generateSignature(params: Map<String, String>, apiSecret: String): String {
        val sortedParams = params.toSortedMap().map { "${it.key}=${it.value}" }.joinToString("&")
        val toSign = sortedParams + apiSecret
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(toSign.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
