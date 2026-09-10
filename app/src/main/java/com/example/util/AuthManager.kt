package com.example.util

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class BiometricStatus {
    object Available : BiometricStatus()
    data class Unavailable(val reason: String) : BiometricStatus()
}

class AuthManager(private val context: Context) {

    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.Unavailable("डिवाइस में बायोमेट्रिक सेंसर उपलब्ध नहीं है।")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable("बायोमेट्रिक सेंसर अभी अनुपलब्ध है।")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.Unavailable("डिवाइस में फिंगरप्रिंट या पिन/पैटर्न सेट नहीं है। कृपया सेटिंग्स से सेट करें।")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.Unavailable("सुरक्षा अपडेट की आवश्यकता है।")
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.Unavailable("बायोमेट्रिक प्रमाणीकरण समर्थित नहीं है।")
            else -> BiometricStatus.Unavailable("बायोमेट्रिक प्रमाणीकरण स्थिति जांचने में असमर्थ।")
        }
    }

    fun promptBiometricAuth(
        activity: FragmentActivity,
        title: String = "Shri Bartan Bhandar App Lock",
        subtitle: String = "अनलॉक करने के लिए अपना फिंगरप्रिंट या डिवाइस पिन/पैटर्न इस्तेमाल करें",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Real OS-level hardware authentication succeeded
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Authentication rejected, cancelled, or hardware error
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric mismatch
                onError("फिंगरप्रिंट या बायोमेट्रिक मैच नहीं हुआ! कृपया पुनः प्रयास करें।")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }

            val promptInfo = promptInfoBuilder.build()
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("बायोमेट्रिक प्रमाणीकरण त्रुटि: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
