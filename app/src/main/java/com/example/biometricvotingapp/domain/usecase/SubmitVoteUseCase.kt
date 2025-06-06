package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.example.biometricvotingapp.domain.repository.AuthRepository // Assuming this interface path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Assuming AuthRepository has a method like:
// suspend fun submitVote(request: VoteRequest): Result<VoteResponse>

class SubmitVoteUseCase(
    private val authRepository: AuthRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(voteRequest: VoteRequest): Result<VoteResponse> {
        return withContext(defaultDispatcher) {
            try {
                // Directly pass through to the repository method.
                // The primary role of this UseCase is to abstract the repository call
                // and provide a clear, single-purpose entry point for this action from the ViewModel.
                // It also ensures execution on the specified dispatcher.
                authRepository.submitVote(voteRequest)
            } catch (e: Exception) {
                // Catch any unexpected errors from the repository layer if they aren't already encapsulated in Result
                Result.failure(e)
            }
        }
    }
}
