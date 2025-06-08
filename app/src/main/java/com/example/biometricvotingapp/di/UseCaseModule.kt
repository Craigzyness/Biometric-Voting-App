package com.example.biometricvotingapp.di

// Corrected import for AuthRepository, assuming it's now in :core
import com.example.biometricvotingapp.core.domain.repository.AuthRepository
// UseCase classes are currently still in app module's domain.usecase package
import com.example.biometricvotingapp.domain.repository.AuthRepository
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
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Corrected path
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // Using SingletonComponent for stateless use cases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Assuming UseCases are stateless and can be singletons
Biometric-Voting-App
object UseCaseModule {

    @Provides
    @Singleton
    fun provideRegisterVoterUseCase(
        anonymizedIdGenerator: AnonymizedIdGenerator,
        authRepository: AuthRepository
    ): RegisterVoterUseCase {
        return RegisterVoterUseCase(anonymizedIdGenerator, authRepository)
        anonymizedIdGenerator: AnonymizedIdGenerator, // Hilt will get this from AppModule
        authRepository: AuthRepository          // Hilt will get this from AppModule
    ): RegisterVoterUseCase {
        return RegisterVoterUseCase(anonymizedIdGenerator, authRepository)
        // Dispatcher is defaulted in UseCase constructor
Biometric-Voting-App
    }

    @Provides
    @Singleton
    fun provideLoginUserUseCase(

        anonymizedIdGenerator: AnonymizedIdGenerator
    ): LoginUserUseCase {
        return LoginUserUseCase(anonymizedIdGenerator)
        anonymizedIdGenerator: AnonymizedIdGenerator // Hilt will get this from AppModule
    ): LoginUserUseCase {
        return LoginUserUseCase(anonymizedIdGenerator)
        // Dispatcher is defaulted
Biometric-Voting-App
    }

    @Provides
    @Singleton
    fun provideGetElectionsUseCase(
        authRepository: AuthRepository
    ): GetElectionsUseCase {
        return GetElectionsUseCase(authRepository)
        authRepository: AuthRepository // Hilt will get this from AppModule
    ): GetElectionsUseCase {
        return GetElectionsUseCase(authRepository)
        // Dispatcher is defaulted
Biometric-Voting-App
    }

    @Provides
    @Singleton
    fun provideSubmitVoteUseCase(
        authRepository: AuthRepository
    ): SubmitVoteUseCase {
        return SubmitVoteUseCase(authRepository)
        authRepository: AuthRepository // Hilt will get this from AppModule
    ): SubmitVoteUseCase {
        return SubmitVoteUseCase(authRepository)
        // Dispatcher is defaulted
Biometric-Voting-App
    }
}
