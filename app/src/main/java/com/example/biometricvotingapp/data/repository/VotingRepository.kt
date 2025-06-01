package com.example.biometricvotingapp.data.repository

import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.network.dto.ErrorResponse
import com.example.biometricvotingapp.data.network.dto.RegistrationRequest
import com.example.biometricvotingapp.data.network.dto.RegistrationResponse
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository for handling voting related operations, including registration,
 * fetching elections, and submitting votes.
 *
 * For MVP, this directly uses the ApiService. More complex error handling or
 * data source management (e.g., caching) could be added later.
 */
class VotingRepository(private val apiService: ApiService) {

    suspend fun registerVoter(anonymizedVoterId: String): Result<RegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegistrationRequest(anonymizedVoterId)
                val response = apiService.registerVoter(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val parsedError = parseErrorBody(response)
                    Result.failure(Exception(parsedError ?: "Registration failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error during registration: ${e.message}", e))
            }
        }
    }

    suspend fun getElections(): Result<List<ElectionDto>> { // Return type changed to List<ElectionDto>
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getElections() // This now returns Response<ElectionListResponse>
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.elections) // Extract the list from the wrapper
                } else {
                    val parsedError = parseErrorBody(response)
                    Result.failure(Exception(parsedError ?: "Fetching elections failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error fetching elections: ${e.message}", e))
            }
        }
    }

    suspend fun submitVote(voteRequest: VoteRequest): Result<VoteResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.submitVote(voteRequest)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val parsedError = parseErrorBody(response)
                    Result.failure(Exception(parsedError ?: "Submitting vote failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error submitting vote: ${e.message}", e))
            }
        }
    }

    private fun <T> parseErrorBody(response: Response<T>): String? {
        val errorBodyString = response.errorBody()?.string()
        if (!errorBodyString.isNullOrBlank()) {
            return try {
                val gson = Gson()
                val errorResponse = gson.fromJson(errorBodyString, ErrorResponse::class.java)
                // Prepend HTTP code to the specific message from backend for more context
                "HTTP ${response.code()}: ${errorResponse?.message ?: "Could not parse error message."}"
            } catch (e: Exception) { // JsonSyntaxException or others
                // If parsing fails, return the raw string (or a part of it) if it's not too long,
                // or a generic parse error message.
                // For now, return a message indicating parse failure, plus original code.
                "HTTP ${response.code()}: Failed to parse error response. Raw error might be logged by interceptor."
            }
        }
        return null // No error body string to parse
    }
}
