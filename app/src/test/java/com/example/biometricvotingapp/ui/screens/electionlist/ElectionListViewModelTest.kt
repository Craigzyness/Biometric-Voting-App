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
    // ViewModel will be instantiated in each test where repository behavior needs to be defined before init
    // private lateinit var viewModel: ElectionListViewModel

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

    // Helper function to replace the repository in the ViewModel using reflection
    private fun replaceViewModelRepository(viewModel: ElectionListViewModel, newRepository: VotingRepository): VotingRepository {
        val field = viewModel.javaClass.getDeclaredField("votingRepository")
        field.isAccessible = true
        val originalRepository = field.get(viewModel) as VotingRepository
        field.set(viewModel, newRepository)
        return originalRepository
    }

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
    fun `initial state is Loading, then Success when elections are loaded`() = runTest(testDispatcher) {
        val mockDtoList = listOf(createTestElectionDto("1", "Election 1"))
        coEvery { mockVotingRepository.getElections() } returns Result.success(mockDtoList)

        val viewModel = ElectionListViewModel(mockApplication)
        // Replace repo *after* init has started but before its coroutine runs, or ensure mock is ready before init
        // The way VM is structured, init calls loadElections. So mock must be ready.
        // Thus, we must inject the mock or use the reflection helper *before* the VM's init's loadElections's coroutine executes.
        // The reflection helper is more for overriding an already existing field.
        // For init block testing, it's cleaner if repo is injected via constructor.
        // Workaround: create VM, then immediately replace repo, then trigger/wait for load.
        // Since loadElections is in init, we need to control the repo *before* VM is fully constructed or its init runs.
        // This test structure will be a bit tricky.

        // Let's re-instantiate ViewModel after setting up the mock for its internally created repo,
        // or better, use the reflection helper immediately.

        // Scenario: ViewModel is created, its init calls loadElections, which launches a coroutine.
        // We need to mock the repo call that will happen inside that coroutine.

        // For this test, we'll make the mock repo globally available via a companion object
        // or pass it to a factory. Since we don't have that, the reflection trick is the chosen evil.
        // The issue: `init` runs during `ElectionListViewModel(mockApplication)`
        // So `mockVotingRepository` needs to be setup *before* that line if the VM used it directly.
        // Since VM creates its own repo, we use reflection to swap it *after* creation but assuming
        // the coroutine in init hasn't completed yet (which `StandardTestDispatcher` helps with).

        val viewModelForTest = ElectionListViewModel(mockApplication)
        val originalRepo = replaceViewModelRepository(viewModelForTest, mockVotingRepository)

        // At this point, the ViewModel's init block has already launched loadElections.
        // The coroutine is now using mockVotingRepository.

        advanceUntilIdle() // Let the coroutine in init complete

        val state = viewModelForTest.uiState.value
        assertTrue("UI State should be Success, but was $state", state is ElectionListUiState.Success)
        val successState = state as ElectionListUiState.Success
        assertEquals(1, successState.elections.size)
        assertEquals(mockDtoList[0].title, successState.elections[0].title)

        replaceViewModelRepository(viewModelForTest, originalRepo) // Clean up
    }

    @Test
    fun `initial state is Loading, then Empty when no elections are loaded`() = runTest(testDispatcher) {
        coEvery { mockVotingRepository.getElections() } returns Result.success(emptyList())

        val viewModel = ElectionListViewModel(mockApplication)
        val originalRepo = replaceViewModelRepository(viewModel, mockVotingRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("UI State should be Empty, but was $state", state is ElectionListUiState.Empty)

        replaceViewModelRepository(viewModel, originalRepo)
    }

    @Test
    fun `initial state is Loading, then Error when elections fetch fails`() = runTest(testDispatcher) {
        val errorMessage = "Network Error"
        coEvery { mockVotingRepository.getElections() } returns Result.failure(Exception(errorMessage))

        val viewModel = ElectionListViewModel(mockApplication)
        val originalRepo = replaceViewModelRepository(viewModel, mockVotingRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("UI State should be Error, but was $state", state is ElectionListUiState.Error)
        assertEquals(errorMessage, (state as ElectionListUiState.Error).message)

        replaceViewModelRepository(viewModel, originalRepo)
    }

    @Test
    fun `loadElections transitions through Loading then to Success`() = runTest(testDispatcher) {
        val mockDtoList = listOf(createTestElectionDto("1", "Election Alpha"))
        coEvery { mockVotingRepository.getElections() } coAnswers { // Use coAnswers for delay
            kotlinx.coroutines.delay(100) // Simulate network delay
            Result.success(mockDtoList)
        }

        val viewModel = ElectionListViewModel(mockApplication)
        val originalRepo = replaceViewModelRepository(viewModel, mockVotingRepository)

        // Immediately after VM creation, init calls loadElections, which sets Loading
        assertEquals(ElectionListUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle() // Execute the coroutine including delay and subsequent updates

        val finalState = viewModel.uiState.value
        assertTrue("Final UI State should be Success, but was $finalState", finalState is ElectionListUiState.Success)
        assertEquals(1, (finalState as ElectionListUiState.Success).elections.size)
        assertEquals("Election Alpha", finalState.elections[0].title)

        replaceViewModelRepository(viewModel, originalRepo)
    }
}
