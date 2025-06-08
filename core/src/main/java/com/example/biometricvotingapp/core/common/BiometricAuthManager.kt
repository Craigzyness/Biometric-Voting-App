package com.example.biometricvotingapp.core.common // Updated package

import android.content.Context
import android.os.Build // Keep for potential future use, though not directly used in this version
import android.util.Log
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL // Optional fallback
import com.example.biometricvotingapp.BuildConfig // Assuming BuildConfig is accessible from :core
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricAuthManager.kt
 *
 * Purpose: Handles biometric authentication logic using Android's BiometricPrompt.
 * Adapted from the initial project documentation.
 */
class BiometricAuthManager(private val context: Context) {

    private val biometricManager = androidx.biometric.BiometricManager.from(context)
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    companion object {
        private const val TAG = "BiometricAuthManager"
    }

    fun canAuthenticateWithBiometrics(): BiometricAvailabilityStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Biometric authentication (STRONG) is available.")
                BiometricAvailabilityStatus.AVAILABLE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "No biometric features available on this device.")
                BiometricAvailabilityStatus.NO_HARDWARE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Biometric features are currently unavailable.")
                BiometricAvailabilityStatus.TEMPORARILY_UNAVAILABLE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "The user hasn't associated any biometric credentials with their account.")
                BiometricAvailabilityStatus.NONE_ENROLLED
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Biometric features are unavailable due to a required security update.")
                BiometricAvailabilityStatus.SECURITY_UPDATE_REQUIRED
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                 if (BuildConfig.DEBUG) Log.e(TAG, "Biometric features are unsupported.")
                BiometricAvailabilityStatus.UNSUPPORTED
            }
            else -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Biometric availability check returned an unknown status.")
                BiometricAvailabilityStatus.UNKNOWN
            }
        }
    }

    private fun createRegistrationPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Register with Fingerprint")
            .setSubtitle("Confirm fingerprint to create your secure, anonymized voting ID")
            .setDescription("Place your finger on the sensor. Your fingerprint data stays on this device and is used to generate an anonymized ID.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    private fun createLoginPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Fingerprint")
            .setSubtitle("Verify your identity to access the app")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    fun promptForRegistration(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit, // Changed from (Int, CharSequence) to just String for simplicity if mapper is used by caller
        onFailed: () -> Unit
    ) {
        val promptInfo = createRegistrationPromptInfo()
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Registration Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (BuildConfig.DEBUG) Log.e(TAG, "Registration Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString()) // Pass CharSequence as String
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (BuildConfig.DEBUG) Log.w(TAG, "Registration Biometric Authentication Failed. Fingerprint not recognized.")
                    onFailed()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    fun promptForLogin(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit, // Changed from (Int, CharSequence) to just String
        onFailed: () -> Unit
    ) {
        val promptInfo = createLoginPromptInfo()
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Login Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (BuildConfig.DEBUG) Log.e(TAG, "Login Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString()) // Pass CharSequence as String
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (BuildConfig.DEBUG) Log.w(TAG, "Login Biometric Authentication Failed. Fingerprint not recognized.")
                    onFailed()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    private fun createVoteConfirmationPromptInfo(electionTitle: String, selectedOption: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Your Vote")
            .setSubtitle("Election: $electionTitle")
            .setDescription("Confirm with your fingerprint to cast your vote for: \"$selectedOption\". This action is final for this session.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    fun promptForVoteConfirmation(
        activity: FragmentActivity,
        electionTitle: String,
        selectedOption: String,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit, // Changed from (Int, CharSequence) to just String
        onFailed: () -> Unit
    ) {
        val promptInfo = createVoteConfirmationPromptInfo(electionTitle, selectedOption)
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Vote Confirmation Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (BuildConfig.DEBUG) Log.e(TAG, "Vote Confirmation Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString()) // Pass CharSequence as String
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (BuildConfig.DEBUG) Log.w(TAG, "Vote Confirmation Biometric Authentication Failed.")
                    onFailed()
                }
            })

        if (cryptoObject != null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Authenticating with CryptoObject for vote confirmation.")
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            if (BuildConfig.DEBUG) Log.w(TAG, "Authenticating for vote confirmation WITHOUT CryptoObject. This may be unintended.")
            biometricPrompt.authenticate(promptInfo)
        }
    }
}

enum class BiometricAvailabilityStatus {
    AVAILABLE,
    NO_HARDWARE,
    TEMPORARILY_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}
