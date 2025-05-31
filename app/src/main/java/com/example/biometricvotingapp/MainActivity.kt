package com.example.biometricvotingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.biometricvotingapp.ui.screens.ElectionListScreen
import com.example.biometricvotingapp.ui.screens.LoginScreen
import com.example.biometricvotingapp.ui.screens.RegistrationScreen
import com.example.biometricvotingapp.ui.screens.getSampleElections
// TODO: Replace with your actual theme if you have one defined, e.g., in ui.theme package
// import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme

/**
 * MainActivity.kt
 *
 * Purpose: Main entry point of the application. Hosts Composable screens and handles
 * basic navigation logic for the MVP.
 */

// Define possible screens/states for navigation
sealed class Screen {
    object Registration : Screen()
    object Login : Screen()
    object ElectionList : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Using a simple MaterialTheme. Replace with your app's specific theme if available.
            MaterialTheme { // Or YourAppTheme { ... }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator()
                }
            }
        }
    }
}

@Composable
fun AppNavigator() {
    // State to keep track of the current screen
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Registration) }

    // Could also store the anonymized ID here if needed across app sessions (with proper persistence)
    // var currentAnonymizedId by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        is Screen.Registration -> {
            RegistrationScreen(
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegistrationSuccess = { generatedId ->
                    // Handle successful registration
                    // For MVP, we can directly navigate to ElectionList or Login.
                    // Let's navigate to ElectionList after registration for now.
                    // currentAnonymizedId = generatedId // Store if needed
                    currentScreen = Screen.ElectionList
                }
            )
        }
        is Screen.Login -> {
            LoginScreen(
                onNavigateToRegister = { currentScreen = Screen.Registration },
                onLoginSuccess = { anonymizedId ->
                    if (anonymizedId != null) {
                        // currentAnonymizedId = anonymizedId // Store if needed
                        currentScreen = Screen.ElectionList
                    } else {
                        // Stay on Login screen, error message is handled within LoginScreen
                    }
                }
            )
        }
        is Screen.ElectionList -> {
            ElectionListScreen(
                elections = getSampleElections(), // Pass the sample data
                onElectionClicked = { election ->
                    // TODO: Navigate to Voting Screen for the selected election (Action Item 3.5)
                    println("Election clicked: ${election.title}") // Placeholder action
                }
                // TODO: Add onLogoutClicked = { currentScreen = Screen.Login; currentAnonymizedId = null }
            )
        }
    }
}

// It's good practice to have a theme defined. If you don't have one,
// MaterialTheme {} provides a default. For a real app, create something like:
// package com.example.biometricvotingapp.ui.theme
// @Composable
// fun BiometricVotingAppTheme(content: @Composable () -> Unit) {
//     MaterialTheme(
//         // Define colorScheme, typography, shapes if needed
//         content = content
//     )
// }
