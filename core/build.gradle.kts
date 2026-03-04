plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.biometricvotingapp.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Libraries typically don't (and shouldn't) minify themselves
                                 // Minification is handled by the app module that consumes them.
            // proguardFiles are for library's own shrinking if isMinifyEnabled were true.
            // Consumer proguard rules are more important for libraries.
        }
        debug {
            // No specific debug settings needed for now
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Assuming libs.kotlin.stdlib is defined in libs.versions.toml
    // If not, it would be: implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("stdlib"))
    implementation(libs.androidx.core.ktx)

    // Dependencies for specific classes being moved to :core:
    // For AnonymizedIdGenerator, SecureSaltProvider, StableIdentifierProvider (which use EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto) // Alias from libs.versions.toml
    // For BiometricAuthManager
    implementation(libs.androidx.biometric)      // Alias from libs.versions.toml (points to biometric-ktx)
    // For PlayIntegrityService
    implementation(libs.play.integrity)         // Alias from libs.versions.toml

    // Test dependencies (for core/src/test/java)
    testImplementation(libs.junit)
    // Add other test dependencies like mockk, truth if tests are moved/created here for :core.

    // Hilt Dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
