package com.example.biometricvotingapp.core.domain.model // Or a sub-package like .request

// Using simple data classes for the interface contract within :core
// The implementation in :app will map these to network DTOs.

data class CoreRegistrationRequest(
    val anonymizedVoterId: String
)

data class CoreVoteRequest(
    val anonymizedVoterId: String,
    val electionId: String,
    val selectedOption: String,
    val encryptedProof: String, // Base64 encoded String
    val iv: String,             // Base64 encoded String
    val playIntegrityToken: String,
    val playIntegrityNonce: String
)

// It's often simpler for repository methods like submitVote to return Result<Unit>
// if no specific data from the response is needed by the domain layer.
// If a specific response structure is needed, define CoreVoteResponse as well.
// For now, assume Result<Unit> for submitVote.
// (No CoreVoteResponse needed based on AuthRepository definition below)
