package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.biometricvotingapp.data.repository.VotingRepository

class ElectionListViewModelFactory(
    private val application: Application,
    private val votingRepository: VotingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ElectionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ElectionListViewModel(application, votingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
