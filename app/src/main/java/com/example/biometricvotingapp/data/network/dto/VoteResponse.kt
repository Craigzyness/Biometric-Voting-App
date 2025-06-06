package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class VoteResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("vote")
    val vote: VoteDetailsDto
)

data class VoteDetailsDto(
    @SerializedName("voteId") // Corresponds to 'id' in DB table Votes, returned by backend
    val voteId: String,
    @SerializedName("electionId") // Corresponds to 'election_id' in DB table Votes
    val electionId: String,
    @SerializedName("selectedOption") // Corresponds to 'selected_option_value' in DB table Votes
    val selectedOption: String,
    @SerializedName("castAtTimestamp") // Corresponds to 'cast_at_timestamp' in DB table Votes
    val castAtTimestamp: String // Assuming ISO date string
)
