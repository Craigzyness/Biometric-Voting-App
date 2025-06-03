# Android App Deployment Guide - Biometric Voting App

## 1. Introduction

This guide provides instructions for building and preparing the Android Biometric Voting App for release. It covers build configurations, signing, pre-release checks, and distribution considerations.

## 2. Prerequisites for Building

*   **Android Studio:** The latest stable version is highly recommended for building and managing the project. (e.g., Android Studio Giraffe | 2022.3.1 or newer).
*   **Android SDK Tools:** Ensure you have the necessary Android SDK platforms and build tools installed via Android Studio's SDK Manager. The project currently targets `compileSdk = 34`.
*   **Compatible JDK:** Android Studio usually bundles a compatible JDK. If building from the command line, ensure your JDK version is compatible with the Android Gradle Plugin version used (typically JDK 17 for recent AGP versions).

## 3. Build Configurations (`app/build.gradle.kts`)

The `app/build.gradle.kts` file defines how the application is built. Key aspects include build types and signing configurations.

### Build Types

The app defines two primary build types:

*   **`debug`:**
    *   Used for development and testing.
    *   Typically enables debug symbols and may include debug-specific code or configurations.
    *   `isMinifyEnabled` is usually `false`.
    *   Signed with a generic debug keystore automatically by Android Studio.
*   **`release`:**
    *   Used for generating production-ready APKs (Android Package Kits) or AABs (Android App Bundles).
    *   **Crucially, `isMinifyEnabled` is set to `true`**. This enables R8 for code shrinking (removing unused code) and obfuscation (renaming classes, methods, and fields to make reverse engineering more difficult).
    *   Uses Proguard rules defined in `app/proguard-rules.pro` to guide the shrinking and obfuscation process.
    *   Must be signed with a secure, private release key.

### Signing Configurations

To distribute your app, especially on the Google Play Store, the release build must be digitally signed with your own private key. The `app/build.gradle.kts` file includes a placeholder signing configuration:

```kotlin
// In app/build.gradle.kts
signingConfigs {
    create("releasePlaceholder") {
        storeFile = file("placeholder.keystore")
        storePassword = "placeholder_password"
        keyAlias = "placeholder_alias"
        keyPassword = "placeholder_key_password"
    }
}
// ...
buildTypes {
    release {
        // ...
        signingConfig = signingConfigs.getByName("releasePlaceholder")
    }
}
```

**Steps to Create and Use a Proper Release Signing Configuration:**

