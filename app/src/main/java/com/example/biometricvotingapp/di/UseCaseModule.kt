package com.example.biometricvotingapp.di

// Corrected import for AuthRepository, assuming it's now in :core
import com.example.biometricvotingapp.core.domain.repository.AuthRepository
// UseCase classes are currently still in app module's domain.usecase package
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.domain.usecase.RegisterVoterUseCase
import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
// Corrected import for AnonymizedIdGenerator, assuming it's now in :core:security and is injectable
import com.example.biometricvotingapp.core.security.AnonymizedIdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideRegisterVoterUseCase(
        anonymizedIdGenerator: AnonymizedIdGenerator,
        authRepository: AuthRepository
    ): RegisterVoterUseCase {
        return RegisterVoterUseCase(anonymizedIdGenerator, authRepository)
    }

    @Provides
    @Singleton
    fun provideLoginUserUseCase(
        anonymizedIdGenerator: AnonymizedIdGenerator
    ): LoginUserUseCase {
        return LoginUserUseCase(anonymizedIdGenerator)
    }

    @Provides
    @Singleton
    fun provideGetElectionsUseCase(
        authRepository: AuthRepository
    ): GetElectionsUseCase {
        return GetElectionsUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSubmitVoteUseCase(
        authRepository: AuthRepository
    ): SubmitVoteUseCase {
        return SubmitVoteUseCase(authRepository)
    }
}
