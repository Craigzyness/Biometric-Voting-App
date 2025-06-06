package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricPrompt // For error codes and AuthResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.data.repository.VotingRepository // Keep for Factory, but VM won't hold it directly
import com.example.biometricvotingapp.domain.repository.AuthRepository // For Factory, assuming VotingRepository implements it
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // For Factory
import com.example.biometricvotingapp.domain.usecase.RegisterVoterUseCase // Import the new use case
import com.example.biometricvotingapp.presentation.common.BiometricErrorMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val application: Application, // Keep application if needed for other things or future use cases
    private val registerVoterUseCase: RegisterVoterUseCase // Use the injected use case
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
        _uiState.value = RegistrationUiState.Loading("Registering your anonymized ID...") // Updated message

        viewModelScope.launch {
            // The UseCase now needs to handle the AnonymizedIdGenerator call.
            // The current AnonymizedIdGenerator.generate() needs context and authResult.
            // The UseCase was defined to call a parameterless generate().
            // This requires either the UseCase to get context/authResult, or AnonymizedIdGenerator to be refactored/wrapped.
            // For this step, we assume the RegisterVoterUseCase handles this internally.
            // If RegisterVoterUseCase needs authResult, it should be passed here.
            // The prompt for the use case implies it does NOT take authResult.
            // This is a design discrepancy point from the prompt.
            // Let's proceed as if registerVoterUseCase() is self-sufficient for now.

            val result = registerVoterUseCase() // Calling the use case

            result.fold(
                onSuccess = { generatedId ->
                    if (BuildConfig.DEBUG) Log.i("RegistrationViewModel", "Registration UseCase Succeeded. Generated ID: ${generatedId.take(8)}")
                    _uiState.value = RegistrationUiState.Success("Registration successful! You can now log in.") // Updated message
                    _eventFlow.emit(RegistrationViewEvent.NavigateToElectionList(generatedId)) // Navigate to ElectionList with ID
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
        val errorMessage = BiometricErrorMapper.mapBiometricErrorCodeToString(errorCode, errString)
        if (BuildConfig.DEBUG) Log.e("RegistrationViewModel", "Biometric Auth Error $errorCode: $errString. Mapped to: $errorMessage")
        _uiState.value = RegistrationUiState.Error(errorMessage)
    }

    fun onBiometricAuthenticationFailed() {
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
    data class Success(val message: String) : RegistrationUiState() // Changed to message from generatedId
    data class Error(val message: String) : RegistrationUiState()
}

// Define one-time events
sealed class RegistrationViewEvent {
    object ShowBiometricPrompt : RegistrationViewEvent()
    data class NavigateToElectionList(val generatedId: String) : RegistrationViewEvent() // Kept this for consistency
}

// ViewModel Factory - To be modified in the next step.
// The prompt asks to modify RegistrationViewModelFactory later.
// For now, this VM's constructor has changed. The factory will need to adapt.
// Let's assume the factory modification is handled in its own step.
@Suppress("UNCHECKED_CAST")
class RegistrationViewModelFactory(
    private val application: Application,
    private val anonymizedIdGenerator: AnonymizedIdGenerator, // Will be used to construct UseCase
    private val votingRepository: VotingRepository // Will be used to construct UseCase (as AuthRepository)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            // This factory will need to be updated to create and pass RegisterVoterUseCase
            // For now, to make this file compilable standalone with the VM change,
            // one might temporarily adjust what's passed or expect this factory to be updated next.
            // The actual modification of the factory is a separate step in the prompt.
            // This current structure for the factory is from the *existing* RegistrationViewModel.
            // It will be updated in the next step.
            // To satisfy the new VM constructor:
            val authRepository = votingRepository as AuthRepository // Assuming VotingRepository implements AuthRepository
            val registerVoterUseCase = RegisterVoterUseCase(anonymizedIdGenerator, authRepository)
            return RegistrationViewModel(application, registerVoterUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
