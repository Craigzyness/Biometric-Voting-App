package com.example.biometricvotingapp.data.network.dto

import com.google.gson.annotations.SerializedName

data class ElectionListResponse(
    @SerializedName("elections")
    val elections: List<ElectionDto>
)
