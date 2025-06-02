# User Interface (UI) Descriptions & User Experience (UX) Flow

This document provides textual descriptions of the key UI screens and outlines the UX flow for the Android Biometric Voting App MVP.

## I. User Interface (UI) Screen Descriptions

### 1. Welcome / Registration Screen
*   **Purpose:** First screen for new users, prompting registration.
*   **Key UI Elements:**
    *   **App Title/Logo:** Prominently displayed (e.g., "Biometric Voting App").
    *   **Brief Introduction:** Short text explaining the app's purpose (secure, anonymous voting).
    *   **Explanatory Text (Security & Anonymity):** A clear, concise message emphasizing that fingerprint data is processed on-device for an anonymized ID and raw data is never stored or transmitted.
    *   **"Register with Fingerprint" Button:** A large, clear button to initiate the registration process.
    *   **"Login" Link/Button:** For users who are already registered.
    *   **(Optional) Status Indicator:** To provide feedback during the fingerprint scanning process.

### 2. Login Screen
*   **Purpose:** For registered users to access the app.
*   **Key UI Elements:**
    *   **App Title/Logo.**
    *   **Login Instruction:** Simple text like "Please authenticate with your fingerprint to continue."
    *   **"Login with Fingerprint" Button:** A prominent button that, when tapped, initiates the BiometricPrompt for fingerprint scanning.
    *   **(Optional) Link to Registration:** For users who landed here by mistake and need to register.

### 3. Election List Screen
*   **Purpose:** Display available elections to a logged-in user.
*   **Key UI Elements:**
    *   **Screen Title:** (e.g., "Available Elections").
    *   **List of Elections:**
        *   Each item in the list should clearly display the election title or a unique identifier.
        *   May include a short description or election period (for MVP, title is sufficient).
        *   Each list item should be tappable to navigate to the voting screen for that election.
    *   **(Optional) Refresh Button:** To update the list of elections (more relevant post-MVP).
    *   **(Optional) Logout Button:** To allow the user to log out.

### 4. Voting Screen
*   **Purpose:** Allow the user to view details of a selected election and cast their vote.
*   **Key UI Elements:**
    *   **Screen Title:** (e.g., "Cast Your Vote" or the Election Title itself).
    *   **Election Question/Details:** Clearly displays the question or proposal being voted on.
    *   **List of Voting Options:**
        *   Presented as radio buttons or a selectable list to ensure only one option can be chosen.
        *   Each option should have clear, concise text.
    *   **"Submit Vote" Button:** Becomes active once an option is selected. Tapping this will trigger the fingerprint confirmation prompt.
    *   **"Cancel/Back" Button:** To return to the Election List screen without voting.

### 5. Vote Confirmation Screen/Dialog
*   **Purpose:** Inform the user about the status of their vote submission.
*   **Description:** After a user confirms their vote with biometrics on the **Voting Screen**, a status message (success or failure) is displayed directly on the Voting Screen. If the vote submission to the backend is successful, the application automatically navigates the user back to the **Election List Screen**. There isn't a separate, manually dismissed confirmation screen/dialog; the feedback is integrated into the Voting Screen, followed by navigation.

## II. User Experience (UX) Flow

The following outlines the typical user journey through the app:

1.  **App Launch:**
    *   User opens the Biometric Voting App.
    *   **If New User (or no existing registration found):** User is presented with the **Welcome / Registration Screen**.
    *   **If Registered User (app determines this locally):** User might be taken directly to the **Login Screen**.

2.  **New User Registration Flow:**
    *   From the **Welcome / Registration Screen**, user taps "Register with Fingerprint."
    *   Android BiometricPrompt appears, requesting fingerprint scan.
    *   **On Success:**
        *   Anonymized ID is generated and stored locally on the device.
        *   User sees a success message (briefly) or is navigated to the **Login Screen** or directly into the app (e.g., **Election List Screen**) with a "Registration Successful" indication.
    *   **On Failure/Cancel:**
        *   User sees an error message on the BiometricPrompt or Registration Screen.
        *   User can retry scanning or cancel registration.

3.  **Registered User Login Flow:**
    *   User is on the **Login Screen**.
    *   User taps "Login with Fingerprint."
    *   Android BiometricPrompt appears.
    *   **On Success:** User is navigated to the **Election List Screen**.
    *   **On Failure/Cancel:** User sees an error message and can retry.

4.  **Voting Flow:**
    *   User is on the **Election List Screen**.
    *   User scrolls and taps on a specific election they wish to vote in.
    *   User is navigated to the **Voting Screen** for the selected election.
    *   On the **Voting Screen**:
        *   User reads the election question/details.
        *   User selects one of the available voting options.
        *   User taps the "Submit Vote" button.
    *   Android BiometricPrompt appears, requesting fingerprint confirmation for the vote.
    *   **On Success (Fingerprint Confirmed):**
        *   The vote is processed (for MVP, this means locally validated and prepared for simulated backend submission).
        *   User is shown the **Vote Confirmation Screen/Dialog** with a success message.
        *   User taps "OK" or "Back to Elections" and is navigated back to the **Election List Screen** (which might now visually indicate that the user has voted in that specific election, e.g., by graying it out or adding a checkmark – optional for MVP).
    *   **On Failure/Cancel (Fingerprint Not Confirmed):**
        *   Vote is not submitted.
        *   User sees an error message.
        *   User remains on the **Voting Screen** and can retry fingerprint confirmation or change their vote option.

5.  **(Optional) Logout Flow:**
    *   From a screen with a logout button (e.g., Election List Screen), user taps "Logout."
    *   User session is cleared locally.
    *   User is navigated to the **Login Screen** or **Welcome/Registration Screen**.
```
