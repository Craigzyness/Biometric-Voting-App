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
// Removed LocalContext import for Application as it's no longer needed for manual factory instantiation
import androidx.hilt.navigation.compose.hiltViewModel // Import for hiltViewModel()
import com.example.biometricvotingapp.BuildConfig
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.screens.ElectionListScreen
import com.example.biometricvotingapp.ui.screens.LoginScreen
import com.example.biometricvotingapp.ui.screens.RegistrationScreen
import com.example.biometricvotingapp.ui.screens.VotingScreen
import dagger.hilt.android.AndroidEntryPoint // Import for Hilt

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
    data class Voting(val election: Election) : Screen()
}

@AndroidEntryPoint // Annotate Activity with @AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Registration) }
    // This state is still needed if VotingScreen takes anonymizedVoterId directly.
    // ViewModels will use their own LoginUserUseCase for internal ID needs.
    var currentAnonymizedIdForVotingScreen by remember { mutableStateOf<String?>(null) }

    // Manual instantiation of repository and application context is no longer needed here
    // as Hilt will provide dependencies to ViewModels.

    when (val screen = currentScreen) {
        is Screen.Registration -> {
            // Obtain ViewModel using Hilt
            val viewModel: com.example.biometricvotingapp.ui.screens.registration.RegistrationViewModel = hiltViewModel()
            RegistrationScreen(
                viewModel = viewModel,
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegistrationSuccess = { generatedId ->
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Registration successful. Generated ID (first 8): ${generatedId.take(8)}")
                    currentAnonymizedIdForVotingScreen = generatedId // Store for VotingScreen
                    currentScreen = Screen.ElectionList
                }
            )
        }
        is Screen.Login -> {
            // Obtain ViewModel using Hilt
            val viewModel: com.example.biometricvotingapp.ui.screens.login.LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { currentScreen = Screen.Registration },
                onLoginSuccess = { loggedInId ->
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Login successful. Anonymized ID (first 8): ${loggedInId.take(8)}")
                    currentAnonymizedIdForVotingScreen = loggedInId // Store for VotingScreen
                    currentScreen = Screen.ElectionList
                }
            )
        }
        is Screen.ElectionList -> {
            // Obtain ViewModel using Hilt. ElectionListViewModel now fetches its own voterId.
            val viewModel: com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModel = hiltViewModel()
            ElectionListScreen(
                viewModel = viewModel,
                onElectionClicked = { selectedElection ->
                    if (BuildConfig.DEBUG) Log.d("AppNavigator", "Election clicked: ${selectedElection.title}")
                    // Check if ID for VotingScreen is available (it should be if user reached ElectionList)
                    if (currentAnonymizedIdForVotingScreen == null) {
                        if (BuildConfig.DEBUG) Log.e("AppNavigator", "Error: User anonymized ID for VotingScreen is null. Returning to Login.")
                        currentScreen = Screen.Login
                    } else {
                        currentScreen = Screen.Voting(selectedElection)
                    }
                },
                onLogout = {
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Logout requested.")
                    currentAnonymizedIdForVotingScreen = null
                    currentScreen = Screen.Login
                }
            )
        }
        is Screen.Voting -> {
            val voterId = currentAnonymizedIdForVotingScreen ?: run {
                if (BuildConfig.DEBUG) Log.e("AppNavigator", "Critical error: Navigated to VotingScreen with null currentAnonymizedIdForVotingScreen. Redirecting to Login.")
                currentScreen = Screen.Login
                return@AppNavigator
            }
            // Obtain ViewModel using Hilt
            val viewModel: com.example.biometricvotingapp.ui.screens.voting.VotingViewModel = hiltViewModel()
            VotingScreen(
                viewModel = viewModel,
                anonymizedVoterId = voterId, // VotingScreen still takes this for onCastVoteClicked
                election = screen.election,
                onVoteConfirmedAndSubmitted = { confirmedElection, selectedOption ->
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Vote confirmed and submitted for ${confirmedElection.title}, option: $selectedOption")
                    currentScreen = Screen.ElectionList
                },
                onNavigateBack = {
                    if (BuildConfig.DEBUG) Log.d("AppNavigator", "Navigating back from VotingScreen to ElectionList.")
                    currentScreen = Screen.ElectionList
                }
            )
        }
    }
}
