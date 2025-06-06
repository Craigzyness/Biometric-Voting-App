package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.ui.screens.registration.RegistrationUiState
import com.example.biometricvotingapp.ui.screens.registration.RegistrationViewModel
import com.example.biometricvotingapp.ui.screens.registration.RegistrationViewEvent
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RegistrationScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: RegistrationViewModel
    private lateinit var mockOnNavigateToLogin: () -> Unit
    private lateinit var mockOnRegistrationSuccess: (String) -> Unit
    private lateinit var uiStateFlow: MutableStateFlow<RegistrationUiState>
    private lateinit var eventFlow: MutableSharedFlow<RegistrationViewEvent>

    @Before
    fun setUp() {
        // hiltRule.inject() // Not needed here

        mockViewModel = mockk(relaxed = true)
        mockOnNavigateToLogin = mockk(relaxed = true)
        mockOnRegistrationSuccess = mockk(relaxed = true)

        uiStateFlow = MutableStateFlow(RegistrationUiState.Idle)
        eventFlow = MutableSharedFlow()

        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.eventFlow } returns eventFlow.asSharedFlow()
    }

    private fun setScreenContent() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                RegistrationScreen(
                    viewModel = mockViewModel,
                    onNavigateToLogin = mockOnNavigateToLogin,
                    onRegistrationSuccess = mockOnRegistrationSuccess
                )
            }
        }
    }

    @Test
    fun registrationScreen_displaysKeyElementsCorrectly_whenIdle() {
        uiStateFlow.value = RegistrationUiState.Idle
        setScreenContent()

        composeTestRule.onNodeWithText("Biometric Voting App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome! Securely register to cast your vote using your fingerprint.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Your privacy is our priority.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Register with Fingerprint").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText("Already registered? Login here").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loadingIndicator", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("statusMessageText", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun registrationScreen_showsLoadingIndicator_whenStateIsLoading() {
        val loadingMessage = "Registering..."
        uiStateFlow.value = RegistrationUiState.Loading(loadingMessage)
        setScreenContent()

        composeTestRule.onNodeWithTag("loadingIndicator", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Register with Fingerprint").assertDoesNotExist()
        composeTestRule.onNodeWithTag("statusMessageText", useUnmergedTree = true).assertTextEquals(loadingMessage)
    }

    @Test
    fun registrationScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Test error message"
        uiStateFlow.value = RegistrationUiState.Error(errorMessage)
        setScreenContent()

        composeTestRule.onNodeWithTag("statusMessageText", useUnmergedTree = true).assertTextEquals(errorMessage)
        composeTestRule.onNodeWithTag("loadingIndicator", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Register with Fingerprint").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun registrationScreen_reflectsAwaitingBiometricsState() {
        val awaitingMessage = "Awaiting biometric authentication..."
        uiStateFlow.value = RegistrationUiState.AwaitingBiometrics
        setScreenContent()

        composeTestRule.onNodeWithTag("loadingIndicator", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Register with Fingerprint").assertDoesNotExist()
        composeTestRule.onNodeWithTag("statusMessageText", useUnmergedTree = true).assertTextEquals(awaitingMessage)
    }


    @Test
    fun registrationSuccess_triggersOnRegistrationSuccessCallback() = runTest {
        val testGeneratedId = "test-id-123"
        val spiedOnRegistrationSuccess = spyk(mockOnRegistrationSuccess)

        uiStateFlow.value = RegistrationUiState.Idle
        composeTestRule.setContent { // Re-set content with spied callback
            BiometricVotingAppTheme {
                RegistrationScreen(
                    viewModel = mockViewModel,
                    onNavigateToLogin = mockOnNavigateToLogin,
                    onRegistrationSuccess = spiedOnRegistrationSuccess
                )
            }
        }

        val job = launch {
            eventFlow.emit(RegistrationViewEvent.NavigateToElectionList(testGeneratedId))
        }

        composeTestRule.waitForIdle()

        verify(timeout = 1000) { spiedOnRegistrationSuccess(testGeneratedId) }

        job.cancel()
    }
}
