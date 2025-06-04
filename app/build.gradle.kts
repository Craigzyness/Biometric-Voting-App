// app/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.gms.google-services") version "4.4.1" // Apply directly with version
    id("com.google.firebase.crashlytics") version "2.9.9" // Apply directly with version
}

android {
    namespace = "com.example.biometricvotingapp"
    compileSdk = 34 // Or latest

    // Signing configurations for APKs
    // IMPORTANT: These are placeholder values. For actual release builds,
    // replace these with your real keystore details and ensure the keystore file
    // is properly secured. It's best practice to load these from a
    // separate, non-version-controlled file (e.g., keystore.properties)
    // or environment variables.
    signingConfigs {
        create("releasePlaceholder") {
            storeFile = file("placeholder.keystore") // Placeholder path, replace with actual keystore
            storePassword = "placeholder_password"   // Placeholder, replace with actual password
            keyAlias = "placeholder_alias"           // Placeholder, replace with actual alias
            keyPassword = "placeholder_key_password" // Placeholder, replace with actual key password
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
            // Assign the placeholder signing configuration to the release build type.
            // For actual releases, ensure this uses a properly configured signing config.
            signingConfig = signingConfigs.getByName("releasePlaceholder")
        }
        debug {
            isMinifyEnabled = false
            // Debug builds are typically signed with a default debug keystore automatically.
            // You could also assign a specific signing config here if needed, e.g.:
            // signingConfig = signingConfigs.getByName("debug") // if you create a 'debug' signingConfig
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

    // Firebase - Bill of Materials (BOM)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // Firebase Analytics (often recommended with Crashlytics)
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Gson converter
    implementation("com.squareup.okhttp3:okhttp:4.9.3") // OkHttp (use a version compatible with Retrofit 2.9.0)
    implementation("com.google.code.gson:gson:2.8.9") // Gson library itself

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
