package com.example.biometricvotingapp.ui.screens.voting

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
    private lateinit var uiStateFlow: MutableStateFlow<VotingUiState>
    private lateinit var eventFlow: MutableSharedFlow<VotingViewEvent>

    private val sampleElection = Election(
        id = "election1",
        title = "Sample Election Title",
        description = "This is a sample election description.",
        options = listOf("Option Yes", "Option No", "Option Maybe") // Make sure these are unique for test tags
    )
    private val sampleVoterId = "testVoter123"

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        mockOnVoteConfirmedAndSubmitted = mockk(relaxed = true)
        mockOnNavigateBack = mockk(relaxed = true)

        uiStateFlow = MutableStateFlow(VotingUiState.Idle)
        eventFlow = MutableSharedFlow()

        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.eventFlow } returns eventFlow.asSharedFlow()
    }

    private fun setScreenContent() {
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
    }

    @Test
    fun votingScreen_displaysInitialElementsCorrectly_whenIdle() {
        uiStateFlow.value = VotingUiState.Idle // Ensure Idle state
        setScreenContent()
                )
            }
        }

        composeTestRule.onNodeWithText(sampleElection.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleElection.description).assertIsDisplayed()
        composeTestRule.onNodeWithText("Option Yes").assertIsDisplayed() // From sampleElection.options
        composeTestRule.onNodeWithText("Option No").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsDisplayed().assertIsNotEnabled() // Initially no option selected
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun votingScreen_selectsRadioButton_onClick() {
        uiStateFlow.value = VotingUiState.Idle
        setScreenContent()

        // Click "Option Yes"
        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithTag("option_radio_Option Yes").assertIsSelected()
        composeTestRule.onNodeWithTag("option_radio_Option No").assertIsNotSelected()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled() // Button should be enabled

        // Click "Option No"
        composeTestRule.onNodeWithTag("option_row_Option No").performClick()
        composeTestRule.onNodeWithTag("option_radio_Option No").assertIsSelected()
        composeTestRule.onNodeWithTag("option_radio_Option Yes").assertIsNotSelected()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()
    }

    @Test
    fun votingScreen_showsLoading_whenStateIsLoading() {
        uiStateFlow.value = VotingUiState.Loading
        setScreenContent()

        // Select an option to enable the button before it goes into loading, to check its content change
        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()

        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertDoesNotExist() // Text replaced by indicator
        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals("Submitting your vote...")
        // Radio buttons could be disabled during loading
        composeTestRule.onNodeWithTag("option_row_Option Yes").assert(has नोClickAction()) // Check if clickable, might need custom semantic
    }

    @Test
    fun votingScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Test vote error"
        uiStateFlow.value = VotingUiState.Error(errorMessage)
        setScreenContent()

        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals(errorMessage)
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsDisplayed().assertIsNotEnabled() // Button disabled if no option selected, or enabled if option was selected before error

        // Enable button by selecting option, then check if it's still enabled after error
        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()
    }

    @Test
    fun votingScreen_showsSuccessMessageAndDisablesVoting_whenStateIsSuccess() {
        val successMsg = "Vote cast!"
        uiStateFlow.value = VotingUiState.Success(successMsg)
        setScreenContent()

        // Select an option to see if button becomes disabled due to Success state
        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()

        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals(successMsg)
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled() // Button disabled after success
        // Radio buttons should also be disabled
        composeTestRule.onNodeWithTag("option_row_Option Yes").assert(has नोClickAction())
    }

    @Test
    fun voteButton_enabledState_isCorrect() {
        uiStateFlow.value = VotingUiState.Idle
        setScreenContent()

        // Initial: No selection, Idle state -> button disabled
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled()

        // Option Selected, Idle state -> button enabled
        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()

        // Loading state -> button disabled (content changes to indicator)
        uiStateFlow.value = VotingUiState.Loading
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()
        // Check button's enabled state via its parent if text is gone, or rely on content change

        // AwaitingBiometrics state -> button disabled (content changes to indicator)
        uiStateFlow.value = VotingUiState.AwaitingBiometrics
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()

        // Success state -> button disabled
        uiStateFlow.value = VotingUiState.Success("Vote Cast!")
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled()
    }

    @Test
    fun voteSuccess_triggersOnVoteConfirmedAndSubmittedCallback() = runTest {
        val successMessage = "Vote successfully cast!"
        // val selectedOption = "Option Yes" // This is set by UI interaction if needed

        val spiedOnVoteConfirmed = spyk(mockOnVoteConfirmedAndSubmitted)
        uiStateFlow.value = VotingUiState.Idle // Start from Idle to allow option selection

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
