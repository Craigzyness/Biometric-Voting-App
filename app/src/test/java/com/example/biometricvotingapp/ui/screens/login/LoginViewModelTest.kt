package com.example.biometricvotingapp.ui.screens.login

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase // Import the use case
import com.example.biometricvotingapp.domain.usecase.UserNotRegisteredException // Import custom exception
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper // For testing error mapping
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
    private lateinit var mockLoginUserUseCase: LoginUserUseCase // Mock the use case

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockLoginUserUseCase = mockk() // Create a mock for LoginUserUseCase

        viewModel = LoginViewModel(mockApplication, mockLoginUserUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
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
        assertTrue("Should emit ShowBiometricPrompt event", events.any { it is LoginViewEvent.ShowBiometricPrompt })

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with use case success leads to Success state and NavigateToElectionList event`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockLoggedInUserId = "test-logged-in-id-123"

        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(mockLoggedInUserId)

        val events = mutableListOf<LoginViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        coVerify { mockLoginUserUseCase.invoke() }

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Success, was $finalState", finalState is LoginUiState.Success)
        assertEquals(mockLoggedInUserId, (finalState as LoginUiState.Success).anonymizedId)

        val emittedEvent = events.firstOrNull { it is LoginViewEvent.NavigateToElectionList }
        assertNotNull("Should emit NavigateToElectionList event", emittedEvent)
        assertEquals(mockLoggedInUserId, (emittedEvent as LoginViewEvent.NavigateToElectionList).anonymizedId)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with UserNotRegisteredException from use case leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val errorMessage = "User not registered test message."
        val useCaseException = UserNotRegisteredException(errorMessage)

        coEvery { mockLoginUserUseCase.invoke() } returns Result.failure(useCaseException)

        // val events = mutableListOf<LoginViewEvent>() // Not expecting navigation event here
        // val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }


        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        coVerify { mockLoginUserUseCase.invoke() }

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals(errorMessage, (finalState as LoginUiState.Error).message)

        // Check if NavigateToRegistration event is emitted, if LoginViewModel is designed to do so
        // Based on current LoginViewModel: it sets error state but doesn't automatically emit NavigateToRegistration.
        // This would be a UI concern or a different event type if needed.
        // For now, assert no navigation to election list.
        // assertTrue("No NavigateToElectionList event should be emitted", events.none { it is LoginViewEvent.NavigateToElectionList })

        // job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with generic exception from use case leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val errorMessage = "Some generic error"
        val useCaseException = Exception(errorMessage)

        coEvery { mockLoginUserUseCase.invoke() } returns Result.failure(useCaseException)

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        coVerify { mockLoginUserUseCase.invoke() }

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals("Login failed: $errorMessage", (finalState as LoginUiState.Error).message)
    }


    @Test
    fun `onBiometricAuthenticationError uses BiometricErrorMapper and sets Error state`() {
        val errorCode = BiometricPrompt.ERROR_LOCKOUT
        val errString = "Biometric lockout"
        val expectedMappedMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)

        viewModel.onBiometricAuthenticationError(errorCode, errString)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is LoginUiState.Error)
        assertEquals(expectedMappedMessage, (finalState as LoginUiState.Error).message)
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
        viewModel.onBiometricAuthenticationFailed()
        assertNotEquals(LoginUiState.Idle, viewModel.uiState.value)

        viewModel.resetStateToIdle()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}
