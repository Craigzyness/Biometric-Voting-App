# Data Handling & Security Requirements

This document outlines the critical security principles and data handling requirements for the Android Biometric Voting App. These are non-negotiable aspects to ensure user privacy, anonymity, and the integrity of the voting process.

## Core Security Principles:

1.  **No Raw Biometric Data Storage or Transmission:**
    *   **Absolute Prohibition:** Under no circumstances should raw fingerprint images, minutiae data, or any direct representation of the user's fingerprint be stored (persistently or temporarily) on the device's filesystem, in app memory accessible to other processes, or transmitted off the device.
    *   **Android BiometricPrompt API:** Leverage Android's `BiometricPrompt` API, which is designed to handle raw biometric data within a secure hardware-backed environment (e.g., Trusted Execution Environment - TEE) and does not expose the raw data to the application itself. The app receives only success/failure callbacks or, in some cases, a cryptographic object associated with the authentication.

2.  **On-Device Anonymized ID Generation:**
    *   **Purpose:** To create a unique, unlinkable identifier for voting without compromising the user's identity.
    *   **Process:**
        *   Immediately after a successful biometric authentication via `BiometricPrompt` (especially during registration), the app must facilitate the creation of an anonymized identifier.
        *   This identifier should be derived using strong cryptographic hashing algorithms (e.g., SHA-256 or SHA-512) applied to a piece of data that is unique to the user/device in the context of the app, potentially combined with a per-user salt stored securely or derived.
        *   Alternatively, if the `BiometricPrompt` is used to unlock a cryptographic key, that key can be used to encrypt a locally generated unique ID, or to sign data, effectively creating a form of anonymized credential.
    *   **Irreversibility:** The generated anonymized ID must be computationally infeasible to reverse-engineer back to the original biometric data or any personally identifiable information.
    *   **Storage:** This anonymized ID can be stored securely on the device (e.g., in EncryptedSharedPreferences using AndroidX Security library) to recognize the user in subsequent sessions.

3.  **Anonymized ID as Sole User Identifier for External Communication:**
    *   **Backend Interaction:** The generated anonymized ID is the *only* piece of user-specific data that should be sent to the backend server for registration, authentication (if the backend performs a secondary check), and vote submission.
    *   **Blockchain Interaction:** Similarly, if an identifier is recorded on the blockchain, it must be this anonymized ID, or a further derived version of it (e.g., a commitment hash).

4.  **Secure Communication (HTTPS/TLS):**
    *   **Encryption in Transit:** All communication between the Android application and any backend server(s) must be encrypted using HTTPS (HTTP over TLS).
    *   **Certificate Pinning (Recommended):** For enhanced security against man-in-the-middle attacks, consider implementing SSL/TLS certificate pinning, especially for sensitive operations.

5.  **Blockchain Data - Minimization and Anonymity:**
    *   **Information on Blockchain:** Carefully define what data is recorded on the blockchain. The goal is transparency of the vote count without compromising individual voter anonymity.
        *   **Vote Content:** The vote itself (e.g., "Option A for Election X"). This might be recorded directly or in an encrypted/hashed form depending on the desired level of ballot secrecy vs. public verifiability.
        *   **Anonymized Voter ID:** The voter's anonymized ID (or a derivative like a commitment to it) to ensure that each ID votes only once per election (prevent double voting).
        *   **Election ID:** A unique identifier for the election.
        *   **Timestamp (Optional but Recommended):** A secure timestamp for when the vote was accepted by the system.
    *   **No Linkable PII:** No personally identifiable information (PII) or data that could be easily correlated to deanonymize the voter should ever be written to the blockchain.
    *   **Aggregation (Consideration for Future):** For enhanced privacy, explore methods of vote aggregation or mixing before final blockchain recording, though this adds complexity beyond the MVP.

6.  **Secure Local Storage:**
    *   **Anonymized ID Storage:** As mentioned, the anonymized ID, if stored locally, must use Android's best practices for secure storage (EncryptedSharedPreferences).
    *   **Session Tokens:** If session tokens are used after login, they must also be stored securely.
    *   **No Sensitive Data in Logs:** Ensure that no sensitive data (anonymized IDs, parts of biometric process, etc.) is inadvertently written to application logs, especially in release builds.

7.  **Code Obfuscation and Anti-Tampering:**
    *   **Release Builds:** Use code obfuscation (e.g., R8/ProGuard) to make reverse engineering of the app more difficult.
    *   **Root Detection/SafetyNet (Consideration):** For enhanced security on the client-side, consider checks like Android's SafetyNet Attestation API or root detection mechanisms to ensure the app is running in a safe environment, although these are not foolproof.

## Regular Security Reviews:
*   The design and implementation of these security requirements should be reviewed regularly, especially before any significant deployment or update.
*   Consider seeking external security expertise if the app handles highly sensitive voting processes.
```
