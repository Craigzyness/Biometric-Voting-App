package com.example.biometricvotingapp.core.domain.repository

import com.example.biometricvotingapp.core.domain.model.Election
import com.example.biometricvotingapp.core.domain.model.CoreRegistrationRequest
import com.example.biometricvotingapp.core.domain.model.CoreVoteRequest
// Using kotlin.Result for return types

interface AuthRepository {
    /**
     * Fetches the list of elections.
     * The implementation is expected to map DTOs to the [Election] domain model.
     * @param voterId The anonymized ID of the voter, if available, to check `hasVoted` status.
     * @return A [Result] wrapping a list of [Election] objects or an exception.
     */
    suspend fun getElections(voterId: String?): Result<List<Election>>

    /**
     * Registers a new voter.
     * @param request The registration request details.
     * @return A [Result] indicating success (Unit) or an exception.
     */
    suspend fun registerVoter(request: CoreRegistrationRequest): Result<Unit>

    /**
     * Submits a vote for an election.
     * @param request The vote submission details.
     * @return A [Result] indicating success (Unit) or an exception.
     */
    suspend fun submitVote(request: CoreVoteRequest): Result<Unit>
}
