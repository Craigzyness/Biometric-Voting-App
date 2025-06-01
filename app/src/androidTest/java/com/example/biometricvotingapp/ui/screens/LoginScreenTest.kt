package com.example.biometricvotingapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme // Assuming this theme exists
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Dependencies like BiometricAuthManager and AnonymizedIdGenerator are not mocked
    // for this initial UI elements presence test.

    @Test
    fun loginScreen_displaysKeyElementsCorrectly() {
        composeTestRule.setContent {
            BiometricVotingAppTheme { // Or MaterialTheme {} if BiometricVotingAppTheme is not fully defined
                LoginScreen(
                    onNavigateToRegister = {}, // Mocked lambda
                    onLoginSuccess = {}       // Mocked lambda
                )
            }
        }

        // Check for the TopAppBar title
        composeTestRule.onNodeWithText("Biometric Voting App - Login").assertIsDisplayed()

        // Check for the main headline text on the screen
        composeTestRule.onNodeWithText("Biometric Voting App", substring = true).assertIsDisplayed()

        // Check for the instructional text
        composeTestRule.onNodeWithText("Please log in using your fingerprint to access the voting features.", substring = true)
            .assertIsDisplayed()

        // Check for the login button text
        composeTestRule.onNodeWithText("Login with Fingerprint").assertIsDisplayed()

        // Check for the navigation to registration link/button text
        composeTestRule.onNodeWithText("Not registered yet? Register here").assertIsDisplayed()
    }
}
