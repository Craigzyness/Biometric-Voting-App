package com.example.biometricvotingapp.data.network

import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
import com.example.biometricvotingapp.data.network.dto.ElectionListResponse
import com.example.biometricvotingapp.data.network.dto.RegistrationRequest
import com.example.biometricvotingapp.data.network.dto.RegistrationResponse
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query // Added missing import for @Query

interface ApiService {

    @POST("register") // Path matches current mock backend
    suspend fun registerVoter(@Body request: RegistrationRequest): Response<RegistrationResponse>

    @GET("elections")
    suspend fun getElections(@Query("anonymizedVoterId") anonymizedVoterId: String? = null): Response<ElectionListResponse>

    @POST("submitVote")
    suspend fun submitVote(@Body request: VoteRequest): Response<VoteResponse>

    companion object {
        // Base URL now sourced from BuildConfig
        private val BASE_URL = BuildConfig.API_BASE_URL

        // Lazy-initialized Retrofit instance
        val instance: ApiService by lazy {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val gson = GsonBuilder()
                .create()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService::class.java)
        }
    }
}
