package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.data.network.dto.RegistrationResponse
import com.example.biometricvotingapp.data.network.dto.VoterDto
import com.example.biometricvotingapp.data.repository.VotingRepository
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
class RegistrationViewModelTest {

    // Rule for running LiveData/ViewModel related tests synchronously
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines
    private val testDispatcher = UnconfinedTestDispatcher() // StandardTestDispatcher could also be used with advanceUntilIdle

    private lateinit var viewModel: RegistrationViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockVotingRepository: VotingRepository
    // AnonymizedIdGenerator is an object, can be used directly or mocked with mockkObject if specific behaviors needed for all tests.
    // For these tests, we pass the real object since its methods are individually mocked per test case.

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockVotingRepository = mockk() // Create a mock for VotingRepository

        // Mock AnonymizedIdGenerator object if its methods are called by all tests in a standard way
        // For per-test mocking, `every { AnonymizedIdGenerator.generate(...) }` is fine.
        mockkObject(AnonymizedIdGenerator) // Ensure it's mockable as an object

        // ViewModel is now instantiated with mocked dependencies
        viewModel = RegistrationViewModel(mockApplication, AnonymizedIdGenerator, mockVotingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(AnonymizedIdGenerator) // Clear the object mock
        // clearAllMocks() // Optionally clear all MockK mocks
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(RegistrationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onRegisterClicked transitions to AwaitingBiometrics and emits ShowBiometricPrompt event`() = runTest {
        // Collect events in a separate coroutine
        val events = mutableListOf<RegistrationViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { // Use UnconfinedTestDispatcher for immediate emission
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.onRegisterClicked()

        assertEquals(RegistrationUiState.AwaitingBiometrics, viewModel.uiState.value)
        assertTrue("Should emit ShowBiometricPrompt event", events.contains(RegistrationViewEvent.ShowBiometricPrompt))

        job.cancel() // Clean up collector
    }

    @Test
    fun `onBiometricAuthenticationError sets Error state`() {
        val errorCode = 123
        val errString = "Test Biometric Error"
        viewModel.onBiometricAuthenticationError(errorCode, errString)
        val expectedMessage = "Error: Biometric Authentication Error $errorCode: $errString"
        assertEquals(RegistrationUiState.Error(expectedMessage), viewModel.uiState.value)
    }

    @Test
    fun `onBiometricAuthenticationFailed sets Error state`() {
        viewModel.onBiometricAuthenticationFailed()
        val expectedMessage = "Error: Biometric Authentication Failed. Fingerprint not recognized."
        assertEquals(RegistrationUiState.Error(expectedMessage), viewModel.uiState.value)
    }

    @Test
    fun `biometric success, ID gen success, repo success leads to Success state and Navigate event`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockGeneratedId = "test-anonymized-id-123"
        val mockVoterDto = VoterDto("db-id", mockGeneratedId, "timestamp", true)
        val mockRepoResponse = RegistrationResponse("Voter registered successfully (mock).", mockVoterDto)

        every { AnonymizedIdGenerator.generate(mockApplication, mockAuthResult) } returns mockGeneratedId

        // We need to mock the repository behavior. Since VM creates it internally,
        // we need a way to control it. The current VM design makes this hard without
        // actual DI or a service locator pattern for VotingRepository.
        // For this test, we'll assume we can make the internal repository behave as needed.
        // This highlights a limitation of not injecting the repository.
        //
        // Workaround: Since VotingRepository(ApiService.instance) is used, if we could mock ApiService.instance
        // before VM instantiation, that would work.
        // Let's assume for the sake of this conceptual test that such mocking is in place for ApiService methods,
        // or that we are actually testing a refactored VM that takes VotingRepository as a param.
        // For now, I'll write the test as if repository calls can be directly mocked,
        // acknowledging the current VM would need refactoring for this to be clean.

        // Let's try to mock the ApiService.instance used by the repository.
        // This is tricky as ApiService.instance is a singleton's lazy property.
        // A cleaner test would be if VotingRepository was injected into RegistrationViewModel.
        // Given the current structure, this test will be more conceptual for the repository part.

        coEvery {
            // This is a placeholder for how one might mock the repository call if it were injectable
            // In reality, we'd mock the ApiService call that the *actual* repository inside the VM makes.
            // For this test, let's imagine:
            // val mockRepo = mockk<VotingRepository>()
            // coEvery { mockRepo.registerVoter(mockGeneratedId) } returns Result.success(mockRepoResponse)
            // And assume viewModel uses this mockRepo.
            // Since it doesn't, this specific coEvery won't work directly on the VM's internal repo.
            // We'll test the state transitions assuming the internal repo call could be controlled.
            // This means the test primarily verifies the VM's reaction to Results.

            // To make it runnable with current VM:
            // We'd have to mock ApiService.instance.registerVoter which is complex due to singleton.
            // Let's focus on the VM's logic *given* a certain repository outcome.
            // We will simulate the repository call and outcome directly in the test for now.

            // Assume this call happens and returns success:
            // votingRepository.registerVoter(generatedId) -> Result.success(mockRepoResponse)
        }

        // Simulate the flow:
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult) // This will call the actual repo.
                                                                // To test this properly, ApiService.instance needs to be mocked.
                                                                // Or, refactor VM to take VotingRepository in constructor.
                                                                // Let's assume the happy path for ID gen for now,
                                                                // and we'll manually drive the repo part if direct mocking is too hard.

        // If ID generation fails (returns null), it should go to Error state.
        // If ID generation succeeds, it should call repository.
        // This test needs to control the outcome of AnonymizedIdGenerator.generate and the repository call.

        // Setup for successful path:
        every { AnonymizedIdGenerator.generate(any(), any()) } returns mockGeneratedId
        // To mock the repository call, we'd need to mock ApiService.instance.
        // AnonymizedIdGenerator.generate is already mocked in setUp or per test.
        // VotingRepository is now injected and mocked.
        coEvery { mockVotingRepository.registerVoter(mockGeneratedId) } returns Result.success(mockRepoResponse)

        val events = mutableListOf<RegistrationViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Success, was $finalState", finalState is RegistrationUiState.Success)
        assertEquals(mockRepoResponse.message, (finalState as RegistrationUiState.Success).message)

        assertTrue("Should emit NavigateToElectionList event with correct ID", events.any { it is RegistrationViewEvent.NavigateToElectionList && it.generatedId == mockGeneratedId })

        job.cancel()
        // No need to restore repo, VM instance is per test or reset if needed
    }

    @Test
    fun `biometric success, ID gen success, repo failure leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockGeneratedId = "test-anonymized-id-456"
        val repoError = Exception("Backend network error")

        every { AnonymizedIdGenerator.generate(mockApplication, mockAuthResult) } returns mockGeneratedId
        coEvery { mockVotingRepository.registerVoter(mockGeneratedId) } returns Result.failure(repoError)

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals("Error: Backend Registration Failed - ${repoError.message}", (finalState as RegistrationUiState.Error).message)
    }

    @Test
    fun `biometric success, ID gen failure leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()

        every { AnonymizedIdGenerator.generate(mockApplication, mockAuthResult) } returns null // Simulate ID generation failure

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals("Error: Failed to generate secure ID locally.", (finalState as RegistrationUiState.Error).message)
    }

    // Reflection helper is no longer needed as repository is injected.
    // private fun replaceViewModelRepository(viewModel: RegistrationViewModel, newRepository: VotingRepository): VotingRepository {
    //     val field = viewModel.javaClass.getDeclaredField("votingRepository")
    //     field.isAccessible = true
    //     val originalRepository = field.get(viewModel) as VotingRepository
    //     field.set(viewModel, newRepository)
    //     return originalRepository
    // }
}
