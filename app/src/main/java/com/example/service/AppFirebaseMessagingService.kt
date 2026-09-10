package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        // Save local copy of token
        val prefs = applicationContext.getSharedPreferences("app_fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_device_token", token).apply()
        com.example.util.NotificationDispatchManager.registerDeviceToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Check if message is a customer transaction or reminder targeted at a specific phone
        val notifType = remoteMessage.data["type"]
        val targetPhone = remoteMessage.data["targetPhone"]
            ?: remoteMessage.data["customerPhone"]

        if (notifType == "TRANSACTION_ALERT" || notifType == "PAYMENT_REMINDER") {
            if (!targetPhone.isNullOrBlank()) {
                val prefs = applicationContext.getSharedPreferences("app_fcm_prefs", Context.MODE_PRIVATE)
                val currentAuthPhone = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber
                } catch (_: Exception) { null }
                val myPhone = prefs.getString("user_phone", "")?.ifBlank { null }
                    ?: prefs.getString("customer_phone", "")?.ifBlank { null }
                    ?: currentAuthPhone
                    ?: ""

                val cleanTarget = targetPhone.filter { it.isDigit() }.takeLast(10)
                val cleanMy = myPhone.filter { it.isDigit() }.takeLast(10)

                // If device has a registered phone and it differs from target, ignore transaction alert
                if (cleanMy.isNotBlank() && cleanTarget.isNotBlank() && cleanMy != cleanTarget) {
                    Log.d(TAG, "Skipping alert for different customer ($cleanTarget vs $cleanMy)")
                    return
                }
            }
        }

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "श्री बर्तन स्टोर"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "आपके खाते में नया अपडेट दर्ज किया गया है।"

        val clickAction = remoteMessage.data["click_action"]
            ?: remoteMessage.notification?.clickAction

        val imageUrl = remoteMessage.data["imageUrl"]
            ?: remoteMessage.data["image_url"]
            ?: remoteMessage.data["image"]
            ?: remoteMessage.notification?.imageUrl?.toString()

        showSystemNotification(title, body, clickAction, imageUrl, remoteMessage.data)
    }

    private fun showSystemNotification(
        title: String,
        body: String,
        clickAction: String?,
        imageUrl: String?,
        data: Map<String, String> = emptyMap()
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ग्राहक लेन-देन व ऑफर सूचनाएँ",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "दुकान लेन-देन, बकाया पेमेंट रिमाइंडर और नए ऑफर्स के अलर्ट"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((k, v) in data) {
                putExtra(k, v)
            }
            if (clickAction != null) {
                putExtra("click_action", clickAction)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Try downloading image if available for rich notification
        var bitmap: Bitmap? = null
        if (!imageUrl.isNullOrBlank()) {
            try {
                bitmap = downloadImage(imageUrl)
            } catch (e: Exception) {
                Log.w(TAG, "Could not download notification image: ${e.message}")
            }
        }

        val style = if (bitmap != null) {
            NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .setBigContentTitle(title)
                .setSummaryText(body)
        } else {
            NotificationCompat.BigTextStyle().bigText(body)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            notificationBuilder.setLargeIcon(bitmap)
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun downloadImage(urlStr: String): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        return try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "FCM_Service"
        const val CHANNEL_ID = "shree_store_notifications_channel"
        const val TOPIC_ALL_CUSTOMERS = "all_customers"
    }
}
