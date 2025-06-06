package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.data.repository.VotingRepository // Keep for Factory
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase // Import the new use case
import com.example.biometricvotingapp.domain.repository.AuthRepository // For Factory casting, as UseCase expects AuthRepository
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
}

class ElectionListViewModel(
    private val application: Application,
    private val getElectionsUseCase: GetElectionsUseCase,
    private val anonymizedVoterId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading)
    val uiState: StateFlow<ElectionListUiState> = _uiState.asStateFlow()

    init {
        loadElections()
    }

    fun loadElections() {
        viewModelScope.launch {
            _uiState.value = ElectionListUiState.Loading
            if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetching elections via UseCase for voter ID: $anonymizedVoterId")

            val result = getElectionsUseCase(anonymizedVoterId)

            result.fold(
                onSuccess = { domainElections ->
                    if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetched ${domainElections.size} domain elections for voter ID: $anonymizedVoterId.")
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
}

// ViewModel Factory
@Suppress("UNCHECKED_CAST")
class ElectionListViewModelFactory(
    private val application: Application,
    private val votingRepository: VotingRepository, // Factory still takes VotingRepository
    private val anonymizedVoterId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ElectionListViewModel::class.java)) {
            // Create GetElectionsUseCase, assuming VotingRepository implements AuthRepository
            // The GetElectionsUseCase now expects AuthRepository as per the prompt's definition for it.
            val authRepository = votingRepository as AuthRepository
            val getElectionsUseCase = GetElectionsUseCase(authRepository)
            return ElectionListViewModel(application, getElectionsUseCase, anonymizedVoterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
