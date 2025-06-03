package com.example.biometricvotingapp.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Keep for direct call if needed, or remove if VM handles all
import com.example.biometricvotingapp.ui.screens.registration.RegistrationUiState
import com.example.biometricvotingapp.ui.screens.registration.RegistrationViewModel
import com.example.biometricvotingapp.ui.screens.registration.RegistrationViewEvent
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel // For viewModel() delegate
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * RegistrationScreen.kt
 *
 * Purpose: Defines the UI for the user registration screen using Jetpack Compose.
 * This screen allows new users to register by initiating a fingerprint scan.
 */

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel, // ViewModel instance is now passed directly
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: (generatedId: String) -> Unit
) {
    val context = LocalContext.current // Still needed for BiometricAuthManager
    val activity = LocalContext.current as? FragmentActivity // Still needed for BiometricAuthManager
    // val application = LocalContext.current.applicationContext as Application // No longer needed here for factory

    // val factory = RegistrationViewModelFactory(application, AnonymizedIdGenerator, votingRepository) // Factory logic moved to caller
    // val viewModel: RegistrationViewModel = viewModel(factory = factory) // VM is passed in

    val uiState by viewModel.uiState.collectAsState()

    // Handle one-time events from ViewModel
    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is RegistrationViewEvent.ShowBiometricPrompt -> {
                    if (activity != null) {
                        val biometricAuthManager = BiometricAuthManager(context) // Could be member if needed elsewhere too
                        // Check for biometric availability before prompting
                        when (biometricAuthManager.canAuthenticateWithBiometrics()) {
                            BiometricAvailabilityStatus.AVAILABLE -> {
                                biometricAuthManager.promptForRegistration(
                                    activity = activity,
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
                                viewModel.onBiometricAuthenticationError(-1, "No fingerprints enrolled. Please enroll in device settings.")
                            }
                            else -> {
                                val availability = biometricAuthManager.canAuthenticateWithBiometrics()
                                viewModel.onBiometricAuthenticationError(-1, "Biometric authentication not available ($availability). Check device settings.")
                            }
                        }
                    } else {
                         viewModel.onBiometricAuthenticationError(-1, "Activity context not available for BiometricPrompt.")
                    }
                }
                is RegistrationViewEvent.NavigateToElectionList -> {
                    onRegistrationSuccess(event.generatedId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Biometric Voting App - Register") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Biometric Voting App",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Welcome! Securely register to cast your vote using your fingerprint.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Your privacy is our priority. Your fingerprint is processed on this device to create a secure, anonymized ID. Raw fingerprint data is NEVER stored or sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { viewModel.onRegisterClicked() },
                enabled = uiState !is RegistrationUiState.Loading && uiState !is RegistrationUiState.AwaitingBiometrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                when (uiState) {
                    is RegistrationUiState.Loading, RegistrationUiState.AwaitingBiometrics -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).testTag("loadingIndicator"),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    else -> Text("Register with Fingerprint")
                }
            }

            // Display status messages from the ViewModel
            val currentStatusMessage = when (val state = uiState) {
                is RegistrationUiState.Loading -> state.message
                is RegistrationUiState.Error -> state.message
                is RegistrationUiState.Success -> state.message // Success message now comes from VM
                is RegistrationUiState.AwaitingBiometrics -> "Awaiting biometric authentication..."
                else -> null // Idle
            }

            currentStatusMessage?.let { message ->
                Text(
                    text = message,
                    color = when (uiState) {
                        is RegistrationUiState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp).testTag("statusMessageText")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Already registered? Login here")
            }

            Spacer(modifier = Modifier.height(32.dp)) // Add some space before the version text

            Text(
                text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Preview function would require more setup (ViewModel mocks, Theme)
// @Preview(showBackground = true)
// @Composable
// fun PreviewRegistrationScreen() {
//     // BiometricVotingAppTheme { // Replace with your app's theme
//         RegistrationScreen(
//             onNavigateToLogin = {},
//             onRegistrationSuccess = {}
//         )
//     // }
// }
