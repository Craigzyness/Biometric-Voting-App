package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme // Assuming this theme exists and can be used
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistrationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: RegistrationViewModel
    private lateinit var mockOnNavigateToLogin: () -> Unit
    private lateinit var mockOnRegistrationSuccess: (String) -> Unit
    private lateinit var events: MutableSharedFlow<RegistrationViewEvent>

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        mockOnNavigateToLogin = mockk(relaxed = true)
        mockOnRegistrationSuccess = mockk(relaxed = true)
        events = MutableSharedFlow() // For emitting test events

        every { mockViewModel.uiState } returns MutableStateFlow(RegistrationUiState.Idle)
        every { mockViewModel.eventFlow } returns events.asSharedFlow()
    }

    @Test
    fun registrationScreen_displaysKeyElementsCorrectly_whenIdle() {
        composeTestRule.setContent {
            BiometricVotingAppTheme {
                RegistrationScreen(
                    viewModel = mockViewModel,
                    onNavigateToLogin = mockOnNavigateToLogin,
                    onRegistrationSuccess = mockOnRegistrationSuccess
                )
            }
        }

        // Check for the main title/headline text
        composeTestRule.onNodeWithText("Biometric Voting App").assertIsDisplayed()

        // Check for the welcome/introductory text
        composeTestRule.onNodeWithText("Welcome! Securely register to cast your vote using your fingerprint.", substring = true)
            .assertIsDisplayed()

        // Check for the privacy explanatory text
        composeTestRule.onNodeWithText("Your privacy is our priority.", substring = true)
            .assertIsDisplayed()

        // Check for the registration button text
        composeTestRule.onNodeWithText("Register with Fingerprint").assertIsDisplayed()

        // Check for the "Login here" link/button text
        composeTestRule.onNodeWithText("Already registered? Login here").assertIsDisplayed()
    }

    @Test
    fun registrationSuccess_triggersOnRegistrationSuccessCallback() = runTest {
        val testGeneratedId = "test-id-123"
        // Ensure the mockOnRegistrationSuccess is a spyk or can be verified
        val spiedOnRegistrationSuccess = spyk(mockOnRegistrationSuccess)

        composeTestRule.setContent {
            BiometricVotingAppTheme {
                RegistrationScreen(
                    viewModel = mockViewModel,
                    onNavigateToLogin = mockOnNavigateToLogin,
                    onRegistrationSuccess = spiedOnRegistrationSuccess
                )
            }
        }

        // Simulate the ViewModel emitting the navigation event
        // Launch in a separate coroutine to allow collection in LaunchedEffect
        val job = launch {
            events.emit(RegistrationViewEvent.NavigateToElectionList(testGeneratedId))
        }

        // Wait for the event to be processed by LaunchedEffect in the Composable
        composeTestRule.waitForIdle() // Ensures LaunchedEffect coroutine runs

        // Verify that the callback was invoked with the correct ID
        verify(timeout = 1000) { spiedOnRegistrationSuccess(testGeneratedId) } // Use timeout if timing is an issue

        job.cancel()
    }
}
