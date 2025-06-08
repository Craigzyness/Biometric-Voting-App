package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.ui.screens.login.LoginUiState
import com.example.biometricvotingapp.ui.screens.login.LoginViewModel
import com.example.biometricvotingapp.ui.screens.login.LoginViewEvent
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
Biometric-Voting-App
import io.mockk.every
import io.mockk.mockk
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
class LoginScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: LoginViewModel
    private lateinit var mockOnNavigateToRegister: () -> Unit
    private lateinit var mockOnLoginSuccess: (String) -> Unit
    private lateinit var uiStateFlow: MutableStateFlow<LoginUiState>
    private lateinit var eventFlow: MutableSharedFlow<LoginViewEvent>

    // Test Tags that would be ideally present in LoginScreen.kt
    // private val loginButtonTag = "loginButton"
    // private val loadingIndicatorTag = "loadingIndicator"
    // private val errorMessageTextTag = "errorMessageText"

    @Before
    fun setUp() {
        // hiltRule.inject() // Not needed here as we are not injecting into the test class itself.
                           // We are manually providing a mocked ViewModel to the Composable.

        mockViewModel = mockk(relaxed = true)
        mockOnNavigateToRegister = mockk(relaxed = true)
        mockOnLoginSuccess = mockk(relaxed = true)

        uiStateFlow = MutableStateFlow(LoginUiState.Idle)
        eventFlow = MutableSharedFlow()
        every { mockViewModel.uiState } returns uiStateFlow

    private val loginButtonTag = "loginButton" // Assuming Button itself has this
    private val loadingIndicatorTag = "loadingIndicator" // Assuming CircularProgressIndicator has this
    private val errorMessageTextTag = "errorMessageText" // Assuming error Text has this

    @Before
    fun setUp() {
        // Mock ViewModel and callbacks
        mockViewModel = mockk(relaxed = true) // relaxed = true to avoid mocking all methods initially
        mockOnNavigateToRegister = mockk(relaxed = true)
        mockOnLoginSuccess = mockk(relaxed = true)

        // Setup StateFlow and SharedFlow for ViewModel mocks
        uiStateFlow = MutableStateFlow(LoginUiState.Idle)
        eventFlow = MutableSharedFlow() // For one-time events
 Biometric-Voting-App
      every { mockViewModel.uiState } returns uiStateFlow
Biometric-Voting-App
        every { mockViewModel.eventFlow } returns eventFlow.asSharedFlow()
    }

    private fun setLoginScreenContent() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                LoginScreen(
                    viewModel = mockViewModel, // Manually passing mocked ViewModel

viewModel = mockViewModel,
Biometric-Voting-App
Biometric-Voting-App
                    onNavigateToRegister = mockOnNavigateToRegister,
                    onLoginSuccess = mockOnLoginSuccess
                )
            }
        }
    }

    @Test
    fun loginScreen_displaysKeyElements_whenIdle() {
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()

@Test
    fun loginScreen_displaysKeyElements_whenIdle() {
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()
Biometric-Voting-App

        composeTestRule.onNodeWithText("Biometric Voting App - Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login with Fingerprint").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText("Not registered yet? Register here").assertIsDisplayed()

        composeTestRule.onNodeWithText("Login with Fingerprint").assertExists()
    }
@Test
    fun loginScreen_showsLoadingIndicator_whenStateIsLoading() {
        uiStateFlow.value = LoginUiState.Loading
        setLoginScreenContent()

        composeTestRule.onNodeWithText("Login with Fingerprint").assertDoesNotExist()
        // Further assertions would benefit from testTags on the indicator and button.
    }


    @Test
    fun loginScreen_showsLoadingIndicator_whenStateIsLoading() {
        uiStateFlow.value = LoginUiState.Loading
        setLoginScreenContent()

        composeTestRule.onNodeWithText("Login with Fingerprint").assertDoesNotExist()
        // Further assertions would benefit from testTags on the indicator and button.
    }

    @Test
    fun loginScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Invalid credentials test"
        uiStateFlow.value = LoginUiState.Error(errorMessage)
        setLoginScreenContent()

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Biometric Voting App - Login").assertIsDisplayed() // TopAppBar
        composeTestRule.onNodeWithText("Login with Fingerprint").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText("Not registered yet? Register here").assertIsDisplayed()

        // Ensure loading indicator and error message are not present
        // Using onNodeWithText for error as testTag might not be there.
        // If LoginScreen.kt puts CircularProgressIndicator inside Button, it might not be found by simple text.
        // For now, we assume button text changes or indicator is separate.
        // LoginScreen.kt shows indicator *inside* the button, replacing text.
        // So, we check that the "Login with Fingerprint" text is there (meaning no indicator).
        composeTestRule.onNodeWithText("Login with Fingerprint").assertExists()
    }

    @Test
    fun loginScreen_showsLoadingIndicator_whenStateIsLoading() {
        uiStateFlow.value = LoginUiState.Loading
        setLoginScreenContent()

        // In LoginScreen.kt, the CircularProgressIndicator replaces the Text in the Button.
        // So, we check that the "Login with Fingerprint" text is NOT displayed.
        // A more robust way would be to add a testTag to the CircularProgressIndicator.
        composeTestRule.onNodeWithText("Login with Fingerprint").assertDoesNotExist()
        // And assert the button is not enabled (as per LoginScreen logic)
        // The button node can be found by its text when not loading, or a common parent.
        // For simplicity, we'll rely on the text disappearing.
        // To find the indicator, a testTag="loadingIndicator" on CircularProgressIndicator in LoginScreen.kt would be ideal.
        // As we cannot modify LoginScreen.kt in this subtask, this test is limited.
        // We can, however, check if the button is disabled.
        // The Button itself can be found by a testTag if added, or by its initial text if we assume it's still part of compose tree.
        // The `enabled` check for the button `uiState !is LoginUiState.Loading` means it should be disabled.
        // We need a reliable way to find the button. Let's assume we can find it via a parent or a tag if it was there.
        // Since `Login with Fingerprint` text is gone, we can't use that.
        // This highlights the need for testTags.
    }

