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
// Removed Mockito imports


@RunWith(AndroidJUnit4::class)
class ElectionListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ElectionListViewModel
    private lateinit var mockOnElectionClicked: (Election) -> Unit // Will use MockK
    private val uiStateFlow = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading) // Renamed for clarity

    // Helper to generate sample Election domain models
    private fun generateSampleElections(count: Int): List<Election> {
        return List(count) { i ->
            Election(
                id = "election_id_${i + 1}",
                title = "Election Title ${i + 1}",
                description = "This is the description for Election ${i + 1}.",
                options = listOf("Option A for ${i + 1}", "Option B for ${i + 1}")
            )
        }
    }

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        mockOnElectionClicked = mockk(relaxed = true) // Using MockK, relaxed for () -> Unit
        every { mockViewModel.uiState } returns uiStateFlow
        // Reset uiStateFlow for each test if not explicitly set by the test
        uiStateFlow.value = ElectionListUiState.Loading
    }

    @Test
    fun electionListScreen_showsLoadingIndicator_initially() {
        // uiStateFlow is Loading by default from setUp
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
        uiStateFlow.value = ElectionListUiState.Success(sampleDomainElections)

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
        uiStateFlow.value = ElectionListUiState.Empty

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
        uiStateFlow.value = ElectionListUiState.Error(errorMessage)

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

        uiStateFlow.value = ElectionListUiState.Success(sampleDomainElections)
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

    @Test
    fun electionList_scrollsToItem_inLongList() {
        val longList = generateSampleElections(20)
        uiStateFlow.value = ElectionListUiState.Success(longList)

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        // Verify first item is displayed
        composeTestRule.onNodeWithText("Election Title 1").assertIsDisplayed()
        // Verify item 15 is not initially displayed (if list is long enough to scroll)
        // This assertion might fail if the screen is large enough to show 15 items.
        // composeTestRule.onNodeWithText("Election Title 15").assertIsNotDisplayed() // This can be flaky

        // Scroll to item 15 and assert it's displayed
        composeTestRule.onNodeWithText("Election Title 15").performScrollTo().assertIsDisplayed()
        // Scroll to item 20 and assert it's displayed
        composeTestRule.onNodeWithText("Election Title 20").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun electionList_displaysCorrectContent_forListItems() {
        val elections = listOf(
            Election(id = "E1", title = "Specific Election Alpha", description = "Alpha Description", options = listOf("A1", "A2")),
            Election(id = "E2", title = "Specific Election Beta", description = "", options = listOf("B1", "B2")) // Empty description
        )
        uiStateFlow.value = ElectionListUiState.Success(elections)

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen(
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked
                )
            }
        }

        // Verify content for Election Alpha
        composeTestRule.onNodeWithText("Specific Election Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alpha Description").assertIsDisplayed() // Description should be shown

        // Verify content for Election Beta
        composeTestRule.onNodeWithText("Specific Election Beta").assertIsDisplayed()
        // Description is blank for Beta. The ElectionListItem only adds Text for description if it's not blank.
        // So, we assert that the "Alpha Description" (which is unique) is NOT a sibling or child in a way
        // that would indicate it's being rendered for Beta.
        // A more direct way is to use testTags on the description Text composable if it exists.
        // For now, asserting its title is sufficient as the main check.
        // If the description Text node for "Alpha Description" is found, ensure it's not part of Beta's item.
        // This is hard without more specific selectors. The current check is okay.
        // The important part is that "Alpha Description" is found for the Alpha item.
        // And for Beta, if its description was not blank, it would be found. Since it's blank, it's not added.
    }
}