1.  **Generate a Java Keystore (JKS) file:**
    *   Use the `keytool` utility (found in the JDK's `bin` directory).
    *   Example command:
        ```bash
        keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
        ```
    *   You will be prompted for passwords for the keystore and the key, and for distinguished name information (Common Name, Organization, etc.).

2.  **Securely Store the Keystore File and Credentials:**
    *   **VERY IMPORTANT:** Keep your `.jks` file private and secure. Back it up in a safe place. Losing it will prevent you from publishing updates to your app under the same identity.
    *   Keep your keystore password, key alias, and key password confidential.

3.  **Create `keystore.properties`:**
    *   In the `app/` directory of your project, create a file named `keystore.properties`.
    *   **Add this file to your project's `.gitignore` file** to prevent it from being committed to version control.
        ```
        # .gitignore
        keystore.properties
        *.jks
        ```

4.  **Add Credentials to `keystore.properties`:**
    The file should contain the following properties:
    ```properties
    storeFile=my-release-key.jks # Or the relative/absolute path to your keystore file
    storePassword=YOUR_STORE_PASSWORD
    keyAlias=my-key-alias
    keyPassword=YOUR_KEY_PASSWORD
    ```
    Replace placeholders with your actual credentials and keystore file name/path.

5.  **Update `app/build.gradle.kts` to Use `keystore.properties`:**
    Modify the `signingConfigs` block in `app/build.gradle.kts` to load these properties:

    ```kotlin
    import java.util.Properties
    import java.io.FileInputStream

    // ... other parts of the file ...

    val keystorePropertiesFile = rootProject.file("app/keystore.properties") // Path relative to rootProject
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    android {
        // ...
        signingConfigs {
            create("release") { // Changed from releasePlaceholder to release
                storeFile = file(keystoreProperties.getProperty("storeFile", "placeholder.keystore"))
                storePassword = keystoreProperties.getProperty("storePassword", "placeholder_password")
                keyAlias = keystoreProperties.getProperty("keyAlias", "placeholder_alias")
                keyPassword = keystoreProperties.getProperty("keyPassword", "placeholder_key_password")
            }
        }
        buildTypes {
            release {
                // ...
                signingConfig = signingConfigs.getByName("release") // Use the new 'release' config
            }
        }
        // ...
    }
    ```
    This setup attempts to load credentials from `keystore.properties`. If the file or properties are not found, it falls back to placeholder values (which will likely cause the build to fail for release signing, reminding you to set them up).

### Product Flavors
Product flavors are not currently used in this project but can be configured in `app/build.gradle.kts` if different versions of the app (e.g., free/paid, different branding) are needed from the same codebase.

## 4. Building for Release

Once your signing configuration is properly set up:

### Generating a Signed APK
*   **Using Gradle command:**
    ```bash
    ./gradlew :app:assembleRelease
    ```
    The signed APK will be located in `app/build/outputs/apk/release/`.
*   **Using Android Studio:**
    1.  Go to "Build" > "Generate Signed Bundle / APK..."
    2.  Select "APK", then "Next".
    3.  Choose your key store path, enter passwords, and select your key alias.
    4.  Select "release" as the build variant.
    5.  Choose APK destination and click "Finish".

### Generating an Android App Bundle (AAB)
The AAB format is Google Play's recommended publishing format, as it allows for optimized APKs to be generated and served by Google Play for specific device configurations.
*   **Using Gradle command:**
    ```bash
    ./gradlew :app:bundleRelease
    ```
    The signed AAB will be located in `app/build/outputs/bundle/release/`.
*   **Using Android Studio:**
    1.  Go to "Build" > "Generate Signed Bundle / APK..."
    2.  Select "Android App Bundle", then "Next".
    3.  Choose your key store path, enter passwords, and select your key alias.
    4.  Select "release" as the build variant.
    5.  Choose AAB destination and click "Finish".

## 5. Pre-Release Checklist & Security

Before publishing your application, perform these critical checks:

*   **Code Obfuscation (Proguard/R8):**
    *   Confirm `isMinifyEnabled = true` is set for the `release` build type in `app/build.gradle.kts`.
    *   Review and ensure `app/proguard-rules.pro` is comprehensive enough to prevent crashes due to over-aggressive code removal (e.g., for classes used via reflection, DTOs for GSON/Retrofit) while maximizing obfuscation.
    *   **Thoroughly test the generated release build** (APK or AAB installed on a device) to ensure all functionality works as expected.

*   **Disable Debugging:**
    *   The `android:debuggable` attribute in `AndroidManifest.xml` is typically handled by the build system (set to `false` for release builds). Verify this is the case.
    *   Remove or guard all development logging calls (e.g., `Log.d`, `Log.i`). Using `if (BuildConfig.DEBUG) { Log.d(...) }` is a common practice, and R8 will remove these blocks in release builds. Timber library, if configured with a release tree that does no logging, also handles this.

*   **Network Security:**
    *   **Backend MUST use HTTPS.**
    *   In `app/src/main/AndroidManifest.xml`, the `android:usesCleartextTraffic="true"` attribute is present for development convenience (e.g., for HTTP communication with `10.0.2.2`).
    *   **For release builds, this MUST be changed to `android:usesCleartextTraffic="false"`.**
    *   If specific domains require cleartext (not recommended for production backends), use a Network Security Configuration file to allow cleartext only for those specific domains. For this app, the production backend should always be HTTPS.

*   **API Keys & Secrets:**
    *   Confirm that no API keys, credentials, or other secrets are hardcoded in the application's code or resource files. Manage these via secure build configurations or by fetching them from a secure server if necessary (though client-side secrets are generally discouraged).

*   **Permissions:**
    *   Review permissions in `AndroidManifest.xml` (`USE_BIOMETRIC`, `INTERNET`) to ensure they are still the minimum necessary for the app's functionality.

*   **App Versioning:**
    *   Before each release, update `versionCode` (increment by at least 1) and `versionName` (e.g., "1.0.1") in `app/build.gradle.kts`. This is crucial for app updates on Google Play.

## 6. Distribution

*   **Google Play Store:**
    *   This is the primary distribution platform for most Android apps.
    *   Requires a Google Play Developer account.
    *   Publishing in AAB format is highly recommended.
    *   You will need to prepare store listing assets (description, screenshots, feature graphic, etc.).
    *   Familiarize yourself with Google Play Console and its policies.

*   **Other Platforms/Methods:**
    *   **Direct APK Distribution:** You can share the signed APK directly for manual installation, often used for enterprise apps or testing.
    *   **Other App Stores:** Platforms like Amazon Appstore, Samsung Galaxy Store have their own submission processes.

Thorough testing of the release build on various devices and Android versions is essential before distributing your application to users.
