# Troubleshooting Guide

This guide provides solutions and diagnostic steps for common issues encountered with the Biometric Voting App.

## Backend Issues

### 1. Server Fails to Start
*   **Symptom:** `npm start` command exits with an error or the server is not reachable on the configured port.
*   **Potential Causes & Solutions:**
    *   **Port Conflict:**
        *   **Check:** Is another application using the configured `PORT` (default 3000)? Use tools like `netstat -tulnp | grep <PORT>` (Linux/macOS) or Resource Monitor (Windows) to check port usage.
        *   **Solution:** Stop the other application or change the `PORT` environment variable for this app.
    *   **Database Connection Failure:**
        *   **Check:** Review server logs (Winston logs, console output) for errors like "Failed to connect to PostgreSQL," "password authentication failed for user ...," "database ... does not exist," or connection refused errors.
        *   **Solutions:**
            *   Ensure your PostgreSQL server is running and accessible from the backend server (check network, firewalls).
            *   Verify that the `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, and `DB_NAME` environment variables are correctly set and match your PostgreSQL instance's configuration.
            *   Confirm that the database user (`DB_USER`) has the necessary permissions to connect to the specified database (`DB_NAME`).
    *   **Missing Dependencies:**
        *   **Check:** Was `npm install` (or `yarn install`) run successfully in the `backend/` directory? Look for a `node_modules` directory.
        *   **Solution:** Navigate to the `backend/` directory and run `npm install` or `yarn install`.
    *   **Syntax Errors or Other Code Issues:**
        *   **Check:** Review server logs for specific error messages and stack traces that point to lines of code in `server.js` or other modules.
        *   **Solution:** Address the reported code issues.
    *   **Node.js Version:**
        *   **Check:** Ensure you are using a compatible Node.js version (e.g., 14.x or later as recommended).
        *   **Solution:** Use a Node Version Manager (like `nvm`) to switch to a compatible version.

### 2. API Endpoints Returning Errors
*   **Symptom:** API requests (e.g., from Postman, curl, or the Android app) return 4xx or 5xx HTTP status codes.
*   **Diagnostic Steps:**
    *   **Check Server Logs:** The primary source for diagnosing backend issues. Winston logs (now configured) will provide structured information (JSON in production/non-dev, formatted text in dev) including timestamps, error messages, stack traces, and request context for errors. Morgan logs will show incoming requests.
    *   **Verify Request Format:** Ensure the client is sending requests with the correct HTTP method, headers (`Content-Type: application/json` for POST/PUT), and JSON body structure. Refer to `docs/backend_design/03_api_specifications_v2.md`.
    *   **Input Validation Errors (400 Bad Request):** If receiving 400 errors, the response body `{"error": "message"}` should indicate which field(s) failed validation and why. Review the validation rules in `server.js` for the specific endpoint.
    *   **Rate Limiting Errors (429 Too Many Requests):** If you receive this status code, your IP address has exceeded the request limit for an endpoint. The response body will indicate this. Wait for the specified time (e.g., 10 or 15 minutes) before retrying.
    *   **Authentication/Authorization Errors (401 Unauthorized, 403 Forbidden):**
        *   For `/submitVote`, a 403 can mean the `anonymizedVoterId` is not registered or the voter is marked ineligible.
    *   **Not Found Errors (404 Not Found):** Ensure the endpoint path is correct and the requested resource (e.g., a specific election ID) exists.
    *   **Conflict Errors (409 Conflict):** For `/register`, this means the `anonymizedVoterId` already exists. For `/submitVote`, it means the voter has already voted in that election.
    *   **Database Issues (often resulting in 500 Internal Server Error):** Server logs might show database constraint violations (e.g., unique ID conflict, foreign key violation) or other PostgreSQL errors. The Winston logs should capture `err.code` and `err.detail` from database errors.

### 3. Incorrect `hasVoted` Status in `GET /api/v1/elections`
*   **Symptom:** The `hasVoted` field in the `/elections` response is not accurate for a given user.
*   **Potential Causes & Solutions:**
    *   **Client-Side:** Ensure the Android app is correctly passing the `anonymizedVoterId` as a query parameter.
    *   **Backend Logic:**
        *   Verify the `anonymizedVoterId` (after being converted to lowercase by the server) exists in the `Voters` table.
        *   Check the `Votes` table to confirm if a record exists linking the voter's internal ID (`Voters.id`) and the specific `election_id`.
        *   Ensure the `anonymizedVoterId` used in the query exactly matches the one stored (case-insensitivity is now handled by server-side lowercase conversion).

## Android App Issues

### 1. App Fails to Build
*   **Symptom:** Gradle build fails in Android Studio or via the command line (`./gradlew build`).
*   **Potential Causes & Solutions:**
    *   **JDK/SDK Issues:**
        *   **Check:** Ensure your Android Studio is configured with a compatible JDK (usually bundled). Verify the Android SDK specified by `compileSdk` (e.g., 34) and build tools are installed via the SDK Manager.
        *   **Solution:** Install/update necessary SDK components. Invalidate caches and restart Android Studio.
    *   **Missing Dependencies:**
        *   **Check:** Review the build output for errors related to dependency resolution (e.g., "Could not find dependency...").
        *   **Solution:** Ensure you have an active internet connection. Click "Sync Project with Gradle Files" in Android Studio. Try cleaning the project (`Build > Clean Project`). Delete `.gradle` directories (project root and user home) as a last resort.
    *   **XML Errors:**
        *   **Check:** Build output will usually point to errors in layout files (`res/layout/`), drawable resources, or `AndroidManifest.xml`.
        *   **Solution:** Correct the XML syntax errors.
    *   **Signing Configuration for Release Builds:**
        *   **Check:** If building a `release` variant and signing fails, errors will relate to keystore access.
        *   **Solution:** Ensure `keystore.properties` is present in the `app/` directory, correctly configured with paths and passwords, and that the keystore file itself is valid and accessible. Refer to `docs/android_app_deployment_guide.md`.
    *   **Kotlin/AGP Version Mismatch:**
        *   **Check:** Ensure the Kotlin Gradle Plugin version is compatible with the Android Gradle Plugin (AGP) version.
        *   **Solution:** Align versions as per Android development recommendations.

### 2. App Crashes on Startup or Specific Screens
*   **Symptom:** Application closes unexpectedly ("Unfortunately, [App Name] has stopped.").
*   **Diagnostic Steps:**
    *   **Logcat:** This is your primary tool. Open Android Studio's Logcat window (View > Tool Windows > Logcat). Filter by your application's package name (e.g., `com.example.biometricvotingapp`) and set the log level to "Error" or "Warn" to find exceptions. Look for stack traces.
    *   **Common Causes:**
        *   `NullPointerException`: Accessing a null object. Check ViewModel initializations, data from intents/arguments, or nullable types.
        *   `ClassCastException`: Trying to cast an object to an incompatible type.
        *   ViewModel Initialization Errors: Issues in ViewModel factories or dependency injection.
        *   Network Errors Not Handled Gracefully: An unhandled exception during an API call (though `VotingRepository` uses `Result` type which should mitigate this if handled correctly in ViewModels).
        *   Resource Not Found: e.g., trying to access a drawable or string that doesn't exist.
        *   Issues with `BiometricPrompt` or `SecurityUtil` initialization or usage.

### 3. Unable to Connect to Backend API
*   **Symptom:** Election list is empty with an error, registration, login, or voting fails with network-related error messages displayed in the app.
*   **Potential Causes & Solutions:**
    *   **Backend Server Not Running:** Ensure the backend server (`server.js`) is running and listening on the correct port.
    *   **Incorrect Base URL in `ApiService.kt`:**
        *   **Emulator:** The default `http://10.0.2.2:3000/api/v1/` should work for connecting to a backend running on `localhost:3000` on your development machine.
        *   **Physical Device (Development):** If testing on a physical device, the device must be on the same Wi-Fi network as your development machine. The IP address in `ApiService.BASE_URL` must be your development machine's local network IP (e.g., `http://192.168.1.100:3000/api/v1/`), not `localhost` or `10.0.2.2`.
    *   **Network Security Configuration (Especially for Release/HTTPS):**
        *   **Release Builds:** Release builds enforce HTTPS due to `network_security_config.xml` setting `cleartextTrafficPermitted="false"`. If your backend is not HTTPS or the SSL certificate is invalid/untrusted, connections will fail.
        *   **Debug Builds:** Debug builds allow cleartext to `10.0.2.2` (and potentially other domains if `cleartextTrafficPermitted="true"` is broadly set in debug overrides). If connecting to a different HTTP address, ensure your debug NSC allows it or temporarily modify it.
    *   **Internet Permission:** The `android.permission.INTERNET` permission is correctly included in `AndroidManifest.xml`.
    *   **Device Network Connectivity:** Ensure the Android device or emulator has network access.
    *   **Firewall:** A firewall on the machine running the backend might be blocking incoming connections to the configured port.

