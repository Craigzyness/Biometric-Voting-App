package com.example.biometricvotingapp.di

import android.app.Application
import android.content.Context
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.util.PlayIntegrityService // Import PlayIntegrityService
import com.example.biometricvotingapp.utils.SecurityUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiService.instance
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService
        // @ApplicationContext context: Context // VotingRepository currently does not take context
    ): AuthRepository {
        return VotingRepository(apiService)
    }

    // TODO: Refactor AnonymizedIdGenerator from 'object' to 'class' that takes Context for Hilt DI.
    // The current provider will not work as expected with 'object AnonymizedIdGenerator'.
    // This provider is written assuming AnonymizedIdGenerator becomes:
    // class AnonymizedIdGenerator @Inject constructor(@ApplicationContext private val context: Context)
    @Provides
    @Singleton
    fun provideAnonymizedIdGenerator(@ApplicationContext context: Context): AnonymizedIdGenerator {
        // return AnonymizedIdGenerator(context) // This would be the line if it were a class
        return com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Returning the object for now, Hilt won't inject this instance if constructor changes.
    }

    // TODO: Refactor SecurityUtil from 'object' to 'class' that takes Context for Hilt DI.
    // The current provider will not work as expected with 'object SecurityUtil'.
    // This provider is written assuming SecurityUtil becomes:
    // class SecurityUtil @Inject constructor(@ApplicationContext private val context: Context)
    @Provides
    @Singleton
    fun provideSecurityUtil(@ApplicationContext context: Context): SecurityUtil {
        // return SecurityUtil(context) // This would be the line if it were a class
        return com.example.biometricvotingapp.utils.SecurityUtil // Returning the object for now.
    }

    @Provides
    @Singleton
    fun providePlayIntegrityService(@ApplicationContext context: Context): PlayIntegrityService {
        // PlayIntegrityService is already a class designed for Hilt injection
        return PlayIntegrityService(context)
    }
}
