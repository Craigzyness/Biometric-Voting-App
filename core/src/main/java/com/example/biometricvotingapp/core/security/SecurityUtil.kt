package com.example.biometricvotingapp.core.security // New package

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException // As per prompt's new class
import androidx.biometric.BiometricPrompt
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

// Logging with Timber or android.util.Log would be added here if needed,
// but removed for now to avoid BuildConfig dependency in :core for this refactor.

@Singleton // To be provided as a singleton by Hilt
class SecurityUtil @Inject constructor(@ApplicationContext private val context: Context) {

    // KeyStore is loaded when the instance is created.
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }

    companion object {
        private const val TAG = "SecurityUtil" // For potential logging if re-added
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val BIOMETRIC_VOTING_KEY_ALIAS = "biometric_voting_key_v1" // Keep alias consistent
        private const val AES_KEY_SIZE_BITS = 256
        private const val AES_MODE = "AES/CBC/PKCS7Padding" // KeyProperties.KEY_ALGORITHM_AES + / + KeyProperties.BLOCK_MODE_CBC + / + KeyProperties.ENCRYPTION_PADDING_PKCS7
        // private const val USER_AUTH_TIMEOUT_SECONDS = 30 // Example, not used in current logic
    }

    private fun generateSecretKeyIfNeeded(): SecretKey {
        if (!keyStore.containsAlias(BIOMETRIC_VOTING_KEY_ALIAS)) {
            // Log.d(TAG, "Generating new secret key with alias: $BIOMETRIC_VOTING_KEY_ALIAS") // Example logging
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
            val builder = KeyGenParameterSpec.Builder(
                BIOMETRIC_VOTING_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 // builder.setUserAuthenticationParameters(USER_AUTH_TIMEOUT_SECONDS, KeyGenParameterSpec.AUTH_BIOMETRIC_STRONG)
                 // For auth per use, -1 is typically used.
                 // The original code did not specify a timeout, implying key is usable after auth until invalidation/lock.
                 // Let's keep it consistent with original behavior unless per-use is strictly required.
                 // For per-use on API 30+:
                 // builder.setUserAuthenticationValidityDurationSeconds(-1)
                 // For now, just setUserAuthenticationRequired(true) is the main point from original.
                 builder.setInvalidatedByBiometricEnrollment(true) // This was in original object
            }

            keyGenerator.init(builder.build())
            return keyGenerator.generateKey().also {
                // Log.i(TAG, "New secret key generated and stored successfully.") // Example logging
            }
        }
        // If key exists, retrieve it.
        // This might throw if key is of wrong type, but getKey should handle this.
        return keyStore.getKey(BIOMETRIC_VOTING_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Gets a CryptoObject for encryption operations.
     * The underlying key requires biometric authentication for its use.
     *
     * @throws UserNotAuthenticatedException If the key requires authentication and the user has not
     *         authenticated recently enough (e.g., for keys with timeouts, or if prompt is required per operation).
     * @throws KeyPermanentlyInvalidatedException If the key has been invalidated (e.g., new biometrics enrolled).
     * @throws Exception For other keystore or cipher initialization errors.
     */
    @Throws(UserNotAuthenticatedException::class, KeyPermanentlyInvalidatedException::class, Exception::class)
    fun getCryptoObjectForEncryption(): BiometricPrompt.CryptoObject {
        val secretKey = generateSecretKeyIfNeeded() // Ensures key exists or is created
        val cipher = Cipher.getInstance(AES_MODE)
        // This init call is the one that can throw UserNotAuthenticatedException if key needs auth
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return BiometricPrompt.CryptoObject(cipher)
    }

    /**
     * Encrypts data using the cipher from the provided CryptoObject.
     * Assumes cipher is already initialized for ENCRYPT_MODE (which getCryptoObjectForEncryption does).
     * Returns Pair of (IV, Ciphertext).
     *
     * @param payload The String data to encrypt.
     * @param cipher The Cipher instance, already initialized for encryption (typically from CryptoObject).
     * @return Pair containing the Initialization Vector (IV) and the encrypted ciphertext.
     * @throws Exception if encryption fails.
     */
    @Throws(Exception::class)
    fun encryptData(payload: String, cipher: Cipher): Pair<ByteArray, ByteArray> {
        // IV is generated by the Cipher during init(Cipher.ENCRYPT_MODE, key) for CBC mode.
        // It must be retrieved *after* init and *before* doFinal for some Android versions/providers,
        // or it can be retrieved after doFinal as well. Retrieving it before doFinal is safer.
        // However, the CryptoObject is created with the cipher *after* init.
        // So, the cipher within CryptoObject should already have the IV.
        val iv = cipher.iv
        if (iv == null) {
            // Log.e(TAG, "IV is null after encryption init. This should not happen with CBC mode.")
            throw IllegalStateException("IV cannot be null for CBC encryption after cipher init.")
        }
        val encryptedBytes = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Pair(iv, encryptedBytes)
    }

    // getCryptoObjectForDecryption, decryptData, and deleteSecretKey methods from the original object
    // are not included in this refactored class as per the prompt's new class structure.
    // They would be added here if needed, following a similar pattern.
}
