package com.example.biometricvotingapp.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase // Import the new use case
import com.example.biometricvotingapp.domain.usecase.UserNotRegisteredException // Import custom exception
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Keep for Factory
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- UI State ---
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val anonymizedId: String) : LoginUiState() // Added Success state
    data class Error(val message: String) : LoginUiState()
}

// --- View Events ---
sealed class LoginViewEvent {
    object ShowBiometricPrompt : LoginViewEvent()
    data class NavigateToElectionList(val anonymizedId: String) : LoginViewEvent() // Renamed from NavigateToHome for consistency
    object NavigateToRegistration : LoginViewEvent() // Added for navigation to registration
}

class LoginViewModel(
    private val application: Application, // Keep application if needed for other things or future use cases
    private val loginUserUseCase: LoginUserUseCase // Use the injected use case
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginClicked() {
        if (BuildConfig.DEBUG) Log.d("LoginViewModel", "Login button clicked")
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _eventFlow.emit(LoginViewEvent.ShowBiometricPrompt)
        }
    }

    fun onBiometricAuthenticationSuccess(authResult: BiometricPrompt.AuthenticationResult) {
        if (BuildConfig.DEBUG) Log.i("LoginViewModel", "Biometric Auth Succeeded.")
        // No longer directly using authResult.cryptoObject here for login logic itself

        _uiState.value = LoginUiState.Loading // Show loading while use case runs

        viewModelScope.launch {
            val result = loginUserUseCase() // Calling the use case
            result.fold(
                onSuccess = { loggedInUserId ->
                    // UseCase now returns Result<String> where success implies non-null ID
                    if (BuildConfig.DEBUG) Log.i("LoginViewModel", "Login UseCase Succeeded. User ID: ${loggedInUserId.take(8)}")
                    _uiState.value = LoginUiState.Success(loggedInUserId) // Update state
                    _eventFlow.emit(LoginViewEvent.NavigateToElectionList(loggedInUserId))
                },
                onFailure = { exception ->
                    if (exception is UserNotRegisteredException) {
                        if (BuildConfig.DEBUG) Log.w("LoginViewModel", "Login UseCase indicated user not registered: ${exception.message}")
                        _uiState.value = LoginUiState.Error(exception.message ?: "User not registered. Please register.")
                        // Optionally, navigate to registration directly or let UI offer the choice
                        // _eventFlow.emit(LoginViewEvent.NavigateToRegistration) // Uncomment if direct navigation is desired
                    } else {
                        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Login UseCase Failed: ${exception.message}", exception)
                        _uiState.value = LoginUiState.Error("Login failed: ${exception.message ?: "Unknown error"}")
                    }
                }
            )
        }
    }

    fun onBiometricAuthenticationError(errorCode: Int, errString: CharSequence) {
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = LoginUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationError(message: String) {
        if (BuildConfig.DEBUG) Log.e("LoginViewModel", "Biometric Auth Error: $message")
        _uiState.value = LoginUiState.Error(message)
    }

    fun onBiometricAuthenticationFailed() {
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
    private val anonymizedIdGenerator: AnonymizedIdGenerator // Keep this to construct the UseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val loginUserUseCase = LoginUserUseCase(anonymizedIdGenerator)
            return LoginViewModel(application, loginUserUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
