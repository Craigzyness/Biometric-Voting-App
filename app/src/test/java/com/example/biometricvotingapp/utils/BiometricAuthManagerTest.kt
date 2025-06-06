package com.example.biometricvotingapp.utils

import android.content.Context
import androidx.biometric.BiometricManager as SystemBiometricManager // Alias to avoid conflict
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE) // Using manifest = Config.NONE for simple context needs
class BiometricAuthManagerTest {

    private lateinit var context: Context
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var mockSystemBiometricManager: SystemBiometricManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkStatic(SystemBiometricManager::class) // Mock the static 'from' method
        mockSystemBiometricManager = mockk<SystemBiometricManager>()
        every { SystemBiometricManager.from(context) } returns mockSystemBiometricManager
        biometricAuthManager = BiometricAuthManager(context)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns AVAILABLE when BIOMETRIC_SUCCESS`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_SUCCESS
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.AVAILABLE, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns NO_HARDWARE for BIOMETRIC_ERROR_NO_HARDWARE`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.NO_HARDWARE, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns TEMPORARILY_UNAVAILABLE for BIOMETRIC_ERROR_HW_UNAVAILABLE`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.TEMPORARILY_UNAVAILABLE, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns NONE_ENROLLED for BIOMETRIC_ERROR_NONE_ENROLLED`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.NONE_ENROLLED, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns SECURITY_UPDATE_REQUIRED for BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.SECURITY_UPDATE_REQUIRED, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns UNSUPPORTED for BIOMETRIC_ERROR_UNSUPPORTED`() {
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns SystemBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.UNSUPPORTED, status)
    }

    @Test
    fun `canAuthenticateWithBiometrics returns UNKNOWN for other error codes`() {
        // Using a generic error code not explicitly handled to test the 'else' branch
        val unknownErrorCode = -100 // Some arbitrary unhandled error code
        every { mockSystemBiometricManager.canAuthenticate(SystemBiometricManager.Authenticators.BIOMETRIC_STRONG) } returns unknownErrorCode
        val status = biometricAuthManager.canAuthenticateWithBiometrics()
        assertEquals(BiometricAvailabilityStatus.UNKNOWN, status)
    }

    @After
    fun tearDown() {
        // No specific tear down needed for these tests with static mocking if managed by MockK framework
        // but good practice if unmockkStatic is needed for other test suites.
    }
}
