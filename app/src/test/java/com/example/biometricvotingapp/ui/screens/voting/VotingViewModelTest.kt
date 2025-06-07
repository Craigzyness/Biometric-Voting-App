package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
import android.util.Base64
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.data.network.dto.VoteDetailsDto
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import com.example.biometricvotingapp.util.PlayIntegrityService // Import PlayIntegrityService
import com.example.biometricvotingapp.util.PlayIntegrityException // Import custom exception
import com.example.biometricvotingapp.utils.SecurityUtil
import com.google.android.play.core.integrity.model.IntegrityErrorCode // Import IntegrityErrorCode
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
// import timber.log.Timber // Not strictly needed for mocking if just calling Timber.d etc.

@ExperimentalCoroutinesApi
class VotingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: VotingViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockGetElectionsUseCase: GetElectionsUseCase
    private lateinit var mockLoginUserUseCase: LoginUserUseCase
    private lateinit var mockSubmitVoteUseCase: SubmitVoteUseCase
    private lateinit var mockSecurityUtil: SecurityUtil
    private lateinit var mockPlayIntegrityService: PlayIntegrityService // Added
    private lateinit var mockAuthResult: BiometricPrompt.AuthenticationResult
    private lateinit var mockCryptoObject: BiometricPrompt.CryptoObject

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockGetElectionsUseCase = mockk(relaxed = true)
        mockLoginUserUseCase = mockk(relaxed = true)
        mockSubmitVoteUseCase = mockk(relaxed = true)
        mockSecurityUtil = mockk(relaxed = true)
        mockPlayIntegrityService = mockk() // Initialize PlayIntegrityService mock
        mockAuthResult = mockk(relaxed = true)
        mockCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true)

        every { mockSecurityUtil.getCryptoObjectForEncryption() } returns mockCryptoObject

        viewModel = VotingViewModel(
            mockApplication,
            mockGetElectionsUseCase,
            mockLoginUserUseCase,
            mockSubmitVoteUseCase,
            mockSecurityUtil,
            mockPlayIntegrityService // Pass mock to constructor
        )

        // For Timber, if Timber.tag("...") is used in VM and no Tree is planted for tests,
        // it might throw. If so, mock Timber:
        // mockkStatic(Timber::class)
        // every { Timber.tag(any()) } returns mockk(relaxed = true) // or a specific Timber.Tree instance
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        // unmockkStatic(Timber::class) // If Timber was mocked
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(VotingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `onCastVoteClicked when SecurityUtil getCryptoObjectForEncryption fails returns Error state`() = runTest(testDispatcher) {
        every { mockSecurityUtil.getCryptoObjectForEncryption() } returns null

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        advanceUntilIdle()

        assertEquals(VotingUiState.Error("Error preparing secure voting session. Please try again."), viewModel.uiState.value)
        assertTrue("Should not emit ShowBiometricPrompt event", events.isEmpty())

        job.cancel()
    }

    @Test
    fun `onCastVoteClicked when SecurityUtil getCryptoObjectForEncryption succeeds emits ShowBiometricPrompt`() = runTest(testDispatcher) {
        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        advanceUntilIdle()

        assertEquals(VotingUiState.AwaitingBiometrics, viewModel.uiState.value)
        val event = events.first()
        assertTrue("Event should be ShowBiometricPrompt", event is VotingViewEvent.ShowBiometricPrompt)
        assertEquals(mockCryptoObject, (event as VotingViewEvent.ShowBiometricPrompt).cryptoObject)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with Play Integrity success and vote submission success`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val testNonce = "test-nonce"
        val testIntegrityToken = "test-integrity-token"
        val vmSuccessMessage = "Vote submitted successfully and recorded anonymously!"
        val mockUseCaseResponseDto = VoteDetailsDto("voteId1", electionId, option, "timestamp")
        val mockUseCaseResponse = VoteResponse(message = "Vote Cast Successfully!", vote = mockUseCaseResponseDto)
        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)
        every { mockPlayIntegrityService.generateNonce() } returns testNonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(testNonce) } returns Result.success(testIntegrityToken)

        val voteRequestSlot = slot<VoteRequest>()
        coEvery { mockSubmitVoteUseCase.invoke(capture(voteRequestSlot)) } returns Result.success(mockUseCaseResponse)

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        // Simulate the flow starting from after biometric prompt is shown
        viewModel.onCastVoteClicked(voterId, electionId, option) // This sets currentVoteArgs
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerifyOrder {
            mockSecurityUtil.encryptData(any(), mockCryptoObject)
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(testNonce)
            mockSubmitVoteUseCase.invoke(any())
        }

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Success, was $finalState", finalState is VotingUiState.Success)
        assertEquals(vmSuccessMessage, (finalState as VotingUiState.Success).message)

        val emittedEvent = events.lastOrNull { it is VotingViewEvent.VoteSubmissionSuccessAndNavigate }
        assertNotNull("Navigate event should be emitted", emittedEvent)
        assertEquals(vmSuccessMessage, (emittedEvent as VotingViewEvent.VoteSubmissionSuccessAndNavigate).message)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with Play Integrity failure sets Error state`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val testNonce = "test-nonce"
        val integrityErrorCode = IntegrityErrorCode.NETWORK_ERROR
        val integrityException = PlayIntegrityException("Integrity fail", errorCode = integrityErrorCode)
        val mappedErrorMessageFromService = "Network error." // From PlayIntegrityService.getErrorMessageForCode

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair("iv".toByteArray(), "proof".toByteArray())
        every { mockPlayIntegrityService.generateNonce() } returns testNonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(testNonce) } returns Result.failure(integrityException)
        every { mockPlayIntegrityService.getErrorMessageForCode(integrityErrorCode) } returns mappedErrorMessageFromService


        viewModel.onCastVoteClicked(voterId, electionId, option) // This sets currentVoteArgs
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerifyOrder {
            mockSecurityUtil.encryptData(any(), mockCryptoObject)
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(testNonce)
        }
        coVerify(exactly = 0) { mockSubmitVoteUseCase.invoke(any()) } // Vote submission should NOT be called

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        val expectedMessage = "Device integrity check failed: $mappedErrorMessageFromService (Code: $integrityErrorCode)"
        assertEquals(expectedMessage, (finalState as VotingUiState.Error).message)
    }

    // ... (other existing tests for biometric errors, failed encryption, failed vote submission from use case, etc. remain largely the same)
    // The existing tests `successful vote submission flow...` and `vote submission failure from use case...`
    // are now effectively sub-cases of "Play Integrity success". I've integrated the Play Integrity success
    // into a new test `onBiometricAuthenticationSuccess with Play Integrity success and vote submission success`.
    // The original `successful vote submission flow...` can be removed or merged.
    // For now, I'll keep them separate and ensure they also mock PlayIntegrity success.

    @Test
    fun `onBiometricAuthenticationError uses BiometricErrorMapper and sets Error state`() {
        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()

        val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
        val errString = "Hardware unavailable"
        val expectedMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)

        viewModel.onBiometricAuthenticationError(errorCode, errString)

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals(expectedMessage, (finalState as VotingUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationFailed sets Error state`() {
        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()

        viewModel.onBiometricAuthenticationFailed()
        val expectedMessage = "Vote confirmation failed. Fingerprint not recognized."
        assertEquals(VotingUiState.Error(expectedMessage), viewModel.uiState.value)
    }

    @Test
    fun `onBiometricAuthenticationSuccess with null cryptoObject in result sets Error state`() = runTest(testDispatcher) {
        every { mockAuthResult.cryptoObject } returns null

        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()

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
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns null

        viewModel.onCastVoteClicked(voterId, electionId, option)
        runCurrent()

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        assertEquals(VotingUiState.Error("Error securing vote. Please try again."), viewModel.uiState.value)
    }

    @Test
    fun `vote submission failure from use case (after Play Integrity success) leads to Error state`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val errorMessage = "Backend error during vote via UseCase"
        val testNonce = "test-nonce"
        val testIntegrityToken = "test-integrity-token"

        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()
        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)
        every { mockPlayIntegrityService.generateNonce() } returns testNonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(testNonce) } returns Result.success(testIntegrityToken)


        coEvery { mockSubmitVoteUseCase.invoke(any()) } returns Result.failure(Exception(errorMessage))

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerifyOrder {
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(testNonce)
            mockSubmitVoteUseCase.invoke(any())
        }

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote Submission Failed - $errorMessage", (finalState as VotingUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationSuccess when currentVoteArgs is null sets Error`() = runTest(testDispatcher) {
        // currentVoteArgs is null because onCastVoteClicked was not called before onBiometricAuthenticationSuccess
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote arguments not found after biometric success.", (finalState as VotingUiState.Error).message)
        coVerify(exactly = 0) { mockPlayIntegrityService.generateNonce() } // Integrity check should not start
        coVerify(exactly = 0) { mockSubmitVoteUseCase.invoke(any()) }
    }

    @Test
    fun `resetStateToIdle sets state to Idle`() {
        viewModel.onBiometricAuthenticationError(1, "test") // Set some error state
        assertNotEquals(VotingUiState.Idle, viewModel.uiState.value)

        viewModel.resetStateToIdle()
        assertEquals(VotingUiState.Idle, viewModel.uiState.value)
    }
}
