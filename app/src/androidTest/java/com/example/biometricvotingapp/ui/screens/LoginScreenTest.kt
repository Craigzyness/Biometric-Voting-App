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
        every { mockViewModel.eventFlow } returns eventFlow.asSharedFlow()
    }

    private fun setLoginScreenContent() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                LoginScreen(
                    viewModel = mockViewModel, // Manually passing mocked ViewModel
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
    fun loginScreen_showsErrorMessage_whenStateIsError() {
        val errorMessage = "Invalid credentials test"
        uiStateFlow.value = LoginUiState.Error(errorMessage)
        setLoginScreenContent()

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
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
        verify(exactly = 1) { mockViewModel.resetStateToIdle() }
    }

    @Test
    fun successfulLoginEvent_triggersOnLoginSuccessCallback() = runTest {
        val testAnonymizedId = "test-id-for-login"
        uiStateFlow.value = LoginUiState.Idle
        setLoginScreenContent()

        val job = launch {
            eventFlow.emit(LoginViewEvent.NavigateToElectionList(testAnonymizedId))
        }

        composeTestRule.waitForIdle()

        verify(timeout = 1000) { mockOnLoginSuccess(testAnonymizedId) }
        job.cancel()
    }
}
