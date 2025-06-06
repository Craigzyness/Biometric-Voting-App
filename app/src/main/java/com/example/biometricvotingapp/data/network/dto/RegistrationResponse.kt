package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class RegistrationResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("voter")
    val voter: VoterDto
)

data class VoterDto(
    @SerializedName("id")
    val id: String, // UUID from backend
    @SerializedName("anonymizedVoterId") // Matches V2 spec field name "anonymizedVoterId"
    val anonymizedVoterId: String,
    @SerializedName("registrationTimestamp") // Matches V2 spec field name "registrationTimestamp"
    val registrationTimestamp: String, // Assuming ISO date string
    @SerializedName("isEligible") // Matches V2 spec field name "isEligible"
    val isEligible: Boolean
)
