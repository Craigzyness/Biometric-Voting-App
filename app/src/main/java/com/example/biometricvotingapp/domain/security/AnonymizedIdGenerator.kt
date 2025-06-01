package com.example.biometricvotingapp.domain.security

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt // Keep for the authResult parameter type
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * AnonymizedIdGenerator.kt
 *
 * Purpose: Responsible for generating a unique, anonymized identifier for the user
 * based on successful biometric authentication, using a securely stored salt and a
 * stable app installation identifier.
 *
 * Cryptographic Strategy:
 * - Hash Algorithm: SHA-256
 * - Salt: A unique, per-installation salt is obtained from `SecureSaltProvider`.
 * - Stable Identifier: A unique, per-installation UUID is obtained from `StableIdentifierProvider`.
 * - Input to Hash: The stable identifier is concatenated with the salt, and the result is hashed.
 *
 * !! WARNING !! - EXPERT REVIEW REQUIRED !! WARNING !!
 * While this implementation aims to follow cryptographic best practices (salting, strong hashing),
 * any security-critical code, especially involving cryptography, MUST be thoroughly reviewed
 * by security experts before use in a production environment.
 * This implementation is for demonstration within the project's development lifecycle.
 * Ensure all aspects align with `docs/security_requirements.md`.
 * !! WARNING !! - EXPERT REVIEW REQUIRED !! WARNING !!
 *
 * Internal Review Notes (Not a substitute for expert review):
 * - Hash Algorithm: SHA-256 is a standard, secure cryptographic hash function.
 * - Salt (`SecureSaltProvider`): The use of a salt is crucial for mitigating pre-computation attacks
 *   (e.g., rainbow tables) on the hashed ID. The security and proper generation/storage of this salt
 *   by `SecureSaltProvider` (implementation not visible here) is critical.
 * - Stable Identifier (`StableIdentifierProvider`): This ensures the ID remains consistent for a given
 *   app installation. Its stability, uniqueness, and resistance to tampering are important.
 *   The method of generating this stable ID by `StableIdentifierProvider` (implementation not visible here)
 *   is a key factor.
 * - Combination: The current method combines the stable identifier and salt, then hashes them:
 *   ID = SHA256(stableIdentifier + salt). This is a common and generally sound pattern for creating
 *   derived identifiers.
 * - Biometric Gatekeeper: Biometric authentication acts as a gatekeeper to allow the generation/use of this
 *   ID, rather than directly deriving the ID from biometric data or using BiometricPrompt.CryptoObject
 *   for this specific ID generation.
 * - Recommendation: The call for expert review remains paramount, especially concerning the actual
 *   implementations of `SecureSaltProvider` and `StableIdentifierProvider`, and the overall
 *   anonymity and security guarantees in the context of the application's threat model.
 *
 * Architectural Consideration (Future):
 * Currently, AnonymizedIdGenerator, SecureSaltProvider, and StableIdentifierProvider are implemented
 * as Kotlin `object` singletons. While this simplifies access for the current MVP, for enhanced
 * testability (especially if AnonymizedIdGenerator itself had more complex internal dependencies
 * or logic needing direct unit testing with different provider behaviors) and for architectural
 * consistency if a DI framework (like Hilt) is broadly adopted, refactoring these could be considered:
 *  1. Change `object AnonymizedIdGenerator` to `class AnonymizedIdGenerator(...)`.
 *  2. Define interfaces for `SecureSaltProvider` and `StableIdentifierProvider` (if not already done for testing).
 *  3. Change their `object` implementations to `class ...Provider(...) : ISecureSaltProvider` etc.
 *  4. Provide these instances via DI where needed (e.g., injected into ViewModels or services
 *     that require ID generation or access to these providers).
 * For the current MVP, the direct object usage is functional and well-contained.
 */
object AnonymizedIdGenerator {

    private const val TAG = "AnonymizedIdGenerator"

