package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
import android.util.Base64 // For Base64 encoding, needed for assertions
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.data.network.dto.VoteDetailsDto
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.utils.SecurityUtil // Import SecurityUtil
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
class VotingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: VotingViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockVotingRepository: VotingRepository
    private lateinit var mockAuthResult: BiometricPrompt.AuthenticationResult
    private lateinit var viewModel: VotingViewModel // Instantiated in setUp
    private lateinit var mockCryptoObject: BiometricPrompt.CryptoObject // For SecurityUtil interactions

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockVotingRepository = mockk(relaxed = true)
        mockAuthResult = mockk(relaxed = true)
        mockCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true) // Mock CryptoObject

        // Mock SecurityUtil as it's an object
        mockkObject(SecurityUtil)
        // Default behavior for SecurityUtil, can be overridden in specific tests
        every { SecurityUtil.getCryptoObjectForEncryption() } returns mockCryptoObject

        viewModel = VotingViewModel(mockApplication, mockVotingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SecurityUtil) // Unmock SecurityUtil
        // clearAllMocks()
    }

    // Reflection helper is no longer needed.

    @Test
    fun `initial state is Idle`() {
        assertEquals(VotingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onCastVoteClicked when SecurityUtil fails returns Error state and no prompt event`() = runTest(testDispatcher) {
        every { SecurityUtil.getCryptoObjectForEncryption() } returns null

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked("voter1", "election1", "optionA")

        assertEquals(VotingUiState.Error("Error preparing secure voting session. Please try again."), viewModel.uiState.value)
        assertTrue("Should not emit ShowBiometricPrompt event", events.isEmpty())

        job.cancel()
    }

    @Test
    fun `onCastVoteClicked when SecurityUtil succeeds emits ShowBiometricPrompt with CryptoObject`() = runTest(testDispatcher) {
        // SecurityUtil.getCryptoObjectForEncryption() already mocked to return mockCryptoObject in setUp

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked("voter1", "election1", "optionA")

        assertEquals(VotingUiState.AwaitingBiometrics, viewModel.uiState.value)
        assertTrue("Events list should not be empty", events.isNotEmpty())
        val event = events.first()
        assertTrue("Event should be ShowBiometricPrompt", event is VotingViewEvent.ShowBiometricPrompt)
        assertEquals(mockCryptoObject, (event as VotingViewEvent.ShowBiometricPrompt).cryptoObject)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationError sets Error state`() {
        // Ensure onCastVoteClicked was called to set state to AwaitingBiometrics if that's a precondition for error display
        viewModel.onCastVoteClicked("voter1", "election1", "optionA") // To set currentVoteArgs and potentially state
        runCurrent() // Let the event emission for ShowBiometricPrompt happen if needed

        viewModel.onBiometricAuthenticationError(123, "Biometric test error")
        val expectedMessage = "Error: Biometric Authentication Error 123: Biometric test error"
        assertEquals(VotingUiState.Error(expectedMessage), viewModel.uiState.value)
    }

    @Test
    fun `onBiometricAuthenticationFailed sets Error state`() {
        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()

        viewModel.onBiometricAuthenticationFailed()
        val expectedMessage = "Error: Biometric Authentication Failed. Fingerprint not recognized."
        assertEquals(VotingUiState.Error(expectedMessage), viewModel.uiState.value)
    }

    @Test
    fun `onBiometricAuthenticationSuccess with null cryptoObject in result sets Error state`() = runTest(testDispatcher) {
        every { mockAuthResult.cryptoObject } returns null

        viewModel.onCastVoteClicked("voter1", "election1", "optionA") // Sets currentVoteArgs
        runCurrent() // Process ShowBiometricPrompt event if any

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        assertEquals(VotingUiState.Error("Critical error: Secure authentication context lost."), viewModel.uiState.value)
    }

    @Test
    fun `onBiometricAuthenticationSuccess with encryption failure sets Error state`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { SecurityUtil.encryptData(any(), mockCryptoObject) } returns null // Simulate encryption failure

        viewModel.onCastVoteClicked(voterId, electionId, option)
        runCurrent()

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        assertEquals(VotingUiState.Error("Error securing vote. Please try again."), viewModel.uiState.value)
    }


    @Test
    fun `successful vote submission flow leads to Success state and Navigate event`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val successMessage = "Vote Cast Successfully!"
        val mockRepoResponse = VoteResponse(successMessage, VoteDetailsDto("voteId1", electionId, option, "timestamp"))

        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()
        val expectedIvString = Base64.encodeToString(mockIvBytes, Base64.NO_WRAP)
        val expectedEncryptedProofString = Base64.encodeToString(mockEncryptedProofBytes, Base64.NO_WRAP)

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { SecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)

        val voteRequestSlot = slot<VoteRequest>()
        coEvery { mockVotingRepository.submitVote(capture(voteRequestSlot)) } returns Result.success(mockRepoResponse)

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        // Simulate the sequence of events
        viewModel.onCastVoteClicked(voterId, electionId, option) // This will emit ShowBiometricPrompt
        // Assume UI would show prompt and then call this:
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)

        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Success, was $finalState", finalState is VotingUiState.Success)
        assertEquals(successMessage, (finalState as VotingUiState.Success).message)

        // Verify captured VoteRequest
        assertTrue(voteRequestSlot.isCaptured)
        assertEquals(expectedIvString, voteRequestSlot.captured.iv)
        assertEquals(expectedEncryptedProofString, voteRequestSlot.captured.encryptedProof)
        assertEquals(voterId, voteRequestSlot.captured.anonymizedVoterId)
        assertEquals(electionId, voteRequestSlot.captured.electionId)
        assertEquals(option, voteRequestSlot.captured.selectedOption)

        // Verify navigation event
        val emittedEvent = events.lastOrNull { it is VotingViewEvent.VoteSubmissionSuccessAndNavigate }
        assertNotNull("Navigate event should be emitted", emittedEvent)
        assertEquals(successMessage, (emittedEvent as VotingViewEvent.VoteSubmissionSuccessAndNavigate).message)

        job.cancel()
    }


    @Test
    fun `vote submission failure leads to Error state`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val errorMessage = "Backend error during vote"

        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()
        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { SecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)

        coEvery { mockVotingRepository.submitVote(any()) } returns Result.failure(Exception(errorMessage))

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote Submission Failed - $errorMessage", (finalState as VotingUiState.Error).message)
    }

    // Test for onCastVoteClicked where currentVoteArgs is not set before onBiometricAuthenticationSuccess
    // is implicitly covered by onBiometricAuthenticationSuccess checking currentVoteArgs.
    // If onCastVoteClicked wasn't called, currentVoteArgs would be null.
    @Test
    fun `onBiometricAuthenticationSuccess when currentVoteArgs is null sets Error`() = runTest(testDispatcher) {
        // Ensure currentVoteArgs is null by not calling onCastVoteClicked or resetting it (if possible)
        // Directly call onBiometricAuthenticationSuccess
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote arguments not found after biometric success.", (finalState as VotingUiState.Error).message)
    }


    @Test
    fun `resetStateToIdle sets state to Idle`() {
        // Set to a non-idle state first
        viewModel.onBiometricAuthenticationError(1, "test")
        assertNotEquals(VotingUiState.Idle, viewModel.uiState.value)

        viewModel.resetStateToIdle()
        assertEquals(VotingUiState.Idle, viewModel.uiState.value)
    }
}
