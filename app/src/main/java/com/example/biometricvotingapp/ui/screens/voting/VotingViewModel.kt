package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.repository.VotingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Define UI States for VotingScreen
sealed interface VotingUiState {
    object Idle : VotingUiState // Default state, or after an action is reset
    object AwaitingBiometrics : VotingUiState // Waiting for user to interact with biometric prompt
    object Loading : VotingUiState // Submitting vote to backend
    data class Success(val message: String) : VotingUiState
    data class Error(val message: String) : VotingUiState
}

// Define one-time events
sealed interface VotingViewEvent {
    object ShowBiometricPrompt : VotingViewEvent
    data class VoteSubmissionSuccessAndNavigate(val message: String) : VotingViewEvent // Includes message for consistency
}


class VotingViewModel(
    private val application: Application,
    private val votingRepository: VotingRepository // Injected
) : ViewModel() {

    private val _uiState = MutableStateFlow<VotingUiState>(VotingUiState.Idle)
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VotingViewEvent>()
    val eventFlow: SharedFlow<VotingViewEvent> = _eventFlow.asSharedFlow()

    private var currentVoteArgs: Triple<String, String, String>? = null // anonymizedVoterId, electionId, selectedOption

    /**
     * Called when the user clicks the main button to initiate voting.
     * Stores arguments and triggers biometric prompt.
     */
    fun onCastVoteClicked(anonymizedVoterId: String, electionId: String, selectedOption: String) {
        Log.d("VotingViewModel", "Cast Vote button clicked for election: $electionId, option: $selectedOption")
        currentVoteArgs = Triple(anonymizedVoterId, electionId, selectedOption)
        _uiState.value = VotingUiState.AwaitingBiometrics
        viewModelScope.launch {
            _eventFlow.emit(VotingViewEvent.ShowBiometricPrompt)
        }
    }

    /**
     * Called by the UI after biometric authentication is successful.
     */
    fun onBiometricAuthenticationSuccess(authResult: androidx.biometric.BiometricPrompt.AuthenticationResult) {
        Log.d("VotingViewModel", "Biometric authentication successful, proceeding to submit vote.")
        _uiState.value = VotingUiState.Loading

        currentVoteArgs?.let { args ->
            viewModelScope.launch {
                val voteRequest = VoteRequest(
                    anonymizedVoterId = args.first,
                    electionId = args.second,
                    selectedOption = args.third
                )
                val voteResult = votingRepository.submitVote(voteRequest)
                voteResult.fold(
                    onSuccess = { backendResponse ->
                        val successMessage = backendResponse.message ?: "Vote cast successfully!"
                        Log.i("VotingViewModel", "Backend vote submission successful: $successMessage")
                        _uiState.value = VotingUiState.Success(successMessage) // Keep success state for a moment
                        viewModelScope.launch { // Emit navigation event
                            _eventFlow.emit(VotingViewEvent.VoteSubmissionSuccessAndNavigate(successMessage))
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = "Error: Vote Submission Failed - ${error.message}"
                        Log.e("VotingViewModel", "Backend vote submission error: ${error.message}")
                        _uiState.value = VotingUiState.Error(errorMessage)
                    }
                )
            }
        } ?: run {
            val errorMessage = "Error: Vote arguments not found after biometric success."
            Log.e("VotingViewModel", errorMessage)
            _uiState.value = VotingUiState.Error(errorMessage)
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        val errorMessage = "Error: Biometric Authentication Error $errorCode: $errString"
        Log.e("VotingViewModel", errorMessage)
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationFailed() {
        val errorMessage = "Error: Biometric Authentication Failed. Fingerprint not recognized."
        Log.w("VotingViewModel", errorMessage)
        _uiState.value = VotingUiState.Error(errorMessage)
    }

    /**
     * Resets the UI state to Idle. Can be called after displaying a message or error.
     */
    fun resetStateToIdle() {
        _uiState.value = VotingUiState.Idle
    }
}
