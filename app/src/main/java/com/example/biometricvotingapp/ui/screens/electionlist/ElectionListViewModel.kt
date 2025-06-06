package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
// Removed ViewModelProvider import as factory is being removed
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
// Removed VotingRepository and AuthRepository imports from ViewModel file scope
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.domain.usecase.UserNotRegisteredException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Define UI States for ElectionListScreen
sealed interface ElectionListUiState {
    object Loading : ElectionListUiState
    data class Success(val elections: List<Election>) : ElectionListUiState
    object Empty : ElectionListUiState
    data class Error(val message: String) : ElectionListUiState
    object UserNotLoggedIn : ElectionListUiState
}

@HiltViewModel
class ElectionListViewModel @Inject constructor(
    private val application: Application,
    private val getElectionsUseCase: GetElectionsUseCase,
    private val loginUserUseCase: LoginUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading)
    val uiState: StateFlow<ElectionListUiState> = _uiState.asStateFlow()

    private var currentAnonymizedId: String? = null

    init {
        fetchCurrentUserAndLoadElections()
    }

    private fun fetchCurrentUserAndLoadElections() {
        viewModelScope.launch {
            _uiState.value = ElectionListUiState.Loading
            val userResult = loginUserUseCase()
            userResult.fold(
                onSuccess = { userId ->
                    currentAnonymizedId = userId
                    if (userId == null) {
                        if (BuildConfig.DEBUG) Log.w("ElectionListViewModel", "User ID is null after LoginUserUseCase call. Assuming not registered/logged in.")
                        _uiState.value = ElectionListUiState.UserNotLoggedIn
                    } else {
                        if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Current user ID: ${userId.take(8)}")
                        loadElectionsInternal(userId)
                    }
                },
                onFailure = { exception ->
                    if (exception is UserNotRegisteredException) {
                        if (BuildConfig.DEBUG) Log.w("ElectionListViewModel", "LoginUserUseCase failed: UserNotRegisteredException - ${exception.message}")
                        _uiState.value = ElectionListUiState.UserNotLoggedIn
                    } else {
                        if (BuildConfig.DEBUG) Log.e("ElectionListViewModel", "Error fetching user ID: ${exception.message}", exception)
                        _uiState.value = ElectionListUiState.Error("Failed to retrieve user session: ${exception.message}")
                    }
                }
            )
        }
    }

    private fun loadElectionsInternal(voterIdToUse: String?) {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetching elections via UseCase for voter ID: $voterIdToUse")

            val result = getElectionsUseCase(voterIdToUse)

            result.fold(
                onSuccess = { domainElections ->
                    if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetched ${domainElections.size} domain elections for voter ID: $voterIdToUse.")
                    if (domainElections.isEmpty()) {
                        _uiState.value = ElectionListUiState.Empty
                    } else {
                        _uiState.value = ElectionListUiState.Success(domainElections)
                    }
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "An unknown error occurred while fetching elections."
                    if (BuildConfig.DEBUG) Log.e("ElectionListViewModel", "Error fetching elections via UseCase: $errorMessage")
                    _uiState.value = ElectionListUiState.Error(errorMessage)
                }
            )
        }
    }

    fun refreshElections() {
        fetchCurrentUserAndLoadElections()
    }
}

// ViewModel Factory has been removed as Hilt will manage ViewModel creation.
