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
import com.example.biometricvotingapp.domain.model.Election
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    election: Election,
    // Renamed and repurposed: called after biometric success
    onVoteConfirmedBiometrically: (election: Election, selectedOption: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? FragmentActivity

    val biometricAuthManager = remember { BiometricAuthManager(context) }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var voteSuccessfullyCast by remember { mutableStateOf(false) } // New state variable

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
                                selected = (selectedOption == optionText),
                                onClick = { selectedOption = optionText; statusMessage = null /* Clear message on new selection */ },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == optionText),
                            onClick = null
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
                    val currentSelectedOption = selectedOption
                    if (currentSelectedOption == null) {
                        statusMessage = "Please select an option before casting your vote."
                        return@Button
                    }
                    if (activity == null) {
                        statusMessage = "Error: Cannot perform biometric authentication."
                        Log.e("VotingScreen", "FragmentActivity context is null for biometrics.")
                        return@Button
                    }

                    isLoading = true
                    statusMessage = null

                    when (biometricAuthManager.canAuthenticateWithBiometrics()) {
                        BiometricAvailabilityStatus.AVAILABLE -> {
                            biometricAuthManager.promptForVoteConfirmation(
                                activity = activity,
                                electionTitle = election.title,
                                selectedOption = currentSelectedOption,
                                onSuccess = { authResult ->
                                    isLoading = false
                                    Log.i("VotingScreen", "Vote Biometric Auth Succeeded. AuthResult: $authResult")

                                    // === This is the new post-vote confirmation logic for MVP ===
                                    voteSuccessfullyCast = true
                                    statusMessage = "Vote for '${election.title}' (Option: '$currentSelectedOption') submitted for processing!"
                                    // ============================================================

                                    // Still call the external callback if MainActivity or other parent needs to know
                                    onVoteConfirmedBiometrically(election, currentSelectedOption)
                                },
                                onError = { errString ->
                                    isLoading = false
                                    statusMessage = "Vote Confirmation Error: $errString"
                                    Log.e("VotingScreen", "Vote Biometric Auth Error: $errString")
                                },
                                onFailed = {
                                    isLoading = false
                                    statusMessage = "Vote Confirmation Failed: Fingerprint not recognized."
                                    Log.w("VotingScreen", "Vote Biometric Auth Failed.")
                                }
                            )
                        }
                        BiometricAvailabilityStatus.NONE_ENROLLED -> {
                            isLoading = false
                            statusMessage = "Error: No fingerprints enrolled. Please enroll a fingerprint to cast your vote."
                        }
                        else -> {
                            isLoading = false
                            val availability = biometricAuthManager.canAuthenticateWithBiometrics()
                            statusMessage = "Error: Biometric authentication not available ($availability)."
                        }
                    }
                },
                enabled = selectedOption != null && !isLoading && !voteSuccessfullyCast,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Cast Vote with Fingerprint")
                }
            }

            statusMessage?.let {
                Text(
                    text = it,
                    color = if (it.contains("Error", ignoreCase = true) || it.contains("Failed", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
//             election = previewElectionSample,
//             onVoteConfirmedBiometrically = { election, option -> println("Vote for ${election.title} - $option confirmed (Preview)") },
//             onNavigateBack = { println("Navigate back clicked (Preview)") }
//         )
//     // }
// }
