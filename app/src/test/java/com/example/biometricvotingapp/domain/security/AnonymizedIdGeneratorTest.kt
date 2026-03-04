package com.example.biometricvotingapp.domain.security

import android.content.Context
import androidx.biometric.BiometricPrompt
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

import com.example.biometricvotingapp.core.common.SecureSaltProvider
import com.example.biometricvotingapp.core.common.StableIdentifierProvider
import com.example.biometricvotingapp.core.security.AnonymizedIdGenerator

class AnonymizedIdGeneratorTest {

    private lateinit var mockSecureSaltProvider: SecureSaltProvider
    private lateinit var mockStableIdentifierProvider: StableIdentifierProvider
    private lateinit var anonymizedIdGenerator: AnonymizedIdGenerator

    private val testSalt = "TestSalt1234567890".toByteArray() // Must be of adequate length if there are internal checks, though not visible here
    private val testStableId = "TestStableIdentifierUUID"

    @Before
    fun setUp() {
        mockSecureSaltProvider = mockk<SecureSaltProvider>()
        mockStableIdentifierProvider = mockk<StableIdentifierProvider>()

        anonymizedIdGenerator = AnonymizedIdGenerator(
            secureSaltProvider = mockSecureSaltProvider,
            stableIdentifierProvider = mockStableIdentifierProvider
        )
    }

    @After
    fun tearDown() {
        unmockkAll() // Important to clean up mocks, especially for objects/static mocks
    }

    @Test
    fun `generate returns non-null SHA-256 hex string on success`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        val anonymizedId = anonymizedIdGenerator.generate()

        assertNotNull("Generated ID should not be null", anonymizedId)
        assertTrue("Generated ID should be a hex string of 64 chars for SHA-256", anonymizedId!!.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun `generate returns same ID for same salt and stable ID`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        val id1 = anonymizedIdGenerator.generate()
        val id2 = anonymizedIdGenerator.generate() // Call again with same mocks

        assertEquals("Generated IDs should be the same for the same input", id1, id2)
    }

    @Test
    fun `generate returns different IDs for different salts`() {
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        every { mockSecureSaltProvider.getSalt() } returns "Salt1".toByteArray()
        val id1 = anonymizedIdGenerator.generate()

        every { mockSecureSaltProvider.getSalt() } returns "Salt2".toByteArray()
        val id2 = anonymizedIdGenerator.generate()

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals("Generated IDs should be different for different salts", id1, id2)
    }

    @Test
    fun `generate returns different IDs for different stable IDs`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt

        every { mockStableIdentifierProvider.getStableIdentifier() } returns "StableID1"
        val id1 = anonymizedIdGenerator.generate()

        every { mockStableIdentifierProvider.getStableIdentifier() } returns "StableID2"
        val id2 = anonymizedIdGenerator.generate()

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals("Generated IDs should be different for different stable IDs", id1, id2)
    }


    @Test
    fun `generate returns null if salt provider returns null`() {
        every { mockSecureSaltProvider.getSalt() } returns null
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        val anonymizedId = anonymizedIdGenerator.generate()
        assertNull("Generated ID should be null if salt is null", anonymizedId)
    }

    @Test
    fun `generate returns null if stable ID provider returns null`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt
        every { mockStableIdentifierProvider.getStableIdentifier() } returns null

        val anonymizedId = anonymizedIdGenerator.generate()
        assertNull("Generated ID should be null if stable ID is null", anonymizedId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns non-null re-derived ID if components exist`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        // Expected ID based on the known testSalt and testStableId
        val combined = testStableId.toByteArray() + testSalt
        val expectedHashBytes = MessageDigest.getInstance("SHA-256").digest(combined)
        val expectedId = expectedHashBytes.joinToString("") { "%02x".format(it) }

        val registeredId = anonymizedIdGenerator.getRegisteredAnonymizedId()

        assertNotNull("Registered ID should not be null if components exist", registeredId)
        assertEquals("Registered ID should match expected re-derived ID", expectedId, registeredId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns null if salt is missing`() {
        every { mockSecureSaltProvider.getSalt() } returns null
        every { mockStableIdentifierProvider.getStableIdentifier() } returns testStableId

        val registeredId = anonymizedIdGenerator.getRegisteredAnonymizedId()
        assertNull("Registered ID should be null if salt is missing", registeredId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns null if stable ID is missing`() {
        every { mockSecureSaltProvider.getSalt() } returns testSalt
        every { mockStableIdentifierProvider.getStableIdentifier() } returns null

        val registeredId = anonymizedIdGenerator.getRegisteredAnonymizedId()
        assertNull("Registered ID should be null if stable ID is missing", registeredId)
    }
}