### 4. Biometric Authentication Issues
*   **Symptom:** Biometric prompt doesn't appear, authentication consistently fails, or errors are shown related to biometrics.
*   **Potential Causes & Solutions:**
    *   **No Biometrics Enrolled on Device:** The user must enroll at least one fingerprint (or other supported biometric) in the Android device's security settings. The app provides feedback for this state via `BiometricAvailabilityStatus.NONE_ENROLLED`.
    *   **Biometric Hardware Unavailable/Error:** The device may not have biometric hardware, or it might be malfunctioning. Check Logcat for specific error codes from `BiometricAuthManager` and `BiometricPrompt`.
    *   **Incorrect `CryptoObject` Usage (for Vote Proof):**
        *   If vote confirmation (which uses `CryptoObject`) fails, there might be an issue with Keystore key generation/retrieval in `SecurityUtil.kt` (e.g., key invalidated after new biometric enrollment).
        *   Ensure `SecurityUtil.getCryptoObjectForEncryption()` returns a valid object and that the same key is available for any corresponding decryption if that were needed (though for vote proof, it's mainly encryption).
    *   **Activity Context:** `BiometricPrompt` requires a `FragmentActivity` context. Ensure it's correctly passed from the Composable screen to `BiometricAuthManager`.
    *   **Permissions:** The `USE_BIOMETRIC` permission is correctly requested in the manifest.

### 5. Data Not Displaying Correctly
*   **Symptom:** Election list shows unexpected data, `hasVoted` status is incorrect, options are missing.
*   **Diagnostic Steps:**
    *   **Logcat:** Check for errors during API calls in `VotingRepository` or data parsing/mapping issues in ViewModels.
    *   **Verify Backend Response:** Use a tool like Postman or `curl` to directly query the backend API endpoints (e.g., `GET /api/v1/elections`) and inspect the raw JSON response. Compare this with what the app receives and expects.
    *   **DTO and Domain Model Mapping:** Ensure `ElectionDto.kt` and other DTOs accurately reflect the JSON structure from the backend. Double-check the mapping logic in ViewModels (e.g., in `ElectionListViewModel.mapDtoToDomain`) for correctness, especially for new fields like `hasVoted`.
    *   **ViewModel State Updates:** Use Android Studio's debugger or log statements to verify that ViewModels are receiving data correctly and updating their `UiState` appropriately.

## General Advice
*   **Check Logs First:** Always start by checking application logs:
    *   **Backend:** Winston logs in the console (or configured files in production).
    *   **Android:** Logcat in Android Studio, filtering by your app's package name and appropriate log levels (Error, Warn, Debug).
*   **Reproduce Consistently:** Try to find reliable steps to reproduce the issue. This makes debugging much easier.
*   **Simplify the Problem:** If possible, isolate the component or code section causing the issue. Comment out parts of the code to narrow down the problem area.
*   **Consult Documentation:** Refer to relevant project documents (`README.md`, API specifications, design documents, and this troubleshooting guide).
*   **Clean Builds:** Occasionally, performing a "Clean Project" and "Rebuild Project" in Android Studio, or deleting `build` and `.gradle` directories, can resolve caching-related build issues.
```
