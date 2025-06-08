package com.example.biometricvotingapp.core.common // Updated package

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom

/**
 * SecureSaltProvider.kt
 *
 * Purpose: Manages the generation, secure storage, and retrieval of a cryptographic salt
 * used in the anonymized ID generation process.
 *
 * Storage Method:
 * - Uses Android's EncryptedSharedPreferences for storing the salt.
 * - EncryptedSharedPreferences is configured to use a MasterKey from the Android Keystore,
 *   providing strong encryption for the salt at rest.
 *
 * Dependencies (conceptual - ensure these are in build.gradle.kts):
 * - androidx.security:security-crypto:1.1.0-alpha06 (or latest)
 */
object SecureSaltProvider {

    private const val TAG = "SecureSaltProvider"

    private const val ENCRYPTED_PREFS_FILE_NAME = "biometric_app_secure_salt_prefs"
    private const val KEY_ALIAS_SALT_PREFS = "biometric_app_salt_master_key_alias"
    private const val PREF_KEY_ANONYMIZATION_SALT = "anonymization_salt_b64"
    private const val SALT_SIZE_BYTES = 16 // 128 bits, a common size for salts

    private var cachedSalt: ByteArray? = null

    @Synchronized
    fun getSalt(context: Context): ByteArray? {
        if (cachedSalt != null) {
            return cachedSalt
        }

        try {
            val masterKey = getOrCreateMasterKey(context)
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val base64Salt = sharedPreferences.getString(PREF_KEY_ANONYMIZATION_SALT, null)

            if (base64Salt == null) {
                Log.i(TAG, "No salt found. Generating and storing a new one.")
                val newSalt = generateNewSalt()
                sharedPreferences.edit()
                    .putString(PREF_KEY_ANONYMIZATION_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP))
                    .apply()
                cachedSalt = newSalt
                Log.i(TAG, "New salt generated and stored successfully.")
                return newSalt
            } else {
                Log.d(TAG, "Existing salt retrieved from secure storage.")
                cachedSalt = Base64.decode(base64Salt, Base64.NO_WRAP)
                return cachedSalt
            }
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Security error while getting/generating salt: ${e.message}", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error while getting/generating salt: ${e.message}", e)
        }
        return null // Return null if any error occurs
    }

    private fun generateNewSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        Log.d(TAG, "Generated new salt with ${SALT_SIZE_BYTES} bytes.")
        return salt
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrCreateMasterKey(context: Context): MasterKey {
        // Defines the specification for the MasterKey
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS_SALT_PREFS, // Alias for the key in Android Keystore
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256) // AES-256
            .build()

        return MasterKey.Builder(context, KEY_ALIAS_SALT_PREFS)
            .setKeyGenParameterSpec(spec)
            .build()
    }

    /**
     * For testing or specific scenarios where clearing the salt might be needed.
     * Use with extreme caution.
     */
    @Synchronized
    fun clearSaltForTesting(context: Context) {
        try {
            Log.w(TAG, "Attempting to clear stored salt for testing purposes.")
            val masterKey = getOrCreateMasterKey(context) // MasterKey needed to open EncryptedSharedPreferences
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().remove(PREF_KEY_ANONYMIZATION_SALT).apply()
            cachedSalt = null
            Log.i(TAG, "Stored salt cleared successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing salt for testing: ${e.message}", e)
        }
    }
}
