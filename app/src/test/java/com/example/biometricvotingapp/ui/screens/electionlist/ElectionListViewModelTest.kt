package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.model.Election
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ElectionListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher() // Use StandardTestDispatcher for more control with advanceUntilIdle

    private lateinit var mockApplication: Application
    private lateinit var mockVotingRepository: VotingRepository
    private lateinit var viewModel: ElectionListViewModel // Instantiated in setUp with mocks

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockVotingRepository = mockk() // Create the mock repository

        // Instantiate ViewModel here, so it's fresh for each test, with the mock repository.
        // This assumes loadElections() in init block will use the mocked repository.
        coEvery { mockVotingRepository.getElections() } returns Result.success(emptyList()) // Default mock for init
        viewModel = ElectionListViewModel(mockApplication, mockVotingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // clearAllMocks() // Optional: if you want to ensure mocks are reset globally
    }

    // Reflection helper is no longer needed.

    private fun createTestElectionDto(id: String, title: String): ElectionDto {
        return ElectionDto(
            id = id,
            electionCode = "CODE_$id",
            title = title,
            description = "Description for $title",
            options = listOf("Option 1", "Option 2"),
            startTimestamp = "2023-01-01T00:00:00Z",
            endTimestamp = "2023-12-31T23:59:59Z",
            status = "ACTIVE"
        )
    }

    @Test
    fun `initial state is Loading, then Success when elections are loaded`() = runTest(testDispatcher) { // Test an already initialized VM
        val mockDtoList = listOf(createTestElectionDto("1", "Election 1"))
        // Override the default mock from setUp for this specific test case
        coEvery { mockVotingRepository.getElections() } returns Result.success(mockDtoList)

        // Re-initialize or trigger load explicitly if VM is designed for it.
        // Since loadElections is in init, we re-create for this specific mock behavior.
        val testViewModel = ElectionListViewModel(mockApplication, mockVotingRepository)

        advanceUntilIdle()

        val state = testViewModel.uiState.value
        assertTrue("UI State should be Success, but was $state", state is ElectionListUiState.Success)
        val successState = state as ElectionListUiState.Success
        assertEquals(1, successState.elections.size)
        assertEquals(mockDtoList[0].title, successState.elections[0].title)
    }

    @Test
    fun `initial state is Loading, then Empty when no elections are loaded`() = runTest(testDispatcher) {
        coEvery { mockVotingRepository.getElections() } returns Result.success(emptyList())

        // Re-create VM to trigger init with this specific mock setup
        val testViewModel = ElectionListViewModel(mockApplication, mockVotingRepository)

        advanceUntilIdle()

        val state = testViewModel.uiState.value
        assertTrue("UI State should be Empty, but was $state", state is ElectionListUiState.Empty)
    }

    @Test
    fun `initial state is Loading, then Error when elections fetch fails`() = runTest(testDispatcher) {
        val errorMessage = "Network Error"
        coEvery { mockVotingRepository.getElections() } returns Result.failure(Exception(errorMessage))

        // Re-create VM
        val testViewModel = ElectionListViewModel(mockApplication, mockVotingRepository)

        advanceUntilIdle()

        val state = testViewModel.uiState.value
        assertTrue("UI State should be Error, but was $state", state is ElectionListUiState.Error)
        assertEquals(errorMessage, (state as ElectionListUiState.Error).message)
    }

    @Test
    fun `loadElections transitions through Loading then to Success`() = runTest(testDispatcher) {
        val mockDtoList = listOf(createTestElectionDto("1", "Election Alpha"))
        coEvery { mockVotingRepository.getElections() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockDtoList)
        }

        // Re-create VM
        val testViewModel = ElectionListViewModel(mockApplication, mockVotingRepository)

        // Immediately after VM creation, init calls loadElections, which sets Loading
        assertEquals(ElectionListUiState.Loading, testViewModel.uiState.value)

        advanceUntilIdle()

        val finalState = testViewModel.uiState.value
        assertTrue("Final UI State should be Success, but was $finalState", finalState is ElectionListUiState.Success)
        assertEquals(1, (finalState as ElectionListUiState.Success).elections.size)
        assertEquals("Election Alpha", finalState.elections[0].title)
    }
}
