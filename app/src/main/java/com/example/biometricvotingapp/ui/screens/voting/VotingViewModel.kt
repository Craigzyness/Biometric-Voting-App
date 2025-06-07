package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
// import android.util.Log // Prefer Timber if it's setup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import com.example.biometricvotingapp.util.PlayIntegrityService
import com.example.biometricvotingapp.util.PlayIntegrityException
import com.example.biometricvotingapp.utils.SecurityUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Base64
import androidx.biometric.BiometricPrompt
// import com.example.biometricvotingapp.BuildConfig
import timber.log.Timber

// Define UI States for VotingScreen
sealed interface VotingUiState {
    object Idle : VotingUiState
    object AwaitingBiometrics : VotingUiState
    data class Loading(val message: String = "Processing...") : VotingUiState
    data class Success(val message: String) : VotingUiState
    data class Error(val message: String) : VotingUiState
}

// Define one-time events
sealed interface VotingViewEvent {
    data class ShowBiometricPrompt(val cryptoObject: BiometricPrompt.CryptoObject) : VotingViewEvent
    data class VoteSubmissionSuccessAndNavigate(val message: String) : VotingViewEvent
}

@HiltViewModel
class VotingViewModel @Inject constructor(
    private val application: Application,
    private val getElectionsUseCase: GetElectionsUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val submitVoteUseCase: SubmitVoteUseCase,
    private val securityUtil: SecurityUtil,
    private val playIntegrityService: PlayIntegrityService
) : ViewModel() {

    private val _uiState = MutableStateFlow<VotingUiState>(VotingUiState.Idle)
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VotingViewEvent>()
    val eventFlow: SharedFlow<VotingViewEvent> = _eventFlow.asSharedFlow()

    private var currentVoteArgs: Triple<String, String, String>? = null

    fun onCastVoteClicked(anonymizedVoterId: String, electionId: String, selectedOption: String) {
        Timber.d("Cast Vote button clicked for election: $electionId, option: $selectedOption by voter: ${anonymizedVoterId.take(8)}")
        currentVoteArgs = Triple(anonymizedVoterId, electionId, selectedOption)
        _uiState.value = VotingUiState.AwaitingBiometrics

        val cryptoForPrompt = securityUtil.getCryptoObjectForEncryption()
        if (cryptoForPrompt == null) {
            Timber.e("Failed to create CryptoObject for encryption.")
            _uiState.value = VotingUiState.Error("Error preparing secure voting session. Please try again.")
            return
        }

        viewModelScope.launch {
            _eventFlow.emit(VotingViewEvent.ShowBiometricPrompt(cryptoForPrompt))
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: androidx.biometric.BiometricPrompt.AuthenticationResult) {
        Timber.d("Biometric authentication successful, proceeding.")

        val cryptoObjectFromResult = authResult.cryptoObject
        if (cryptoObjectFromResult == null) {
            Timber.e("CryptoObject is null in AuthenticationResult. This should not happen.")
            _uiState.value = VotingUiState.Error("Critical error: Secure authentication context lost.")
            return
        }

        val capturedArgs = currentVoteArgs
        if (capturedArgs == null) {
            Timber.e("Vote arguments not found after biometric success.")
            _uiState.value = VotingUiState.Error("Error: Vote arguments not found after biometric success.")
            return
        }

        _uiState.value = VotingUiState.Loading("Encrypting vote proof...")
        val voteProofPayload = "Vote for ${capturedArgs.second} at ${System.currentTimeMillis()}"
        val encryptionResult = securityUtil.encryptData(voteProofPayload, cryptoObjectFromResult)

        if (encryptionResult == null) {
            Timber.e("Failed to encrypt vote proof.")
            _uiState.value = VotingUiState.Error("Error securing vote. Please try again.")
            return
        }
        val (ivBytes, encryptedProofBytes) = encryptionResult
        val ivString = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        val encryptedProofString = Base64.encodeToString(encryptedProofBytes, Base64.NO_WRAP)

        viewModelScope.launch {
            _uiState.value = VotingUiState.Loading("Verifying device integrity...")
            val nonce = playIntegrityService.generateNonce()
            val integrityTokenResult = playIntegrityService.requestIntegrityToken(nonce)

            integrityTokenResult.fold(
                onSuccess = { token ->
                    Timber.i("Play Integrity token obtained successfully.")
                    // Timber.d("Play Integrity token (first 10): %s", token.take(10)) // Debug only

                    _uiState.value = VotingUiState.Loading("Submitting vote...")

                    val voteRequest = VoteRequest(
                        anonymizedVoterId = capturedArgs.first,
                        electionId = capturedArgs.second,
                        selectedOption = capturedArgs.third,
                        encryptedProof = encryptedProofString,
                        iv = ivString,
                        playIntegrityToken = token,      // Populate the new field
                        playIntegrityNonce = nonce       // Populate the new field
                    )

                    val voteSubmissionResult = submitVoteUseCase(voteRequest)
                    voteSubmissionResult.fold(
                        onSuccess = { backendResponse ->
                            val successMessage = "Vote submitted successfully and recorded anonymously!"
                            Timber.i("Backend vote submission successful. Response: ${backendResponse.message}")
                            _uiState.value = VotingUiState.Success(successMessage)
                            viewModelScope.launch {
                                _eventFlow.emit(VotingViewEvent.VoteSubmissionSuccessAndNavigate(successMessage))
                            }
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "Vote submission failed via UseCase.")
                            _uiState.value = VotingUiState.Error("Vote submission failed: ${exception.message ?: "Unknown error"}")
                        }
                    )
                },
                onFailure = { exception ->
                    Timber.e(exception, "Play Integrity check failed.")
                    val errorMessage = if (exception is PlayIntegrityException) {
                        "Device integrity check failed: ${playIntegrityService.getErrorMessageForCode(exception.errorCode ?: -100)} (Code: ${exception.errorCode ?: "N/A"})"
                    } else {
                        "Device integrity check failed: ${exception.message ?: "Unknown error"}"
                    }
                    _uiState.value = VotingUiState.Error(errorMessage)
                }
            )
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        Timber.e("Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationFailed() {
        val errorMessage = "Vote confirmation failed. Fingerprint not recognized."
        Timber.w(errorMessage)
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    fun resetStateToIdle() {
        _uiState.value = VotingUiState.Idle
    }
}
