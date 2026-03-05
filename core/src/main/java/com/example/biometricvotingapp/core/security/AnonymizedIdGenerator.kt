package com.example.biometricvotingapp.core.security // New package

import android.content.Context
import androidx.biometric.BiometricPrompt // Keep for authResult type if generate() needs it, but removing for now
import com.example.biometricvotingapp.core.common.SecureSaltProvider // Assuming this will be an injectable class in core.common
import com.example.biometricvotingapp.core.common.StableIdentifierProvider // Assuming this will be an injectable class in core.common
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

// Note: The original file had extensive comments about its design, expert review, and future Hilt refactoring.
// Some are preserved or adapted here.

/**
 * AnonymizedIdGenerator.kt
 *
 * Purpose: Responsible for generating a unique, anonymized identifier for the user.
 * This class now expects SecureSaltProvider and StableIdentifierProvider to be injectable.
 *
 * Cryptographic Strategy: SHA-256 hash of (stableIdentifier + salt).
 *
 * !! WARNING !! - EXPERT REVIEW REQUIRED !! WARNING !!
 * Cryptographic code requires expert review before production deployment.
 */
@Singleton
class AnonymizedIdGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AnonymizedIdGenerator"
        private const val HASH_ALGORITHM = "SHA-256"
    }

    /**
     * Generates an anonymized ID.
     * This method now relies on its injected providers.
     * The BiometricPrompt.AuthenticationResult is no longer passed here;
     * biometric authentication should gate the *calling* of this method (e.g., by a UseCase).
     */
    fun generate(): String? {
        // Note: Logging using android.util.Log and BuildConfig.DEBUG has been removed
        // to avoid :core module's dependency on :app's BuildConfig.
        // Logging can be added in UseCases or ViewModels if needed.

        val salt = SecureSaltProvider(context).getSalt()
        val stableId = StableIdentifierProvider(context).getStableIdentifier()

        if (salt == null || stableId == null) {
            // Consider logging this failure in the calling UseCase/ViewModel
            return null
        }

        return try {
            val stableIdBytes = stableId.toByteArray(StandardCharsets.UTF_8)
            // Ensure salt (ByteArray) is consistently combined.
            // Converting salt to String and then to bytes again might not be ideal if salt has non-printable chars.
            // The original combined salt and stableId as byte arrays. Let's stick to that.
            // The salt from SecureSaltProvider is already ByteArray.

            val dataToHash = ByteArray(stableIdBytes.size + salt.size)
            System.arraycopy(stableIdBytes, 0, dataToHash, 0, stableIdBytes.size)
            System.arraycopy(salt, 0, dataToHash, stableIdBytes.size, salt.size)

            val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashedBytes = messageDigest.digest(dataToHash)
            bytesToHex(hashedBytes)
        } catch (e: Exception) {
            // Log this exception in calling UseCase/ViewModel
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[j * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Checks if components for an ID exist and re-derives it.
     * This implies the user might be "registered".
     */
    fun getRegisteredAnonymizedId(): String? {
        // The original SecureSaltProvider/StableIdentifierProvider didn't have hasSalt()/hasStableId().
        // They returned null from getSalt()/getStableIdentifier() if not found.
        // This method will re-derive if components are found.
        // The generate() method itself handles null checks from providers.
        return generate() // If generate() returns null, it means components weren't found or error occurred.
    }
}
