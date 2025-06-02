package com.example.biometricvotingapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.biometricvotingapp.BuildConfig // Import BuildConfig
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.biometricvotingapp.ui.screens.login.LoginUiState
import com.example.biometricvotingapp.ui.screens.login.LoginViewModel
import com.example.biometricvotingapp.ui.screens.login.LoginViewEvent
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus
import kotlinx.coroutines.flow.collectLatest
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * LoginScreen.kt
 *
 * Purpose: Defines the UI for the user login screen using Jetpack Compose.
 * This screen allows registered users to log in using their fingerprint.
 */

@Composable
fun LoginScreen(
    viewModel: LoginViewModel, // ViewModel instance passed from AppNavigator
    onNavigateToRegister: () -> Unit, // For users who are not registered
    onLoginSuccess: (String) -> Unit // Callback for successful login, passing the anonymized ID
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? FragmentActivity // BiometricPrompt requires FragmentActivity
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewEvent.ShowBiometricPrompt -> {
                    if (activity == null) {
                        viewModel.onBiometricAuthenticationError("Activity context not available for BiometricPrompt.")
                        return@collectLatest
                    }
                    // It's good practice to create BiometricAuthManager instance just before use,
                    // or ensure its context is still valid if it's a longer-lived instance.
                    val biometricAuthManager = BiometricAuthManager(context)
                    when (val availability = biometricAuthManager.canAuthenticateWithBiometrics()) {
                        BiometricAvailabilityStatus.AVAILABLE -> {
                            biometricAuthManager.promptForLogin(
                                activity = activity,
                                onSuccess = { authResult -> viewModel.onBiometricAuthenticationSuccess(authResult) },
                                onError = { _, errString -> viewModel.onBiometricAuthenticationError(errString.toString()) }, // Pass CharSequence as String
                                onFailed = { viewModel.onBiometricAuthenticationFailed() }
                            )
                        }
                        BiometricAvailabilityStatus.NONE_ENROLLED -> {
                            viewModel.onBiometricAuthenticationError("No fingerprints enrolled. Please enroll a fingerprint in your device settings or register if you are a new user.")
                        }
                        else -> {
                             viewModel.onBiometricAuthenticationError("Biometric authentication not available ($availability). Check device settings.")
                        }
                    }
                }
                is LoginViewEvent.NavigateToElectionList -> {
                    onLoginSuccess(event.anonymizedId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Biometric Voting App - Login") })
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
                text = "Please log in using your fingerprint to access the voting features.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { viewModel.onLoginClicked() },
                enabled = uiState !is LoginUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login with Fingerprint")
                }
            }

            val currentStatusMessage = when (val state = uiState) {
                is LoginUiState.Error -> state.message
                // Success messages are generally handled by navigation or a temporary positive indication.
                // If LoginUiState had a Success state with a message, it could be displayed here.
                else -> null 
            }

            currentStatusMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error, // Assuming all messages from this state are errors for now
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = {
                viewModel.resetStateToIdle() // Reset any error messages before navigating
                onNavigateToRegister()
            }) {
                Text("Not registered yet? Register here")
            }
        }
    }
}

// @Preview(showBackground = true)
// @Composable
// fun PreviewLoginScreen() {
//    // YourAppTheme {
//         LoginScreen(
//             onNavigateToRegister = {},
//             onLoginSuccess = {}
//         )
//    // }
// }
