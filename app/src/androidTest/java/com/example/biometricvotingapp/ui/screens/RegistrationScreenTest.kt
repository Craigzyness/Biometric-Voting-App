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

    // For this initial test, dependencies like BiometricAuthManager,
    // AnonymizedIdGenerator, and VotingRepository are not mocked.
    // We are testing the initial UI composition and presence of elements.
    // More complex tests would require a Test Double strategy for these dependencies.

    @Test
    fun registrationScreen_displaysKeyElementsCorrectly() {
        // Set content for the test
        composeTestRule.setContent {
            // It's good practice to wrap the Composable under test with your app's theme,
            // if it provides one. If BiometricVotingAppTheme is not yet defined or causes issues
            // in test, MaterialTheme {} can be used as a fallback for basic theming.
            // For now, assuming BiometricVotingAppTheme is available as per previous setup.
            // If not, replace with MaterialTheme {}.
             BiometricVotingAppTheme { // Or MaterialTheme {}
                RegistrationScreen(
                    onNavigateToLogin = {}, // Mocked lambda, does nothing in this test
                    onRegistrationSuccess = {}  // Mocked lambda, does nothing in this test
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
}
