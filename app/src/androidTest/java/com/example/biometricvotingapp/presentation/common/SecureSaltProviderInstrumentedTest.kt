package com.example.biometricvotingapp.presentation.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File // For clearing EncryptedSharedPreferences backing file if needed

@RunWith(AndroidJUnit4::class)
class SecureSaltProviderInstrumentedTest {

    private lateinit var context: Context
    // The actual SecureSaltProvider instance from the main source set will be tested.
    private lateinit var secureSaltProvider: com.example.biometricvotingapp.core.common.SecureSaltProvider


    // Define constants from SecureSaltProvider for test verification
    private companion object {
        // These constants must match those in the actual SecureSaltProvider implementation
        private const val PREFS_FILENAME = "biometric_app_secure_salt_prefs"
        private const val SALT_KEY = "anonymization_salt_b64" // Actual key from SecureSaltProvider
        private const val MASTER_KEY_ALIAS = "biometric_app_salt_master_key_alias" // Actual alias from SecureSaltProvider
        private const val SALT_SIZE = 16 // Actual salt size from SecureSaltProvider
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearSharedPreferences()
        // Initialize the actual provider from the main source set
        secureSaltProvider = com.example.biometricvotingapp.core.common.SecureSaltProvider(context)
    }

    @After
    fun tearDown() {
        clearSharedPreferences()
        // Reset the cached salt in the object to ensure test isolation if tests run in same process
        secureSaltProvider.clearSaltForTesting()
    }

    private fun clearSharedPreferences() {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE).edit().clear().apply()
        // Attempt to delete the backing file for EncryptedSharedPreferences for a cleaner slate.
        // Note: This path might be slightly different based on Android version or device,
        // but it's a common location.
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "$PREFS_FILENAME.xml")
        val bakFile = File(prefsDir, "$PREFS_FILENAME.bak")
        if (prefsFile.exists()) {
            prefsFile.delete()
        }
        if (bakFile.exists()) {
            bakFile.delete()
        }
    }

    @Test
    fun getSalt_shouldGenerateAndStoreSalt_whenCalledFirstTime() {
        // Act
        val salt1 = secureSaltProvider.getSalt()

        // Assert
        assertThat(salt1).isNotNull()
        assertThat(salt1!!.size).isEqualTo(SALT_SIZE)

        // Verify directly from EncryptedSharedPreferences that it was stored correctly
        val masterKeySpec = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKeySpec, // Use the same master key spec as the provider
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val storedSaltBase64 = sharedPreferences.getString(SALT_KEY, null)
        assertThat(storedSaltBase64).isNotNull()
        val decodedStoredSalt = android.util.Base64.decode(storedSaltBase64, android.util.Base64.NO_WRAP)
        assertThat(decodedStoredSalt).isEqualTo(salt1)
    }

    @Test
    fun getSalt_shouldReturnExistingSalt_whenCalledMultipleTimes() {
        // Act
        val salt1 = secureSaltProvider.getSalt()
        // Call again; SecureSaltProvider uses a cached salt internally after first retrieval/generation
        val salt2 = secureSaltProvider.getSalt()

        // Assert
        assertThat(salt1).isNotNull()
        assertThat(salt2).isNotNull()
        assertThat(salt1!!.size).isEqualTo(SALT_SIZE)
        assertThat(salt2!!.size).isEqualTo(SALT_SIZE)
        assertThat(salt1).isEqualTo(salt2)
    }

    @Test
    fun getSalt_returnsNewSalt_ifSharedPreferencesAreClearedAndCacheIsBypassed() {
        // First provider and salt
        val salt1 = secureSaltProvider.getSalt()
        assertThat(salt1).isNotNull()

        // Clear preferences and reset the provider's cache to simulate a fresh state
        clearSharedPreferences()
        secureSaltProvider.clearSaltForTesting() // Resets cachedSalt

        val salt2 = secureSaltProvider.getSalt() // Should generate a new one

        assertThat(salt2).isNotNull()
        assertThat(salt1.contentEquals(salt2!!)).isFalse()
    }

    @Test
    fun saltIsRandomlyGenerated_acrossDifferentInstancesWithClearedStorage() {
        // Test that subsequent generations produce different salts if storage is cleared
        clearSharedPreferences()
        secureSaltProvider.clearSaltForTesting()
        val saltA = secureSaltProvider.getSalt()

        clearSharedPreferences()
        secureSaltProvider.clearSaltForTesting()
        val saltB = secureSaltProvider.getSalt() // Re-call on the same instance after clearing

        clearSharedPreferences()
        secureSaltProvider.clearSaltForTesting()
        val saltC = secureSaltProvider.getSalt()

        assertThat(saltA).isNotNull()
        assertThat(saltB).isNotNull()
        assertThat(saltC).isNotNull()
        assertThat(saltA!!.size).isEqualTo(SALT_SIZE)
        assertThat(saltB!!.size).isEqualTo(SALT_SIZE)
        assertThat(saltC!!.size).isEqualTo(SALT_SIZE)

        // Check that they are different byte arrays
        assertThat(saltA.contentEquals(saltB!!)).isFalse()
        assertThat(saltA.contentEquals(saltC!!)).isFalse()
        assertThat(saltB.contentEquals(saltC!!)).isFalse()
    }
}
