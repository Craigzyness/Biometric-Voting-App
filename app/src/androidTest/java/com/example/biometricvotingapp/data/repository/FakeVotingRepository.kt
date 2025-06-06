package com.example.biometricvotingapp.data.repository

import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.network.dto.RegistrationRequest
import com.example.biometricvotingapp.data.network.dto.RegistrationResponse
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.example.biometricvotingapp.data.network.dto.VoterDto
import com.example.biometricvotingapp.data.network.dto.VoteDetailsDto

/**
 * Fake implementation of VotingRepository for instrumented tests.
 * Allows controlling the responses of its methods for testing UI states.
 */
class FakeVotingRepository : VotingRepository(ApiService.instance) { // Pass real ApiService.instance, but methods are overridden

    var electionsToReturn: List<ElectionDto>? = null
    var registrationResponseToReturn: RegistrationResponse? = null
    var voteResponseToReturn: VoteResponse? = null

    var shouldReturnError: Boolean = false
    var generalErrorMessage: String = "Fake network error"

    // --- Overrides for getElections ---
    private var getElectionsDelay: Long = 0
    fun setElectionsDelay(delayMillis: Long) {
        getElectionsDelay = delayMillis
    }
    override suspend fun getElections(): Result<List<ElectionDto>> {
        kotlinx.coroutines.delay(getElectionsDelay) // Simulate delay
        return when {
            shouldReturnError -> Result.failure(Exception(generalErrorMessage))
            electionsToReturn != null -> Result.success(electionsToReturn!!)
            else -> Result.success(emptyList()) // Default to empty list
        }
    }

    // --- Overrides for registerVoter ---
    override suspend fun registerVoter(anonymizedVoterId: String): Result<RegistrationResponse> {
        return when {
            shouldReturnError -> Result.failure(Exception(generalErrorMessage))
            registrationResponseToReturn != null -> Result.success(registrationResponseToReturn!!)
            else -> { // Default success for registration if not specified
                Result.success(
                    RegistrationResponse(
                        message = "Voter registered successfully (fake).",
                        voter = VoterDto(
                            id = "fake-db-id-${System.currentTimeMillis()}",
                            anonymizedVoterId = anonymizedVoterId,
                            registrationTimestamp = java.time.Instant.now().toString(),
                            isEligible = true
                        )
                    )
                )
            }
        }
    }

    // --- Overrides for submitVote ---
    override suspend fun submitVote(voteRequest: VoteRequest): Result<VoteResponse> {
        return when {
            shouldReturnError -> Result.failure(Exception(generalErrorMessage))
            voteResponseToReturn != null -> Result.success(voteResponseToReturn!!)
            else -> { // Default success for vote submission if not specified
                Result.success(
                    VoteResponse(
                        message = "Vote submitted successfully (fake).",
                        vote = VoteDetailsDto(
                            voteId = "fake-vote-id-${System.currentTimeMillis()}",
                            electionId = voteRequest.electionId,
                            selectedOption = voteRequest.selectedOption,
                            castAtTimestamp = java.time.Instant.now().toString()
                        )
                    )
                )
            }
        }
    }

    fun reset() {
        electionsToReturn = null
        registrationResponseToReturn = null
        voteResponseToReturn = null
        shouldReturnError = false
        generalErrorMessage = "Fake network error"
        getElectionsDelay = 0
    }
}
