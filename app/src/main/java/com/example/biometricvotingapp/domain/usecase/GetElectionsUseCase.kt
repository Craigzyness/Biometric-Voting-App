package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.example.biometricvotingapp.data.network.dto.ElectionDto // Corrected DTO import path
import com.example.biometricvotingapp.data.network.dto.OptionDto // Assuming OptionDto for complex options
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// This interface would typically be in domain/repository/AuthRepository.kt
// For the purpose of this file's compilation, it's assumed to exist.
// interface AuthRepository {
//    suspend fun getElections(voterId: String?): Result<List<ElectionDto>>
// }

// Assuming DTO structure for OptionDto if options are complex
// package com.example.biometricvotingapp.data.network.dto
// data class OptionDto(val id: String, val name: String, val description: String?)


class GetElectionsUseCase(
    private val authRepository: AuthRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(anonymizedVoterId: String?): Result<List<Election>> {
        return withContext(defaultDispatcher) {
            try {
                val resultDto : Result<List<ElectionDto>> = authRepository.getElections(anonymizedVoterId)
                resultDto.map { dtoList ->
                    dtoList.map { electionDto ->
                        // This mapping assumes Election domain model is richer and OptionDto exists.
                        // Current ElectionDto.options is List<String>.
                        // Current Election domain model is simpler.
                        // This will only fully work if those are updated.
                        Election(
                            id = electionDto.id,
                            title = electionDto.title, // Using 'title' as per current DTO/Domain. Prompt used 'name'.
                            description = electionDto.description ?: "",
                            // startTimestamp = electionDto.startTimestamp, // Requires domain model update & Date parsing
                            // endDate = electionDto.endTimestamp,       // Requires domain model update & Date parsing
                            options = electionDto.options, // Kept as List<String> to match current DTO/Domain
                                                           // If options were complex DTOs (OptionDto), mapping would be:
                                                           // options = electionDto.options.map { optionDto ->
                                                           //     Election.Option(
                                                           //         id = optionDto.id,
                                                           //         name = optionDto.name,
                                                           //         description = optionDto.description ?: ""
                                                           //     )
                                                           // },
                            hasVoted = electionDto.hasVoted ?: false
                            // electionCode = electionDto.electionCode, // Requires domain model update
                            // status = electionDto.status             // Requires domain model update
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
