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
import com.example.biometricvotingapp.util.PlayIntegrityService
import com.example.biometricvotingapp.util.PlayIntegrityException
import com.example.biometricvotingapp.utils.SecurityUtil
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.common.truth.Truth.assertThat // Import for Truth assertions

import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase // Added
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase  // Added
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import com.example.biometricvotingapp.utils.SecurityUtil // Assuming this is now an injectable class
Biometric-Voting-App

import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.* // Keep JUnit asserts for non-Truth assertions if any
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
    private lateinit var mockGetElectionsUseCase: GetElectionsUseCase
    private lateinit var mockLoginUserUseCase: LoginUserUseCase
    private lateinit var mockSubmitVoteUseCase: SubmitVoteUseCase
    private lateinit var mockSecurityUtil: SecurityUtil
    private lateinit var mockPlayIntegrityService: PlayIntegrityService

    private lateinit var mockSecurityUtil: SecurityUtil // Assuming SecurityUtil is now an injectable class
Biometric-Voting-App

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
        mockPlayIntegrityService = mockk()
        mockAuthResult = mockk(relaxed = true)
        mockCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true)

        mockSubmitVoteUseCase = mockk(relaxed = true) // relaxed = true as it returns Result
        mockSecurityUtil = mockk(relaxed = true)
        mockAuthResult = mockk(relaxed = true)
        mockCryptoObject = mockk<BiometricPrompt.CryptoObject>(relaxed = true)

        // If SecurityUtil is an injectable class, mock its instance methods