    /**
     * Generates an anonymized ID using SHA-256, a per-installation salt, and a stable app ID.
     *
     * This function should be called after a successful biometric authentication.
     * The `authResult` parameter is present to align with the BiometricPrompt flow but is not
     * directly used in this specific ID generation logic (which relies on stored salt & stable ID).
     * In more advanced scenarios (e.g., using CryptoObject), authResult would be critical.
     *
     * @param context The application context, needed to access providers.
     * @param authResult The result from a successful biometric authentication. (Currently informational for this impl).
     * @return A hex-encoded SHA-256 hash as the anonymized ID, or null if generation fails.
     */
    fun generate(context: Context, authResult: BiometricPrompt.AuthenticationResult?): String? {
        Log.d(TAG, "Attempting to generate secure anonymized ID...")

        val salt = SecureSaltProvider.getSalt(context)
        if (salt == null) {
            Log.e(TAG, "Failed to get or generate salt. Cannot generate anonymized ID.")
            return null
        }

        val stableInstallId = StableIdentifierProvider.getStableIdentifier(context)
        if (stableInstallId == null) {
            Log.e(TAG, "Failed to get or generate stable app installation ID. Cannot generate anonymized ID.")
            return null
        }

        try {
            // Combine stable ID and salt. Order matters and should be consistent.
            // Converting stable ID to bytes first, then appending salt.
            val stableIdBytes = stableInstallId.toByteArray(StandardCharsets.UTF_8)
            val dataToHash = ByteArray(stableIdBytes.size + salt.size)

            System.arraycopy(stableIdBytes, 0, dataToHash, 0, stableIdBytes.size)
            System.arraycopy(salt, 0, dataToHash, stableIdBytes.size, salt.size)

            val messageDigest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = messageDigest.digest(dataToHash)

            // Convert byte array to hex string
            val hexString = bytesToHexString(hashedBytes)

            Log.i(TAG, "Successfully generated SHA-256 based Anonymized ID (first 8 chars): ${hexString.take(8)}...")
            // Log.d(TAG, "Full Anonymized ID: $hexString") // Avoid logging full ID in production

            // !! IMPORTANT !!
            // This generated ID should now be securely stored or used as needed.
            // The `authResult` could be used here if the ID needed to be tied more directly
            // to the specific biometric authentication event (e.g., signing this ID with a key
            // unlocked by biometrics via authResult.cryptoObject). For this strategy,
            // the biometric auth acts as a gatekeeper to *allow* ID generation/use.

            return hexString

        } catch (e: Exception) { // Catch any generic exception during crypto operations
            Log.e(TAG, "Error during SHA-256 ID generation: ${e.message}", e)
            return null
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Attempts to retrieve the components (stable ID, salt) and re-derive the
     * anonymized ID. This can be used to check if a user has "registered"
     * in the sense that these components exist.
     *
     * This is not retrieving a stored *anonymized ID* directly, but rather
     * confirming the underlying components for its generation are present.
     *
     * @param context The application context.
     * @return The re-derived anonymized ID as a hex string if components exist,
     *         otherwise null.
     */
    fun getRegisteredAnonymizedId(context: Context): String? {
        Log.d(TAG, "Attempting to get/re-derive registered anonymized ID...")

        val salt = SecureSaltProvider.getSalt(context)
        if (salt == null) {
            Log.i(TAG, "Salt not found. User likely not (fully) registered.")
            return null
        }

        val stableInstallId = StableIdentifierProvider.getStableIdentifier(context)
        if (stableInstallId == null) {
            Log.i(TAG, "Stable app installation ID not found. User likely not (fully) registered.")
            return null
        }

        // If we reached here, both components exist. Proceed to re-derive the ID.
        try {
            val stableIdBytes = stableInstallId.toByteArray(StandardCharsets.UTF_8)
            val dataToHash = ByteArray(stableIdBytes.size + salt.size)

            System.arraycopy(stableIdBytes, 0, dataToHash, 0, stableIdBytes.size)
            System.arraycopy(salt, 0, dataToHash, stableIdBytes.size, salt.size)

            val messageDigest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = messageDigest.digest(dataToHash)

            val hexString = bytesToHexString(hashedBytes) // Relies on bytesToHexString being defined correctly above this
            Log.d(TAG, "Successfully re-derived anonymized ID for registered user.")
            return hexString

        } catch (e: Exception) {
            Log.e(TAG, "Error during SHA-256 ID re-derivation: ${e.message}", e)
            return null
        }
    }

    /**
     * TODO: Consider further enhancements:
     *  - If `authResult.cryptoObject` is available and configured with a signing key,
     *    this generated `hexString` (or the `stableInstallId`) could be signed by it
     *    to further bind the ID to the biometric authentication event. This would make
     *    the ID usable as a proof of biometric presence for that specific action.
     *  - Evaluate if the `stableInstallId` needs more protection against specific OS/backup
     *    behaviors if it's intended to be absolutely non-resettable by easier means than
     *    app uninstall/reinstall or data clear.
     */
}
