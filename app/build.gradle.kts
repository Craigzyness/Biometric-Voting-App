// app/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("kotlin-kapt") // Added kotlin-kapt for Hilt
    alias(libs.plugins.hilt) // Added Hilt plugin
    id("com.google.gms.google-services") version "4.4.1"
    id("com.google.firebase.crashlytics") version "2.9.9"
}

// Define keystore properties file
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.example.biometricvotingapp"
    compileSdk = 34 // Or latest

    // Signing configurations for APKs
    // IMPORTANT: For actual release builds, create a `keystore.properties` file in the
    // root project directory (alongside your root build.gradle.kts or settings.gradle.kts).
    // This file should NOT be committed to version control (add it to .gitignore).
    //
    // The `keystore.properties` file should contain the following properties:
    // storeFile=<path_to_your_keystore_file_relative_to_root_project_dir_e.g._app/release.keystore.jks>
    // storePassword=<your_keystore_password>
    // keyAlias=<your_key_alias>
    // keyPassword=<your_key_password>
    //
    // If `keystore.properties` is not found, or properties are missing,
    // placeholder values will be used below. These placeholders WILL NOT create a valid
    // runnable release build and are only for allowing Gradle sync to pass.
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile") ?: "placeholder.keystore")
            storePassword = keystoreProperties.getProperty("storePassword") ?: "placeholder_password"
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "placeholder_alias"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: "placeholder_key_password"

            // It's good practice to validate if properties were actually loaded if you want to fail early
            // For example, you could add checks here:
            if (storeFile.name == "placeholder.keystore" && keystorePropertiesFile.exists()) {
                println("Warning: 'storeFile' not found in keystore.properties. Using placeholder.")
            }
            // Add similar checks for other properties if desired.
        }
    }

    defaultConfig {
        applicationId = "com.example.biometricvotingapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"

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
            // Assign the 'release' signing configuration to the release build type.
            signingConfig = signingConfigs.getByName("release")
            // TODO: Replace with your actual production API base URL
            buildConfigField("String", "API_BASE_URL", "\"https://your.production.api/api/v1/\"")
            // Coverage usually not enabled for release builds
            // enableUnitTestCoverage = false
            // enableAndroidTestCoverage = false
        }
        debug {
            isMinifyEnabled = false
            // Debug builds are typically signed with a default debug keystore automatically.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/v1/\"")
            // Enable code coverage for debug builds
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.11" // Use a recent stable JaCoCo version
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
        buildConfig = true // Ensure BuildConfig is generated (usually true by default for app modules)
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
    // Required for Hilt to work with Kotlin 1.9.0+ and KSP (if used, not used here yet)
    // or for kapt
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(project(":core")) // Added :core module dependency

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

    // Hilt Dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Firebase - Bill of Materials (BOM)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // Firebase Analytics (often recommended with Crashlytics)
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Play Integrity API
    implementation(libs.play.integrity)

    // Network
    // TODO: After updating libraries, ensure app compiles, tests pass, and perform manual testing.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Gson converter
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Updated OkHttp
    implementation("com.google.code.gson:gson:2.10.1") // Updated Gson library

    // Testing
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8") // Using version from libs.versions.toml (mockk = "1.13.8")
    testImplementation(libs.mockk.agent) // For final class mocking if needed
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    // mockk-android is for androidTest, not testImplementation here.
    // If we need specific android mocking utilities for unit tests (e.g. with Robolectric),
    // they might come from mockk-android, but standard mocking is io.mockk:mockk.
    // For now, this setup should be fine for the planned tests.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
