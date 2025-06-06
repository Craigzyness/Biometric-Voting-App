package com.example.biometricvotingapp.ui.screens.login

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: LoginViewModel
    private lateinit var mockApplication: Application
    // AnonymizedIdGenerator is an object, will be mocked using mockkObject

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockkObject(AnonymizedIdGenerator) // Mock the object

        viewModel = LoginViewModel(mockApplication, AnonymizedIdGenerator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(AnonymizedIdGenerator) // Unmock the object
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onLoginClicked transitions to Loading and emits ShowBiometricPrompt event`() = runTest {
        val events = mutableListOf<LoginViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onLoginClicked()

        assertEquals(LoginUiState.Loading, viewModel.uiState.value)
        assertTrue("Should emit ShowBiometricPrompt event", events.contains(LoginViewEvent.ShowBiometricPrompt))

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with valid ID retrieval leads to NavigateToElectionList event`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockAnonymizedId = "test-anonymized-id-123"

        every { AnonymizedIdGenerator.getRegisteredAnonymizedId(mockApplication) } returns mockAnonymizedId

        val events = mutableListOf<LoginViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        // State should ideally return to Idle or a specific success state before navigation event
        // Current LoginViewModel sets it to Idle.
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        val emittedEvent = events.firstOrNull { it is LoginViewEvent.NavigateToElectionList }
        assertNotNull("Should emit NavigateToElectionList event", emittedEvent)
        assertEquals(mockAnonymizedId, (emittedEvent as LoginViewEvent.NavigateToElectionList).anonymizedId)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with null ID retrieval leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()

        every { AnonymizedIdGenerator.getRegisteredAnonymizedId(mockApplication) } returns null // Simulate ID not found/registered

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals(
            "Biometric recognized, but app registration not found. Please register if you haven't.",
            (finalState as LoginUiState.Error).message
        )
    }

    @Test
    fun `onBiometricAuthenticationError (with code and string) sets Error state`() {
        val errorCode = BiometricPrompt.ERROR_LOCKOUT
        val errString = "Biometric lockout"
        viewModel.onBiometricAuthenticationError(errorCode, errString)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals("Login Error: $errString", (finalState as LoginUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationError (with string message) sets Error state`() {
        val errMessage = "A custom error message for biometrics"
        viewModel.onBiometricAuthenticationError(errMessage)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals(errMessage, (finalState as LoginUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationFailed sets Error state`() {
        viewModel.onBiometricAuthenticationFailed()

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals(
            "Login Failed: Fingerprint not recognized. Please try again.",
            (finalState as LoginUiState.Error).message
        )
    }

    @Test
    fun `resetStateToIdle sets state to Idle`() {
        // Set to a non-idle state first
        viewModel.onBiometricAuthenticationFailed() // This sets state to Error
        assertNotEquals(LoginUiState.Idle, viewModel.uiState.value)

        viewModel.resetStateToIdle()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}
