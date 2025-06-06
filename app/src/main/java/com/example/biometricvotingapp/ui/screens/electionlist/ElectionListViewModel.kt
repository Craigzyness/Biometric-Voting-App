package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.model.Election // Assuming this is your domain model
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
    private val votingRepository: VotingRepository,
    private val anonymizedVoterId: String? // Add anonymizedVoterId
) : ViewModel() {

    private val _uiState = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading)
    val uiState: StateFlow<ElectionListUiState> = _uiState.asStateFlow()

    init {
        loadElections()
    }

    fun loadElections() {
        viewModelScope.launch {
            _uiState.value = ElectionListUiState.Loading
            if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetching elections for voter ID: $anonymizedVoterId")
            val result = votingRepository.getElections(anonymizedVoterId = anonymizedVoterId) // Pass the ID
            result.fold(
                onSuccess = { electionDtoList ->
                    if (BuildConfig.DEBUG) Log.d("ElectionListViewModel", "Fetched ${electionDtoList.size} elections DTOs for voter ID: $anonymizedVoterId.")
                    if (electionDtoList.isEmpty()) {
                        _uiState.value = ElectionListUiState.Empty
                    } else {
                        val domainElections = electionDtoList.map { mapDtoToDomain(it) }
                        _uiState.value = ElectionListUiState.Success(domainElections)
                    }
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "An unknown error occurred while fetching elections."
                    if (BuildConfig.DEBUG) Log.e("ElectionListViewModel", "Error fetching elections: $errorMessage")
                    _uiState.value = ElectionListUiState.Error(errorMessage)
                }
            )
        }
    }

    // Mapper function from ElectionDto (network) to Election (domain)
    private fun mapDtoToDomain(dto: ElectionDto): Election {
        return Election(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            options = dto.options,
            hasVoted = dto.hasVoted ?: false // Map hasVoted, default to false if null
            // Note: ElectionDto has more fields (electionCode, status, timestamps)
            // which are not currently in the Election domain model.
            // If needed, the Election domain model should be updated for those too.
        )
    }
}
