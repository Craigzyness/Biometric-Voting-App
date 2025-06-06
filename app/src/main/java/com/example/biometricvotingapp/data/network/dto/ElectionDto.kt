package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class ElectionDto(
    @SerializedName("id") val id: String,
    @SerializedName("electionCode") val electionCode: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("options") val options: List<String>,
    @SerializedName("startTimestamp") val startTimestamp: String, // Assuming ISO String
    @SerializedName("endTimestamp") val endTimestamp: String, // Assuming ISO String
    @SerializedName("status") val status: String,
    @SerializedName("hasVoted") val hasVoted: Boolean? = null // Added nullable hasVoted
)
