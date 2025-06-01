package com.example.biometricvotingapp.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.nio.charset.StandardCharsets // For String to ByteArray and vice-versa

object SecurityUtil {

    private const val TAG = "SecurityUtil"

    private const val KEY_ALIAS = "biometric_voting_key_v1" // Added _v1 for potential versioning
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun generateSecretKey(): SecretKey {
        Log.d(TAG, "Generating new secret key with alias: $KEY_ALIAS")
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true) // Require user authentication (biometric) to use the key
            .setKeySize(256) // Explicitly set key size

        // For API 30+ (Android R+), invalidate key on new biometric enrollment.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyGenParameterSpecBuilder.setInvalidatedByBiometricEnrollment(true)
        }
        // For API 28+ (Android P+), can require user confirmation for each use (not used here)
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        //     keyGenParameterSpecBuilder.setUserConfirmationRequired(true)
        // }

        keyGenerator.init(keyGenParameterSpecBuilder.build())
        return keyGenerator.generateKey().also {
            Log.i(TAG, "New secret key generated and stored in Keystore successfully.")
        }
    }

    fun getOrCreateSecretKey(): SecretKey? {
        return try {
            keyStore.getKey(KEY_ALIAS, null)?.let { key ->
                if (key is SecretKey) {
                    Log.d(TAG, "Existing secret key found with alias: $KEY_ALIAS")
                    key
                } else {
                    Log.w(TAG, "Key found with alias $KEY_ALIAS, but it's not a SecretKey. Generating new one.")
                    keyStore.deleteEntry(KEY_ALIAS) // Remove incorrect key type
                    generateSecretKey()
                }
            } ?: generateSecretKey()
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Secret key was permanently invalidated (e.g., biometrics changed). Deleting and generating a new one.", e)
            try {
                keyStore.deleteEntry(KEY_ALIAS)
            } catch (deleteEx: Exception) {
                Log.e(TAG, "Failed to delete invalidated key: $KEY_ALIAS", deleteEx)
            }
            generateSecretKey() // Generate a new key after invalidation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating secret key: ${e.message}", e)
            null
        }
    }


    fun getCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }

    fun getCryptoObjectForEncryption(): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey()
            if (secretKey == null) {
                Log.e(TAG, "Failed to get or create secret key for encryption.")
                return null
            }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            Log.d(TAG, "CryptoObject for encryption created successfully.")
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Failed to initialize cipher for encryption due to key invalidation. A new key might be needed.", e)
            // Attempt to delete the invalidated key so a new one can be generated on next attempt
            try {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.i(TAG, "Invalidated key $KEY_ALIAS deleted. User may need to retry operation.")
            } catch (deleteEx: Exception) {
                Log.e(TAG, "Failed to delete invalidated key $KEY_ALIAS after cipher init failure.", deleteEx)
            }
            null
        }
        catch (e: Exception) {
            Log.e(TAG, "Error creating crypto object for encryption: ${e.message}", e)
            null
        }
    }

    fun getCryptoObjectForDecryption(iv: ByteArray): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey() // Key should already exist and be valid
            if (secretKey == null) {
                Log.e(TAG, "Failed to get secret key for decryption.")
                return null
            }
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            Log.d(TAG, "CryptoObject for decryption created successfully.")
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Failed to initialize cipher for decryption due to key invalidation. Data might be permanently undecryptable with this key.", e)
            // Consider implications: if key is invalidated, old data encrypted with it cannot be decrypted.
            null
        }
        catch (e: Exception) {
            Log.e(TAG, "Error creating crypto object for decryption: ${e.message}", e)
            null
        }
    }

    fun encryptData(data: String, cryptoObject: BiometricPrompt.CryptoObject): Pair<ByteArray, ByteArray>? {
        return try {
            val cipher = cryptoObject.cipher ?: run {
                Log.e(TAG, "Cipher is null in provided CryptoObject for encryption.")
                return null
            }
            val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            val iv = cipher.iv // IV is generated by the cipher during init for ENCRYPT_MODE (CBC)
            if (iv == null) {
                Log.e(TAG, "IV is null after encryption. This should not happen with CBC mode.")
                return null
            }
            Log.d(TAG, "Data encrypted successfully. IV size: ${iv.size}, Encrypted data size: ${encryptedData.size}")
            Pair(iv, encryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting data: ${e.message}", e)
            null
        }
    }

    fun decryptData(iv: ByteArray, encryptedData: ByteArray, cryptoObject: BiometricPrompt.CryptoObject): String? {
        return try {
            val cipher = cryptoObject.cipher ?: run {
                Log.e(TAG, "Cipher is null in provided CryptoObject for decryption.")
                return null
            }
            // The cipher within cryptoObject for decryption should have been initialized with this IV already.
            val decryptedData = cipher.doFinal(encryptedData)
            Log.d(TAG, "Data decrypted successfully.")
            String(decryptedData, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting data: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes the secret key from the Keystore.
     * Useful if the key is known to be compromised or for resetting.
     * Returns true if deletion was successful or key didn't exist, false otherwise.
     */
    fun deleteSecretKey(): Boolean {
        Log.w(TAG, "Attempting to delete secret key: $KEY_ALIAS")
        return try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.i(TAG, "Secret key $KEY_ALIAS deleted successfully.")
            } else {
                Log.i(TAG, "Secret key $KEY_ALIAS did not exist, no action taken.")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting secret key $KEY_ALIAS: ${e.message}", e)
            false
        }
    }
}
