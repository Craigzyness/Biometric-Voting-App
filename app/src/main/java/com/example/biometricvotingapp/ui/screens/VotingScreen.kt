package com.example.biometricvotingapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons // Required for ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Required for ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.ui.screens.voting.VotingUiState
import com.example.biometricvotingapp.ui.screens.voting.VotingViewModel
import com.example.biometricvotingapp.ui.screens.voting.VotingViewEvent
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    anonymizedVoterId: String,
    election: Election,
    onVoteConfirmedAndSubmitted: (election: Election, selectedOption: String) -> Unit, // For navigation
    onNavigateBack: () -> Unit,
    viewModel: VotingViewModel // ViewModel instance is now passed directly
    // votingRepository parameter removed
) {
    val context = LocalContext.current // Still needed for BiometricAuthManager
    val activity = LocalContext.current as? FragmentActivity // Still needed for BiometricAuthManager
    // val application = LocalContext.current.applicationContext as Application // No longer needed for factory here

    // val factory = VotingViewModelFactory(application, votingRepository) // Factory logic moved to caller
    // val viewModel: VotingViewModel = viewModel(factory = factory) // VM is passed in

    val uiState by viewModel.uiState.collectAsState()
    var selectedOptionState by remember { mutableStateOf<String?>(null) } // Local state for radio button selection

    // Handle one-time events from ViewModel
    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is VotingViewEvent.ShowBiometricPrompt -> {
                    if (activity != null && selectedOptionState != null) {
                        val biometricAuthManager = BiometricAuthManager(context) // Could be member
                        // Check for biometric availability before prompting
                         when (biometricAuthManager.canAuthenticateWithBiometrics()) {
                            BiometricAvailabilityStatus.AVAILABLE -> {
                                biometricAuthManager.promptForVoteConfirmation(
                                    activity = activity,
                                    electionTitle = election.title,
                                    selectedOption = selectedOptionState!!, // Safe due to check
                                    cryptoObject = event.cryptoObject, // Pass the cryptoObject
                                    onSuccess = { authResult ->
                                        viewModel.onBiometricAuthenticationSuccess(authResult)
                                    },
                                    onError = { errorCode, errString ->
                                        viewModel.onBiometricAuthenticationError(errorCode, errString)
                                    },
                                    onFailed = {
                                        viewModel.onBiometricAuthenticationFailed()
                                    }
                                )
                            }
                            BiometricAvailabilityStatus.NONE_ENROLLED -> {
                                viewModel.onBiometricAuthenticationError(-1, "No fingerprints enrolled. Please enroll to vote.")
                            }
                            else -> {
                                val availability = biometricAuthManager.canAuthenticateWithBiometrics()
                                viewModel.onBiometricAuthenticationError(-1, "Biometric authentication not available ($availability).")
                            }
                        }
                    } else if (selectedOptionState == null) {
                        // This case should ideally be handled by button enabled state, but as a fallback:
                        viewModel.onBiometricAuthenticationError(-1, "Please select an option first.")
                    } else { // activity == null
                        viewModel.onBiometricAuthenticationError(-1, "Activity context not available for BiometricPrompt.")
                    }
                }
                is VotingViewEvent.VoteSubmissionSuccessAndNavigate -> {
                    // selectedOptionState should not be null here if flow is correct
                    onVoteConfirmedAndSubmitted(election, selectedOptionState ?: "")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cast Your Vote") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // Enable back navigation
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = election.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (election.description.isNotBlank()) {
                Text(
                    text = election.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Text(
                text = "Please select an option:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Column(Modifier.selectableGroup()) {
                election.options.forEach { optionText ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (selectedOptionState == optionText),
                                onClick = {
                                    selectedOptionState = optionText
                                    if (uiState is VotingUiState.Error || uiState is VotingUiState.Success) {
                                        viewModel.resetStateToIdle() // Reset message if user changes option after an error/success
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOptionState == optionText),
                            onClick = null // Recommended for accessibility with selectable parent
                        )
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedOptionState?.let { option ->
                        viewModel.onCastVoteClicked(anonymizedVoterId, election.id, option)
                    }
                },
                enabled = selectedOptionState != null && uiState !is VotingUiState.Loading && uiState !is VotingUiState.AwaitingBiometrics && uiState !is VotingUiState.Success,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 16.dp)
            ) {
                when (uiState) {
                    is VotingUiState.Loading, VotingUiState.AwaitingBiometrics -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    }
                    else -> Text("Cast Vote with Fingerprint")
                }
            }

            val currentStatusMessage = when (val state = uiState) {
                is VotingUiState.Loading -> "Submitting your vote..."
                is VotingUiState.Error -> state.message
                is VotingUiState.Success -> state.message // Success message displayed briefly before navigation
                is VotingUiState.AwaitingBiometrics -> "Please confirm with biometrics..."
                else -> null // Idle
            }

            currentStatusMessage?.let { message ->
                Text(
                    text = message,
                    color = when (uiState) {
                        is VotingUiState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary // Includes Loading, Awaiting, Success
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// Sample data for previewing the VotingScreen
val previewElectionSample = Election(
    id = "preview_election_id",
    title = "Sample Election for Preview",
    description = "This is a detailed description of the sample election to see how it looks on the voting screen. It can span multiple lines.",
    options = listOf("Option Alpha", "Option Beta", "Option Gamma", "Option Delta")
)

// TODO: Implement Preview function if needed, after setting up theme and actual dependencies.
// @Preview(showBackground = true)
// @Composable
// fun PreviewVotingScreen() {
//     // YourAppTheme { // Replace with your app's theme
//         VotingScreen(
//             anonymizedVoterId = "previewVoterId",
//             election = previewElectionSample,
//             onVoteConfirmedAndSubmitted = { election, option -> println("Vote for ${election.title} - $option confirmed (Preview)") },
//             onNavigateBack = { println("Navigate back clicked (Preview)") }
//         )
//     // }
// }
