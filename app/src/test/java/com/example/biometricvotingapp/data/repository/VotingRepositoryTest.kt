package com.example.biometricvotingapp.data.repository

import com.example.biometricvotingapp.data.network.ApiService
import com.example.biometricvotingapp.data.network.dto.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
class VotingRepositoryTest {

    private lateinit var mockApiService: ApiService
    private lateinit var votingRepository: VotingRepository

    @Before
    fun setUp() {
        mockApiService = mockk()
        votingRepository = VotingRepository(mockApiService)
    }

    // --- registerVoter Tests ---

    @Test
    fun `registerVoter success returns Result_success`() = runTest {
        val mockRequest = RegistrationRequest("testVoterId123")
        val mockVoterDto = VoterDto("uuid-test", "testVoterId123", "2023-01-01T12:00:00Z", true)
        val mockResponseData = RegistrationResponse("Success", mockVoterDto)
        val mockSuccessResponse: Response<RegistrationResponse> = Response.success(mockResponseData)

        coEvery { mockApiService.registerVoter(mockRequest) } returns mockSuccessResponse

        val result = votingRepository.registerVoter("testVoterId123")

        assertTrue(result.isSuccess)
        assertEquals(mockResponseData, result.getOrNull())
    }

    @Test
    fun `registerVoter HTTP error returns Result_failure`() = runTest {
        val mockRequest = RegistrationRequest("testVoterId123")
        val errorResponseBody = "{\"error\":\"Registration failed\"}".toResponseBody(mockk())
        val mockErrorResponse: Response<RegistrationResponse> = Response.error(400, errorResponseBody)

        coEvery { mockApiService.registerVoter(mockRequest) } returns mockErrorResponse

        val result = votingRepository.registerVoter("testVoterId123")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Registration failed: 400") ?: false)
    }

    @Test
    fun `registerVoter network IOException returns Result_failure`() = runTest {
        val mockRequest = RegistrationRequest("testVoterId123")
        val ioException = IOException("Network unavailable")

        coEvery { mockApiService.registerVoter(mockRequest) } throws ioException

        val result = votingRepository.registerVoter("testVoterId123")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Network error during registration") ?: false)
        assertEquals(ioException, exception?.cause)
    }

    // --- getElections Tests ---

    @Test
    fun `getElections success returns Result_success with election list`() = runTest {
        val electionDto1 = ElectionDto("id1", "E1", "Desc1", "Desc1?", listOf("A","B"), "start1", "end1", "ACTIVE")
        val electionDto2 = ElectionDto("id2", "E2", "Desc2", "Desc2?", listOf("C","D"), "start2", "end2", "ACTIVE")
        val mockElectionList = listOf(electionDto1, electionDto2)
        val mockListResponse = ElectionListResponse(mockElectionList) // Wrapper object
        val mockSuccessResponse: Response<ElectionListResponse> = Response.success(mockListResponse)

        coEvery { mockApiService.getElections() } returns mockSuccessResponse

        val result = votingRepository.getElections()

        assertTrue(result.isSuccess)
        assertEquals(mockElectionList, result.getOrNull())
    }

    @Test
    fun `getElections HTTP error returns Result_failure`() = runTest {
        val errorResponseBody = "{\"error\":\"Cannot fetch\"}".toResponseBody(mockk())
        val mockErrorResponse: Response<ElectionListResponse> = Response.error(500, errorResponseBody)

        coEvery { mockApiService.getElections() } returns mockErrorResponse

        val result = votingRepository.getElections()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Fetching elections failed: 500") ?: false)
    }

    @Test
    fun `getElections network IOException returns Result_failure`() = runTest {
        val ioException = IOException("Network unavailable")
        coEvery { mockApiService.getElections() } throws ioException

        val result = votingRepository.getElections()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Network error fetching elections") ?: false)
        assertEquals(ioException, exception?.cause)
    }


    // --- submitVote Tests ---

    @Test
    fun `submitVote success returns Result_success`() = runTest {
        val mockVoteRequest = VoteRequest("voter1", "election1", "OptionA")
        val mockVoteDetailsDto = VoteDetailsDto("voteId1", "election1", "OptionA", "2023-01-01T13:00:00Z")
        val mockResponseData = VoteResponse("Vote submitted", mockVoteDetailsDto)
        val mockSuccessResponse: Response<VoteResponse> = Response.success(mockResponseData)

        coEvery { mockApiService.submitVote(mockVoteRequest) } returns mockSuccessResponse

        val result = votingRepository.submitVote(mockVoteRequest)

        assertTrue(result.isSuccess)
        assertEquals(mockResponseData, result.getOrNull())
    }

    @Test
    fun `submitVote HTTP error returns Result_failure`() = runTest {
        val mockVoteRequest = VoteRequest("voter1", "election1", "OptionA")
        val errorResponseBody = "{\"error\":\"Vote failed\"}".toResponseBody(mockk())
        val mockErrorResponse: Response<VoteResponse> = Response.error(409, errorResponseBody)

        coEvery { mockApiService.submitVote(mockVoteRequest) } returns mockErrorResponse

        val result = votingRepository.submitVote(mockVoteRequest)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Submitting vote failed: 409") ?: false)
    }

    @Test
    fun `submitVote network IOException returns Result_failure`() = runTest {
        val mockVoteRequest = VoteRequest("voter1", "election1", "OptionA")
        val ioException = IOException("Network unavailable")

        coEvery { mockApiService.submitVote(mockVoteRequest) } throws ioException

        val result = votingRepository.submitVote(mockVoteRequest)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Network error submitting vote") ?: false)
        assertEquals(ioException, exception?.cause)
    }
}
