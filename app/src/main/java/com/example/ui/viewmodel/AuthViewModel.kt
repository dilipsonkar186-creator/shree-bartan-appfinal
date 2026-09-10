package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.FirestoreRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isGuestMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isFirestoreSaved: Boolean = false,
    val isOtpSent: Boolean = false,
    val verificationId: String? = null,
    val isBiometricLockEnabled: Boolean = true,
    val isAppUnlocked: Boolean = false
)

class AuthViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    companion object {
        const val AUTHORIZED_OWNER_EMAIL = "dilip.sonkar.186@gmail.com"
    }

    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e("AuthViewModel", "FirebaseAuth.getInstance() error: ${e.message}")
            null
        }
    }

    private val _uiState = MutableStateFlow(
        run {
            val current = try { getAuth()?.currentUser } catch (e: Throwable) { null }
            if (current != null && current.email.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
                AuthUiState(user = current)
            } else {
                if (current != null) {
                    try { getAuth()?.signOut() } catch (_: Throwable) {}
                }
                AuthUiState(user = null)
            }
        }
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        try {
            getAuth()?.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                Log.d("AuthViewModel", "AuthStateListener updated: ${currentUser?.uid} (${currentUser?.email ?: currentUser?.phoneNumber})")
                if (currentUser != null) {
                    if (currentUser.email.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            user = currentUser,
                            isGuestMode = false,
                            isLoading = false
                        )
                        saveUserToFirestore(currentUser)
                    } else {
                        Log.w("AuthViewModel", "Unauthorized user detected: ${currentUser.email}. Signing out immediately.")
                        try { firebaseAuth.signOut() } catch (_: Throwable) {}
                        _uiState.value = AuthUiState(
                            user = null,
                            isGuestMode = false,
                            isLoading = false,
                            errorMessage = "अनधिकृत खाता! केवल अधिकृत मालिक ($AUTHORIZED_OWNER_EMAIL) ही लॉग इन कर सकते हैं।"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        user = null,
                        isGuestMode = false,
                        isLoading = false
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e("AuthViewModel", "Error adding AuthStateListener: ${e.message}", e)
        }
    }

    /**
     * Real Google Sign-In via Credential Manager & Firebase Authentication
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                Log.d("AuthViewModel", "Requesting Google credentials via CredentialManager...")
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("583177365912-2n9a9bt92nnp26l23oqpdgrd1icn0d8s.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authInstance = getAuth()
                    if (authInstance == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Firebase Auth service unavailable"
                        )
                        return@launch
                    }
                    val authResult = authInstance.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser != null) {
                        if (!firebaseUser.email.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
                            Log.w("AuthViewModel", "Unauthorized Google login attempt: ${firebaseUser.email}")
                            authInstance.signOut()
                            _uiState.value = _uiState.value.copy(
                                user = null,
                                isLoading = false,
                                errorMessage = "अनधिकृत Google खाता! केवल $AUTHORIZED_OWNER_EMAIL ही लॉग इन कर सकते हैं।"
                            )
                            return@launch
                        }
                        Log.d("AuthViewModel", "Google Firebase Auth Success: ${firebaseUser.email}")
                        _uiState.value = _uiState.value.copy(
                            user = firebaseUser,
                            isGuestMode = false,
                            isLoading = false,
                            successMessage = "मालिक लॉगिन सफल: ${firebaseUser.email}",
                            errorMessage = null
                        )
                        saveUserToFirestore(firebaseUser)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Firebase user is null after Google sign-in"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unrecognized credential type"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In Exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Google Sign-In failed: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Handles result from GoogleSignIn Intent with full Drive Scopes and Firebase linking.
     */
    fun handleGoogleSignInResult(account: GoogleSignInAccount?, context: Context) {
        if (account == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Google sign-in was cancelled or failed"
            )
            return
        }
        val email = account.email
        if (email == null || !email.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                user = null,
                isLoading = false,
                errorMessage = "अनधिकृत Google खाता! केवल अधिकृत मालिक ($AUTHORIZED_OWNER_EMAIL) ही लॉग इन कर सकते हैं।"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val email = account.email
                if (!email.isNullOrBlank()) {
                    com.example.util.GoogleDriveManager.saveConnectedAccountEmail(context, email)
                }
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authInstance = getAuth()
                    if (authInstance != null) {
                        val authResult = authInstance.signInWithCredential(authCredential).await()
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            Log.d("AuthViewModel", "Google & Drive Auth Success: ${firebaseUser.email}")
                            _uiState.value = _uiState.value.copy(
                                user = firebaseUser,
                                isLoading = false,
                                successMessage = "Logged in & Google Drive Connected: ${firebaseUser.email}",
                                errorMessage = null
                            )
                            saveUserToFirestore(firebaseUser)
                            return@launch
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Google Drive Connected: ${account.email}",
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in handleGoogleSignInResult: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Google Auth Error: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Real Firebase Phone OTP - Step 1: Send OTP code
     */
    fun sendPhoneOtp(activity: Activity, phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid phone number with country code (e.g. +91 9876543210)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val authInstance = getAuth()
                if (authInstance == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Firebase Auth service is unavailable on this device."
                    )
                    return@launch
                }
                Log.d("AuthViewModel", "Sending Phone OTP to $cleanPhone")
                val options = PhoneAuthOptions.newBuilder(authInstance)
                    .setPhoneNumber(cleanPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            Log.d("AuthViewModel", "Phone verification completed automatically")
                            viewModelScope.launch {
                                try {
                                    authInstance.signInWithCredential(credential).await()
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        isOtpSent = false,
                                        successMessage = "Phone verification completed!"
                                    )
                                } catch (e: Exception) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        errorMessage = "Sign-in failed: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Log.e("AuthViewModel", "Phone verification failed: ${e.message}", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "OTP request failed: ${e.localizedMessage}"
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            Log.d("AuthViewModel", "OTP code sent to $cleanPhone")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isOtpSent = true,
                                verificationId = verificationId,
                                successMessage = "OTP sent to $cleanPhone. Enter 6-digit code below."
                            )
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in sendPhoneOtp: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to send OTP: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Real Firebase Phone OTP - Step 2: Verify OTP code
     */
    fun verifyPhoneOtp(otpCode: String) {
        val verificationId = _uiState.value.verificationId
        val cleanOtp = otpCode.trim()

        if (verificationId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Verification ID missing. Please resend OTP.")
            return
        }
        if (cleanOtp.isBlank() || cleanOtp.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter complete 6-digit OTP code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val authInstance = getAuth()
                if (authInstance == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Firebase Auth service unavailable"
                    )
                    return@launch
                }
                val credential = PhoneAuthProvider.getCredential(verificationId, cleanOtp)
                val authResult = authInstance.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        isOtpSent = false,
                        verificationId = null,
                        successMessage = "Phone number verified successfully!"
                    )
                    saveUserToFirestore(user)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to sign in with OTP"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "verifyPhoneOtp Exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid OTP code: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Owner-Only Email & Password Authentication strictly restricted to AUTHORIZED_OWNER_EMAIL.
     */
    fun signInWithEmailPassword(email: String, pass: String, isRegistering: Boolean = false) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (!cleanEmail.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "अनाधिकृत ईमेल! केवल अधिकृत मालिक ($AUTHORIZED_OWNER_EMAIL) ही इस ऐप में प्रवेश कर सकते हैं।"
            )
            return
        }
        if (cleanPass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "कृपया अपना पासवर्ड दर्ज करें।")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val authInstance = getAuth()
                if (authInstance == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Firebase Auth service unavailable"
                    )
                    return@launch
                }

                try {
                    val authResult = authInstance.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                    val user = authResult.user

                    if (user != null && user.email.equals(AUTHORIZED_OWNER_EMAIL, ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            user = user,
                            isGuestMode = false,
                            isLoading = false,
                            successMessage = "मालिक लॉगिन सफल! (Owner Login Successful)"
                        )
                        saveUserToFirestore(user)
                    } else {
                        authInstance.signOut()
                        _uiState.value = _uiState.value.copy(
                            user = null,
                            isLoading = false,
                            errorMessage = "अनधिकृत खाता! केवल $AUTHORIZED_OWNER_EMAIL को अनुमति है।"
                        )
                    }
                } catch (e: FirebaseAuthInvalidUserException) {
                    // If the owner account hasn't been created yet on Firebase Auth, initialize it
                    if (cleanPass.length < 6) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।"
                        )
                        return@launch
                    }
                    Log.d("AuthViewModel", "Creating initial owner account for $cleanEmail")
                    val createResult = authInstance.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                    val newUser = createResult.user
                    if (newUser != null) {
                        _uiState.value = _uiState.value.copy(
                            user = newUser,
                            isGuestMode = false,
                            isLoading = false,
                            successMessage = "मालिक खाता सफलतापूर्वक सेट हुआ और लॉगिन किया गया!"
                        )
                        saveUserToFirestore(newUser)
                    }
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("AuthViewModel", "Invalid credentials / wrong password: ${e.errorCode}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "गलत पासवर्ड! कृपया सही पासवर्ड दर्ज करें या नीचे 'पासवर्ड भूल गए?' पर क्लिक करें।"
                )
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.e("AuthViewModel", "Email already in use: ${e.errorCode}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "खाता पहले से मौजूद है। कृपया अपना सही पासवर्ड दर्ज करें।"
                )
            } catch (e: FirebaseAuthWeakPasswordException) {
                Log.e("AuthViewModel", "Weak password: ${e.reason}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "पासवर्ड बहुत छोटा है। कम से कम 6 अक्षर दर्ज करें।"
                )
            } catch (e: FirebaseAuthException) {
                Log.e("AuthViewModel", "FirebaseAuthException: errorCode=${e.errorCode}, msg=${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseFirebaseAuthError(e.errorCode, e.localizedMessage)
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email auth exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseFirebaseAuthError(null, e.localizedMessage)
                )
            }
        }
    }

    private fun parseFirebaseAuthError(errorCode: String?, rawMessage: String?): String {
        val code = errorCode?.lowercase() ?: ""
        val message = rawMessage?.lowercase() ?: ""

        return when {
            code.contains("wrong-password") || code.contains("error_wrong_password") || message.contains("wrong-password") || message.contains("invalid password") ->
                "गलत पासवर्ड! कृपया सही पासवर्ड दर्ज करें या नीचे 'पासवर्ड भूल गए?' पर क्लिक करें।"
            code.contains("user-not-found") || code.contains("error_user_not_found") || message.contains("user-not-found") ->
                "खाता नहीं मिला। अधिकृत मालिक के पासवर्ड से पहली बार लॉगिन करने पर यह स्वतः सक्रिय हो जाएगा।"
            code.contains("invalid-email") || code.contains("error_invalid_email") || message.contains("invalid-email") ->
                "कृपया सही ईमेल पता दर्ज करें।"
            code.contains("weak-password") || code.contains("error_weak_password") || message.contains("weak-password") ->
                "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।"
            code.contains("too-many-requests") || message.contains("too-many-requests") ->
                "बहुत अधिक असफल प्रयास। कृपया कुछ समय बाद पुनः प्रयास करें।"
            else -> rawMessage ?: "प्रमाणीकरण विफल। कृपया अपना पासवर्ड जांचें और पुनः प्रयास करें।"
        }
    }

    /**
     * Real Firebase Password Reset Email sent directly to AUTHORIZED_OWNER_EMAIL.
     */
    fun sendPasswordResetEmail(onResult: (Boolean, String) -> Unit) {
        val targetEmail = AUTHORIZED_OWNER_EMAIL
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val authInstance = getAuth()
                if (authInstance == null) {
                    val msg = "Firebase Auth सेवा उपलब्ध नहीं है"
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                    onResult(false, msg)
                    return@launch
                }
                authInstance.sendPasswordResetEmail(targetEmail).await()
                val msg = "पासवर्ड रीसेट लिंक $targetEmail पर सफलतापूर्वक भेज दिया गया है। कृपया अपना Gmail खोलें और नया पासवर्ड सेट करें।"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = msg
                )
                onResult(true, msg)
            } catch (e: Exception) {
                val msg = "पासवर्ड रीसेट ईमेल भेजने में विफल: ${e.localizedMessage}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = msg
                )
                onResult(false, msg)
            }
        }
    }

    /**
     * Guest mode is permanently disabled for owner security.
     */
    fun signInAnonymously() {
        _uiState.value = _uiState.value.copy(
            isGuestMode = false,
            errorMessage = "गेस्ट मोड बंद है। केवल अधिकृत मालिक ($AUTHORIZED_OWNER_EMAIL) पासवर्ड द्वारा लॉगिन कर सकते हैं।"
        )
    }

    private fun saveUserToFirestore(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                val result = firestoreRepository.saveUserData(
                    uid = user.uid,
                    name = user.displayName ?: user.email?.substringBefore("@") ?: user.phoneNumber ?: "User",
                    email = user.email ?: user.phoneNumber ?: "anonymous",
                    photoUrl = user.photoUrl?.toString()
                )
                if (result.isSuccess) {
                    Log.d("AuthViewModel", "User ${user.uid} saved to Firestore")
                    _uiState.value = _uiState.value.copy(isFirestoreSaved = true)
                } else {
                    Log.w("AuthViewModel", "Firestore user save notice: ${result.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(isFirestoreSaved = false)
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Firestore save exception: ${e.message}")
                _uiState.value = _uiState.value.copy(isFirestoreSaved = false)
            }
        }
    }

    fun resetOtpState() {
        _uiState.value = _uiState.value.copy(isOtpSent = false, verificationId = null)
    }

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error signing out", e)
        }
        _uiState.value = AuthUiState(
            user = null,
            isGuestMode = false,
            isLoading = false,
            errorMessage = null,
            successMessage = null,
            isFirestoreSaved = false,
            isOtpSent = false,
            verificationId = null
        )
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _uiState.value = _uiState.value.copy(isAppUnlocked = unlocked)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricLockEnabled = enabled)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
