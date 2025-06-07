# Biometric Integration Flow (Conceptual)

## 1. Introduction
This document outlines the conceptual flow for integrating biometrics into the User Registration and Authentication processes of the Biometric Voting App. It leverages the recommended approach of using on-device biometric capabilities via a library like `react-native-biometrics` to manage cryptographic keys.

## 2. Key Principles
- **No Raw Biometric Data Storage:** The application will never store or transmit raw biometric data.
- **On-Device Biometric Authentication:** Biometric checks (fingerprint, face) are performed locally on the user's device by the OS.
- **Cryptographic Verification:** The backend will verify biometric authentication by means of cryptographic signatures. A public/private key pair is generated on the device; the private key is protected by biometrics, and the public key is stored by the backend.
- **User Consent:** Users will be explicitly asked if they want to enable biometric authentication.

## 3. Biometric Setup Flow (Post-Initial Registration)

This flow occurs *after* a user has successfully registered with a username, email, and password.

1.  **Prompt to Enable Biometrics:**
    -   **Trigger:** After successful initial registration, or from a user profile/settings screen.
    -   **UI:** The app prompts the user: "Would you like to enable biometric authentication for quicker and secure login/actions?" (Yes/No options).

2.  **User Consents (Clicks "Yes"):**
    -   **Action:** The app calls the chosen React Native biometric library (e.g., `react-native-biometrics`).
    -   **Key Generation:** The library is instructed to:
        -   Check if biometrics are available and enrolled on the device. If not, guide the user to OS settings.
        -   Generate a new ECC (Elliptic Curve Cryptography) public/private key pair.
        -   The **private key** is stored securely in the device's Keychain/Keystore and is configured to require biometric authentication for any signing operations.
        -   The **public key** is returned to the React Native application.
    -   **Error Handling:** If key generation fails or biometrics are not available/enrolled, inform the user.

3.  **Send Public Key to Backend:**
    -   **Action:** The React Native app sends the generated **public key** to the backend, associated with the logged-in user.
    -   **API Endpoint (New):** `POST /users/me/biometric-keys` (or similar, needs to be protected by session/token authentication).
    -   **Backend Storage:** The backend stores this public key (e.g., in a new column `biometric_public_key` in the `Users` table) associated with the user's record.

4.  **Confirmation:**
    -   **UI:** The app informs the user: "Biometric authentication has been successfully set up!"

## 4. Biometric Authentication Flow (Example: Login or Confirming a Critical Action)

This flow describes how a user might log in or confirm an action using their enrolled biometrics.

1.  **Initiate Action Requiring Biometric Auth:**
    -   **Trigger:** User attempts to log in (after entering username, or app remembers username) or perform a sensitive action (e.g., confirm vote).
    -   **UI:** App prompts: "Authenticate with biometrics to continue."

2.  **Backend Challenge (If applicable for login/session-based auth):**
    -   *(For login)*: The app requests a unique, single-use challenge string from the backend.
    -   **API Endpoint (New):** `GET /auth/challenge?username=<username>`
    -   *(For other actions)*: The challenge might be the data to be signed itself (e.g., a hash of the vote details).

3.  **On-Device Biometric Prompt & Signing:**
    -   **Action:** The app uses the biometric library to:
        -   Prompt the user for biometric authentication (OS handles the UI).
        -   If successful, the library uses the stored **private key** (associated with the app and protected by biometrics) to sign the challenge received from the server (or the action-specific data).
    -   **Output:** The signed challenge (signature).
    -   **Error Handling:** If biometric authentication fails on the device (e.g., too many attempts, user cancels), the process stops.

4.  **Send Signed Challenge to Backend:**
    -   **Action:** The app sends the original challenge (or a reference to it) and the **signature** to the backend.
    -   **API Endpoint (New):** `POST /auth/biometric-login` or `POST /actions/confirm-biometric`
    -   **Payload:** `{ "username": "user1", "challenge": "challenge_string", "signature": "signed_challenge_string" }`

5.  **Backend Verification:**
    -   **Action:** The backend retrieves the user's stored **public key**.
    -   It uses the public key to verify the **signature** against the original challenge.
    -   **If verification is successful:**
        -   *(For login)*: Backend issues a session token/cookie, same as password login.
        -   *(For other actions)*: Backend marks the action as confirmed.
    -   **If verification fails:** Access is denied.

6.  **Confirmation/Access Granted:**
    -   **UI:** App logs the user in or confirms the action was successful.

## 5. Data to be Stored

-   **Frontend (Device Keychain/Keystore):**
    -   The **private key** generated for biometric operations. This is managed by the biometric library and the OS, protected by hardware security and biometric access. The app code cannot directly read this key.
-   **Backend (Database - `Users` table):**
    -   `biometric_public_key` (string): The public key corresponding to the private key stored on the user's device.
    -   `biometrics_enabled` (boolean, optional): Flag to indicate if the user has set up biometrics.

## 6. Security Considerations
-   **Challenge-Response:** Use unique, short-lived challenges for login to prevent replay attacks.
-   **Private Key Security:** The entire security of this system relies on the OS's secure storage and biometric protection of the private key.
-   **Endpoint Protection:** All backend endpoints must be protected by HTTPS. Authentication endpoints (like adding a public key) must require prior user session authentication (e.g., password login initially).
-   **User Education:** Inform users about what biometric authentication means in the context of the app.

This flow provides a robust way to integrate on-device biometrics for enhanced security and user experience without compromising user's raw biometric data.
