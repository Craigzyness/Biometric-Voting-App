package com.example.biometricvotingapp.ui.screens.registration

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator

class RegistrationViewModelFactory(
    private val application: Application,
    private val anonymizedIdGenerator: AnonymizedIdGenerator, // AnonymizedIdGenerator is an object, so we pass the instance
    private val votingRepository: VotingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(application, anonymizedIdGenerator, votingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
