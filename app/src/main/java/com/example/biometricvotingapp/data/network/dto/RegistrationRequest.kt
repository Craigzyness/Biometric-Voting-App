package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    @SerializedName("anonymizedVoterId")
    val anonymizedVoterId: String
)
