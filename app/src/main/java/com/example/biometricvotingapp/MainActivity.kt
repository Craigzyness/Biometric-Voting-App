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
// TODO: Replace with your actual theme if you have one defined, e.g., in ui.theme package
// import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
// Removed getSampleElections import as it's no longer used here.

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
    var currentAnonymizedId by remember { mutableStateOf<String?>(null) } // Store the anonymized ID

    when (val screen = currentScreen) { // Use 'screen' for smart casting
        is Screen.Registration -> {
            RegistrationScreen(
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegistrationSuccess = { generatedId ->
                    Log.i("AppNavigator", "Registration successful. Generated ID (first 8): ${generatedId.take(8)}")
                    currentAnonymizedId = generatedId // Store the ID
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
                        currentAnonymizedId = anonymizedId // Store the ID
                        currentScreen = Screen.ElectionList
                    } else {
                        Log.w("AppNavigator", "Login failed or app registration not found.")
                        // LoginScreen handles displaying its own error message.
                    }
                }
            )
        }
        is Screen.ElectionList -> {
            ElectionListScreen(
                // elections = getSampleElections(), // Removed, fetched from network now
                onElectionClicked = { selectedElection ->
                    Log.d("AppNavigator", "Election clicked: ${selectedElection.title}")
                    if (currentAnonymizedId == null) {
                        Log.e("AppNavigator", "Error: User anonymized ID is null. Cannot navigate to Voting screen. Returning to Login.")
                        currentScreen = Screen.Login // Or handle error appropriately
                    } else {
                        currentScreen = Screen.Voting(selectedElection)
                    }
                }
                // TODO: Add onLogoutClicked = { currentScreen = Screen.Login; currentAnonymizedId = null }
            )
        }
        is Screen.Voting -> {
            // Ensure currentAnonymizedId is not null before navigating here, or handle gracefully.
            // The check in ElectionList -> onElectionClicked is one way.
            // Alternatively, VotingScreen itself could have a check or AppNavigator could redirect if null.
            val voterId = currentAnonymizedId ?: run {
                Log.e("AppNavigator", "Critical error: Navigated to VotingScreen with null anonymizedVoterId. Redirecting to Login.")
                currentScreen = Screen.Login
                return // Exit when block early
            }
            VotingScreen(
                anonymizedVoterId = voterId,
                election = screen.election,
                onVoteConfirmedAndSubmitted = { confirmedElection, selectedOption ->
                    Log.i("AppNavigator", "Vote confirmed and submitted for ${confirmedElection.title}, option: $selectedOption")
                    currentScreen = Screen.ElectionList // Navigate back to election list
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
