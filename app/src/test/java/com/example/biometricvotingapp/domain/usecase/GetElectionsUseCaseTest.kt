package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.data.network.dto.ElectionDto // Corrected DTO path
// Removed OptionDto import as current ElectionDto.options is List<String>
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.repository.AuthRepository // UseCase depends on AuthRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
// Removed java.util.Date as DTOs use String for dates

@ExperimentalCoroutinesApi
class GetElectionsUseCaseTest {
    private lateinit var getElectionsUseCase: GetElectionsUseCase
    private val mockAuthRepository: AuthRepository = mockk() // Mock AuthRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        getElectionsUseCase = GetElectionsUseCase(mockAuthRepository, testDispatcher)
    }

    // Helper function to create ElectionDto instances for testing
    private fun createElectionDto(
        id: String,
        title: String,
        description: String?,
        options: List<String>, // Simple list of strings
        electionCode: String, // Field exists in DTO
        status: String,       // Field exists in DTO
        startTimestamp: String, // Field exists in DTO
        endTimestamp: String,   // Field exists in DTO
        hasVoted: Boolean?
    ): ElectionDto {
        return ElectionDto(
            id = id,
            electionCode = electionCode,
            title = title,
            description = description,
            options = options,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            status = status,
            hasVoted = hasVoted
        )
    }

    @Test
    fun `invoke success - fetches and maps DTOs to simplified domain Elections correctly`() = runTest(testDispatcher) {
        val voterId = "voter1"
        val electionDtoList = listOf(
            createElectionDto(
                id = "1", title = "Election 1", description = "Desc 1",
                options = listOf("Opt1A", "Opt1B"), electionCode = "ELEC1", status = "ACTIVE",
                startTimestamp = "2023-01-01T00:00:00Z", endTimestamp = "2023-01-31T23:59:59Z", hasVoted = true
            ),
            createElectionDto(
                id = "2", title = "Election 2", description = null, // Null description
                options = listOf("Opt2A"), electionCode = "ELEC2", status = "PENDING",
                startTimestamp = "2023-02-01T00:00:00Z", endTimestamp = "2023-02-28T23:59:59Z", hasVoted = null // Null hasVoted
            )
        )
        coEvery { mockAuthRepository.getElections(voterId) } returns Result.success(electionDtoList)

        val result = getElectionsUseCase.invoke(voterId)

        assertThat(result.isSuccess).isTrue()
        val elections = result.getOrNull()
        assertThat(elections).isNotNull()
        assertThat(elections!!.size).isEqualTo(2)

        // Verify mapping based on the GetElectionsUseCase created in this subtask's Step 1
        // which maps only the fields currently present in the simpler Election.kt domain model
        val election1 = elections.find { it.id == "1" }
        assertThat(election1).isNotNull()
        assertThat(election1!!.title).isEqualTo("Election 1")
        assertThat(election1.description).isEqualTo("Desc 1")
        assertThat(election1.options).isEqualTo(listOf("Opt1A", "Opt1B"))
        assertThat(election1.hasVoted).isTrue()
        // Note: electionCode, status, startTimestamp, endTimestamp are NOT asserted here
        // because the current GetElectionsUseCase (from Step 1 of this subtask)
        // does not map them to the current simple Election domain model.

        val election2 = elections.find { it.id == "2" }
        assertThat(election2).isNotNull()
        assertThat(election2!!.title).isEqualTo("Election 2")
        assertThat(election2.description).isEqualTo("") // Null description mapped to empty string
        assertThat(election2.options).isEqualTo(listOf("Opt2A"))
        assertThat(election2.hasVoted).isFalse() // Null hasVoted mapped to false

        coVerify(exactly = 1) { mockAuthRepository.getElections(voterId) }
    }

    @Test
    fun `invoke success - repository returns empty list`() = runTest(testDispatcher) {
        val voterId = "voterEmpty"
        coEvery { mockAuthRepository.getElections(voterId) } returns Result.success(emptyList())

        val result = getElectionsUseCase.invoke(voterId)

        assertThat(result.isSuccess).isTrue()
        val elections = result.getOrNull()
        assertThat(elections).isNotNull()
        assertThat(elections!!.isEmpty()).isTrue()
        coVerify(exactly = 1) { mockAuthRepository.getElections(voterId) }
    }

    @Test
    fun `invoke failure - repository returns error`() = runTest(testDispatcher) {
        val voterId = "voterError"
        val exception = Exception("Network Error")
        coEvery { mockAuthRepository.getElections(voterId) } returns Result.failure(exception)

        val result = getElectionsUseCase.invoke(voterId)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
        coVerify(exactly = 1) { mockAuthRepository.getElections(voterId) }
    }

    @Test
    fun `invoke with null voterId calls repository with null`() = runTest(testDispatcher) {
        val electionDtoList = listOf(
            createElectionDto(
                id = "3", title = "Election 3", description = "Desc 3",
                options = listOf("Opt3A"), electionCode = "ELEC3", status = "CLOSED",
                startTimestamp = "2022-01-01T00:00:00Z", endTimestamp = "2022-01-31T23:59:59Z", hasVoted = false
            )
        )
        coEvery { mockAuthRepository.getElections(null) } returns Result.success(electionDtoList)

        val result = getElectionsUseCase.invoke(null) // Call with null voterId

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()!!.size).isEqualTo(1)
        coVerify(exactly = 1) { mockAuthRepository.getElections(null) }
    }
}
