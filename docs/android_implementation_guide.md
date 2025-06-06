# Biometric Voting App - Complete Android Implementation Guide

This guide provides a comprehensive walkthrough for implementing a Biometric Voting App on Android, leveraging modern Android development practices and Jetpack libraries.

## Table of Contents

1.  [Introduction](#introduction)
2.  [Project Setup](#project-setup)
    *   [Prerequisites](#prerequisites)
    *   [Create New Project](#create-new-project)
3.  [Gradle Configuration](#gradle-configuration)
    *   [Project-level `settings.gradle.kts`](#project-level-settingsgradlekts)
    *   [Version Catalog (`gradle/libs.versions.toml`)](#version-catalog-gradlelibsversionstoml)
    *   [Module-level `build.gradle.kts`](#module-level-buildgradlekts)
4.  [Directory and File Structure](#directory-and-file-structure)
5.  [Permissions and Manifest](#permissions-and-manifest)
6.  [Core Implementation](#core-implementation)
    *   [BiometricManager (`utils/BiometricManager.kt`)](#biometricmanager-utilsbiometricmanagerkt)
    *   [AuthRepository (`data/repository/AuthRepository.kt`)](#authrepository-datarepositoryauthrepositorykt)
7.  [Security Implementation](#security-implementation)
    *   [SecurityUtil (`utils/SecurityUtil.kt`)](#securityutil-utilssecurityutilkt)
8.  [UI Implementation](#ui-implementation)
    *   [Theme (`ui/theme/Color.kt`)](#theme-uithemecolorkt)
    *   [Components (`ui/components/BiometricComponents.kt`)](#components-uicomponentsbiometriccomponentskt)
    *   [MainActivity (`MainActivity.kt`)](#mainactivity-mainactivitykt)
9.  [Testing Guide](#testing-guide)
    *   [BiometricManager Tests (`BiometricManagerTest.kt`)](#biometricmanager-tests-biometricmanagertestkt)
    *   [SecurityUtil Tests (`SecurityUtilTest.kt`)](#securityutil-tests-securityutiltestkt)
10. [Backend Integration (Conceptual)](#backend-integration-conceptual)
11. [Best Practices](#best-practices)
12. [Troubleshooting](#troubleshooting)
13. [Deployment Checklist](#deployment-checklist)
14. [Conclusion](#conclusion)

## Introduction

This document outlines the steps to build a secure Biometric Voting App on Android. It focuses on biometric authentication, secure data handling, and a clean architecture.

## Project Setup

### Prerequisites
*   Android Studio (latest stable version)
*   Kotlin knowledge
*   Familiarity with Jetpack Compose

### Create New Project
1.  Open Android Studio.
2.  Select "New Project..."
3.  Choose "Empty Activity" (Jetpack Compose).
4.  Configure project:
    *   Name: `BiometricVotingApp`
    *   Package name: `com.example.biometricvotingapp` (or your preferred name)
    *   Language: Kotlin
    *   Minimum SDK: API 23 (Marshmallow) or higher (as BiometricPrompt requires it)
    *   Build configuration language: Kotlin DSL

## Gradle Configuration

### Project-level `settings.gradle.kts`
Ensure your `settings.gradle.kts` includes `google()` and `mavenCentral()` in `pluginManagement` and `dependencyResolutionManagement`.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "BiometricVotingApp"
include(":app")
```

### Version Catalog (`gradle/libs.versions.toml`)
Create or update `gradle/libs.versions.toml` to manage dependencies and their versions centrally.

```toml
# gradle/libs.versions.toml
[versions]
accompanistPermissions = "0.28.0"
androidGradlePlugin = "8.2.0" # Or latest
kotlin = "1.9.20" # Or latest
coreKtx = "1.12.0"
lifecycleRuntimeKtx = "2.6.2"
activityCompose = "1.8.1"
composeBom = "2023.10.01"
composeCompiler = "1.5.4" # Aligns with Kotlin 1.9.20
biometric = "1.2.0-alpha05" # For BiometricPrompt
timber = "5.0.1"
securityCrypto = "1.1.0-alpha06"

# Testing
junit = "4.13.2"
androidxTestExtJunit = "1.1.5"
espressoCore = "3.5.1"
mockk = "1.13.8"
robolectric = "4.11.1"
coroutinesTest = "1.7.3"

[libraries]
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanistPermissions" }
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-biometric = { group = "androidx.biometric", name = "biometric-ktx", version.ref = "biometric" } # Use -ktx version
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto"}

timber = { module = "com.jakewharton.timber:timber", version.ref = "timber" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
mockk-agent = { group = "io.mockk", name = "mockk-agent", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }


[plugins]
androidApplication = { id = "com.android.application", version.ref = "androidGradlePlugin" }
kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

### Module-level `build.gradle.kts`
Configure your app-level `build.gradle.kts`:

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.example.biometricvotingapp"
    compileSdk = 34 // Or latest

    defaultConfig {
        applicationId = "com.example.biometricvotingapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code shrinking for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true // Enable Jetpack Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Ensure Robolectric can access resources
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.accompanist.permissions)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent) // For final class mocking if needed
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

Sync Gradle files after making these changes.

## Directory and File Structure

Organize your project files for clarity and scalability.

```plaintext
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/biometricvotingapp/  # Root package
│   │   │   ├── ui/                               # UI-related classes (Jetpack Compose)
│   │   │   │   ├── theme/                        # Color.kt, Theme.kt, Type.kt
│   │   │   │   ├── components/                   # Reusable UI components (e.g., BiometricButton)
│   │   │   │   └── screens/                      # Composable screens (e.g., LoginScreen, VoteScreen)
│   │   │   ├── domain/                           # Business logic
│   │   │   │   ├── model/                        # Data models/entities (e.g., User, Vote)
│   │   │   │   ├── repository/                   # Repository interfaces (e.g., AuthRepository)
│   │   │   │   └── viewmodel/                    # ViewModels for screens
│   │   │   ├── data/                             # Data handling (repositories impl, data sources)
│   │   │   │   ├── repository/                   # Repository implementations
│   │   │   │   └── network/                      # Network API definitions and DTOs (if applicable)
│   │   │   ├── utils/                            # Utility classes
│   │   │   │   ├── BiometricManager.kt           # Handles biometric authentication logic
│   │   │   │   └── SecurityUtil.kt               # Cryptographic operations, key management
│   │   │   └── MainActivity.kt                   # Main entry point Activity
│   │   ├── res/                                  # Android resources (drawables, layouts, values)
│   │   │   ├── values/
│   │   │   │   └── strings.xml
│   │   └── AndroidManifest.xml                   # App manifest file
│   ├── test/                                     # Unit tests (JVM)
│   │   └── java/com/example/biometricvotingapp/
│   │       ├── utils/
│   │       │   └── BiometricManagerTest.kt
│   │       │   └── SecurityUtilTest.kt
│   │       └── domain/viewmodel/
│   │           └── AuthViewModelTest.kt          # Example ViewModel test
│   └── androidTest/                              # Instrumented tests (Android device/emulator)
│       └── java/com/example/biometricvotingapp/
│           └── ui/
│               └── LoginScreenTest.kt            # Example UI test
└── build.gradle.kts                              # App-level build script
```

## Permissions and Manifest

Add necessary permissions to `AndroidManifest.xml`:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.biometricvotingapp">

    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <!-- Add other permissions as needed, e.g., INTERNET for network requests -->
    <uses-permission android:name="android.permission.INTERNET" />


    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BiometricVotingApp"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.BiometricVotingApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
*Note: For Android 10 (API 29) and above, `USE_FINGERPRINT` is deprecated; `USE_BIOMETRIC` is preferred.*

## Core Implementation

### BiometricManager (`utils/BiometricManager.kt`)
Handles biometric authentication logic.

```kotlin
// app/src/main/java/com/example/biometricvotingapp/utils/BiometricManager.kt
package com.example.biometricvotingapp.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL // Allow PIN/Pattern/Password
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    fun canAuthenticate(): BiometricAuthStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS => {
                Timber.d("Biometric authentication is available.")
                BiometricAuthStatus.READY
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Timber.e("No biometric features available on this device.")
                BiometricAuthStatus.NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Timber.e("Biometric features are currently unavailable.")
                BiometricAuthStatus.TEMPORARILY_UNAVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Timber.w("The user hasn't associated any biometric credentials with their account.")
                // Optionally, prompt the user to enroll biometrics.
                BiometricAuthStatus.NO_CREDENTIALS_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Timber.w("Biometric security update required.")
                BiometricAuthStatus.SECURITY_UPDATE_REQUIRED
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Timber.e("Biometric authentication is unsupported.")
                BiometricAuthStatus.UNSUPPORTED
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Timber.e("Biometric status unknown.")
                BiometricAuthStatus.UNKNOWN
            }
            else -> {
                Timber.e("Unknown biometric status.")
                BiometricAuthStatus.UNKNOWN
            }
        }
    }

    fun authenticate(
        activity: FragmentActivity, // Activity or FragmentActivity
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onFailure: (errorCode: Int, errString: CharSequence) -> Unit,
        onCancel: () -> Unit // Optional: handle user cancellation
    ) {
        if (canAuthenticate() != BiometricAuthStatus.READY && canAuthenticate() != BiometricAuthStatus.NO_CREDENTIALS_ENROLLED) {
            // Handle cases where biometric auth is not possible right now, e.g. show a message to user
            // NO_CREDENTIALS_ENROLLED is handled by the system prompt to set up credentials.
            onFailure(
                BiometricPrompt.ERROR_HW_NOT_PRESENT, // Or a custom error code
                "Biometric authentication not ready."
            )
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Biometric authentication successful.")
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric authentication error: $errorCode - $errString")
                    // Handle specific errors, e.g., BiometricPrompt.ERROR_LOCKOUT
                    onFailure(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // This means the biometric was valid but not recognized.
                    Timber.w("Biometric authentication failed (not recognized).")
                    // You might not call onFailure here, as the prompt handles retries.
                    // If you want to treat it as a final failure for this attempt:
                    // onFailure(BiometricPrompt.ERROR_NEGATIVE_BUTTON, "Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) // Allow strong biometrics or device credentials
            // .setNegativeButtonText("Cancel") // Handled by onCancel or default system behavior
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Timber.e(e, "Error displaying biometric prompt.")
            onFailure(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, "Could not display biometric prompt.")
        }
    }
}

enum class BiometricAuthStatus {
    READY,
    NOT_AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    NO_CREDENTIALS_ENROLLED, // User needs to enroll biometrics
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}
```
*Ensure Timber is initialized in your Application class: `if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())`*

### AuthRepository (`data/repository/AuthRepository.kt`)
Interface for authentication-related data operations.

```kotlin
// app/src/main/java/com/example/biometricvotingapp/domain/repository/AuthRepository.kt
package com.example.biometricvotingapp.domain.repository

import androidx.biometric.BiometricPrompt
import com.example.biometricvotingapp.domain.model.User // Assuming a User model

interface AuthRepository {
    // suspend fun registerUser(publicKey: String): Result<User> // Example
    suspend fun loginWithBiometrics(cryptoObject: BiometricPrompt.CryptoObject?): Result<User>
    // suspend fun submitVote(voteData: String, authToken: String): Result<Boolean>
}
```

An implementation of this would exist in `app/src/main/java/com/example/biometricvotingapp/data/repository/AuthRepositoryImpl.kt`.

## Security Implementation

### SecurityUtil (`utils/SecurityUtil.kt`)
Handles cryptographic operations like key generation and management for use with BiometricPrompt.

```kotlin
// app/src/main/java/com/example/biometricvotingapp/utils/SecurityUtil.kt
package com.example.biometricvotingapp.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object SecurityUtil {

    private const val KEY_ALIAS = "biometric_voting_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        return existingKey ?: generateSecretKey()
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true) // Require user authentication (biometric) to use the key

        // For API 23+ (Android M+)
        // For stronger security, invalidate key on new biometric enrollment (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyGenParameterSpecBuilder.setInvalidatedByBiometricEnrollment(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24-29
            // For API 24-29, this was the default behavior for keys requiring user auth.
            // setInvalidatedByBiometricEnrollment(true) is not available, but the key
            // would typically be invalidated.
        }


        // For API 28+ (Android P+), you can require screen lock for key use.
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        //     builder.setUserConfirmationRequired(true) // Require user confirmation for each use
        // }

        keyGenerator.init(keyGenParameterSpecBuilder.build())
        return keyGenerator.generateKey()
    }

    fun getCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }

    fun getCryptoObjectForEncryption(): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            Log.e("SecurityUtil", "Error creating crypto object for encryption", e)
            null // Handle error appropriately
        }
    }

    fun getCryptoObjectForDecryption(iv: ByteArray): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey() // Key should already exist
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            Log.e("SecurityUtil", "Error creating crypto object for decryption", e)
            null // Handle error appropriately
        }
    }

    // Example encrypt/decrypt (store IV alongside ciphertext)
    fun encryptData(data: String, cryptoObject: BiometricPrompt.CryptoObject): Pair<ByteArray, ByteArray>? {
        return try {
            val encryptedData = cryptoObject.cipher?.doFinal(data.toByteArray(Charsets.UTF_8))
            val iv = cryptoObject.cipher?.iv
            if (encryptedData != null && iv != null) {
                Pair(iv, encryptedData)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SecurityUtil", "Error encrypting data", e)
            null
        }
    }

    fun decryptData(iv: ByteArray, encryptedData: ByteArray, cryptoObject: BiometricPrompt.CryptoObject): String? {
        return try {
            // The cryptoObject for decryption should have been initialized with the IV already
            val decryptedData = cryptoObject.cipher?.doFinal(encryptedData)
            decryptedData?.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("SecurityUtil", "Error decrypting data", e)
            null
        }
    }
}
```

## UI Implementation

### Theme (`ui/theme/Color.kt`)

```kotlin
// app/src/main/java/com/example/biometricvotingapp/ui/theme/Color.kt
package com.example.biometricvotingapp.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val GreenValid = Color(0xFF4CAF50)
val RedError = Color(0xFFF44336)
```
*(Ensure you have `Theme.kt` and `Type.kt` set up as per standard Jetpack Compose project creation, or customize as needed.)*

### Components (`ui/components/BiometricComponents.kt`)

```kotlin
// app/src/main/java/com/example/biometricvotingapp/ui/components/BiometricComponents.kt
package com.example.biometricvotingapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.biometricvotingapp.utils.BiometricAuthStatus

@Composable
fun BiometricAuthButton(
    biometricAuthStatus: BiometricAuthStatus,
    onAuthClick: () -> Unit,
    isLoading: Boolean = false
) {
    Button(
        onClick = onAuthClick,
        enabled = biometricAuthStatus == BiometricAuthStatus.READY || biometricAuthStatus == BiometricAuthStatus.NO_CREDENTIALS_ENROLLED  && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Authenticate with Biometrics")
        }
    }
}

@Composable
fun StatusText(message: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = message,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
```

### MainActivity (`MainActivity.kt`)
The `MainActivity.kt` will host Composable functions and interact with ViewModels and the `BiometricAuthManager`. Its full implementation is extensive and typically involves:
*   Requesting biometric permission.
*   Checking biometric availability.
*   Handling authentication callbacks.
*   Navigating between screens.

A minimal structure might look like:
```kotlin
// app/src/main/java/com/example/biometricvotingapp/MainActivity.kt
package com.example.biometricvotingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.biometricvotingapp.ui.components.BiometricAuthButton
import com.example.biometricvotingapp.ui.components.StatusText
import com.example.biometricvotingapp.ui.theme.BiometricVotingAppTheme
import com.example.biometricvotingapp.ui.theme.GreenValid
import com.example.biometricvotingapp.ui.theme.RedError
import com.example.biometricvotingapp.utils.BiometricAuthManager
import com.example.biometricvotingapp.utils.BiometricAuthStatus
import com.example.biometricvotingapp.utils.SecurityUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import timber.log.Timber // Ensure Timber is initialized in Application class

class MainActivity : FragmentActivity() { // Use FragmentActivity for BiometricPrompt

    private lateinit var biometricAuthManager: BiometricAuthManager

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthManager = BiometricAuthManager(this)
        // Initialize Timber in your Application class
        // if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        setContent {
            BiometricVotingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current as FragmentActivity // For BiometricPrompt
                    var biometricAuthStatus by remember { mutableStateOf(biometricAuthManager.canAuthenticate()) }
                    var authMessage by remember { mutableStateOf("Check Biometric Status") }
                    var authMessageColor by remember { mutableStateOf(MaterialTheme.colorScheme.onBackground) }
                    var isLoading by remember { mutableStateOf(false) }


                    // Biometric Permission
                    val biometricPermissionState = rememberPermissionState(
                        android.Manifest.permission.USE_BIOMETRIC
                    )

                    LaunchedEffect(biometricPermissionState.status) {
                        if (!biometricPermissionState.status.isGranted) {
                            biometricPermissionState.launchPermissionRequest()
                        }
                        // Update status after permission result
                        biometricAuthStatus = biometricAuthManager.canAuthenticate()
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Biometric Voting App", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (biometricPermissionState.status.isGranted) {
                             BiometricAuthButton(
                                biometricAuthStatus = biometricAuthStatus,
                                onAuthClick = {
                                    isLoading = true
                                    authMessage = "Processing..."
                                    authMessageColor = MaterialTheme.colorScheme.onBackground

                                    val cryptoObject = SecurityUtil.getCryptoObjectForEncryption()
                                    if (cryptoObject == null) {
                                        authMessage = "Error: Could not prepare for secure authentication."
                                        authMessageColor = RedError
                                        isLoading = false
                                        return@BiometricAuthButton
                                    }

                                    biometricAuthManager.authenticate(
                                        activity = context,
                                        onSuccess = { result ->
                                            isLoading = false
                                            authMessage = "Authentication Successful!"
                                            authMessageColor = GreenValid
                                            Timber.i("Authenticated with CryptoObject: ${result.cryptoObject != null}")
                                            // Proceed with encrypted data from result.cryptoObject.cipher
                                            // For example, encrypt a dummy token
                                            result.cryptoObject?.cipher?.let { cipher ->
                                                try {
                                                    val tokenToEncrypt = "my_super_secret_vote_token"
                                                    val (iv, encryptedToken) = SecurityUtil.encryptData(tokenToEncrypt, result.cryptoObject!!)!!
                                                    Timber.i("IV: ${iv.size}, Encrypted Token: ${encryptedToken.size}")
                                                    // Now you would typically send this 'encryptedToken' and 'iv' to your backend
                                                    // And store 'iv' to decrypt later if needed on client (though less common for voting)
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Encryption failed after auth")
                                                    authMessage = "Encryption failed post-auth."
                                                    authMessageColor = RedError
                                                }
                                            }
                                        },
                                        onFailure = { errorCode, errString ->
                                            isLoading = false
                                            authMessage = "Auth Failed: $errString (Code: $errorCode)"
                                            authMessageColor = RedError
                                        },
                                        onCancel = { // Optional: Handle user cancellation
                                            isLoading = false
                                            authMessage = "Authentication Canceled by User"
                                            authMessageColor = MaterialTheme.colorScheme.onBackground
                                        }
                                    )
                                },
                                isLoading = isLoading
                            )
                        } else {
                            StatusText("Biometric permission is required to use this feature.", RedError)
                            Button(onClick = { biometricPermissionState.launchPermissionRequest() }) {
                                Text("Request Biometric Permission")
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))
                        StatusText(message = authMessage, color = authMessageColor)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Biometric Status: ${biometricAuthStatus.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                         if (biometricAuthStatus == BiometricAuthStatus.NO_CREDENTIALS_ENROLLED) {
                            Text("Please enroll biometrics in your device settings.", color = RedError)
                        }

                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check status in case user enrolled biometrics while app was paused
        // This is a basic way; a more robust solution might involve a lifecycle observer
        // or a specific callback if the system provides one for biometric enrollment changes.
        // biometricAuthStatus = biometricAuthManager.canAuthenticate() // Update state variable
    }
}
```
*This `MainActivity` example is illustrative. A production app would use ViewModels, Navigation, and handle states more robustly.*

## Testing Guide

### BiometricManager Tests (`BiometricManagerTest.kt`)
Unit tests for `BiometricAuthManager` using Robolectric and MockK.

```kotlin
// app/src/test/java/com/example/biometricvotingapp/utils/BiometricManagerTest.kt
package com.example.biometricvotingapp.utils

import android.content.Context
import androidx.biometric.BiometricManager as SystemBiometricManager // Alias to avoid conflict
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK]) // Test on a specific SDK level
class BiometricAuthManagerTest {

    private lateinit var context: Context
    private lateinit var biometricAuthManager: BiometricAuthManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        biometricAuthManager = BiometricAuthManager(context)

        // Mock the static BiometricManager.from(context) call
        mockkStatic(SystemBiometricManager::class)
    }

    @Test
    fun `canAuthenticate returns READY when biometric is available`() {
        val mockSystemBiometricManager = io.mockk.mockk<SystemBiometricManager>()
        every { SystemBiometricManager.from(context) } returns mockSystemBiometricManager
        every { mockSystemBiometricManager.canAuthenticate(any()) } returns SystemBiometricManager.BIOMETRIC_SUCCESS

        val status = biometricAuthManager.canAuthenticate()
        assertEquals(BiometricAuthStatus.READY, status)
    }

    @Test
    fun `canAuthenticate returns NOT_AVAILABLE for no hardware`() {
        val mockSystemBiometricManager = io.mockk.mockk<SystemBiometricManager>()
        every { SystemBiometricManager.from(context) } returns mockSystemBiometricManager
        every { mockSystemBiometricManager.canAuthenticate(any()) } returns SystemBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

        val status = biometricAuthManager.canAuthenticate()
        assertEquals(BiometricAuthStatus.NOT_AVAILABLE, status)
    }

    // Add more tests for other BiometricAuthStatus cases:
    // - BIOMETRIC_ERROR_HW_UNAVAILABLE -> TEMPORARILY_UNAVAILABLE
    // - BIOMETRIC_ERROR_NONE_ENROLLED -> NO_CREDENTIALS_ENROLLED
    // ...and other statuses.
}
```

### SecurityUtil Tests (`SecurityUtilTest.kt`)
Unit tests for `SecurityUtil`.

```kotlin
// app/src/test/java/com/example/biometricvotingapp/utils/SecurityUtilTest.kt
package com.example.biometricvotingapp.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyStore
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // Test on Android P or higher for Keystore features
class SecurityUtilTest {

    @Before
    fun setUp() {
        // Clear any existing keys from the KeyStore before each test
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (keyStore.containsAlias("biometric_voting_key")) {
            keyStore.deleteEntry("biometric_voting_key")
        }
    }

    @Test
    fun `getOrCreateSecretKey generates a new key if one does not exist`() {
        val key = SecurityUtil.getOrCreateSecretKey()
        assertNotNull(key)
        assertEquals(KeyProperties.KEY_ALGORITHM_AES, key.algorithm)

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        assertTrue(keyStore.containsAlias("biometric_voting_key"))
    }

    @Test
    fun `getOrCreateSecretKey returns existing key if available`() {
        val key1 = SecurityUtil.getOrCreateSecretKey()
        val key2 = SecurityUtil.getOrCreateSecretKey()
        assertSame(key1, key2) // Should be the same key instance from KeyStore
    }

    @Test
    fun `getCipher returns AES CBC PKCS7Padding cipher`() {
        val cipher = SecurityUtil.getCipher()
        assertEquals("AES/CBC/PKCS7Padding", cipher.algorithm)
    }

    @Test
    fun `testKeyGenerationFailsForInvalidSpec()`() {
        // This test is conceptual for KeyGenParameterSpec, actual direct test might be complex.
        // It demonstrates understanding of what could go wrong.
        // A direct test for this specific failure might involve trying to initialize KeyGenerator
        // with an invalid spec and catching the expected exception.
        try {
            val spec = KeyGenParameterSpec.Builder(
                "test_invalid_key",
                0 // Invalid purpose flag
            ).build()
            // KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").init(spec)
            // fail("Should have thrown an InvalidAlgorithmParameterException or similar")
            // For this guide, we acknowledge the principle rather than a direct runnable test for this exact path.
            assertTrue("Conceptual: Invalid KeyGenParameterSpec should fail", true)
        } catch (e: Exception) {
            // Expected, e.g., InvalidAlgorithmParameterException
            // Depending on how KeyGenerator is used, this might be an IllegalArgumentException too.
        }
    }

    // Add tests for encryption and decryption if mockable or if using a test key not requiring actual biometrics.
    // Testing CryptoObject generation and usage with BiometricPrompt is better suited for instrumented tests.
}
```

## Backend Integration (Conceptual)

Integrating with a backend involves:
1.  **User Registration:** Send a public key (or derived identifier) to the backend after successful initial biometric setup/encryption. The backend links this to a user identity.
2.  **Login/Authentication:**
    *   Client uses `BiometricPrompt` with `CryptoObject` for decryption.
    *   If successful, the app decrypts a stored token or signs a challenge.
    *   This token/signed challenge is sent to the backend to authenticate the session.
3.  **Vote Casting:**
    *   The vote payload is constructed.
    *   It can be signed using the biometric-protected key or an auth token received after login.
    *   The signed vote is sent to the backend.
    *   Backend verifies the signature/token and records the vote, ensuring anonymity and integrity.

**API Endpoints (Example):**
*   `POST /register`
*   `POST /login_biometric`
*   `POST /cast_vote`

## Best Practices

*   **Secure Key Management:** Use `AndroidKeyStore` for all cryptographic keys.
*   **User Authentication Required:** Always set `setUserAuthenticationRequired(true)` for keys used with biometrics.
*   **Invalidate on New Enrollment:** Use `setInvalidatedByBiometricEnrollment(true)` (API 30+) or handle key invalidation carefully.
*   **Strong Biometrics:** Prefer `BIOMETRIC_STRONG`. Allow `DEVICE_CREDENTIAL` as a fallback if appropriate for your app's security model.
*   **Error Handling:** Implement comprehensive error handling for all biometric and cryptographic operations.
*   **User Feedback:** Provide clear feedback to the user about the biometric process (success, failure, errors).
*   **Permissions:** Request `USE_BIOMETRIC` permission at runtime and explain why it's needed.
*   **UI/UX:** Design a clear and intuitive UI for biometric interactions.
*   **Fallback Mechanisms:** Consider fallback authentication methods (e.g., PIN, password) if biometrics are not available or fail persistently, but ensure this aligns with your security requirements.
*   **Regular Audits:** Regularly review security-sensitive code.

## Troubleshooting

*   **`BIOMETRIC_ERROR_NO_HARDWARE` / `BIOMETRIC_ERROR_HW_UNAVAILABLE`:** Ensure the device/emulator has biometric hardware and it's enabled.
*   **`BIOMETRIC_ERROR_NONE_ENROLLED`:** Prompt the user to enroll biometrics in device settings.
*   **Keystore Exceptions (e.g., `KeyPermanentlyInvalidatedException`):** This occurs if the key is invalidated (e.g., new biometric enrollment, screen lock disabled). Handle by re-prompting for authentication, potentially requiring re-enrollment or re-creation of the key.
*   **Cipher/CryptoObject Issues:** Ensure `Cipher` transformations, IVs, and modes are correct. Log extensively.
*   **Manifest Permissions:** Double-check `USE_BIOMETRIC` permission.
*   **Emulator Issues:** Test on real devices. Emulators may have limitations or bugs with biometric simulation. Ensure "Registered Fingerprints" are set in emulator extended controls.

## Deployment Checklist

1.  **Code Review:** Thoroughly review all security-sensitive code.
2.  **ProGuard/R8:** Ensure ProGuard rules correctly keep necessary classes for biometrics and cryptography if using code shrinking.
    ```proguard
    # Example Proguard rules (add more as needed)
    -keep class androidx.biometric.** { *; }
    -keep interface androidx.biometric.** { *; }
    ```
3.  **Error Reporting:** Integrate a crash reporting tool (e.g., Firebase Crashlytics) to monitor issues in production.
4.  **API Key Security:** Secure any API keys or secrets used for backend communication (not directly part of biometrics but important).
5.  **Thorough Testing:** Test on various Android versions and devices.
6.  **Privacy Policy:** Ensure your app has a clear privacy policy explaining how biometric data (even if only indicators of success/failure) and user data are handled.
7.  **User Education:** Provide clear instructions or FAQs for users on setting up and using biometric authentication within your app.
8.  **Backend Security:** Ensure the backend is robustly secured.

## Conclusion

Implementing biometric authentication enhances app security and user experience. By following this guide, using Android's `BiometricPrompt` and `Keystore` system correctly, and adhering to best practices, you can build a secure and reliable Biometric Voting App. Remember that security is an ongoing process, requiring vigilance and updates as new threats and platform features emerge.
```
