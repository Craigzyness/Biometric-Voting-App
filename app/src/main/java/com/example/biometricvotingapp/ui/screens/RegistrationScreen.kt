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
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAvailabilityStatus
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * RegistrationScreen.kt
 *
 * Purpose: Defines the UI for the user registration screen using Jetpack Compose.
 * This screen allows new users to register by initiating a fingerprint scan.
 */

@Composable
fun RegistrationScreen(
    // onNavigateToLogin: () -> Unit // For existing users - can be re-added if a separate login screen is built
    // For MVP, successful registration might lead to a simulated "logged in" state or a simple success message.
    // We'll manage navigation/feedback via statusMessage for now.
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? FragmentActivity // BiometricPrompt requires FragmentActivity

    // It's better to manage manager instances via ViewModel and DI in a real app.
    // For this step, instantiating directly for simplicity.
    val biometricAuthManager = remember { BiometricAuthManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var registeredId by remember { mutableStateOf<String?>(null) }


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
                onClick = {
                    if (activity == null) {
                        statusMessage = "Error: Could not get required Activity context."
                        Log.e("RegistrationScreen", "FragmentActivity context is null. Cannot show BiometricPrompt.")
                        return@Button
                    }

                    isLoading = true
                    statusMessage = null
                    registeredId = null

                    when (biometricAuthManager.canAuthenticateWithBiometrics()) {
                        BiometricAvailabilityStatus.AVAILABLE -> {
                            biometricAuthManager.promptForRegistration(
                                activity = activity,
                                onSuccess = { authResult ->
                                    isLoading = false
                                    // CRITICAL: Call the updated Anonymized ID Generator
                                    val generatedId = AnonymizedIdGenerator.generate(context, authResult) // Pass context
                                    if (generatedId != null) {
                                        registeredId = generatedId // Store for display
                                        statusMessage = "Registration Successful! (Secure ID: ${generatedId.take(8)}...)"
                                        Log.i("RegistrationScreen", "Biometric Auth Succeeded. Secure ID (first 8 chars): ${generatedId.take(8)}")
                                    } else {
                                        statusMessage = "Registration Error: Failed to generate secure ID."
                                        Log.e("RegistrationScreen", "Failed to generate secure ID after biometric auth.")
                                    }
                                    // TODO: In a real app, save the generatedId securely (if not null) and navigate.
                                },
                                onError = { errString ->
                                    isLoading = false
                                    statusMessage = "Registration Error: $errString"
                                    Log.e("RegistrationScreen", "Biometric Auth Error: $errString")
                                },
                                onFailed = {
                                    isLoading = false
                                    statusMessage = "Registration Failed: Fingerprint not recognized. Please try again."
                                    Log.w("RegistrationScreen", "Biometric Auth Failed.")
                                }
                            )
                        }
                        BiometricAvailabilityStatus.NONE_ENROLLED -> {
                            isLoading = false
                            statusMessage = "Error: No fingerprints enrolled. Please enroll a fingerprint in your device settings."
                            Log.w("RegistrationScreen", "No biometrics enrolled.")
                            // TODO: Offer to navigate to device security settings.
                        }
                        else -> {
                            isLoading = false
                            val availability = biometricAuthManager.canAuthenticateWithBiometrics()
                            statusMessage = "Error: Biometric authentication not available ($availability). Check device settings."
                            Log.w("RegistrationScreen", "Biometrics not available: $availability")
                        }
                    }
                },
                enabled = !isLoading && registeredId == null, // Disable if loading or already registered in this session
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Register with Fingerprint")
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

            // Temporarily remove the Login navigation for simplicity in this step,
            // as the focus is on the registration flow itself.
            // Spacer(modifier = Modifier.height(24.dp))
            // TextButton(onClick = onNavigateToLogin) {
            //     Text("Already registered? Login here")
            // }
        }
    }
}

// Preview function would require more setup (ViewModel mocks, Theme)
// @Preview(showBackground = true)
// @Composable
// fun PreviewRegistrationScreen() {
//     // BiometricVotingAppTheme { // Replace with your app's theme
//         RegistrationScreen()
//     // }
// }
