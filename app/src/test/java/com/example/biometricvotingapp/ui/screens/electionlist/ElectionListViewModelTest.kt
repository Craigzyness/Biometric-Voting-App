package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.repository.VotingRepository
import com.example.biometricvotingapp.domain.model.Election
import io.mockk.*
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

    private val testDispatcher = StandardTestDispatcher() // Use StandardTestDispatcher for more control

    private lateinit var viewModel: ElectionListViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockVotingRepository: VotingRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockVotingRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(anonymizedVoterId: String?): ElectionListViewModel {
        // Ensure the default mock for getElections is general enough or reset it if it causes issues.
        // For specific test cases, coEvery will override this.
        coEvery { mockVotingRepository.getElections(any()) } returns Result.success(emptyList())
        return ElectionListViewModel(mockApplication, mockVotingRepository, anonymizedVoterId)
    }

    @Test
    fun `initial state is Loading and loadElections is called`() = runTest(testDispatcher) {
        // ViewModel calls loadElections in init block.
        // Here, we verify the state after init has completed its work.
        // The createViewModel helper already sets up a default coEvery for getElections.
        viewModel = createViewModel(null)
        advanceUntilIdle()

        // If getElections returns emptyList by default in createViewModel's coEvery, then Empty state is expected.
        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadElections with null anonymizedVoterId success with data maps hasVoted from DTO`() = runTest(testDispatcher) {
        val electionDtos = listOf(
            ElectionDto("id1", "E1", "Title1", "Desc1", listOf("A"), "s1", "e1", "ACTIVE", hasVoted = true),
            ElectionDto("id2", "E2", "Title2", "Desc2", listOf("B"), "s2", "e2", "ACTIVE", hasVoted = null) // null hasVoted from backend
        )
        coEvery { mockVotingRepository.getElections(null) } returns Result.success(electionDtos)

        viewModel = createViewModel(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Success)
        val elections = (state as ElectionListUiState.Success).elections
        assertEquals(2, elections.size)
        // ViewModel mapping logic: hasVoted = dto.hasVoted ?: false
        assertEquals(true, elections.find { it.id == "id1" }?.hasVoted) // True from DTO becomes true
        assertEquals(false, elections.find { it.id == "id2" }?.hasVoted) // Null from DTO becomes false
    }

    @Test
    fun `loadElections with valid anonymizedVoterId success maps hasVoted correctly from DTO`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        val electionDtos = listOf(
            ElectionDto("id1", "E1", "Title1", "Desc1", listOf("A"), "s1", "e1", "ACTIVE", hasVoted = true),
            ElectionDto("id2", "E2", "Title2", "Desc2", listOf("B"), "s2", "e2", "ACTIVE", hasVoted = false),
            ElectionDto("id3", "E3", "Title3", "Desc3", listOf("C"), "s3", "e3", "ACTIVE", null)
        )
        coEvery { mockVotingRepository.getElections(testVoterId) } returns Result.success(electionDtos)

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Success)
        val elections = (state as ElectionListUiState.Success).elections
        assertEquals(3, elections.size)
        assertEquals(true, elections.find { it.id == "id1" }?.hasVoted)
        assertEquals(false, elections.find { it.id == "id2" }?.hasVoted)
        assertEquals(false, elections.find { it.id == "id3" }?.hasVoted) // Null from DTO maps to false
    }

    @Test
    fun `loadElections success with empty list sets Empty state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        coEvery { mockVotingRepository.getElections(testVoterId) } returns Result.success(emptyList())

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadElections failure sets Error state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        val errorMessage = "Network error"
        coEvery { mockVotingRepository.getElections(testVoterId) } returns Result.failure(Exception(errorMessage))

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Error)
        assertEquals(errorMessage, (state as ElectionListUiState.Error).message)
    }
}