Biometric-Voting-App

        every { mockSecurityUtil.getCryptoObjectForEncryption() } returns mockCryptoObject

        viewModel = VotingViewModel(
            mockApplication,
            mockGetElectionsUseCase,
            mockLoginUserUseCase,
            mockSubmitVoteUseCase,
            mockSecurityUtil,
            mockPlayIntegrityService
mockSecurityUtil
Biometric-Voting-App

        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
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
Biometric-Voting-App
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

    fun `onBiometricAuthenticationSuccess with Play Integrity success and vote submission success, verifies VoteRequest content`() = runTest(testDispatcher) {

 fun `onBiometricAuthenticationSuccess with Play Integrity success and vote submission success, verifies VoteRequest content`() = runTest(testDispatcher) {
 Biometric-Voting-App
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val expectedNonce = "test-nonce-123" // Specific nonce for verification
        val expectedToken = "test-integrity-token-abc" // Specific token for verification
        val vmSuccessMessage = "Vote submitted successfully and recorded anonymously!"
        val mockUseCaseResponseDto = VoteDetailsDto("voteId1", electionId, option, "timestamp")
        val mockUseCaseResponse = VoteResponse(message = "Vote Cast Successfully!", vote = mockUseCaseResponseDto)

        val mockIvBytes = "testIV-bytes".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof-bytes".toByteArray()
        val expectedIvString = Base64.encodeToString(mockIvBytes, Base64.NO_WRAP)
        val expectedEncryptedProofString = Base64.encodeToString(mockEncryptedProofBytes, Base64.NO_WRAP)

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)
        every { mockPlayIntegrityService.generateNonce() } returns expectedNonce // Use specific nonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(expectedNonce) } returns Result.success(expectedToken) // Use specific token

        val voteRequestSlot = slot<VoteRequest>()
        coEvery { mockSubmitVoteUseCase.invoke(capture(voteRequestSlot)) } returns Result.success(mockUseCaseResponse)

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerifyOrder {
            mockSecurityUtil.encryptData(any(), mockCryptoObject)
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(expectedNonce)
            mockSubmitVoteUseCase.invoke(any())
        }

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Success, was $finalState", finalState is VotingUiState.Success)
        assertEquals(vmSuccessMessage, (finalState as VotingUiState.Success).message)

        val emittedEvent = events.lastOrNull { it is VotingViewEvent.VoteSubmissionSuccessAndNavigate }
        assertNotNull("Navigate event should be emitted", emittedEvent)
        assertEquals(vmSuccessMessage, (emittedEvent as VotingViewEvent.VoteSubmissionSuccessAndNavigate).message)

        // Assertions for captured VoteRequest
        assertTrue(voteRequestSlot.isCaptured)
        val capturedRequest = voteRequestSlot.captured
        assertThat(capturedRequest.anonymizedVoterId).isEqualTo(voterId)
        assertThat(capturedRequest.electionId).isEqualTo(electionId)
        assertThat(capturedRequest.selectedOption).isEqualTo(option)
        assertThat(capturedRequest.encryptedProof).isEqualTo(expectedEncryptedProofString)
        assertThat(capturedRequest.iv).isEqualTo(expectedIvString)
        assertThat(capturedRequest.playIntegrityToken).isEqualTo(expectedToken)
        assertThat(capturedRequest.playIntegrityNonce).isEqualTo(expectedNonce)

        job.cancel()
    }

    @Test
    fun `onBiometricAuthenticationSuccess with Play Integrity failure sets Error state`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val testNonce = "test-nonce-fail"
        val integrityErrorCode = IntegrityErrorCode.NETWORK_ERROR
        val integrityException = PlayIntegrityException("Integrity fail", errorCode = integrityErrorCode)
        val mappedErrorMessageFromService = "Network error."

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair("iv".toByteArray(), "proof".toByteArray())
        every { mockPlayIntegrityService.generateNonce() } returns testNonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(testNonce) } returns Result.failure(integrityException)
        every { mockPlayIntegrityService.getErrorMessageForCode(integrityErrorCode) } returns mappedErrorMessageFromService

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerifyOrder {
            mockSecurityUtil.encryptData(any(), mockCryptoObject)
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(testNonce)
        }
        coVerify(exactly = 0) { mockSubmitVoteUseCase.invoke(any()) }

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        val expectedMessage = "Device integrity check failed: $mappedErrorMessageFromService (Code: $integrityErrorCode)"
        assertEquals(expectedMessage, (finalState as VotingUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationError uses BiometricErrorMapper and sets Error state`() {
        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()


    fun `onBiometricAuthenticationError uses BiometricErrorMapper and sets Error state`() {
        viewModel.onCastVoteClicked("voter1", "election1", "optionA")
        runCurrent()

 Biometric-Voting-App
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
every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns null // Simulate encryption failure
Biometric-Voting-App

        viewModel.onCastVoteClicked(voterId, electionId, option)
        runCurrent()

        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        assertEquals(VotingUiState.Error("Error securing vote. Please try again."), viewModel.uiState.value)
    }


    @Test
    fun `vote submission failure from use case (after Play Integrity success) leads to Error state`() = runTest(testDispatcher) {

@Test
    fun `vote submission failure from use case (after Play Integrity success) leads to Error state`() = runTest(testDispatcher) {

    @Test
    fun `successful vote submission flow with use case leads to Success state and Navigate event`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val vmSuccessMessage = "Vote submitted successfully and recorded anonymously!"
        val useCaseSuccessMessage = "Vote Cast Successfully from UseCase!" // Different to distinguish
        val mockUseCaseResponseDto = VoteDetailsDto("voteId1", electionId, option, "timestamp")
        val mockUseCaseResponse = VoteResponse(message = useCaseSuccessMessage, vote = mockUseCaseResponseDto)

        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()
        val expectedIvString = Base64.encodeToString(mockIvBytes, Base64.NO_WRAP)
        val expectedEncryptedProofString = Base64.encodeToString(mockEncryptedProofBytes, Base64.NO_WRAP)

        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)

        val voteRequestSlot = slot<VoteRequest>()
        coEvery { mockSubmitVoteUseCase.invoke(capture(voteRequestSlot)) } returns Result.success(mockUseCaseResponse)

        val events = mutableListOf<VotingViewEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events.add(it) } }

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        coVerify { mockSubmitVoteUseCase.invoke(any()) }

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Success, was $finalState", finalState is VotingUiState.Success)
        assertEquals(vmSuccessMessage, (finalState as VotingUiState.Success).message) // VM uses its own success message

        assertTrue(voteRequestSlot.isCaptured)
        assertEquals(expectedIvString, voteRequestSlot.captured.iv)
        assertEquals(expectedEncryptedProofString, voteRequestSlot.captured.encryptedProof)
        assertEquals(voterId, voteRequestSlot.captured.anonymizedVoterId)
        assertEquals(electionId, voteRequestSlot.captured.electionId)
        assertEquals(option, voteRequestSlot.captured.selectedOption)

        val emittedEvent = events.lastOrNull { it is VotingViewEvent.VoteSubmissionSuccessAndNavigate }
        assertNotNull("Navigate event should be emitted", emittedEvent)
        assertEquals(vmSuccessMessage, (emittedEvent as VotingViewEvent.VoteSubmissionSuccessAndNavigate).message)

        job.cancel()
    }

    @Test
    fun `vote submission failure from use case leads to Error state`() = runTest(testDispatcher) {
Biometric-Voting-App

        val voterId = "voter1"
        val electionId = "election1"
        val option = "optionA"
        val errorMessage = "Backend error during vote via UseCase"
        val testNonce = "test-nonce"
        val testIntegrityToken = "test-integrity-token"

Biometric-Voting-App

        val mockIvBytes = "testIV".toByteArray()
        val mockEncryptedProofBytes = "testEncryptedProof".toByteArray()
        every { mockAuthResult.cryptoObject } returns mockCryptoObject
        every { mockSecurityUtil.encryptData(any(), mockCryptoObject) } returns Pair(mockIvBytes, mockEncryptedProofBytes)
        every { mockPlayIntegrityService.generateNonce() } returns testNonce
        coEvery { mockPlayIntegrityService.requestIntegrityToken(testNonce) } returns Result.success(testIntegrityToken)
Biometric-Voting-App

        coEvery { mockSubmitVoteUseCase.invoke(any()) } returns Result.failure(Exception(errorMessage))

        viewModel.onCastVoteClicked(voterId, electionId, option)
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()


        coVerifyOrder {
coVerifyOrder {
Biometric-Voting-App
            mockSecurityUtil.encryptData(any(), mockCryptoObject)
            mockPlayIntegrityService.generateNonce()
            mockPlayIntegrityService.requestIntegrityToken(testNonce)
            mockSubmitVoteUseCase.invoke(any())
        }
coVerify { mockSubmitVoteUseCase.invoke(any()) }
Biometric-Voting-App

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote Submission Failed - $errorMessage", (finalState as VotingUiState.Error).message)
    }

    @Test
    fun `onBiometricAuthenticationSuccess when currentVoteArgs is null sets Error`() = runTest(testDispatcher) {
        viewModel.onBiometricAuthenticationSuccess(mockAuthResult)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("UI State should be Error, was $finalState", finalState is VotingUiState.Error)
        assertEquals("Error: Vote arguments not found after biometric success.", (finalState as VotingUiState.Error).message)

        coVerify(exactly = 0) { mockPlayIntegrityService.generateNonce() }

coVerify(exactly = 0) { mockPlayIntegrityService.generateNonce() }
 Biometric-Voting-App
        coVerify(exactly = 0) { mockSubmitVoteUseCase.invoke(any()) }
    }

    @Test
    fun `resetStateToIdle sets state to Idle`() {
        viewModel.onBiometricAuthenticationError(1, "test")
        assertNotEquals(VotingUiState.Idle, viewModel.uiState.value)

        viewModel.resetStateToIdle()
        assertEquals(VotingUiState.Idle, viewModel.uiState.value)
    }
}
