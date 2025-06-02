package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.biometricvotingapp.data.repository.VotingRepository

/**
 * Factory for creating ElectionListViewModel instances, providing necessary dependencies.
 */
class ElectionListViewModelFactory(
    private val application: Application,
    private val votingRepository: VotingRepository,
    private val anonymizedVoterId: String? // Add anonymizedVoterId
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ElectionListViewModel::class.java)) {
            // Pass anonymizedVoterId to the ViewModel constructor
            return ElectionListViewModel(application, votingRepository, anonymizedVoterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
