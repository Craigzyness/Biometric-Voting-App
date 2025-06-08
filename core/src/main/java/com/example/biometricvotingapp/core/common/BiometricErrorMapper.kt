package com.example.biometricvotingapp.core.common // Updated package

import android.content.Context
import androidx.biometric.BiometricPrompt

// For now, using hardcoded strings.
// TODO: Refactor to use R.string resources for localization, passing Context.
object BiometricErrorMapper {

    fun mapBiometricErrorCodeToString(
        // context: Context,
        errorCode: Int,
        errString: CharSequence?
    ): String {
        val defaultError = errString?.toString() ?: "An unknown biometric error occurred."
        return when (errorCode) {
            BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                "Biometric authentication is not available on this device due to hardware issues."
            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                "No biometrics enrolled. Please add a fingerprint or other biometric credential in your device settings."
            BiometricPrompt.ERROR_HW_NOT_PRESENT ->
                "This device does not have the required biometric hardware."
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
                "No device credential (PIN, pattern, or password) is set up. Biometric authentication cannot be used without one for security reasons."
            BiometricPrompt.ERROR_USER_CANCELED ->
                "Biometric authentication was canceled by the user."
            BiometricPrompt.ERROR_TIMEOUT ->
                "Biometric authentication timed out. Please try again."
            BiometricPrompt.ERROR_LOCKOUT ->
                "Too many attempts. Biometric authentication is temporarily locked. Try again later."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                "Too many attempts. Biometric authentication is permanently locked. Device credential fallback may be required, or you may need to wait longer or reconfigure device security."
            BiometricPrompt.ERROR_CANCELED ->
                "Biometric authentication was canceled."
            BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                "Biometric authentication was dismissed."
            else ->
                "Biometric authentication error: $defaultError (Code: $errorCode)"
        }
    }
}
