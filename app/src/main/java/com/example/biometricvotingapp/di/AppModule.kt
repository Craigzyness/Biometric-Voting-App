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
import android.app.Application
import android.content.Context
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.util.PlayIntegrityService // Import PlayIntegrityService
import com.example.biometricvotingapp.utils.SecurityUtil
import android.app.Application // For ApplicationContext if AnonymizedIdGenerator/SecurityUtil need it broadly
import android.content.Context
import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.repository.VotingRepository // Implementation for AuthRepository
import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Corrected path
import com.example.biometricvotingapp.utils.SecurityUtil // Corrected path
Biometric-Voting-App
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
Biometric-Voting-App
        return ApiService.instance
    }

    @Provides
    @Singleton
    fun provideAuthRepository(

        apiService: ApiService
        // VotingRepository (in :app) implements AuthRepository (in :core)


        apiService: ApiService
        // @ApplicationContext context: Context // VotingRepository currently does not take context
Biometric-Voting-App
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
    fun provideAnonymizedIdGenerator(@ApplicationContext context: Context): AnonymizedIdGenerator {
        return AnonymizedIdGenerator(context)
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
Biometric-Voting-App
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

    fun providePlayIntegrityService(@ApplicationContext context: Context): PlayIntegrityService {
        // PlayIntegrityService is already a class designed for Hilt injection
        return PlayIntegrityService(context)
    }

        apiService: ApiService,
        // @ApplicationContext context: Context // Assuming VotingRepository will be updated to take context
                                            // Current VotingRepository(apiService) only.
                                            // If VotingRepository needs context, its constructor must change.
                                            // For now, let's assume it only needs ApiService as per its current constructor.
                                            // If it needs context, the VotingRepository file itself has to be modified.
    ): AuthRepository {
        // Provide VotingRepository as the implementation for AuthRepository.
        // Current VotingRepository constructor: class VotingRepository(private val apiService: ApiService)
        // If it were to take context: VotingRepository(apiService, context)
        return VotingRepository(apiService) // Using current VotingRepository constructor
    }

    // The following providers assume AnonymizedIdGenerator and SecurityUtil
    // will be refactored from 'object' to 'class' and accept Context via constructor.
    // If they remain 'object's, they don't need to be provided by Hilt this way.
    // Their methods would continue to be called directly, e.g., AnonymizedIdGenerator.generate(context, ...).

    @Provides
    @Singleton

    fun providePlayIntegrityService(@ApplicationContext context: Context): PlayIntegrityService {
        return com.example.biometricvotingapp.core.security.PlayIntegrityService(context)
    }

    fun provideSecurityUtil(@ApplicationContext context: Context): SecurityUtil {
        // Similar to AnonymizedIdGenerator, SecurityUtil is currently an 'object'.
        // For Hilt to provide it as an injectable dependency that takes context,
        // SecurityUtil would need to be refactored into a class.
        // e.g., class SecurityUtil @Inject constructor(@ApplicationContext private val context: Context) { ... }
        // I will write this provider assuming this refactoring is intended.
        return com.example.biometricvotingapp.utils.SecurityUtil // This will also be problematic until SecurityUtil is a class.
                                                                // If it remains object, it's globally accessible.
    }

    // ApplicationContext is automatically provided by Hilt with @ApplicationContext
    // No need for:
    // @Provides
    // @Singleton
    // fun provideApplicationContext(application: Application): Context {
    //     return application.applicationContext
    // }
Biometric-Voting-App

}
