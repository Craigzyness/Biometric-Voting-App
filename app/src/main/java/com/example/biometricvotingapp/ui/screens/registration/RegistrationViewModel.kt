package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt // For error codes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val application: Application,
    private val anonymizedIdGenerator: AnonymizedIdGenerator, // Injected
    private val votingRepository: VotingRepository // Injected
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    // For one-time events like navigation or showing a biometric prompt
    private val _eventFlow = MutableSharedFlow<RegistrationViewEvent>()
    val eventFlow: SharedFlow<RegistrationViewEvent> = _eventFlow.asSharedFlow()

    fun onRegisterClicked() {
        if (BuildConfig.DEBUG) Log.d("RegistrationViewModel", "Register button clicked")
        _uiState.value = RegistrationUiState.AwaitingBiometrics
        viewModelScope.launch {
            _eventFlow.emit(RegistrationViewEvent.ShowBiometricPrompt)
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: androidx.biometric.BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.d("RegistrationViewModel", "Biometric authentication successful")
        _uiState.value = RegistrationUiState.Loading("Generating ID and registering with server...")

        viewModelScope.launch {
            // Use injected anonymizedIdGenerator
            val generatedId = anonymizedIdGenerator.generate(application, authResult) // Pass application context
            if (generatedId != null) {
                if (BuildConfig.DEBUG) Log.i("RegistrationViewModel", "Local ID generated: ${generatedId.take(8)}")
                // Use injected votingRepository
                val registrationResult = votingRepository.registerVoter(generatedId)
                registrationResult.fold(
                    onSuccess = { backendResponse ->
                        if (BuildConfig.DEBUG) Log.i("RegistrationViewModel", "Backend registration successful: ${backendResponse.message}")
                        _uiState.value = RegistrationUiState.Success(backendResponse.message ?: "Registered successfully!")
                        viewModelScope.launch { // Launch a new coroutine for emitting event
                            _eventFlow.emit(RegistrationViewEvent.NavigateToElectionList(generatedId))
                        }
                    },
                    onFailure = { error ->
                        val specificErrorMessage = if (error.message?.contains("already registered", ignoreCase = true) == true ||
                                                       error.message?.contains("409", ignoreCase = true) == true) {
                            "This identity is already registered. Please try logging in."
                        } else {
                            "Error: Backend Registration Failed - ${error.message}"
                        }
                        if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", "Backend registration error: ${error.message}")
                        _uiState.value = RegistrationUiState.Error(specificErrorMessage)
                    }
                )
            } else {
                val errorMessage = "Error: Failed to generate secure ID locally."
                if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", errorMessage)
                _uiState.value = RegistrationUiState.Error(errorMessage)
            }
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        val specificMessage = when (errorCode) {
            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_HW_NOT_PRESENT ->
                "Biometric hardware not available or not detected. Please check your device."
            BiometricPrompt.ERROR_NO_BIOMETRICS ->
                "No biometrics enrolled. Please add a fingerprint in your device settings."
            BiometricPrompt.ERROR_LOCKOUT ->
                "Too many attempts. Biometric authentication is temporarily locked."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                "Too many attempts. Biometric authentication is permanently locked. You may need to reconfigure device security."
            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                "Biometric authentication cancelled."
            // Consider BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL if device credential fallback is ever enabled.
            else -> "Biometric authentication error: $errString (Code: $errorCode)"
        }
        if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $specificMessage")
        _uiState.value = RegistrationUiState.Error(specificMessage)
    }

    fun onBiometricAuthenticationFailed() {
        val errorMessage = "Biometric authentication failed. Fingerprint not recognized." // Made slightly more user-friendly
        if (BuildConfig.DEBUG) Log.w("RegistrationViewModel", errorMessage)
        _uiState.value = RegistrationUiState.Error(errorMessage)
    }

    // If Biometric prompt is handled entirely by UI and only calls back success/error,
    // then onBiometricPromptShown might not be strictly needed in VM.
    // However, if VM needs to clear the trigger, it would be.
    // For now, assuming UI handles showing prompt based on ShowBiometricPrompt event.
}

// Define UI States
sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object AwaitingBiometrics : RegistrationUiState() // Waiting for user to interact with biometric prompt
    data class Loading(val message: String?) : RegistrationUiState()
    data class Success(val message: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}

// Define one-time events
sealed class RegistrationViewEvent {
    object ShowBiometricPrompt : RegistrationViewEvent()
    data class NavigateToElectionList(val generatedId: String) : RegistrationViewEvent()
    // Could add NavigateToLogin here if needed from VM
}
