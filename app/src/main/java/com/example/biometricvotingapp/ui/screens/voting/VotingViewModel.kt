package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
// Removed ViewModelProvider import
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.data.network.dto.VoteRequest
// Removed unused repository imports from ViewModel file scope
import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
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
import com.example.biometricvotingapp.BuildConfig

// Define UI States for VotingScreen
sealed interface VotingUiState {
    object Idle : VotingUiState
    object AwaitingBiometrics : VotingUiState
    object Loading : VotingUiState
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
    private val securityUtil: SecurityUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow<VotingUiState>(VotingUiState.Idle)
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VotingViewEvent>()
    val eventFlow: SharedFlow<VotingViewEvent> = _eventFlow.asSharedFlow()

    private var currentVoteArgs: Triple<String, String, String>? = null

    fun onCastVoteClicked(anonymizedVoterId: String, electionId: String, selectedOption: String) {
        if (BuildConfig.DEBUG) Log.d("VotingViewModel", "Cast Vote button clicked for election: $electionId, option: $selectedOption by voter: ${anonymizedVoterId.take(8)}")
        currentVoteArgs = Triple(anonymizedVoterId, electionId, selectedOption)
        _uiState.value = VotingUiState.AwaitingBiometrics

        val cryptoForPrompt = securityUtil.getCryptoObjectForEncryption()
        if (cryptoForPrompt == null) {
            if (BuildConfig.DEBUG) Log.e("VotingViewModel", "Failed to create CryptoObject for encryption.")
            _uiState.value = VotingUiState.Error("Error preparing secure voting session. Please try again.")
            return
        }

        viewModelScope.launch {
            _eventFlow.emit(VotingViewEvent.ShowBiometricPrompt(cryptoForPrompt))
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: androidx.biometric.BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.d("VotingViewModel", "Biometric authentication successful, proceeding to submit vote.")
        _uiState.value = VotingUiState.Loading

        val cryptoObjectFromResult = authResult.cryptoObject
        if (cryptoObjectFromResult == null) {
            if (BuildConfig.DEBUG) Log.e("VotingViewModel", "CryptoObject is null in AuthenticationResult. This should not happen.")
            _uiState.value = VotingUiState.Error("Critical error: Secure authentication context lost.")
            return
        }

        currentVoteArgs?.let { args ->
            val voteProofPayload = "Vote for ${args.second} at ${System.currentTimeMillis()}"
            val encryptionResult = securityUtil.encryptData(voteProofPayload, cryptoObjectFromResult)

            if (encryptionResult == null) {
                if (BuildConfig.DEBUG) Log.e("VotingViewModel", "Failed to encrypt vote proof.")
                _uiState.value = VotingUiState.Error("Error securing vote. Please try again.")
                return@let
            }

            val (ivBytes, encryptedProofBytes) = encryptionResult
            val ivString = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
            val encryptedProofString = Base64.encodeToString(encryptedProofBytes, Base64.NO_WRAP)

            viewModelScope.launch {
                val voteRequest = VoteRequest(
                    anonymizedVoterId = args.first,
                    electionId = args.second,
                    selectedOption = args.third,
                    encryptedProof = encryptedProofString,
                    iv = ivString
                )
                val voteResult = submitVoteUseCase(voteRequest)

                voteResult.fold(
                    onSuccess = { backendResponse ->
                        val newSuccessMessage = "Vote submitted successfully and recorded anonymously!"
                        if (BuildConfig.DEBUG) Log.i("VotingViewModel", "Backend vote submission successful. UseCase response: ${backendResponse.message}, New msg: $newSuccessMessage")
                        _uiState.value = VotingUiState.Success(newSuccessMessage)
                        viewModelScope.launch {
                            _eventFlow.emit(VotingViewEvent.VoteSubmissionSuccessAndNavigate(newSuccessMessage))
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = "Error: Vote Submission Failed - ${error.message}"
                        if (BuildConfig.DEBUG) Log.e("VotingViewModel", "Backend vote submission error via UseCase: ${error.message}")
                        _uiState.value = VotingUiState.Error(errorMessage)
                    }
                )
            }
        } ?: run {
            val errorMessage = "Error: Vote arguments not found after biometric success."
            if (BuildConfig.DEBUG) Log.e("VotingViewModel", errorMessage)
            _uiState.value = VotingUiState.Error(errorMessage)
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("VotingViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationFailed() {
        val errorMessage = "Vote confirmation failed. Fingerprint not recognized."
        if (BuildConfig.DEBUG) Log.w("VotingViewModel", errorMessage)
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    fun resetStateToIdle() {
        _uiState.value = VotingUiState.Idle
    }
}

// ViewModel Factory has been removed as Hilt will manage ViewModel creation.
