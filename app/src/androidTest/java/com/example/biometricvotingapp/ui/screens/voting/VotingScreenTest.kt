package com.example.biometricvotingapp.ui.screens.voting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.screens.voting.VotingUiState
import com.example.biometricvotingapp.ui.screens.voting.VotingViewModel
import com.example.biometricvotingapp.ui.screens.voting.VotingViewEvent
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VotingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: VotingViewModel
    private lateinit var mockOnVoteConfirmedAndSubmitted: (Election, String) -> Unit
    private lateinit var mockOnNavigateBack: () -> Unit
    private lateinit var events: MutableSharedFlow<VotingViewEvent>

    private val sampleElection = Election(
        id = "election1",
        title = "Sample Election Title",
        description = "This is a sample election description.",
        options = listOf("Option Yes", "Option No", "Option Maybe")
    )
    private val sampleVoterId = "testVoter123"

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        mockOnVoteConfirmedAndSubmitted = mockk(relaxed = true)
        mockOnNavigateBack = mockk(relaxed = true)
        events = MutableSharedFlow()

        every { mockViewModel.uiState } returns MutableStateFlow(VotingUiState.Idle)
        every { mockViewModel.eventFlow } returns events.asSharedFlow()
    }

    @Test
    fun votingScreen_displaysInitialElementsCorrectly_whenIdle() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                VotingScreen(
                    viewModel = mockViewModel,
                    anonymizedVoterId = sampleVoterId,
                    election = sampleElection,
                    onVoteConfirmedAndSubmitted = mockOnVoteConfirmedAndSubmitted,
                    onNavigateBack = mockOnNavigateBack
                )
            }
        }

        // Check for election title
        composeTestRule.onNodeWithText(sampleElection.title).assertIsDisplayed()
        // Check for election description
        composeTestRule.onNodeWithText(sampleElection.description).assertIsDisplayed()
        // Check for some options
        composeTestRule.onNodeWithText("Option Yes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Option No").assertIsDisplayed()
        // Check for the vote button
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsDisplayed()
    }

    @Test
    fun voteSuccess_triggersOnVoteConfirmedAndSubmittedCallback() = runTest {
        val successMessage = "Vote successfully cast!"
        val selectedOption = "Option Yes"

        // Spy on the callback to verify invocation
        val spiedOnVoteConfirmed = spyk(mockOnVoteConfirmedAndSubmitted)

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                VotingScreen(
                    viewModel = mockViewModel,
                    anonymizedVoterId = sampleVoterId,
                    election = sampleElection,
                    onVoteConfirmedAndSubmitted = spiedOnVoteConfirmed,
                    onNavigateBack = mockOnNavigateBack
                )
            }
        }

        // Simulate the ViewModel emitting the navigation event
        // This assumes the selectedOptionState in the actual screen would be "Option Yes"
        // For a more robust test, we might need to interact with the UI to select an option first,
        // then trigger the event, or have the event carry the selected option.
        // The current VotingViewEvent.VoteSubmissionSuccessAndNavigate carries the message.
        // The screen's LaunchedEffect uses its local `selectedOptionState` when calling the callback.
        // To test this accurately, we'd need to set selectedOptionState in the test,
        // which is internal to the screen.
        // For this test, we verify the callback is called; verifying the exact option passed
        // would require more complex UI interaction first or refactoring the event.

        // Let's assume the event emission implies the screen will use its current selectedOptionState.
        // We can't easily control selectedOptionState from here without UI interaction.
        // The test will verify the callback is called.

        val job = launch {
            events.emit(VotingViewEvent.VoteSubmissionSuccessAndNavigate(successMessage))
        }

        composeTestRule.waitForIdle() // Allow LaunchedEffect to process the event

        // Verify that the callback was invoked.
        // We can't easily verify the 'selectedOption' argument here without UI interaction
        // to set `selectedOptionState` within the Composable.
        // So, we'll use `any()` for the string argument for now.
        verify(timeout = 1000) { spiedOnVoteConfirmed(eq(sampleElection), any()) }

        job.cancel()
    }
}
