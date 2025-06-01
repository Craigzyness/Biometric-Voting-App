package com.example.biometricvotingapp.data.repository

import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.network.dto.RegistrationRequest
import com.example.biometricvotingapp.data.network.dto.RegistrationResponse
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
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
                    // Simple error handling: include status code and error message if available
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("Registration failed: ${response.code()} - $errorBody"))
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
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("Fetching elections failed: ${response.code()} - $errorBody"))
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
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("Submitting vote failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error submitting vote: ${e.message}", e))
            }
        }
    }
}