Biometric-Voting-App
    @Test
    fun loginScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Invalid credentials test"
        uiStateFlow.value = LoginUiState.Error(errorMessage)
        setLoginScreenContent()


        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

        // Error message is displayed in a separate Text composable
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        // Button should be enabled again
 Biometric-Voting-App


        composeTestRule.onNodeWithText("Login with Fingerprint").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun loginButton_onClick_callsViewModelOnLoginClicked() {
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()

        composeTestRule.onNodeWithText("Login with Fingerprint").performClick()
        verify(exactly = 1) { mockViewModel.onLoginClicked() }
    }

    @Test
    fun navigationToRegistration_isTriggered_onClick() {
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()

        composeTestRule.onNodeWithText("Not registered yet? Register here").performClick()
        verify(exactly = 1) { mockOnNavigateToRegister() }
Biometric-Voting-App
        verify(exactly = 1) { mockViewModel.resetStateToIdle() }
    }

    @Test
    fun successfulLoginEvent_triggersOnLoginSuccessCallback() = runTest {
        val testAnonymizedId = "test-id-for-login"
Biometric-Voting-App
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()

        val job = launch {
            eventFlow.emit(LoginViewEvent.NavigateToElectionList(testAnonymizedId))
        }

        composeTestRule.waitForIdle()


uiStateFlow.value = LoginUiState.Idle // Start in a state that allows login flow
        setLoginScreenContent()

        // Simulate ViewModel emitting the navigation event
        val job = launch { // Need to launch in a coroutine to emit on SharedFlow
            eventFlow.emit(LoginViewEvent.NavigateToElectionList(testAnonymizedId))
        }

        composeTestRule.waitForIdle() // Ensure LaunchedEffect in Screen processes the event
 Biometric-Voting-App

        verify(timeout = 1000) { mockOnLoginSuccess(testAnonymizedId) }
        job.cancel()
    }
}
