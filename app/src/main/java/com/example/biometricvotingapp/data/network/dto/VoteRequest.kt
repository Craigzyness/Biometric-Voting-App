package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class VoteRequest(
    @SerializedName("anonymizedVoterId")
    val anonymizedVoterId: String,
    @SerializedName("electionId")
    val electionId: String,
    @SerializedName("selectedOption")
    val selectedOption: String,
    @SerializedName("encrypted_proof")
    val encryptedProof: String?, // Base64 encoded encrypted data
    @SerializedName("iv")
    val iv: String? // Base64 encoded Initialization Vector
)
