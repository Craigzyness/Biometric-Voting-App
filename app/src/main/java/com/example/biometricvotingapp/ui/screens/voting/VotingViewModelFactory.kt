package com.example.biometricvotingapp.ui.screens.voting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.biometricvotingapp.data.repository.VotingRepository

class VotingViewModelFactory(
    private val application: Application,
    private val votingRepository: VotingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VotingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VotingViewModel(application, votingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
