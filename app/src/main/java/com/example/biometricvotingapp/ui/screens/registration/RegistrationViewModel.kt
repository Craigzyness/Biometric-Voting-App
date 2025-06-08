package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt // For error codes and AuthResult
import androidx.lifecycle.ViewModel
// Removed ViewModelProvider import as factory is being removed
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
// Removed unused repository/generator imports from ViewModel file scope
import com.example.biometricvotingapp.domain.usecase.RegisterVoterUseCase
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
// Removed ApiService import as it's not directly used
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper // Import the new mapper
Biometric-Voting-App
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val application: Application,
    private val registerVoterUseCase: RegisterVoterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RegistrationViewEvent>()
    val eventFlow: SharedFlow<RegistrationViewEvent> = _eventFlow.asSharedFlow()

    fun onRegisterClicked() {
        if (BuildConfig.DEBUG) Log.d("RegistrationViewModel", "Register button clicked")
        _uiState.value = RegistrationUiState.AwaitingBiometrics
        viewModelScope.launch {
            _eventFlow.emit(RegistrationViewEvent.ShowBiometricPrompt)
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.d("RegistrationViewModel", "Biometric authentication successful")
        _uiState.value = RegistrationUiState.Loading("Registering your anonymized ID...")

        viewModelScope.launch {
            val result = registerVoterUseCase()

            result.fold(
                onSuccess = { generatedId ->
                    if (BuildConfig.DEBUG) Log.i("RegistrationViewModel", "Registration UseCase Succeeded. Generated ID: ${generatedId.take(8)}")
                    _uiState.value = RegistrationUiState.Success("Registration successful! You can now log in.")
                    _eventFlow.emit(RegistrationViewEvent.NavigateToElectionList(generatedId))
                },
                onFailure = { exception ->
                    val specificErrorMessage = if (exception.message?.contains("already registered", ignoreCase = true) == true ||
                                                   exception.message?.contains("409", ignoreCase = true) == true) {
                        "This identity is already registered. Please try logging in."
                    } else {
                        "Registration failed: ${exception.message ?: "Unknown error"}"
                    }
                    if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", "Registration UseCase Failed: ${exception.message}", exception)
                    _uiState.value = RegistrationUiState.Error(specificErrorMessage)
                }
            )
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
Biometric-Voting-App
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = RegistrationUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationFailed() {
 Biometric-Voting-App
        val errorMessage = "Biometric authentication failed. Fingerprint not recognized."
        if (BuildConfig.DEBUG) Log.w("RegistrationViewModel", errorMessage)
        _uiState.value = RegistrationUiState.Error(errorMessage)
    }
}

// Define UI States
sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object AwaitingBiometrics : RegistrationUiState()
    data class Loading(val message: String?) : RegistrationUiState()
    data class Success(val message: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}

// Define one-time events
sealed class RegistrationViewEvent {
    object ShowBiometricPrompt : RegistrationViewEvent()
    data class NavigateToElectionList(val generatedId: String) : RegistrationViewEvent()
}

// ViewModel Factory has been removed as Hilt will manage ViewModel creation.
