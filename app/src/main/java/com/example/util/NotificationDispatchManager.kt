package com.example.util

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object NotificationDispatchManager {

    private const val TAG = "NotificationDispatch"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Subscribe app instance to topics so device can receive alerts.
     * Safely guards against hard failures when running on emulators or devices
     * without Google Play Services / FCM registration.
     */
    fun setupCustomerTopicSubscriptions(context: Context? = null) {
        try {
            if (context != null) {
                val gmsAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                if (gmsAvailability != ConnectionResult.SUCCESS) {
                    Log.d(TAG, "Google Play Services not ready/available (status: $gmsAvailability). Skipping FCM topic sync.")
                    return
                }
            }

            // Only attempt topic registration if an FCM token is successfully obtained
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        Log.d(TAG, "FCM token verified: $token")
                        if (context != null) {
                            registerDeviceToken(context, token)
                        }
                        try {
                            FirebaseMessaging.getInstance().isAutoInitEnabled = true
                            // Always subscribe to general customer announcements & offers
                            FirebaseMessaging.getInstance().subscribeToTopic("all_customers")
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d(TAG, "Subscribed to topic: all_customers")
                                    } else {
                                        Log.d(TAG, "Subscription to all_customers skipped: ${task.exception?.message}")
                                    }
                                }

                            // Subscribe to transaction alerts topic
                            FirebaseMessaging.getInstance().subscribeToTopic("transactions")

                            // If phone is stored, subscribe to customer-specific topic
                            if (context != null) {
                                val prefs = context.getSharedPreferences("app_fcm_prefs", Context.MODE_PRIVATE)
                                val currentAuthPhone = try {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber
                                } catch (_: Exception) { null }
                                val phone = prefs.getString("user_phone", "")?.ifBlank { null }
                                    ?: prefs.getString("customer_phone", "")?.ifBlank { null }
                                    ?: currentAuthPhone
                                    ?: ""
                                val cleanPhone = phone.filter { it.isDigit() }.takeLast(10)
                                if (cleanPhone.isNotBlank()) {
                                    FirebaseMessaging.getInstance().subscribeToTopic("customer_$cleanPhone")
                                    FirebaseMessaging.getInstance().subscribeToTopic("customer_91$cleanPhone")
                                    FirebaseMessaging.getInstance().subscribeToTopic("customer_+91$cleanPhone")
                                    Log.d(TAG, "Subscribed to customer personal topics for: $cleanPhone")
                                }
                            }
                        } catch (t: Throwable) {
                            Log.d(TAG, "Topic subscription skipped: ${t.message}")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "FCM registration not active in current environment (e.g. streaming emulator): ${exception.message}")
                }
        } catch (e: Throwable) {
            Log.d(TAG, "Gracefully ignored FCM init exception: ${e.message}")
        }
    }

    /**
     * Send Transaction Push Notification to Customer:
     * e.g., "प्रिय ग्राहक, आपके खाते में ₹[रकम] का [सामान/जमा] दर्ज किया गया है। कुल बकाया: ₹[बकाया]"
     */
    suspend fun sendTransactionNotification(
        context: Context,
        customerId: Long,
        customerName: String,
        customerPhone: String,
        type: String, // "GOODS_PROVIDED" or "PAYMENT_DEPOSIT" or "GOODS_RETURNED"
        amount: Double,
        itemDesc: String,
        updatedDues: Double,
        storeName: String = "श्री बर्तन स्टोर",
        imageUrl: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val typeHindi = when (type) {
            "GOODS_PROVIDED" -> "सामान (उधार)"
            "GOODS_RETURNED" -> "सामान वापसी"
            else -> "जमा (पेमेंट)"
        }
        val amountStr = String.format("%,.0f", amount)
        val duesStr = String.format("%,.0f", updatedDues)

        val title = "$storeName — लेन-देन अपडेट"
        val body = "प्रिय $customerName जी, आपके खाते में ₹$amountStr का $typeHindi दर्ज किया गया है। कुल बकाया: ₹$duesStr"

        val cleanPhone = customerPhone.filter { it.isDigit() }.takeLast(10)
        val customerTopic = if (cleanPhone.isNotBlank()) "customer_$cleanPhone" else "all_customers"

        // Update customer document directly in Firestore so real-time listeners receive the update immediately
        try {
            val firestore = FirebaseFirestore.getInstance()
            val notifSummary = hashMapOf<String, Any>(
                "title" to title,
                "body" to body,
                "type" to type,
                "amount" to amount,
                "itemDescription" to itemDesc,
                "updatedDues" to updatedDues,
                "timestamp" to System.currentTimeMillis()
            )
            if (!imageUrl.isNullOrBlank()) {
                notifSummary["imageUrl"] = imageUrl
                notifSummary["image_url"] = imageUrl
                notifSummary["image"] = imageUrl
            }

            val customerUpdate = hashMapOf<String, Any>(
                "customerId" to customerId,
                "customerName" to customerName,
                "phone" to customerPhone,
                "currentDues" to updatedDues,
                "lastNotification" to notifSummary,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("shree").document("customer_$customerId")
                .set(customerUpdate, com.google.firebase.firestore.SetOptions.merge())

            if (cleanPhone.isNotBlank()) {
                firestore.collection("shree").document("customer_by_phone_$cleanPhone")
                    .set(customerUpdate, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firestore customer notification sync note: ${e.message}")
        }

        val payload = mutableMapOf<String, Any>(
            "type" to "TRANSACTION_ALERT",
            "click_action" to "OPEN_TRANSACTION",
            "customerId" to customerId.toString(),
            "customerPhone" to customerPhone,
            "customerName" to customerName,
            "transactionType" to type,
            "amount" to amount,
            "itemDescription" to itemDesc,
            "updatedDues" to updatedDues,
            "title" to title,
            "body" to body,
            "timestamp" to System.currentTimeMillis()
        )
        if (!imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = imageUrl
            payload["image_url"] = imageUrl
            payload["image"] = imageUrl
        }

        dispatchNotification(
            context = context,
            title = title,
            body = body,
            targetPhone = customerPhone,
            targetTopic = customerTopic,
            data = payload
        )
    }

    /**
     * Send Payment Reminder Notification to Customer:
     * e.g., "बकाया पेमेंट सूचना: प्रिय [ग्राहक], आपका बकाया बैलेंस ₹[रकम] है..."
     */
    suspend fun sendPaymentReminderNotification(
        context: Context,
        customerId: Long,
        customerName: String,
        customerPhone: String,
        dues: Double,
        storeName: String = "श्री बर्तन स्टोर"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val duesStr = String.format("%,.0f", dues)
        val title = "🔔 $storeName — पेमेंट याददिहानी"
        val body = "प्रिय $customerName जी, आपके खाते में ₹$duesStr का बकाया शेष है। कृपया सुविधानुसार यथाशीघ्र भुगतान करें। धन्यवाद!"

        val cleanPhone = customerPhone.filter { it.isDigit() }.takeLast(10)
        val customerTopic = if (cleanPhone.isNotBlank()) "customer_$cleanPhone" else "all_customers"

        val payload = mapOf(
            "type" to "PAYMENT_REMINDER",
            "click_action" to "OPEN_CUSTOMER_DUES",
            "customerId" to customerId.toString(),
            "customerPhone" to customerPhone,
            "customerName" to customerName,
            "dues" to dues,
            "title" to title,
            "body" to body,
            "timestamp" to System.currentTimeMillis()
        )

        dispatchNotification(
            context = context,
            title = title,
            body = body,
            targetPhone = customerPhone,
            targetTopic = customerTopic,
            data = payload
        )
    }

    /**
     * Broadcast to All Customers (Flash Sale / Product Offer Alert):
     * Sent to 'all_customers' FCM topic.
     */
    suspend fun broadcastProductOfferToAll(
        context: Context,
        productId: Long? = null,
        productName: String,
        sellingPrice: Double,
        mrp: Double,
        imageUrl: String?,
        storeName: String = "श्री बर्तन स्टोर"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val priceStr = String.format("%,.0f", sellingPrice)
        val discount = if (mrp > sellingPrice && mrp > 0) {
            val pct = (((mrp - sellingPrice) / mrp) * 100).toInt()
            " ($pct% OFF)"
        } else ""

        val title = "🎉 $storeName — धमाकेदार ऑफर!"
        val body = "नया ऑफर: $productName अब केवल ₹$priceStr में उपलब्ध है$discount! अभी दुकान से संपर्क करें।"

        val payload = mutableMapOf<String, Any>(
            "type" to "PRODUCT_OFFER",
            "click_action" to "OPEN_PRODUCT",
            "productId" to (productId?.toString() ?: ""),
            "productName" to productName,
            "sellingPrice" to sellingPrice,
            "mrp" to mrp,
            "title" to title,
            "body" to body,
            "targetTopic" to "all_customers",
            "timestamp" to System.currentTimeMillis()
        )
        if (!imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = imageUrl
            payload["image_url"] = imageUrl
            payload["image"] = imageUrl
        }

        dispatchNotification(
            context = context,
            title = title,
            body = body,
            targetTopic = "all_customers",
            targetPhone = null,
            data = payload
        )
    }

    /**
     * Broadcast Live Announcement / Greetings / Festival wishes to All Customers:
     * Sent to 'all_customers' topic and saved in Firestore notifications queue.
     */
    suspend fun broadcastAnnouncementToAll(
        context: Context,
        title: String,
        announcementMessage: String,
        storeName: String = "श्री बर्तन स्टोर",
        imageUrl: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "type" to "SHOP_ANNOUNCEMENT",
            "click_action" to "OPEN_SHOP_NOTICE",
            "title" to title,
            "body" to announcementMessage,
            "targetTopic" to "all_customers",
            "storeName" to storeName,
            "timestamp" to System.currentTimeMillis()
        )
        if (!imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = imageUrl
            payload["image_url"] = imageUrl
            payload["image"] = imageUrl
        }

        dispatchNotification(
            context = context,
            title = title,
            body = announcementMessage,
            targetTopic = "all_customers",
            targetPhone = null,
            data = payload
        )
    }

    /**
     * Registers current device FCM token in Firestore customer_tokens and topics
     */
    fun registerDeviceToken(context: Context, token: String, phoneNumber: String? = null) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val prefs = context.getSharedPreferences("app_fcm_prefs", Context.MODE_PRIVATE)
            val phone = phoneNumber ?: prefs.getString("user_phone", "") ?: ""
            val cleanPhone = phone.filter { it.isDigit() }.takeLast(10)

            val tokenData = hashMapOf<String, Any>(
                "token" to token,
                "fcmToken" to token,
                "phone" to cleanPhone,
                "updatedAt" to System.currentTimeMillis()
            )

            if (cleanPhone.isNotBlank()) {
                firestore.collection("customer_tokens").document(cleanPhone).set(tokenData)
                firestore.collection("customer_tokens").document("+91$cleanPhone").set(tokenData)
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic("customer_$cleanPhone")
                } catch (_: Exception) {}
            }
            firestore.collection("customer_tokens").document(token.takeLast(20)).set(tokenData)
        } catch (e: Exception) {
            Log.w(TAG, "registerDeviceToken exception: ${e.message}")
        }
    }

    /**
     * Core dispatch engine:
     * 1. Logs notification to Firestore queue ('notification_queue' / 'shree_notifications')
     *    which triggers Firebase Cloud Functions or backend worker.
     * 2. Also checks for customer-specific FCM tokens saved in Firestore 'customer_tokens' or 'users'.
     * 3. Calls Cloud Functions HTTP endpoint if configured, or sends via direct webhook.
     */
    private suspend fun dispatchNotification(
        context: Context,
        title: String,
        body: String,
        targetTopic: String? = null,
        targetPhone: String? = null,
        data: Map<String, Any>
    ): Result<Boolean> {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val notifId = UUID.randomUUID().toString()

            // 1. Log to Firestore 'notification_queue' for Cloud Functions triggers
            val queueDoc = hashMapOf<String, Any>(
                "id" to notifId,
                "title" to title,
                "body" to body,
                "targetTopic" to (targetTopic ?: "all_customers"),
                "targetPhone" to (targetPhone ?: ""),
                "status" to "QUEUED",
                "createdAt" to System.currentTimeMillis(),
                "data" to data
            )

            // Save in both root notification_queue and shree subcollection for visibility
            firestore.collection("notification_queue").document(notifId).set(queueDoc).await()
            firestore.collection("shree").document("recent_notifications")
                .collection("history").document(notifId).set(queueDoc).await()

            // Also save directly into customer's personal notifications inbox in Firestore
            val cleanPhone = targetPhone?.filter { it.isDigit() }?.takeLast(10) ?: ""
            if (cleanPhone.isNotBlank()) {
                try {
                    firestore.collection("customers").document(cleanPhone)
                        .collection("notifications").document(notifId).set(queueDoc)
                    firestore.collection("customer_notifications").document(cleanPhone)
                        .collection("items").document(notifId).set(queueDoc)
                } catch (_: Exception) {}
            }

            Log.d(TAG, "Notification enqueued to Firestore: $notifId for topic: $targetTopic, phone: $targetPhone")

            // 2. If phone is given, try finding matching user device token in 'customer_tokens' or 'users'
            var foundToken: String? = null
            if (cleanPhone.isNotBlank()) {
                try {
                    val tokenDoc = firestore.collection("customer_tokens").document(cleanPhone).get().await()
                    if (tokenDoc.exists()) {
                        foundToken = tokenDoc.getString("fcmToken") ?: tokenDoc.getString("token")
                    }
                    if (foundToken.isNullOrBlank()) {
                        val tokenDoc91 = firestore.collection("customer_tokens").document("+91$cleanPhone").get().await()
                        if (tokenDoc91.exists()) {
                            foundToken = tokenDoc91.getString("fcmToken") ?: tokenDoc91.getString("token")
                        }
                    }
                    if (foundToken.isNullOrBlank()) {
                        val userDoc = firestore.collection("users").document(cleanPhone).get().await()
                        if (userDoc.exists()) {
                            foundToken = userDoc.getString("fcmToken") ?: userDoc.getString("token")
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No specific token found for $cleanPhone: ${e.message}")
                }
            }

            // 3. Trigger Cloud Function dispatch URL if available or execute via FCM backend
            try {
                val cloudFunctionUrl = "https://us-central1-clientfolder-manager.cloudfunctions.net/sendFcmPushNotification"
                val resolvedImageUrl = (data["imageUrl"] ?: data["image_url"] ?: data["image"])?.toString()
                val jsonPayload = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    if (!targetTopic.isNullOrBlank()) {
                        put("topic", targetTopic)
                    }
                    if (!foundToken.isNullOrBlank()) {
                        put("token", foundToken)
                    }
                    if (!resolvedImageUrl.isNullOrBlank()) {
                        put("imageUrl", resolvedImageUrl)
                        put("image_url", resolvedImageUrl)
                        put("image", resolvedImageUrl)
                    }
                    val dataJson = JSONObject()
                    for ((k, v) in data) {
                        dataJson.put(k, v.toString())
                    }
                    put("data", dataJson)
                    // Also include notification object with image for native FCM rich display
                    val notifObj = JSONObject().apply {
                        put("title", title)
                        put("body", body)
                        if (!resolvedImageUrl.isNullOrBlank()) {
                            put("image", resolvedImageUrl)
                        }
                    }
                    put("notification", notifObj)
                }

                val request = Request.Builder()
                    .url(cloudFunctionUrl)
                    .post(jsonPayload.toString().toRequestBody(JSON))
                    .build()

                // Non-blocking fire call
                val response = httpClient.newCall(request).execute()
                Log.d(TAG, "Cloud Function dispatch response: ${response.code}")
            } catch (e: Exception) {
                // Cloud Function endpoint may not be deployed yet, queue in Firestore handles it
                Log.i(TAG, "Direct HTTP trigger note (Firestore Queue is primary): ${e.message}")
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch notification", e)
            Result.failure(e)
        }
    }
}
