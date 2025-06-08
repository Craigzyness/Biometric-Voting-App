package com.example.biometricvotingapp.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
// Removed ViewModelProvider import as factory is being removed
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.domain.usecase.UserNotRegisteredException
// Removed AnonymizedIdGenerator import from ViewModel file scope
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper // Import the new mapper
// Removed BiometricAuthManager and BiometricAvailabilityStatus imports as they are not directly used in this VM
Biometric-Voting-App
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- UI State ---
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val anonymizedId: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

// --- View Events ---
sealed class LoginViewEvent {
    object ShowBiometricPrompt : LoginViewEvent()
    data class NavigateToElectionList(val anonymizedId: String) : LoginViewEvent()
    object NavigateToRegistration : LoginViewEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val application: Application,
    private val loginUserUseCase: LoginUserUseCase
private val anonymizedIdGenerator: AnonymizedIdGenerator
Biometric-Voting-App
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginClicked() {
        if (BuildConfig.DEBUG) Log.d("LoginViewModel", "Login button clicked")
        _uiState.value = LoginUiState.Loading

      _uiState.value = LoginUiState.Loading // Indicate loading before showing prompt
Biometric-Voting-App

        viewModelScope.launch {
            _eventFlow.emit(LoginViewEvent.ShowBiometricPrompt)
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.i("LoginViewModel", "Biometric Auth Succeeded.")

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            val result = loginUserUseCase()
            result.fold(
                onSuccess = { loggedInUserId ->
                    if (BuildConfig.DEBUG) Log.i("LoginViewModel", "Login UseCase Succeeded. User ID: ${loggedInUserId.take(8)}")
                    _uiState.value = LoginUiState.Success(loggedInUserId)
                    _eventFlow.emit(LoginViewEvent.NavigateToElectionList(loggedInUserId))
                },
                onFailure = { exception ->
                    if (exception is UserNotRegisteredException) {
                        if (BuildConfig.DEBUG) Log.w("LoginViewModel", "Login UseCase indicated user not registered: ${exception.message}")
                        _uiState.value = LoginUiState.Error(exception.message ?: "User not registered. Please register.")
                        _eventFlow.emit(LoginViewEvent.NavigateToRegistration)
                    } else {
                        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Login UseCase Failed: ${exception.message}", exception)
                        _uiState.value = LoginUiState.Error("Login failed: ${exception.message ?: "Unknown error"}")
                    }
                }
            )
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
Biometric-Voting-App

        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = LoginUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationError(message: String) {
    fun onBiometricAuthenticationError(message: String) { // Overload for non-API errors from UI (e.g. activity context null)
Biometric-Voting-App
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error: $message")
        _uiState.value = LoginUiState.Error(message)
    }

    fun onBiometricAuthenticationFailed() {

        // This callback means the biometric was valid (e.g. a fingerprint) but not recognized.
Biometric-Voting-App
        val errorMessage = "Login Failed: Fingerprint not recognized. Please try again."
        if (BuildConfig.DEBUG) Log.w("LoginViewModel", errorMessage)
        _uiState.value = LoginUiState.Error(errorMessage)
    }

    fun resetStateToIdle() {
        _uiState.value = LoginUiState.Idle
    }
}

// ViewModel Factory has been removed as Hilt will manage ViewModel creation.
