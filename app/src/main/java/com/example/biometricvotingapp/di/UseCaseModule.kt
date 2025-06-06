package com.example.biometricvotingapp.di

import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase
import com.example.biometricvotingapp.domain.usecase.RegisterVoterUseCase
import com.example.biometricvotingapp.domain.usecase.SubmitVoteUseCase
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Corrected path
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // Using SingletonComponent for stateless use cases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Assuming UseCases are stateless and can be singletons
object UseCaseModule {

    @Provides
    @Singleton
    fun provideRegisterVoterUseCase(
        anonymizedIdGenerator: AnonymizedIdGenerator, // Hilt will get this from AppModule
        authRepository: AuthRepository          // Hilt will get this from AppModule
    ): RegisterVoterUseCase {
        return RegisterVoterUseCase(anonymizedIdGenerator, authRepository)
        // Dispatcher is defaulted in UseCase constructor
    }

    @Provides
    @Singleton
    fun provideLoginUserUseCase(
        anonymizedIdGenerator: AnonymizedIdGenerator // Hilt will get this from AppModule
    ): LoginUserUseCase {
        return LoginUserUseCase(anonymizedIdGenerator)
        // Dispatcher is defaulted
    }

    @Provides
    @Singleton
    fun provideGetElectionsUseCase(
        authRepository: AuthRepository // Hilt will get this from AppModule
    ): GetElectionsUseCase {
        return GetElectionsUseCase(authRepository)
        // Dispatcher is defaulted
    }

    @Provides
    @Singleton
    fun provideSubmitVoteUseCase(
        authRepository: AuthRepository // Hilt will get this from AppModule
    ): SubmitVoteUseCase {
        return SubmitVoteUseCase(authRepository)
        // Dispatcher is defaulted
    }
}
