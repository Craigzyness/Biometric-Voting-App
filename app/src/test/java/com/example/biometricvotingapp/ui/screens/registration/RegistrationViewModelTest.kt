package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
// Removed Context import as Application is used
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
// Removed RegistrationResponse and VoterDto imports as they are now encapsulated by the UseCase
// Removed VotingRepository import as it's replaced by the UseCase for registration logic
// Removed AnonymizedIdGenerator import from here as it's a dependency of the UseCase, not directly the ViewModel
import com.example.biometricvotingapp.domain.usecase.RegisterVoterUseCase // Import the use case
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper // Ensure this is imported if used by VM error paths
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
class RegistrationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: RegistrationViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockRegisterVoterUseCase: RegisterVoterUseCase // Mock the use case

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockRegisterVoterUseCase = mockk() // Create a mock for RegisterVoterUseCase

        // ViewModel is now instantiated with the mocked use case
        viewModel = RegistrationViewModel(mockApplication, mockRegisterVoterUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks() // Clear all MockK mocks, including the use case
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(RegistrationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onRegisterClicked transitions to AwaitingBiometrics and emits ShowBiometricPrompt event`() = runTest {
        val events = mutableListOf<RegistrationViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.onRegisterClicked()

        assertEquals(RegistrationUiState.AwaitingBiometrics, viewModel.uiState.value)
        assertTrue("Should emit ShowBiometricPrompt event", events.any { it is RegistrationViewEvent.ShowBiometricPrompt })

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationError uses BiometricErrorMapper and sets Error state`() {
        val errorCode = BiometricPrompt.ERROR_LOCKOUT
        val errString = "Test Biometric Error"
        // We don't need to mock BiometricErrorMapper as it's an object with a static method.
        // We test that the ViewModel uses it correctly and the state reflects the mapped message.
        val expectedMappedMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)

        viewModel.onBiometricAuthenticationError(errorCode, errString)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals(expectedMappedMessage, (finalState as RegistrationUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationFailed sets Error state with specific message`() {
        viewModel.onBiometricAuthenticationFailed()
        val expectedMessage = "Biometric authentication failed. Fingerprint not recognized."
        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals(expectedMessage, (finalState as RegistrationUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationSuccess calls use case and on success leads to Success state and Navigate event`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockGeneratedId = "test-anonymized-id-123"

        // Mock the behavior of the use case
        coEvery { mockRegisterVoterUseCase.invoke() } returns Result.success(mockGeneratedId)

        val events = mutableListOf<RegistrationViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        coVerify { mockRegisterVoterUseCase.invoke() } // Verify the use case was called

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Success, was $finalState", finalState is RegistrationUiState.Success)
        // The success message in VM is now generic "Registration successful! You can now log in."
        assertEquals("Registration successful! You can now log in.", (finalState as RegistrationUiState.Success).message)


        val emittedEvent = events.firstOrNull { it is RegistrationViewEvent.NavigateToElectionList }
        assertNotNull("Should emit NavigateToElectionList event", emittedEvent)
        assertEquals(mockGeneratedId, (emittedEvent as RegistrationViewEvent.NavigateToElectionList).generatedId)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess calls use case and on failure leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val useCaseError = Exception("UseCase failed: Network error")

        // Mock the behavior of the use case to return failure
        coEvery { mockRegisterVoterUseCase.invoke() } returns Result.failure(useCaseError)

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        coVerify { mockRegisterVoterUseCase.invoke() } // Verify the use case was called

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals("Registration failed: ${useCaseError.message}", (finalState as RegistrationUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationSuccess with specific 409 error from use case shows specific message`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        // Simulate an error message that implies "already registered" or contains "409"
        val useCaseErrorAlreadyRegistered = Exception("HTTP 409: Conflict - voter already registered")

        coEvery { mockRegisterVoterUseCase.invoke() } returns Result.failure(useCaseErrorAlreadyRegistered)

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        coVerify { mockRegisterVoterUseCase.invoke() }

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals("This identity is already registered. Please try logging in.", (finalState as RegistrationUiState.Error).message)
    }
}
