package com.example.biometricvotingapp.ui.screens.electionlist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.domain.usecase.GetElectionsUseCase
import com.example.biometricvotingapp.domain.usecase.LoginUserUseCase // Import LoginUserUseCase
import com.example.biometricvotingapp.domain.usecase.UserNotRegisteredException // Import custom exception
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
    private lateinit var mockGetElectionsUseCase: GetElectionsUseCase
    private lateinit var mockLoginUserUseCase: LoginUserUseCase // Add mock for LoginUserUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockGetElectionsUseCase = mockk()
        mockLoginUserUseCase = mockk() // Initialize LoginUserUseCase mock

        // Default mocks for use cases, can be overridden in specific tests
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success("defaultVoterId")
        coEvery { mockGetElectionsUseCase.invoke(any()) } returns Result.success(emptyList())

        // Instantiate ViewModel with all mocked dependencies
        viewModel = ElectionListViewModel(mockApplication, mockGetElectionsUseCase, mockLoginUserUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init fetches current user and loads elections, results in Empty for default mocks`() = runTest(testDispatcher) {
        // ViewModel calls fetchCurrentUserAndLoadElections in init block.
        // Default mocks: login success ("defaultVoterId"), getElections success (emptyList)
        advanceUntilIdle() // Let the init block and internal calls complete

        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
        coVerify { mockLoginUserUseCase.invoke() }
        coVerify { mockGetElectionsUseCase.invoke("defaultVoterId") }
    }

    @Test
    fun `init when LoginUserUseCase returns null user ID sets UserNotLoggedIn state`() = runTest(testDispatcher) {
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(null) // Simulate no logged-in user

        // Re-create viewModel with this specific mock for LoginUserUseCase for this test's init
        viewModel = ElectionListViewModel(mockApplication, mockGetElectionsUseCase, mockLoginUserUseCase)
        advanceUntilIdle()

        assertEquals(ElectionListUiState.UserNotLoggedIn, viewModel.uiState.value)
        coVerify { mockLoginUserUseCase.invoke() }
        coVerify(exactly = 0) { mockGetElectionsUseCase.invoke(any()) } // GetElectionsUseCase should not be called
    }

    @Test
    fun `init when LoginUserUseCase fails with UserNotRegisteredException sets UserNotLoggedIn state`() = runTest(testDispatcher) {
        coEvery { mockLoginUserUseCase.invoke() } returns Result.failure(UserNotRegisteredException("Not registered"))

        viewModel = ElectionListViewModel(mockApplication, mockGetElectionsUseCase, mockLoginUserUseCase)
        advanceUntilIdle()

        assertEquals(ElectionListUiState.UserNotLoggedIn, viewModel.uiState.value)
        coVerify { mockLoginUserUseCase.invoke() }
        coVerify(exactly = 0) { mockGetElectionsUseCase.invoke(any()) }
    }

    @Test
    fun `init when LoginUserUseCase fails with generic exception sets Error state`() = runTest(testDispatcher) {
        val errorMessage = "Generic login error"
        coEvery { mockLoginUserUseCase.invoke() } returns Result.failure(Exception(errorMessage))

        viewModel = ElectionListViewModel(mockApplication, mockGetElectionsUseCase, mockLoginUserUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Error)
        assertEquals("Failed to retrieve user session: $errorMessage", (state as ElectionListUiState.Error).message)
        coVerify { mockLoginUserUseCase.invoke() }
        coVerify(exactly = 0) { mockGetElectionsUseCase.invoke(any()) }
    }

    @Test
    fun `refreshElections success with data from use case sets Success state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        val domainElections = listOf(
            Election(id = "id1", title = "Title1", description = "Desc1", options = listOf("A"), hasVoted = true),
            Election(id = "id2", title = "Title2", description = "Desc2", options = listOf("B"), hasVoted = false)
        )
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(domainElections)

        // ViewModel is already created in setUp, init calls fetchCurrentUserAndLoadElections.
        // To test refreshElections, we call it again after init has run.
        // Reset mocks or ensure new coEvery overrides if init already made calls.
        // For simplicity, let's assume init ran with default mocks leading to Empty or UserNotLoggedIn.
        // Then we call refreshElections.
        clearMocks(mockLoginUserUseCase, mockGetElectionsUseCase) // Clear init calls
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(domainElections)

        viewModel.refreshElections() // Call the public refresh function
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Success)
        assertEquals(domainElections, (state as ElectionListUiState.Success).elections)
        coVerify(exactly = 1) { mockLoginUserUseCase.invoke() } // Verifies it was called by refresh
        coVerify(exactly = 1) { mockGetElectionsUseCase.invoke(testVoterId) } // Verifies it was called by refresh
    }

    @Test
    fun `refreshElections success with empty list from use case sets Empty state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(emptyList())

        clearMocks(mockLoginUserUseCase, mockGetElectionsUseCase) // Clear init calls
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.success(emptyList())

        viewModel.refreshElections()
        advanceUntilIdle()

        assertEquals(ElectionListUiState.Empty, viewModel.uiState.value)
        coVerify(exactly = 1) { mockLoginUserUseCase.invoke() }
        coVerify(exactly = 1) { mockGetElectionsUseCase.invoke(testVoterId) }
    }

    @Test
    fun `refreshElections failure from GetElectionsUseCase sets Error state`() = runTest(testDispatcher) {
        val testVoterId = "voter-test-id"
        val errorMessage = "Network error from GetElectionsUseCase"
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.failure(Exception(errorMessage))

        clearMocks(mockLoginUserUseCase, mockGetElectionsUseCase)
        coEvery { mockLoginUserUseCase.invoke() } returns Result.success(testVoterId)
        coEvery { mockGetElectionsUseCase.invoke(testVoterId) } returns Result.failure(Exception(errorMessage))

        viewModel.refreshElections()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ElectionListUiState.Error)
        assertEquals(errorMessage, (state as ElectionListUiState.Error).message)
        coVerify(exactly = 1) { mockLoginUserUseCase.invoke() }
        coVerify(exactly = 1) { mockGetElectionsUseCase.invoke(testVoterId) }
    }
}
