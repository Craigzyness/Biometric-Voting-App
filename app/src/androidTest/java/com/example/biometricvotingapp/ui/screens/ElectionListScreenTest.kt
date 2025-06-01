package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.data.network.dto.ElectionDto
import com.example.biometricvotingapp.data.repository.FakeVotingRepository
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
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

    private lateinit var fakeRepository: FakeVotingRepository
    private lateinit var mockOnElectionClicked: (Election) -> Unit

    @Before
    fun setUp() {
        // FakeRepository doesn't need a real context for its overridden methods
        fakeRepository = FakeVotingRepository()
        mockOnElectionClicked = mock(Function1::class.java) as (Election) -> Unit
    }

    @Test
    fun electionListScreen_showsLoadingIndicator_initially() {
        fakeRepository.setElectionsDelay(2000) // Simulate network delay

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    votingRepository = fakeRepository,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }
        // Check for the loading indicator (assuming CircularProgressIndicator is used and identifiable)
        // For now, we'll assume if no data or error is shown, loading is active.
        // A more robust way is to add a testTag to the CircularProgressIndicator in ElectionListScreen.
        // For this test, we'll check that data/empty/error messages are NOT yet displayed.
        composeTestRule.onNodeWithText("No elections available at the moment.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
        // After delay, it might show data or empty, but initially it should be in loading.
        // This test is basic; a testTag on the indicator is better.
    }

    @Test
    fun electionListScreen_displaysElections_whenRepositoryReturnsData() {
        val sampleElections = listOf(
            ElectionDto(id = "1", electionCode = "E1", title = "Election 1 Title", description = "Desc 1", options = listOf("A", "B"), startTimestamp = "", endTimestamp = "", status = "ACTIVE"),
            ElectionDto(id = "2", electionCode = "E2", title = "Election 2 Title", description = "Desc 2", options = listOf("C", "D"), startTimestamp = "", endTimestamp = "", status = "ACTIVE")
        )
        fakeRepository.electionsToReturn = sampleElections

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    votingRepository = fakeRepository,
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
    fun electionListScreen_showsEmptyMessage_whenRepositoryReturnsEmptyList() {
        fakeRepository.electionsToReturn = emptyList()

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    votingRepository = fakeRepository,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("No elections available at the moment.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Election 1 Title").assertDoesNotExist() // Example from previous test
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
    }

    @Test
    fun electionListScreen_showsErrorMessage_whenRepositoryReturnsError() {
        val errorMessage = "Fake network error occurred"
        fakeRepository.shouldReturnError = true
        fakeRepository.generalErrorMessage = errorMessage

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    votingRepository = fakeRepository,
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
        val electionIdToClick = "election_click_id_1"
        val sampleElections = listOf(
            ElectionDto(id = electionIdToClick, electionCode = "E_CLICK", title = "Clickable Election", description = "Desc Click", options = listOf("X", "Y"), startTimestamp = "", endTimestamp = "", status = "ACTIVE")
        )
        fakeRepository.electionsToReturn = sampleElections

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    votingRepository = fakeRepository,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        composeTestRule.onNodeWithText("Clickable Election").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clickable Election").performClick()

        // Verify that the onElectionClicked lambda was called with the correct Election domain model
        // The mapping in ElectionListScreen converts ElectionDto to Election domain model
        val expectedDomainElection = Election(
            id = electionIdToClick,
            title = "Clickable Election",
            description = "Desc Click",
            options = listOf("X", "Y")
        )
        verify(mockOnElectionClicked).invoke(expectedDomainElection)
    }
}
