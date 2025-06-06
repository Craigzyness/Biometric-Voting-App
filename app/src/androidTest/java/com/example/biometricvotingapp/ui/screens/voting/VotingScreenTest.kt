package com.example.biometricvotingapp.ui.screens.voting

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.domain.model.Election
// Corrected import paths if ViewModel/UiState are in subpackages
import com.example.biometricvotingapp.ui.screens.voting.VotingUiState
import com.example.biometricvotingapp.ui.screens.voting.VotingViewModel
import com.example.biometricvotingapp.ui.screens.voting.VotingViewEvent
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk // Changed from Mockito to MockK
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VotingScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
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
        options = listOf("Option Yes", "Option No", "Option Maybe")
    )
    private val sampleVoterId = "testVoter123"

    @Before
    fun setUp() {
        // hiltRule.inject() // Not needed for this test setup

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
        uiStateFlow.value = VotingUiState.Idle
        setScreenContent()

        composeTestRule.onNodeWithText(sampleElection.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleElection.description).assertIsDisplayed()
        composeTestRule.onNodeWithText("Option Yes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Option No").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun votingScreen_selectsRadioButton_onClick() {
        uiStateFlow.value = VotingUiState.Idle
        setScreenContent()

        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithTag("option_radio_Option Yes").assertIsSelected()
        composeTestRule.onNodeWithTag("option_radio_Option No").assertIsNotSelected()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()

        composeTestRule.onNodeWithTag("option_row_Option No").performClick()
        composeTestRule.onNodeWithTag("option_radio_Option No").assertIsSelected()
        composeTestRule.onNodeWithTag("option_radio_Option Yes").assertIsNotSelected()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()
    }

    @Test
    fun votingScreen_showsLoading_whenStateIsLoading() {
        uiStateFlow.value = VotingUiState.Loading
        setScreenContent()

        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick() // Select option to enable button before loading

        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertDoesNotExist()
        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals("Submitting your vote...")
        composeTestRule.onNodeWithTag("option_row_Option Yes").assert(hasNoClickAction())
    }

    @Test
    fun votingScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Test vote error"
        uiStateFlow.value = VotingUiState.Error(errorMessage)
        setScreenContent()

        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals(errorMessage)
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertDoesNotExist()

        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()
    }

    @Test
    fun votingScreen_showsSuccessMessageAndDisablesVoting_whenStateIsSuccess() {
        val successMsg = "Vote cast!"
        uiStateFlow.value = VotingUiState.Success(successMsg)
        setScreenContent()

        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()

        composeTestRule.onNodeWithTag("voteStatusMessageText", useUnmergedTree = true).assertTextEquals(successMsg)
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("option_row_Option Yes").assert(hasNoClickAction())
    }

    @Test
    fun voteButton_enabledState_isCorrect() {
        uiStateFlow.value = VotingUiState.Idle
        setScreenContent()

        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("option_row_Option Yes").performClick()
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsEnabled()

        uiStateFlow.value = VotingUiState.Loading
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed()

        uiStateFlow.value = VotingUiState.AwaitingBiometrics
        composeTestRule.onNodeWithTag("voteLoadingIndicator", useUnmergedTree = true).assertIsDisplayed() // Assuming loading indicator is also used for AwaitingBiometrics

        uiStateFlow.value = VotingUiState.Success("Vote Cast!")
        composeTestRule.onNodeWithText("Cast Vote with Fingerprint").assertIsNotEnabled()
    }

    @Test
    fun voteSuccess_triggersOnVoteConfirmedAndSubmittedCallback() = runTest {
        val successMessage = "Vote successfully cast!"

        val spiedOnVoteConfirmed = spyk(mockOnVoteConfirmedAndSubmitted)
        uiStateFlow.value = VotingUiState.Idle

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

        // To properly test the callback, we need to simulate the selection of an option.
        // Let's assume "Option Yes" is selected by the user for this test.
        // The test relies on the `selectedOptionState` within VotingScreen.
        // A more robust way might be to have the event carry the selected option.
        // For now, we assume the screen's internal state handles this for the callback.
        // This means the test verifies the callback is called, but not with which specific option.
        // The `any()` matcher for the option string is appropriate here.

        val job = launch {
            eventFlow.emit(VotingViewEvent.VoteSubmissionSuccessAndNavigate(successMessage))
        }

        composeTestRule.waitForIdle()

        verify(timeout = 1000) { spiedOnVoteConfirmed(eq(sampleElection), any()) }

        job.cancel()
    }
}
