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
    private lateinit var mockVotingRepository: VotingRepository // Will be created by VM, but we can mock its source (ApiService) if needed
                                                              // For this test, we'll mock VotingRepository itself, assuming it's injectable or the VM is refactored.
                                                              // Current VM instantiates it directly. So we must mock AnonymizedIdGenerator and ApiService interactions.

    // For simplicity, we'll mock AnonymizedIdGenerator and ApiService which is used by VotingRepository
    // rather than trying to inject a mock VotingRepository into the current VM structure.
    // This tests the VM's interaction with these direct/indirect dependencies.

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set main dispatcher for ViewModelScope

        mockApplication = mockk(relaxed = true) // Relaxed mock for Application context

        // Mock AnonymizedIdGenerator as it's an object
        mockkObject(AnonymizedIdGenerator)

        // Mock ApiService.instance as VotingRepository uses it
        // This is a bit complex due to singleton pattern. Alternative: pass mock repo to VM.
        // For now, assuming we can control what VotingRepository returns by mocking its underlying ApiService calls.
        // However, the current VM instantiates VotingRepository(ApiService.instance).
        // A better approach for testability would be to inject VotingRepository into the ViewModel.
        // Let's proceed by mocking the direct dependencies of the code *within* the ViewModel:
        // AnonymizedIdGenerator.generate(...) and votingRepository.registerVoter(...)
        // This means we need a way to inject mockVotingRepository or mock the ApiService globally.
        // Given the VM structure `private val votingRepository = VotingRepository(ApiService.instance)`,
        // we'll mock ApiService.instance for these tests. This is not ideal but works for the current VM.

        // Re-creating the VM for each test with fresh mocks might be better if state is an issue.
        // For now, one instance and mock behaviors per test.

        // Let's refine: The task implies the VM takes Application.
        // The VM then creates VotingRepository(ApiService.instance).
        // So, we need to mock ApiService.instance for repository calls.
        // And mock AnonymizedIdGenerator.generate.

        // The ViewModel constructor is: RegistrationViewModel(application: Application)
        // It then creates `votingRepository = VotingRepository(ApiService.instance)`
        // To mock repository calls, we must mock `ApiService.instance` or refactor VM.
        // Let's assume for *this test file*, we'll test the VM as is.
        // We can't directly inject a mock VotingRepository without changing VM constructor.
        // So, we mock the dependencies of the VotingRepository (ApiService) and AnonymizedIdGenerator.

        viewModel = RegistrationViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher
        unmockkObject(AnonymizedIdGenerator)
        // If ApiService.instance was mocked: clearMockk(ApiService.instance) or similar
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
        // For this test, we use reflection to inject a mock repository.
        // This is a workaround due to the ViewModel not using constructor injection for VotingRepository.
        // Ideally, VotingRepository should be injected.
        val mockRepository = mockk<VotingRepository>()
        coEvery { mockRepository.registerVoter(mockGeneratedId) } returns Result.success(mockRepoResponse)

        val originalRepo = replaceViewModelRepository(viewModel, mockRepository)

        val events = mutableListOf<RegistrationViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Success, was $finalState", finalState is RegistrationUiState.Success)
        assertEquals(mockRepoResponse.message, (finalState as RegistrationUiState.Success).message)

        assertTrue("Should emit NavigateToElectionList event with correct ID", events.any { it is RegistrationViewEvent.NavigateToElectionList && it.generatedId == mockGeneratedId })

        job.cancel()
        replaceViewModelRepository(viewModel, originalRepo) // Restore original
    }

    @Test
    fun `biometric success, ID gen success, repo failure leads to Error state`() = runTest {
        val mockAuthResult = mockk<BiometricPrompt.AuthenticationResult>()
        val mockGeneratedId = "test-anonymized-id-456"
        val repoError = Exception("Backend network error")

        every { AnonymizedIdGenerator.generate(mockApplication, mockAuthResult) } returns mockGeneratedId

        val mockRepository = mockk<VotingRepository>()
        coEvery { mockRepository.registerVoter(mockGeneratedId) } returns Result.failure(repoError)

        val originalRepo = replaceViewModelRepository(viewModel, mockRepository)

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        val finalState = viewModel.uiState.value
        assertTrue("UI state should be Error, was $finalState", finalState is RegistrationUiState.Error)
        assertEquals("Error: Backend Registration Failed - ${repoError.message}", (finalState as RegistrationUiState.Error).message)

        replaceViewModelRepository(viewModel, originalRepo)
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

    // Helper function to replace the repository in the ViewModel using reflection (for test purposes only)
    private fun replaceViewModelRepository(viewModel: RegistrationViewModel, newRepository: VotingRepository): VotingRepository {
        val field = viewModel.javaClass.getDeclaredField("votingRepository")
        field.isAccessible = true
        val originalRepository = field.get(viewModel) as VotingRepository
        field.set(viewModel, newRepository)
        return originalRepository
    }
}
