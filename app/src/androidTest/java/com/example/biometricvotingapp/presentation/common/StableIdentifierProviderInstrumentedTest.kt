package com.example.biometricvotingapp.presentation.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.core.common.StableIdentifierProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StableIdentifierProviderInstrumentedTest {

    private lateinit var context: Context
    // The actual StableIdentifierProvider instance from the main source set will be tested.
    // Ensure the import path is correct.
    private lateinit var stableIdentifierProvider: StableIdentifierProvider

    // Define constants from StableIdentifierProvider for test verification
    private companion object {
        // These constants must match those in the actual StableIdentifierProvider implementation
        private const val PREFS_FILENAME = "biometric_app_stable_id_prefs"
        private const val ID_KEY = "stable_app_installation_id" // Corrected key from StableIdentifierProvider
        private const val MASTER_KEY_ALIAS = "biometric_app_stable_id_master_key_alias" // Actual alias from StableIdentifierProvider
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearSharedPreferences()
        // Initialize the actual provider from the main source set
        stableIdentifierProvider = StableIdentifierProvider(context)
    }

    @After
    fun tearDown() {
        clearSharedPreferences()
        // Reset the cached ID in the object to ensure test isolation
        stableIdentifierProvider.clearStableIdForTesting()
    }

    private fun clearSharedPreferences() {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE).edit().clear().apply()
        // Attempt to delete the backing file for EncryptedSharedPreferences for a cleaner slate.
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

    private fun isValidUUID(uuidString: String?): Boolean {
        if (uuidString == null) return false
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    @Test
    fun getStableId_shouldGenerateAndStoreUUID_whenCalledFirstTime() {
        // Act
        val id1 = stableIdentifierProvider.getStableIdentifier()

        // Assert
        assertThat(id1).isNotNull()
        assertThat(isValidUUID(id1)).isTrue()

        // Verify directly from EncryptedSharedPreferences
        val masterKeySpec = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build() // This should retrieve the existing key or create one if not present

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKeySpec, // Use the same master key spec as the provider
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val storedId = sharedPreferences.getString(ID_KEY, null)
        assertThat(storedId).isNotNull()
        assertThat(storedId).isEqualTo(id1)
    }

    @Test
    fun getStableId_shouldReturnExistingUUID_whenCalledMultipleTimes() {
        // Act
        val id1 = stableIdentifierProvider.getStableIdentifier()
        // Call again; StableIdentifierProvider uses a cached ID internally after first retrieval/generation
        val id2 = stableIdentifierProvider.getStableIdentifier()

        // Assert
        assertThat(id1).isNotNull()
        assertThat(isValidUUID(id1)).isTrue()
        assertThat(id2).isNotNull()
        assertThat(isValidUUID(id2)).isTrue()
        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun getStableId_returnsNewUUID_ifSharedPreferencesAreClearedAndCacheIsBypassed() {
        // First provider and ID
        val id1 = stableIdentifierProvider.getStableIdentifier()
        assertThat(id1).isNotNull()
        assertThat(isValidUUID(id1)).isTrue()

        // Clear preferences and reset the provider's cache to simulate a fresh state
        clearSharedPreferences()
        stableIdentifierProvider.clearStableIdForTesting() // Resets cachedStableId

        val id2 = stableIdentifierProvider.getStableIdentifier() // Should generate a new one

        assertThat(id2).isNotNull()
        assertThat(isValidUUID(id2)).isTrue()
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun idIsRandomlyGeneratedUUID_acrossDifferentInstancesWithClearedStorage() {
        // Test that subsequent generations produce different UUIDs if storage is cleared
        clearSharedPreferences()
        stableIdentifierProvider.clearStableIdForTesting()
        val idA = stableIdentifierProvider.getStableIdentifier()

        clearSharedPreferences()
        stableIdentifierProvider.clearStableIdForTesting()
        val idB = stableIdentifierProvider.getStableIdentifier()

        clearSharedPreferences()
        stableIdentifierProvider.clearStableIdForTesting()
        val idC = stableIdentifierProvider.getStableIdentifier()

        assertThat(idA).isNotNull()
        assertThat(isValidUUID(idA)).isTrue()
        assertThat(idB).isNotNull()
        assertThat(isValidUUID(idB)).isTrue()
        assertThat(idC).isNotNull()
        assertThat(isValidUUID(idC)).isTrue()

        assertThat(idA).isNotEqualTo(idB)
        assertThat(idA).isNotEqualTo(idC)
        assertThat(idB).isNotEqualTo(idC)
    }
}
