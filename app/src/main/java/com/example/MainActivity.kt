package com.example

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FolderViewModel
import com.example.ui.viewmodel.FolderViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : FragmentActivity() {

    private var activeViewModel: FolderViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyCN9xZ4Uo3kSnIABCcFwEV-bcT6VhYYUiA")
                        .setProjectId("clientfolder-manager")
                        .setApplicationId("1:583177365912:android:1a9ad390f097f5764ca3ed")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp init failed: ${e.message}", e)
        }

        // Ensure 24-Hour Rolling CSV Auto-Backup is scheduled to run in background
        try {
            com.example.worker.AutoBackupWorker.scheduleDailyAutoBackup(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "AutoBackupWorker scheduling error: ${e.message}")
        }

        // Setup FCM Customer Topics & Token Dispatch
        try {
            com.example.util.NotificationDispatchManager.setupCustomerTopicSubscriptions(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "setupCustomerTopicSubscriptions error: ${e.message}")
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FolderViewModel = viewModel(
                        factory = FolderViewModelFactory(application)
                    )
                    activeViewModel = viewModel

                    // Handle intent on initial start
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        handleNotificationIntent(intent, viewModel)
                    }

                    AppNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeViewModel?.let { vm ->
            handleNotificationIntent(intent, vm)
        }
    }

    private fun handleNotificationIntent(intent: Intent?, viewModel: FolderViewModel) {
        if (intent == null) return
        try {
            val clickAction = intent.getStringExtra("click_action")
            val notifType = intent.getStringExtra("type")

            val productIdStr = intent.getStringExtra("productId")
                ?: intent.data?.getQueryParameter("productId")
            val productId = productIdStr?.toLongOrNull()
                ?: intent.getLongExtra("productId", -1L).takeIf { it > 0 }

            val productName = intent.getStringExtra("productName")
                ?: intent.getStringExtra("name")
                ?: intent.data?.getQueryParameter("productName")

            val sellingPriceStr = intent.getStringExtra("sellingPrice")
                ?: intent.data?.getQueryParameter("sellingPrice")
            val sellingPrice = sellingPriceStr?.toDoubleOrNull()
                ?: intent.getDoubleExtra("sellingPrice", 0.0)

            val mrpStr = intent.getStringExtra("mrp")
                ?: intent.data?.getQueryParameter("mrp")
            val mrp = mrpStr?.toDoubleOrNull()
                ?: intent.getDoubleExtra("mrp", 0.0)

            val imageUrl = intent.getStringExtra("imageUrl")
                ?: intent.getStringExtra("image")
                ?: intent.data?.getQueryParameter("imageUrl")

            val description = intent.getStringExtra("description")
                ?: intent.getStringExtra("body")
                ?: intent.getStringExtra("itemDescription")
                ?: ""

            if (!productName.isNullOrBlank() || productId != null || notifType == "PRODUCT_OFFER" || clickAction == "OPEN_PRODUCT") {
                Log.d("MainActivity", "Notification clicked for offer product: $productName (id: $productId)")
                viewModel.openOfferNotification(
                    com.example.ui.viewmodel.NotificationOfferProduct(
                        id = productId,
                        name = productName?.ifBlank { "स्पेशल ऑफर बर्तन" } ?: "स्पेशल ऑफर बर्तन",
                        sellingPrice = sellingPrice,
                        mrp = mrp,
                        imageUrl = imageUrl,
                        description = description
                    )
                )
            }

            val customerIdStr = intent.getStringExtra("customerId")
                ?: intent.data?.getQueryParameter("customerId")
            val customerId = customerIdStr?.toLongOrNull()
                ?: intent.getLongExtra("customerId", -1L).takeIf { it > 0 }

            if (customerId != null) {
                Log.d("MainActivity", "Notification clicked for customer ID: $customerId")
                viewModel.openCustomerFromNotification(customerId)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Error parsing notification intent extras: ${e.message}")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                coil.Coil.imageLoader(this).memoryCache?.clear()
            }
        } catch (_: Exception) {}
    }
}
