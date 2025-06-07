package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.data.network.dto.VoteDetailsDto
import com.example.biometricvotingapp.data.network.dto.VoteRequest
import com.example.biometricvotingapp.data.network.dto.VoteResponse
import com.example.biometricvotingapp.domain.repository.AuthRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SubmitVoteUseCaseTest {
    private lateinit var submitVoteUseCase: SubmitVoteUseCase
    private val mockAuthRepository: AuthRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        submitVoteUseCase = SubmitVoteUseCase(mockAuthRepository, testDispatcher)
    }

    @Test
    fun `invoke success - calls repository submitVote and returns success result`() = runTest(testDispatcher) {
        val voteRequest = VoteRequest("voter1", "election1", "option1", "proof", "iv")
        val voteDetailsDto = VoteDetailsDto("voteId123", "election1", "option1", "timestamp")
        val expectedRepoResponse = VoteResponse("Vote submitted successfully from repo", voteDetailsDto)
        val expectedResult = Result.success(expectedRepoResponse)

        coEvery { mockAuthRepository.submitVote(voteRequest) } returns expectedResult

        val actualResult = submitVoteUseCase(voteRequest)

        assertThat(actualResult.isSuccess).isTrue()
        assertThat(actualResult.getOrNull()).isEqualTo(expectedRepoResponse)
        coVerify(exactly = 1) { mockAuthRepository.submitVote(voteRequest) }
    }

    @Test
    fun `invoke failure - repository returns failure, use case propagates failure`() = runTest(testDispatcher) {
        val voteRequest = VoteRequest("voter1", "election1", "option1", "proof", "iv")
        val exception = Exception("Network error from repository")
        val expectedResult = Result.failure<VoteResponse>(exception)

        coEvery { mockAuthRepository.submitVote(voteRequest) } returns expectedResult

        val actualResult = submitVoteUseCase(voteRequest)

        assertThat(actualResult.isFailure).isTrue()
        assertThat(actualResult.exceptionOrNull()).isEqualTo(exception)
        coVerify(exactly = 1) { mockAuthRepository.submitVote(voteRequest) }
    }

    @Test
    fun `invoke failure - repository throws exception, use case catches and returns failure`() = runTest(testDispatcher) {
        val voteRequest = VoteRequest("voter1", "election1", "option1", "proof", "iv")
        val exception = RuntimeException("Database connection failed")

        coEvery { mockAuthRepository.submitVote(voteRequest) } throws exception

        val actualResult = submitVoteUseCase(voteRequest)

        assertThat(actualResult.isFailure).isTrue()
        assertThat(actualResult.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        assertThat(actualResult.exceptionOrNull()?.message).isEqualTo("Database connection failed")
        coVerify(exactly = 1) { mockAuthRepository.submitVote(voteRequest) }
    }
}
