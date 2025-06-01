package com.example.biometricvotingapp.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL // Optional fallback
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity // Required for BiometricPrompt

/**
 * BiometricManager.kt
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

    /**
     * Checks if biometric authentication (specifically BIOMETRIC_STRONG) is available on the device.
     */
    fun canAuthenticateWithBiometrics(): BiometricAvailabilityStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometric authentication (STRONG) is available.")
                BiometricAvailabilityStatus.AVAILABLE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e(TAG, "No biometric features available on this device.")
                BiometricAvailabilityStatus.NO_HARDWARE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e(TAG, "Biometric features are currently unavailable.")
                BiometricAvailabilityStatus.TEMPORARILY_UNAVAILABLE
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e(TAG, "The user hasn't associated any biometric credentials with their account.")
                // NOTE: For registration, this state might mean the user needs to enroll first.
                // For login, this means they can't use biometrics.
                BiometricAvailabilityStatus.NONE_ENROLLED
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.e(TAG, "Biometric features are unavailable due to a required security update.")
                BiometricAvailabilityStatus.SECURITY_UPDATE_REQUIRED
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                 Log.e(TAG, "Biometric features are unsupported.")
                BiometricAvailabilityStatus.UNSUPPORTED
            }
            else -> {
                Log.e(TAG, "Biometric availability check returned an unknown status.")
                BiometricAvailabilityStatus.UNKNOWN
            }
        }
    }

    /**
     * Creates BiometricPrompt.PromptInfo for user registration.
     */
    private fun createRegistrationPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Register with Fingerprint")
            .setSubtitle("Confirm fingerprint to create your secure, anonymized voting ID")
            .setDescription("Place your finger on the sensor. Your fingerprint data stays on this device and is used to generate an anonymized ID.")
            .setNegativeButtonText("Cancel") // User can cancel the biometric prompt
            .setConfirmationRequired(true) // Requires user to explicitly confirm after successful scan (e.g. tap a button in the prompt) - recommended for sensitive operations
            // .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) // Example if allowing device credentials as fallback
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    /**
     * Creates BiometricPrompt.PromptInfo for user login.
     */
    private fun createLoginPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Fingerprint")
            .setSubtitle("Verify your identity to access the app")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }


    /**
     * Shows the biometric prompt for registration.
     *
     * @param activity The FragmentActivity that hosts the prompt.
     * @param onSuccess Callback invoked when authentication is successful. The AuthenticationResult is passed.
     * @param onError Callback invoked when an unrecoverable error occurs. An error message string is passed.
     * @param onFailed Callback invoked when authentication fails (e.g., fingerprint not recognized).
     */
    fun promptForRegistration(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val promptInfo = createRegistrationPromptInfo()
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Registration Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Registration Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Registration Biometric Authentication Failed. Fingerprint not recognized.")
                    onFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Shows the biometric prompt for login.
     * (Similar to promptForRegistration, but uses createLoginPromptInfo)
     */
    fun promptForLogin(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val promptInfo = createLoginPromptInfo()
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Login Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Login Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Login Biometric Authentication Failed. Fingerprint not recognized.")
                    onFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Creates BiometricPrompt.PromptInfo for vote confirmation.
     */
    private fun createVoteConfirmationPromptInfo(electionTitle: String, selectedOption: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Your Vote")
            .setSubtitle("Election: $electionTitle")
            .setDescription("Confirm with your fingerprint to cast your vote for: \"$selectedOption\". This action is final for this session.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true) // Recommended for sensitive actions
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    /**
     * Shows the biometric prompt for vote confirmation.
     *
     * @param activity The FragmentActivity that hosts the prompt.
     * @param electionTitle The title of the election.
     * @param selectedOption The option the user has selected.
     * @param onSuccess Callback invoked when authentication is successful.
     * @param onError Callback invoked when an unrecoverable error occurs.
     * @param onFailed Callback invoked when authentication fails.
     */
    fun promptForVoteConfirmation(
        activity: FragmentActivity,
        electionTitle: String,
        selectedOption: String,
        cryptoObject: BiometricPrompt.CryptoObject?, // New parameter
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val promptInfo = createVoteConfirmationPromptInfo(electionTitle, selectedOption)
        val biometricPrompt = BiometricPrompt(activity, mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Vote Confirmation Biometric Authentication Succeeded!")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Vote Confirmation Biometric Authentication Error: $errorCode - $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Vote Confirmation Biometric Authentication Failed.")
                    onFailed()
                }
            })

        if (cryptoObject != null) {
            Log.d(TAG, "Authenticating with CryptoObject for vote confirmation.")
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            // This case should ideally not be hit if cryptoObject is always prepared by ViewModel.
            // If it can be null, then the backend needs to handle votes without proof,
            // or this path should be treated as an error.
            Log.w(TAG, "Authenticating for vote confirmation WITHOUT CryptoObject. This may be unintended.")
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
