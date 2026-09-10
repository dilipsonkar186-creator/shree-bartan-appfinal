package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GlobalSmsConfig(
    val storeName: String = "Shree Bartan Store",
    val storePhone: String = "",
    val globalSmsEnabled: Boolean = true,
    val defaultLanguage: String = "HINDI", // "HINDI" or "ENGLISH"
    val defaultStyle: String = "DETAILED", // "DETAILED" or "BRIEF"
    val customReminderNote: String = "कृपया जल्द से जल्द बकाया राशि का भुगतान करें। धन्यवाद!",
    val customStoreFooter: String = "श्री बर्तन स्टोर"
)

object SmsUtils {

    private const val PREFS_NAME = "global_sms_preferences"
    private const val KEY_STORE_NAME = "key_store_name"
    private const val KEY_STORE_PHONE = "key_store_phone"
    private const val KEY_GLOBAL_SMS_ENABLED = "key_global_sms_enabled"
    private const val KEY_DEFAULT_LANGUAGE = "key_default_lang"
    private const val KEY_DEFAULT_STYLE = "key_default_style"
    private const val KEY_CUSTOM_REMINDER = "key_custom_reminder"
    private const val KEY_STORE_FOOTER = "key_store_footer"

    fun getGlobalSmsConfig(context: Context): GlobalSmsConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GlobalSmsConfig(
            storeName = prefs.getString(KEY_STORE_NAME, "Shree Bartan Store") ?: "Shree Bartan Store",
            storePhone = prefs.getString(KEY_STORE_PHONE, "") ?: "",
            globalSmsEnabled = prefs.getBoolean(KEY_GLOBAL_SMS_ENABLED, true),
            defaultLanguage = prefs.getString(KEY_DEFAULT_LANGUAGE, "HINDI") ?: "HINDI",
            defaultStyle = prefs.getString(KEY_DEFAULT_STYLE, "DETAILED") ?: "DETAILED",
            customReminderNote = prefs.getString(KEY_CUSTOM_REMINDER, "कृपया जल्द से जल्द बकाया राशि का भुगतान करें। धन्यवाद!") ?: "कृपया जल्द से जल्द बकाया राशि का भुगतान करें। धन्यवाद!",
            customStoreFooter = prefs.getString(KEY_STORE_FOOTER, "श्री बर्तन स्टोर") ?: "श्री बर्तन स्टोर"
        )
    }

    fun saveGlobalSmsConfig(context: Context, config: GlobalSmsConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_STORE_NAME, config.storeName)
            .putString(KEY_STORE_PHONE, config.storePhone)
            .putBoolean(KEY_GLOBAL_SMS_ENABLED, config.globalSmsEnabled)
            .putString(KEY_DEFAULT_LANGUAGE, config.defaultLanguage)
            .putString(KEY_DEFAULT_STYLE, config.defaultStyle)
            .putString(KEY_CUSTOM_REMINDER, config.customReminderNote)
            .putString(KEY_STORE_FOOTER, config.customStoreFooter)
            .apply()
    }

    fun shareMessage(context: Context, message: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Message via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open sharing options", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, message: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("SMS Message", message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun makeCall(context: Context, phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatPhoneNumberForWhatsApp(phone: String): String {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.length == 10) {
            "91$digitsOnly"
        } else {
            digitsOnly
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWhatsAppBusiness(context: Context, phone: String, message: String): Boolean {
        val formattedPhone = formatPhoneNumberForWhatsApp(phone)
        if (formattedPhone.isBlank()) {
            Toast.makeText(context, "Phone number is missing", Toast.LENGTH_SHORT).show()
            return false
        }
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp.w4b")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            try {
                val directUri = Uri.parse("whatsapp://send?phone=$formattedPhone&text=${Uri.encode(message)}")
                val directIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage("com.whatsapp.w4b")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(directIntent)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp Business ऐप आपके फ़ोन में नहीं मिला", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }

    fun openWhatsAppRegular(context: Context, phone: String, message: String): Boolean {
        val formattedPhone = formatPhoneNumberForWhatsApp(phone)
        if (formattedPhone.isBlank()) {
            Toast.makeText(context, "Phone number is missing", Toast.LENGTH_SHORT).show()
            return false
        }
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            try {
                val directUri = Uri.parse("whatsapp://send?phone=$formattedPhone&text=${Uri.encode(message)}")
                val directIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(directIntent)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp ऐप आपके फ़ोन में नहीं मिला", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val formattedPhone = formatPhoneNumberForWhatsApp(phone)
        if (formattedPhone.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }

        val hasWb = isPackageInstalled(context, "com.whatsapp.w4b")
        val hasWa = isPackageInstalled(context, "com.whatsapp")

        if (hasWb && !hasWa) {
            openWhatsAppBusiness(context, phone, message)
        } else if (hasWa && !hasWb) {
            openWhatsAppRegular(context, phone, message)
        } else {
            // Both or neither installed: try generic action_view with chooser or fallback to share
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Open with WhatsApp / WhatsApp Business"))
            } catch (e: Exception) {
                openSmsComposer(context, phone, message)
            }
        }
    }

    fun buildAccountSummarySms(
        customerName: String,
        totalGoods: Double,
        totalPaid: Double,
        dues: Double,
        isBrief: Boolean = false,
        context: Context? = null
    ): String {
        val config = context?.let { getGlobalSmsConfig(it) } ?: GlobalSmsConfig()
        val isHindi = config.defaultLanguage == "HINDI"
        val store = config.storeName.ifBlank { "Shree Bartan Store" }
        val footer = config.customStoreFooter.ifBlank { store }

        return if (isBrief || config.defaultStyle == "BRIEF") {
            if (isHindi) {
                "$store: प्रिय $customerName जी, आपका खाता विवरण - कुल खरीदारी: ₹${String.format("%.0f", totalGoods)}, जमा भुगतान: ₹${String.format("%.0f", totalPaid)}, कुल बकाया: ₹${String.format("%.0f", dues)}।"
            } else {
                "$store: Dear $customerName, your account summary - Total Purchased: Rs.${String.format("%.0f", totalGoods)}, Total Paid: Rs.${String.format("%.0f", totalPaid)}, Outstanding Dues Balance: Rs.${String.format("%.0f", dues)}."
            }
        } else {
            if (isHindi) {
                buildString {
                    append("नमस्ते $customerName जी,\n")
                    append("$store की तरफ से आपके खाते का विवरण:\n\n")
                    append("📦 कुल सामान/खरीदारी: ₹${String.format("%.2f", totalGoods)}\n")
                    append("💳 कुल जमा भुगतान (स्क्रैप सहित): ₹${String.format("%.2f", totalPaid)}\n")
                    append("💰 कुल बकाया राशि (Dues): ₹${String.format("%.2f", dues)}\n\n")
                    if (dues > 0) {
                        append("${config.customReminderNote}\n")
                    } else {
                        append("आपका खाता पूरी तरह चुकता है। धन्यवाद!\n")
                    }
                    append("— $footer")
                }
            } else {
                buildString {
                    append("Dear $customerName,\n")
                    append("This is an account statement summary from $store:\n\n")
                    append("📦 Total Goods Purchased: Rs. ${String.format("%.2f", totalGoods)}\n")
                    append("💳 Total Payments & Scrap Credit: Rs. ${String.format("%.2f", totalPaid)}\n")
                    append("💰 Current Dues Balance: Rs. ${String.format("%.2f", dues)}\n\n")
                    if (dues > 0) {
                        append("${config.customReminderNote}\n")
                    } else {
                        append("Your account is clear. Thank you for your business!\n")
                    }
                    append("— $footer")
                }
            }
        }
    }

    fun buildPaymentReminderSms(
        customerName: String,
        dues: Double,
        isBrief: Boolean = false,
        context: Context? = null
    ): String {
        val config = context?.let { getGlobalSmsConfig(it) } ?: GlobalSmsConfig()
        val isHindi = config.defaultLanguage == "HINDI"
        val store = config.storeName.ifBlank { "Shree Bartan Store" }
        val footer = config.customStoreFooter.ifBlank { store }

        return if (isBrief || config.defaultStyle == "BRIEF") {
            if (isHindi) {
                "$store भुगतान सूचना: प्रिय $customerName जी, आपका बकाया बैलेंस ₹${String.format("%.0f", dues)} है। कृपया यथाशीघ्र भुगतान करें।"
            } else {
                "Reminder from $store: Dear $customerName, your pending dues balance is Rs.${String.format("%.0f", dues)}. Please pay at your earliest convenience."
            }
        } else {
            if (isHindi) {
                buildString {
                    append("नमस्ते $customerName जी,\n")
                    append("$store की तरफ से बकाया भुगतान की याददिहानी:\n\n")
                    append("💰 कुल बकाया राशि (Balance): ₹${String.format("%.2f", dues)}\n\n")
                    append("${config.customReminderNote}\n\n")
                    append("— $footer")
                }
            } else {
                buildString {
                    append("Dear $customerName,\n")
                    append("Friendly payment reminder from $store.\n\n")
                    append("💰 Pending Dues Balance: Rs. ${String.format("%.2f", dues)}\n\n")
                    append("${config.customReminderNote}\n\n")
                    append("— $footer")
                }
            }
        }
    }

    fun openSmsComposer(context: Context, phone: String, message: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "No phone number set for customer", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanPhone")
                    putExtra("sms_body", message)
                }
                context.startActivity(sendIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open messaging application", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildSingleTransactionSms(
        customerName: String,
        type: String,
        itemDescription: String,
        quantity: Double,
        unitType: String,
        unitPrice: Double,
        totalAmount: Double,
        updatedDues: Double,
        notes: String,
        context: Context? = null
    ): String {
        val config = context?.let { getGlobalSmsConfig(it) } ?: GlobalSmsConfig()
        val isHindi = config.defaultLanguage == "HINDI"
        val store = config.storeName.ifBlank { "Shree Bartan Store" }
        val footer = config.customStoreFooter.ifBlank { store }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        return when (type) {
            "GOODS_PROVIDED" -> {
                val qtyFormatted = if (quantity > 0) {
                    val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
                    "$q $unitType @ ₹${String.format("%.0f", unitPrice)}"
                } else ""

                if (isHindi) {
                    buildString {
                        append("नमस्ते $customerName जी,\n")
                        append("$store में खरीदारी के लिए धन्यवाद!\n\n")
                        append("📦 नया बिल / सामान विवरण:\n")
                        append("सामान: $itemDescription\n")
                        if (qtyFormatted.isNotBlank()) append("मात्रा/रेट: $qtyFormatted\n")
                        append("कुल बिल राशि: ₹${String.format("%.2f", totalAmount)}\n")
                        if (notes.isNotBlank()) append("नोट: $notes\n")
                        append("दिनांक: $dateStr\n\n")
                        append("💰 अब कुल बकाया राशि: ₹${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                } else {
                    buildString {
                        append("Dear $customerName,\n")
                        append("Thank you for your order at $store!\n\n")
                        append("📦 SALE ENTRY DETAILS:\n")
                        append("Item: $itemDescription\n")
                        if (qtyFormatted.isNotBlank()) append("Qty/Rate: $qtyFormatted\n")
                        append("Total Amount: Rs. ${String.format("%.2f", totalAmount)}\n")
                        if (notes.isNotBlank()) append("Notes: $notes\n")
                        append("Date: $dateStr\n\n")
                        append("💰 Current Dues Balance: Rs. ${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                }
            }
            "PAYMENT_DEPOSIT" -> {
                if (isHindi) {
                    buildString {
                        append("नमस्ते $customerName जी,\n")
                        append("भुगतान प्राप्त हुआ, धन्यवाद!\n\n")
                        append("💳 जमा राशि विवरण:\n")
                        append("प्राप्त राशि: ₹${String.format("%.2f", totalAmount)}\n")
                        append("विवरण: $itemDescription\n")
                        if (notes.isNotBlank()) append("नोट: $notes\n")
                        append("दिनांक: $dateStr\n\n")
                        append("💰 शेष बकाया राशि: ₹${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                } else {
                    buildString {
                        append("Dear $customerName,\n")
                        append("Payment received with thanks!\n\n")
                        append("💳 PAYMENT DETAILS:\n")
                        append("Amount Received: Rs. ${String.format("%.2f", totalAmount)}\n")
                        append("Description: $itemDescription\n")
                        if (notes.isNotBlank()) append("Ref/Notes: $notes\n")
                        append("Date: $dateStr\n\n")
                        append("💰 Remaining Dues Balance: Rs. ${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                }
            }
            "GOODS_RETURNED" -> {
                if (isHindi) {
                    buildString {
                        append("नमस्ते $customerName जी,\n")
                        append("सामान वापसी दर्ज की गई:\n\n")
                        append("❌ वापसी विवरण:\n")
                        append("सामान: $itemDescription\n")
                        append("घटाई गई राशि: ₹${String.format("%.2f", totalAmount)}\n")
                        if (notes.isNotBlank()) append("कारण/नोट: $notes\n")
                        append("दिनांक: $dateStr\n\n")
                        append("💰 कुल बकाया राशि: ₹${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                } else {
                    buildString {
                        append("Dear $customerName,\n")
                        append("Item return processed:\n\n")
                        append("❌ RETURN DETAILS:\n")
                        append("Item: $itemDescription\n")
                        append("Deducted Amount: -Rs. ${String.format("%.2f", totalAmount)}\n")
                        if (notes.isNotBlank()) append("Reason/Notes: $notes\n")
                        append("Date: $dateStr\n\n")
                        append("💰 Updated Dues Balance: Rs. ${String.format("%.2f", updatedDues)}\n\n")
                        append("— $footer")
                    }
                }
            }
            else -> {
                "Transaction for $customerName: $itemDescription (Rs. ${String.format("%.2f", totalAmount)}). Balance: Rs. ${String.format("%.2f", updatedDues)}"
            }
        }
    }

    fun buildMultipleItemsTransactionSms(
        customerName: String,
        itemsList: List<TransactionEntity>,
        updatedDues: Double,
        notes: String,
        context: Context? = null
    ): String {
        val config = context?.let { getGlobalSmsConfig(it) } ?: GlobalSmsConfig()
        val isHindi = config.defaultLanguage == "HINDI"
        val store = config.storeName.ifBlank { "Shree Bartan Store" }
        val footer = config.customStoreFooter.ifBlank { store }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val totalBill = itemsList.sumOf { it.totalAmount }

        val itemsSummary = itemsList.joinToString("\n") { item ->
            val q = if (item.quantityDouble > 0) {
                if (item.quantityDouble % 1.0 == 0.0) item.quantityDouble.toInt().toString() else String.format("%.1f", item.quantityDouble)
            } else item.quantity.toString()
            "• ${item.itemDescription} ($q ${item.unitType}) = ₹${String.format("%.2f", item.totalAmount)}"
        }

        return if (isHindi) {
            buildString {
                append("नमस्ते $customerName जी,\n")
                append("$store में खरीदारी के लिए धन्यवाद!\n\n")
                append("📦 बिल सामान विवरण (${itemsList.size} आइटम्स):\n")
                append(itemsSummary)
                append("\n\nकुल बिल राशि: ₹${String.format("%.2f", totalBill)}\n")
                if (notes.isNotBlank()) append("नोट: $notes\n")
                append("दिनांक: $dateStr\n\n")
                append("💰 अब कुल बकाया राशि: ₹${String.format("%.2f", updatedDues)}\n\n")
                append("— $footer")
            }
        } else {
            buildString {
                append("Dear $customerName,\n")
                append("Thank you for purchasing at $store!\n\n")
                append("📦 BILL ITEM DETAILS (${itemsList.size} Items):\n")
                append(itemsSummary)
                append("\n\nTotal Bill Amount: Rs. ${String.format("%.2f", totalBill)}\n")
                if (notes.isNotBlank()) append("Notes: $notes\n")
                append("Date: $dateStr\n\n")
                append("💰 Current Dues Balance: Rs. ${String.format("%.2f", updatedDues)}\n\n")
                append("— $footer")
            }
        }
    }
}

