package com.example.biometricvotingapp.ui.screens

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
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * LoginScreen.kt
 *
 * Purpose: Defines the UI for the user login screen using Jetpack Compose.
 * This screen allows registered users to log in using their fingerprint.
 */

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit, // For users who are not registered
    onLoginSuccess: (String?) -> Unit // Callback for successful login, passing a message or ID
    // In a real app, this might pass an AuthenticationResult or trigger navigation via ViewModel
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? FragmentActivity // BiometricPrompt requires FragmentActivity

    // Better to manage via ViewModel and DI in a real app.
    val biometricAuthManager = remember { BiometricAuthManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                onClick = {
                    if (activity == null) {
                        statusMessage = "Error: Could not get required Activity context for BiometricPrompt."
                        Log.e("LoginScreen", "FragmentActivity context is null.")
                        return@Button
                    }

                    isLoading = true
                    statusMessage = null

                    when (biometricAuthManager.canAuthenticateWithBiometrics()) {
                        BiometricAvailabilityStatus.AVAILABLE -> {
                            biometricAuthManager.promptForLogin(
                                activity = activity,
                                onSuccess = { authResult ->
                                    isLoading = false
                                    Log.i("LoginScreen", "Login Biometric Auth Succeeded. AuthResult: $authResult")

                                    // Now, try to get/re-derive the anonymized ID to confirm registration.
                                    val registeredAnonymizedId = AnonymizedIdGenerator.getRegisteredAnonymizedId(context)

                                    if (registeredAnonymizedId != null) {
                                        statusMessage = "Login Successful! Welcome back. (ID: ${registeredAnonymizedId.take(8)}...)"
                                        Log.i("LoginScreen", "User is registered. Anonymized ID (first 8 chars): ${registeredAnonymizedId.take(8)}")
                                        onLoginSuccess(registeredAnonymizedId) // Pass the ID or a success status
                                    } else {
                                        statusMessage = "Biometric recognized, but app registration not found. Please register if you haven't."
                                        Log.w("LoginScreen", "Biometric auth success, but no registered anonymized ID components found.")
                                        onLoginSuccess(null) // or some error status if the callback expects it
                                    }
                                    // TODO: Navigate to main app screen on successful login and ID retrieval.
                                },
                                onError = { errString ->
                                    isLoading = false
                                    statusMessage = "Login Error: $errString"
                                    Log.e("LoginScreen", "Login Biometric Auth Error: $errString")
                                },
                                onFailed = {
                                    isLoading = false
                                    statusMessage = "Login Failed: Fingerprint not recognized. Please try again."
                                    Log.w("LoginScreen", "Login Biometric Auth Failed.")
                                }
                            )
                        }
                        BiometricAvailabilityStatus.NONE_ENROLLED -> {
                            isLoading = false
                            statusMessage = "Error: No fingerprints enrolled. Please enroll a fingerprint in your device settings or register if you are a new user."
                            Log.w("LoginScreen", "No biometrics enrolled for login.")
                        }
                        else -> {
                            isLoading = false
                            val availability = biometricAuthManager.canAuthenticateWithBiometrics()
                            statusMessage = "Error: Biometric authentication not available ($availability)."
                            Log.w("LoginScreen", "Biometrics not available for login: $availability")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login with Fingerprint")
                }
            }

            statusMessage?.let {
                Text(
                    text = it,
                    color = if (it.contains("Error", ignoreCase = true) || it.contains("Failed", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
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
