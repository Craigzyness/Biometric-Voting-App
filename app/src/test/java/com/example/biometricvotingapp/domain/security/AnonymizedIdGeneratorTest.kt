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

// Mockable placeholder for SecureSaltProvider
object SecureSaltProvider {
    fun getSalt(context: Context): ByteArray? = byteArrayOf() // Default behavior for mocking
}

// Mockable placeholder for StableIdentifierProvider
object StableIdentifierProvider {
    fun getStableIdentifier(): String? = "" // Default behavior for mocking
}

class AnonymizedIdGeneratorTest {

    private lateinit var mockContext: Context
    private lateinit var mockAuthResult: BiometricPrompt.AuthenticationResult

    private val testSalt = "TestSalt1234567890".toByteArray() // Must be of adequate length if there are internal checks, though not visible here
    private val testStableId = "TestStableIdentifierUUID"

    @Before
    fun setUp() {
        mockContext = mockk<Context>()
        mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>() // Not directly used by current ID gen logic but part of method signature

        // Mock the provider objects
        mockkObject(SecureSaltProvider)
        mockkObject(StableIdentifierProvider)
    }

    @After
    fun tearDown() {
        unmockkAll() // Important to clean up mocks, especially for objects/static mocks
    }

    @Test
    fun `generate returns non-null SHA-256 hex string on success`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        val anonymizedId = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)

        assertNotNull("Generated ID should not be null", anonymizedId)
        assertTrue("Generated ID should be a hex string of 64 chars for SHA-256", anonymizedId!!.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun `generate returns same ID for same salt and stable ID`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        val id1 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)
        val id2 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult) // Call again with same mocks

        assertEquals("Generated IDs should be the same for the same input", id1, id2)
    }

    @Test
    fun `generate returns different IDs for different salts`() {
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        every { SecureSaltProvider.getSalt(mockContext) } returns "Salt1".toByteArray()
        val id1 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)

        every { SecureSaltProvider.getSalt(mockContext) } returns "Salt2".toByteArray()
        val id2 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals("Generated IDs should be different for different salts", id1, id2)
    }

    @Test
    fun `generate returns different IDs for different stable IDs`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt

        every { StableIdentifierProvider.getStableIdentifier() } returns "StableID1"
        val id1 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)

        every { StableIdentifierProvider.getStableIdentifier() } returns "StableID2"
        val id2 = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals("Generated IDs should be different for different stable IDs", id1, id2)
    }


    @Test
    fun `generate returns null if salt provider returns null`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns null
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        val anonymizedId = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)
        assertNull("Generated ID should be null if salt is null", anonymizedId)
    }

    @Test
    fun `generate returns null if stable ID provider returns null`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt
        every { StableIdentifierProvider.getStableIdentifier() } returns null

        val anonymizedId = AnonymizedIdGenerator.generate(mockContext, mockAuthResult)
        assertNull("Generated ID should be null if stable ID is null", anonymizedId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns non-null re-derived ID if components exist`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        // Expected ID based on the known testSalt and testStableId
        val combined = testStableId.toByteArray() + testSalt
        val expectedHashBytes = MessageDigest.getInstance("SHA-256").digest(combined)
        val expectedId = expectedHashBytes.joinToString("") { "%02x".format(it) }

        val registeredId = AnonymizedIdGenerator.getRegisteredAnonymizedId(mockContext)

        assertNotNull("Registered ID should not be null if components exist", registeredId)
        assertEquals("Registered ID should match expected re-derived ID", expectedId, registeredId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns null if salt is missing`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns null
        every { StableIdentifierProvider.getStableIdentifier() } returns testStableId

        val registeredId = AnonymizedIdGenerator.getRegisteredAnonymizedId(mockContext)
        assertNull("Registered ID should be null if salt is missing", registeredId)
    }

    @Test
    fun `getRegisteredAnonymizedId returns null if stable ID is missing`() {
        every { SecureSaltProvider.getSalt(mockContext) } returns testSalt
        every { StableIdentifierProvider.getStableIdentifier() } returns null

        val registeredId = AnonymizedIdGenerator.getRegisteredAnonymizedId(mockContext)
        assertNull("Registered ID should be null if stable ID is missing", registeredId)
    }
}
