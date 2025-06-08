package com.example.biometricvotingapp.di

import android.content.Context
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.repository.VotingRepository
// Corrected import for AuthRepository, assuming it's now in :core
import com.example.biometricvotingapp.core.domain.repository.AuthRepository
import com.example.biometricvotingapp.core.common.SecureSaltProvider
import com.example.biometricvotingapp.core.common.StableIdentifierProvider
import com.example.biometricvotingapp.core.security.PlayIntegrityService
import com.example.biometricvotingapp.core.security.SecurityUtil
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
        // VotingRepository (in :app) implements AuthRepository (in :core)
    ): AuthRepository {
        return VotingRepository(apiService)
    }

    // AnonymizedIdGenerator is a class in :core:security with @Inject constructor and @Singleton.
    // Hilt will provide it automatically as long as its dependencies
    // (SecureSaltProvider, StableIdentifierProvider from :core:common) are provided.
    // No explicit provider needed for AnonymizedIdGenerator itself.

    @Provides
    @Singleton
    fun provideSecureSaltProvider(@ApplicationContext context: Context): SecureSaltProvider {
        // This assumes SecureSaltProvider is refactored to a class in core.common taking Context.
        // TODO: Refactor SecureSaltProvider from 'object' to 'class' that takes Context for Hilt DI.
        return com.example.biometricvotingapp.core.common.SecureSaltProvider(context)
    }

    @Provides
    @Singleton
    fun provideStableIdentifierProvider(@ApplicationContext context: Context): StableIdentifierProvider {
        // This assumes StableIdentifierProvider is refactored to a class in core.common taking Context.
        // TODO: Refactor StableIdentifierProvider from 'object' to 'class' that takes Context for Hilt DI.
        return com.example.biometricvotingapp.core.common.StableIdentifierProvider(context)
    }

    // SecurityUtil has been refactored to an injectable class in core.security
    @Provides
    @Singleton
    fun provideSecurityUtil(@ApplicationContext context: Context): SecurityUtil {
        return SecurityUtil(context)
    }

    @Provides
    @Singleton
    fun providePlayIntegrityService(@ApplicationContext context: Context): PlayIntegrityService {
        return com.example.biometricvotingapp.core.security.PlayIntegrityService(context)
    }
}
