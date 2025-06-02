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
import androidx.compose.ui.platform.LocalContext // Needed for Application context
import androidx.lifecycle.viewmodel.compose.viewModel // Needed for viewModel() delegate
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
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

    // Create and remember VotingRepository instance
    val votingRepository = remember {
        com.example.biometricvotingapp.data.repository.VotingRepository(
            com.example.biometricvotingapp.data.network.ApiService.instance
        )
    }
    // Get Application context for ViewModel factories
    val application = LocalContext.current.applicationContext as android.app.Application

    when (val screen = currentScreen) { // Use 'screen' for smart casting
        is Screen.Registration -> {
            val factory = com.example.biometricvotingapp.ui.screens.registration.RegistrationViewModelFactory(
                application,
                com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator,
                votingRepository
            )
            val viewModel: com.example.biometricvotingapp.ui.screens.registration.RegistrationViewModel = viewModel(factory = factory)
            RegistrationScreen(
                viewModel = viewModel,
                onNavigateToLogin = { currentScreen = Screen.Login },
                onRegistrationSuccess = { generatedId ->
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Registration successful. Generated ID (first 8): ${generatedId.take(8)}")
                    currentAnonymizedId = generatedId // Store the ID
                    currentScreen = Screen.ElectionList
                }
            )
        }
        is Screen.Login -> {
            val factory = com.example.biometricvotingapp.ui.screens.login.LoginViewModelFactory(
                application,
                com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
            )
            val viewModel: com.example.biometricvotingapp.ui.screens.login.LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { currentScreen = Screen.Registration },
                onLoginSuccess = { generatedId -> // Renamed from anonymizedId for clarity from VM event
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Login successful. Anonymized ID (first 8): ${generatedId.take(8)}")
                    currentAnonymizedId = generatedId // Store the ID
                    currentScreen = Screen.ElectionList
                }
            )
        }
        is Screen.ElectionList -> {
            val factory = com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModelFactory(application, votingRepository)
            val viewModel: com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModel = viewModel(factory = factory)
            ElectionListScreen(
                viewModel = viewModel,
                onElectionClicked = { selectedElection ->
                    if (BuildConfig.DEBUG) Log.d("AppNavigator", "Election clicked: ${selectedElection.title}")
                    if (currentAnonymizedId == null) {
                        if (BuildConfig.DEBUG) Log.e("AppNavigator", "Error: User anonymized ID is null. Cannot navigate to Voting screen. Returning to Login.")
                        currentScreen = Screen.Login // Or handle error appropriately
                    } else {
                        currentScreen = Screen.Voting(selectedElection)
                    }
                },
                onLogout = {
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Logout requested.")
                    currentAnonymizedId = null
                    currentScreen = Screen.Login
                }
            )
        }
        is Screen.Voting -> {
            val voterId = currentAnonymizedId ?: run {
                if (BuildConfig.DEBUG) Log.e("AppNavigator", "Critical error: Navigated to VotingScreen with null anonymizedVoterId. Redirecting to Login.")
                currentScreen = Screen.Login
                return@AppNavigator // Corrected return for Composable
            }
            val factory = com.example.biometricvotingapp.ui.screens.voting.VotingViewModelFactory(application, votingRepository)
            val viewModel: com.example.biometricvotingapp.ui.screens.voting.VotingViewModel = viewModel(factory = factory)
            VotingScreen(
                viewModel = viewModel,
                anonymizedVoterId = voterId,
                election = screen.election,
                onVoteConfirmedAndSubmitted = { confirmedElection, selectedOption ->
                    if (BuildConfig.DEBUG) Log.i("AppNavigator", "Vote confirmed and submitted for ${confirmedElection.title}, option: $selectedOption")
                    currentScreen = Screen.ElectionList // Navigate back to election list
                },
                onNavigateBack = {
                    if (BuildConfig.DEBUG) Log.d("AppNavigator", "Navigating back from VotingScreen to ElectionList.")
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
