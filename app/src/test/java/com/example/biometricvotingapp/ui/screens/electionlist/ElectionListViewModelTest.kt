package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
// Removed ElectionDto import as VM now gets domain models from UseCase
// Removed VotingRepository import
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase // Import the use case
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

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ElectionListViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockGetElectionsUseCase: GetElectionsUseCase // Mock the use case

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockGetElectionsUseCase = mockk() // Create a mock for GetElectionsUseCase
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // Helper to create ViewModel instance for tests
    private fun createViewModel(anonymizedVoterId: String?): ElectionListViewModel {
        // Default mock for the use case, can be overridden in specific tests
        coEvery { mockGetElectionsUseCase.invoke(any()) } returns Result.success(emptyList())
        return ElectionListViewModel(mockApplication, mockGetElectionsUseCase, anonymizedVoterId)
    }

    @Test
    fun `initial state is Loading and loadElections is called, resulting in Empty for default mock`() = runTest(testDispatcher) {
        viewModel = createViewModel(null) // anonymizedVoterId is null
        advanceUntilIdle() // Let the init block and loadElections complete

        // Default mock in createViewModel returns Result.success(emptyList())
        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
        coVerify { mockGetElectionsUseCase.invoke(null) } // Verify use case was called with null
    }

    @Test
    fun `loadElections success with data from use case sets Success state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        // UseCase now returns domain models directly
        val domainElections = listOf(
            Election(id = "id1", title = "Title1", description = "Desc1", options = listOf("A"), hasVoted = true),
            Election(id = "id2", title = "Title2", description = "Desc2", options = listOf("B"), hasVoted = false)
        )
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(domainElections)

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Success)
        val elections = (state as ElectionListUiState.Success).elections
        assertEquals(2, elections.size)
        assertEquals(domainElections, elections) // Direct comparison as VM receives domain models
        assertEquals(true, elections.find { it.id == "id1" }?.hasVoted)
        assertEquals(false, elections.find { it.id == "id2" }?.hasVoted)
        coVerify { mockGetElectionsUseCase.invoke(testVoterId) }
    }

    @Test
    fun `loadElections with null anonymizedVoterId success with data from use case`() = runTest(testDispatcher) {
        val domainElectionsFromUseCase = listOf(
            Election(id = "id1", title = "Election Alpha", description = "Desc Alpha", options = listOf("A1"), hasVoted = true),
            Election(id = "id2", title = "Election Beta", description = "Desc Beta", options = listOf("B1"), hasVoted = false)
        )
        coEvery { mockGetElectionsUseCase.invoke(null) } returns Result.success(domainElectionsFromUseCase)

        viewModel = createViewModel(null) // Pass null voterId
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Success)
        assertEquals(domainElectionsFromUseCase, (state as ElectionListUiState.Success).elections)
        coVerify { mockGetElectionsUseCase.invoke(null) }
    }


    @Test
    fun `loadElections success with empty list from use case sets Empty state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(emptyList())

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
        coVerify { mockGetElectionsUseCase.invoke(testVoterId) }
    }

    @Test
    fun `loadElections failure from use case sets Error state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        val errorMessage = "Network error from use case"
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.failure(Exception(errorMessage))

        viewModel = createViewModel(testVoterId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Error)
        assertEquals(errorMessage, (state as ElectionListUiState.Error).message)
        coVerify { mockGetElectionsUseCase.invoke(testVoterId) }
    }
}
