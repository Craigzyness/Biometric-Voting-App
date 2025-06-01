package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class ElectionDto(
    @SerializedName("id")
    val id: String, // UUID from backend
    @SerializedName("electionCode")
    val electionCode: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?, // Nullable as per V2 spec
    @SerializedName("options")
    val options: List<String>, // Assuming options are still List<String> from JSON
    @SerializedName("startTimestamp")
    val startTimestamp: String, // ISO date string
    @SerializedName("endTimestamp")
    val endTimestamp: String, // ISO date string
    @SerializedName("status")
    val status: String
)

// The ElectionResponse typealias (List<ElectionDto>) is removed.
// A new ElectionListResponse.kt will be created for the object wrapper { "elections": [...] }.
