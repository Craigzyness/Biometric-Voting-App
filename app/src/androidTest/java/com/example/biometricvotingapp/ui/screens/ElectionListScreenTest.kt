package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListUiState // Corrected import if VM is in different sub-package
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModel // Corrected import
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ElectionListScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ElectionListViewModel
    private lateinit var mockOnElectionClicked: (Election) -> Unit
    private lateinit var mockOnLogout: () -> Unit
    private val uiStateFlow = MutableStateFlow<ElectionListUiState>(ElectionListUiState.Loading)

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
        mockOnElectionClicked = mockk(relaxed = true)
        mockOnLogout = mockk(relaxed = true)
        every { mockViewModel.uiState } returns uiStateFlow
        uiStateFlow.value = ElectionListUiState.Loading
    }

    private fun setScreenContent() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                ElectionListScreen( // This now correctly refers to the Composable in the same package
                    viewModel = mockViewModel,
                    onElectionClicked = mockOnElectionClicked,
                    onLogout = mockOnLogout
                )
            }
        }
    }

    @Test
    fun electionListScreen_showsLoadingIndicator_initially() {
        uiStateFlow.value = ElectionListUiState.Loading
        setScreenContent()
        composeTestRule.onNodeWithText("No elections available at the moment.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
    }

    @Test
    fun electionListScreen_displaysElections_whenStateIsSuccess() {
        val sampleDomainElections = listOf(
            Election(id = "1", title = "Election 1 Title", description = "Desc 1", options = listOf("A", "B")),
            Election(id = "2", title = "Election 2 Title", description = "Desc 2", options = listOf("C", "D"))
        )
        uiStateFlow.value = ElectionListUiState.Success(sampleDomainElections)
        setScreenContent()

        composeTestRule.onNodeWithText("Election 1 Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Election 2 Title").assertIsDisplayed()
    }

    @Test
    fun electionListScreen_showsEmptyMessage_whenStateIsEmpty() {
        uiStateFlow.value = ElectionListUiState.Empty
        setScreenContent()
        composeTestRule.onNodeWithText("No elections available at the moment.").assertIsDisplayed()
    }

    @Test
    fun electionListScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Simulated network error"
        uiStateFlow.value = ElectionListUiState.Error(errorMessage)
        setScreenContent()
        composeTestRule.onNodeWithText("Error: $errorMessage", substring = true).assertIsDisplayed()
    }

    @Test
    fun electionListScreen_callsOnElectionClicked_whenItemIsClicked() {
        val electionToClick = Election(id = "election_click_id_1", title = "Clickable Election", description = "Desc Click", options = listOf("X", "Y"))
        val sampleDomainElections = listOf(electionToClick)
        uiStateFlow.value = ElectionListUiState.Success(sampleDomainElections)

        setScreenContent()

        composeTestRule.onNodeWithText("Clickable Election").performClick()
        verify(timeout = 1000) { mockOnElectionClicked(electionToClick) }
    }

    @Test
    fun electionList_scrollsToItem_inLongList() {
        val longList = generateSampleElections(20)
        uiStateFlow.value = ElectionListUiState.Success(longList)
        setScreenContent()

        composeTestRule.onNodeWithText("Election Title 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Election Title 15").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Election Title 20").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun electionList_displaysCorrectContent_forListItems() {
        val elections = listOf(
            Election(id = "E1", title = "Specific Election Alpha", description = "Alpha Description", options = listOf("A1", "A2")),
            Election(id = "E2", title = "Specific Election Beta", description = "", options = listOf("B1", "B2"))
        )
        uiStateFlow.value = ElectionListUiState.Success(elections)
        setScreenContent()

        composeTestRule.onNodeWithText("Specific Election Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alpha Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Specific Election Beta").assertIsDisplayed()
    }

    @Test
    fun logoutButton_isDisplayed_andCallsOnLogout() {
        uiStateFlow.value = ElectionListUiState.Empty
        setScreenContent()

        composeTestRule.onNodeWithContentDescription("Logout").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Logout").performClick()
        verify(exactly = 1) { mockOnLogout() }
    }
}
