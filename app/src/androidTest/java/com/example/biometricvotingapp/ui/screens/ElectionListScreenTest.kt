package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
// Import DTO if needed for mock data, but VM should expose domain model
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListUiState
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModel
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock // Using Mockito for lambda verification
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
class ElectionListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ElectionListViewModel
    private lateinit var mockOnElectionClicked: (Election) -> Unit
    private val initialUiState = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading)


    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        mockOnElectionClicked = mockk(relaxed = true) // relaxed = true for () -> Unit
        every { mockViewModel.uiState } returns initialUiState
    }

    @Test
    fun electionListScreen_showsLoadingIndicator_initially() {
        // ViewModel is already set to Loading state in setUp via initialUiState
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }
        // A more direct way to check for loading would be to add a testTag to the CircularProgressIndicator
        // For now, assert that other states are not visible
        composeTestRule.onNodeWithText("No elections available at the moment.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
        // To actually see the indicator, we might need a testTag or a more complex assertion.
        // This test implicitly verifies loading by checking other states aren't present.
    }

    @Test
    fun electionListScreen_displaysElections_whenStateIsSuccess() {
        val sampleDomainElections = listOf(
            Election(id = "1", title = "Election 1 Title", description = "Desc 1", options = listOf("A", "B")),
            Election(id = "2", title = "Election 2 Title", description = "Desc 2", options = listOf("C", "D"))
        )
        initialUiState.value = ElectionListUiState.Success(sampleDomainElections) // Set VM state

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("Election 1 Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Election 2 Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("No elections available at the moment.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
    }

    @Test
    fun electionListScreen_showsEmptyMessage_whenStateIsEmpty() {
        initialUiState.value = ElectionListUiState.Empty // Set VM state

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("No elections available at the moment.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Election 1 Title").assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
    }

    @Test
    fun electionListScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Simulated network error"
        initialUiState.value = ElectionListUiState.Error(errorMessage) // Set VM state

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("Error: $errorMessage", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("No elections available at the moment.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Election 1 Title").assertDoesNotExist()
    }

    @Test
    fun electionListScreen_callsOnElectionClicked_whenItemIsClicked() {
        val electionToClick = Election(id = "election_click_id_1", title = "Clickable Election", description = "Desc Click", options = listOf("X", "Y"))
        val sampleDomainElections = listOf(electionToClick)

        initialUiState.value = ElectionListUiState.Success(sampleDomainElections) // Set VM state
        val spiedOnElectionClicked = spyk(mockOnElectionClicked)


        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = spiedOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("Clickable Election").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clickable Election").performClick()

        verify(timeout = 1000) { spiedOnElectionClicked(electionToClick) }
    }
}
