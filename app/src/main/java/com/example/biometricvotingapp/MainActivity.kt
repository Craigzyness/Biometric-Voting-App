package com.example.biometricvotingapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.biometricvotingapp.domain.model.Election // Import Election model
import com.example.biometricvotingapp.ui.screens.ElectionListScreen
import com.example.biometricvotingapp.ui.screens.LoginScreen
import com.example.biometricvotingapp.ui.screens.RegistrationScreen
import com.example.biometricvotingapp.ui.screens.VotingScreen // Import VotingScreen
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
    data class Voting(val election: Election) : Screen() // Added Voting screen with Election data
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

    when (val screen = currentScreen) { // Use 'screen' for smart casting
        is Screen.Registration -> {
            RegistrationScreen(
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegistrationSuccess = { generatedId ->
                    // Handle successful registration
                    Log.i("AppNavigator", "Registration successful. Generated ID (first 8): ${generatedId.take(8)}")
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
                        Log.i("AppNavigator", "Login successful. Anonymized ID (first 8): ${anonymizedId.take(8)}")
                        // currentAnonymizedId = anonymizedId // Store if needed
                        currentScreen = Screen.ElectionList
                    } else {
                        // Stay on Login screen, error message is handled within LoginScreen
                        Log.w("AppNavigator", "Login failed or app registration not found.")
                    }
                }
            )
        }
        is Screen.ElectionList -> {
            ElectionListScreen(
                elections = getSampleElections(), // Pass the sample data
                onElectionClicked = { selectedElection ->
                    Log.d("AppNavigator", "Election clicked: ${selectedElection.title}")
                    currentScreen = Screen.Voting(selectedElection) // Navigate to VotingScreen
                }
                // TODO: Add onLogoutClicked = { currentScreen = Screen.Login; currentAnonymizedId = null }
            )
        }
        is Screen.Voting -> { // New case for VotingScreen
            VotingScreen(
                election = screen.election, // Pass the election from the state
                onVoteConfirmedBiometrically = { confirmedElection, selectedOption ->
                    Log.i("AppNavigator", "Vote confirmed for ${confirmedElection.title}, option: $selectedOption")
                    // For MVP, navigate back to election list after vote.
                    // TODO: Here you would typically send the vote to a backend/blockchain.
                    // For now, we simulate success and navigate.
                    currentScreen = Screen.ElectionList
                },
                onNavigateBack = {
                    Log.d("AppNavigator", "Navigating back from VotingScreen to ElectionList.")
                    currentScreen = Screen.ElectionList
                }
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
