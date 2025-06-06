package com.example.biometricvotingapp.domain.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.UUID

/**
 * StableIdentifierProvider.kt
 *
 * Purpose: Manages the creation, secure storage, and retrieval of a stable, unique
 * identifier for this app installation. This ID is part of the input for generating
 * the anonymized voter ID.
 *
 * Storage Method:
 * - Uses Android's EncryptedSharedPreferences for storing the stable ID.
 * - EncryptedSharedPreferences is configured to use a MasterKey from the Android Keystore.
 */
object StableIdentifierProvider {

    private const val TAG = "StableIdentifierProv"

    private const val ENCRYPTED_PREFS_FILE_NAME = "biometric_app_stable_id_prefs"
    private const val KEY_ALIAS_STABLE_ID_PREFS = "biometric_app_stable_id_master_key_alias"
    private const val PREF_KEY_STABLE_INSTALL_ID = "stable_install_id_uuid"

    private var cachedStableId: String? = null

    @Synchronized
    fun getStableIdentifier(context: Context): String? {
        if (cachedStableId != null) {
            return cachedStableId
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

            var stableId = sharedPreferences.getString(PREF_KEY_STABLE_INSTALL_ID, null)

            if (stableId == null) {
                Log.i(TAG, "No stable identifier found. Generating and storing a new one.")
                stableId = UUID.randomUUID().toString()
                sharedPreferences.edit()
                    .putString(PREF_KEY_STABLE_INSTALL_ID, stableId)
                    .apply()
                Log.i(TAG, "New stable identifier generated and stored successfully: $stableId")
            } else {
                Log.d(TAG, "Existing stable identifier retrieved: $stableId")
            }
            cachedStableId = stableId
            return stableId
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Security error while getting/generating stable ID: ${e.message}", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error while getting/generating stable ID: ${e.message}", e)
        }
        return null // Return null if any error occurs
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrCreateMasterKey(context: Context): MasterKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS_STABLE_ID_PREFS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return MasterKey.Builder(context, KEY_ALIAS_STABLE_ID_PREFS)
            .setKeyGenParameterSpec(spec)
            .build()
    }

    /**
     * For testing or specific scenarios where clearing the ID might be needed.
     * Use with extreme caution.
     */
    @Synchronized
    fun clearStableIdForTesting(context: Context) {
        try {
            Log.w(TAG, "Attempting to clear stored stable ID for testing purposes.")
            val masterKey = getOrCreateMasterKey(context)
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().remove(PREF_KEY_STABLE_INSTALL_ID).apply()
            cachedStableId = null
            Log.i(TAG, "Stored stable ID cleared successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing stable ID for testing: ${e.message}", e)
        }
    }
}
