package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Data class to represent a structured error response from the backend.
 * Assumes the backend sends errors in the format: { "error": "Error message content" }
 */
data class ErrorResponse(
    @SerializedName("error") // Matches the backend JSON key
    val message: String?
)
