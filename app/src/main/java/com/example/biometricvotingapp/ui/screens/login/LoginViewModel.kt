package com.example.biometricvotingapp.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper // Import the new mapper
// Removed BiometricAuthManager and BiometricAvailabilityStatus imports as they are not directly used in this VM
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- UI State ---
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    // No explicit Success state needed here as navigation handles it via event
}

// --- View Events ---
sealed class LoginViewEvent {
    object ShowBiometricPrompt : LoginViewEvent()
    data class NavigateToElectionList(val anonymizedId: String) : LoginViewEvent()
    // Could add NavigateToRegistration if login needs to trigger that (currently handled by UI)
}

class LoginViewModel(
    private val application: Application,
    private val anonymizedIdGenerator: AnonymizedIdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginClicked() {
        if (BuildConfig.DEBUG) Log.d("LoginViewModel", "Login button clicked")
        _uiState.value = LoginUiState.Loading // Indicate loading before showing prompt
        viewModelScope.launch {
            _eventFlow.emit(LoginViewEvent.ShowBiometricPrompt)
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.i("LoginViewModel", "Biometric Auth Succeeded. Has CryptoObject: ${authResult.cryptoObject != null}")
        // Attempt to get the registered anonymized ID
        val registeredAnonymizedId = anonymizedIdGenerator.getRegisteredAnonymizedId(application)

        if (registeredAnonymizedId != null) {
            if (BuildConfig.DEBUG) Log.i("LoginViewModel", "User is registered. Anonymized ID (first 8): ${registeredAnonymizedId.take(8)}")
            _uiState.value = LoginUiState.Idle // Or a temporary success state if needed
            viewModelScope.launch {
                _eventFlow.emit(LoginViewEvent.NavigateToElectionList(registeredAnonymizedId))
            }
        } else {
            if (BuildConfig.DEBUG) Log.w("LoginViewModel", "Biometric auth success, but no registered anonymized ID found.")
            _uiState.value = LoginUiState.Error("Biometric recognized, but app registration not found. Please register if you haven't.")
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        // Use the centralized BiometricErrorMapper
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = LoginUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationError(message: String) { // Overload for non-API errors from UI (e.g. activity context null)
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error: $message")
        _uiState.value = LoginUiState.Error(message)
    }

    fun onBiometricAuthenticationFailed() {
        // This callback means the biometric was valid (e.g. a fingerprint) but not recognized.
        val errorMessage = "Login Failed: Fingerprint not recognized. Please try again."
        if (BuildConfig.DEBUG) Log.w("LoginViewModel", errorMessage)
        _uiState.value = LoginUiState.Error(errorMessage)
    }

    fun resetStateToIdle() {
        _uiState.value = LoginUiState.Idle
    }
}

// --- ViewModel Factory ---
@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory(
    private val application: Application,
    private val anonymizedIdGenerator: AnonymizedIdGenerator
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(application, anonymizedIdGenerator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
