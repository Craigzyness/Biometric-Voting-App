# Biometric SDK Options for React Native - Research Summary

## Introduction
This document outlines potential approaches and libraries for integrating biometric authentication (fingerprint, facial recognition) into a React Native application. The goal is to identify options suitable for the Biometric Voting App, considering ease of integration, platform support, security, and licensing.

## Native Platform Capabilities (iOS & Android)

Both iOS (Touch ID, Face ID) and Android (FingerprintManager/BiometricPrompt) provide native APIs for biometric authentication. React Native applications can leverage these native features.

**Pros:**
-   **Security:** Utilizes platform-level security, often storing biometric data in a secure enclave on the device. Raw biometric data is typically not exposed to the application.
-   **User Experience:** Consistent with the OS's native biometric prompts, providing a familiar UX.
-   **No Third-Party Server:** Authentication is primarily on-device.

**Cons:**
-   **Local Authentication Only:** Primarily verifies the user to the device. For server-side authentication, the app needs to securely communicate to the backend that the user was successfully authenticated locally (e.g., by signing a challenge with a key protected by biometrics).
-   **No Centralized Biometric Database:** Biometric data is managed by the device's OS. This is generally a good thing for privacy but means the app itself doesn't "enroll" biometrics in its own database directly from raw sensor data.

## React Native Libraries Leveraging Native Biometrics

Several React Native libraries provide a bridge to these native biometric functionalities.

### 1. `react-native-biometrics`
    - **Description:** A popular library that provides access to native biometric authentication on both iOS (Touch ID, Face ID) and Android (Fingerprint/BiometricPrompt).
    - **Features:**
        - Simple API for prompting biometric authentication.
        - Can be used to check for biometric hardware availability and enrollment status.
        - Can be used to create and retrieve cryptographic keys that are protected by biometric authentication. This is crucial for server-side verification:
            1. App generates a public/private key pair where the private key is stored in the device's secure keystore and requires biometric authentication for access.
            2. Public key is sent to the server during registration (or a "biometric setup" phase).
            3. For login/authentication, the server sends a challenge, the app signs the challenge using the private key (requiring biometric prompt), and sends the signed challenge back to the server for verification with the stored public key.
    - **Platform Support:** iOS, Android.
    - **Licensing:** MIT License.
    - **Considerations:** Well-maintained, good community support. Focuses on leveraging on-device biometric capabilities and keychain services.

### 2. `react-native-keychain`
    - **Description:** While not exclusively a biometrics library, `react-native-keychain` allows storing and retrieving credentials (like passwords, tokens, or even private keys) securely in the device's Keychain (iOS) or Keystore/SharedPreferences (Android). It can be configured to require biometric authentication for accessing these stored credentials.
    - **Features:**
        - Secure storage for sensitive data.
        - Option to require user presence (biometrics) for item access.
    - **Platform Support:** iOS, Android.
    - **Licensing:** MIT License.
    - **Considerations:** Can be used in conjunction with biometric prompts. For example, after a successful biometric scan (using a library like `react-native-biometrics` or a native module), you could retrieve a token from the keychain to authenticate with your backend. Or, store a private key in the keychain that requires biometrics to use.

### 3. `expo-local-authentication` (For Expo Managed Workflow)
    - **Description:** If using the Expo managed workflow, Expo provides its own `expo-local-authentication` module.
    - **Features:**
        - Provides access to Touch ID (iOS) and Fingerprint/Face/Iris authentication (Android).
        - Checks for hardware support and enrollment.
        - Prompts the user for biometric authentication.
    - **Platform Support:** iOS, Android (within Expo environment).
    - **Licensing:** MIT License.
    - **Considerations:** Best choice if already committed to the Expo managed workflow. Similar to `react-native-biometrics` in functionality for local authentication.

## Third-Party Biometric SDKs (Cloud-based or Advanced)

These are typically more comprehensive solutions from specialized vendors, often involving cloud components for identity verification, liveness detection, and management of biometric templates.

**Examples (General, may or may not have direct React Native SDKs):**
-   AWS Rekognition, Azure Cognitive Services (Face API), FaceTec, Onfido, Jumio, etc.

**Pros:**
-   **Advanced Features:** May offer superior liveness detection, anti-spoofing, and potentially more robust cross-device identity verification if templates are managed centrally (with user consent and strong security).
-   **Identity Verification:** Often used for KYC/AML processes, which might be overkill or relevant depending on the voting app's requirements for voter identity proofing.

**Cons:**
-   **Complexity & Cost:** Generally more complex to integrate and often involve commercial licensing fees.
-   **Data Privacy Concerns:** Storing biometric templates (even if encrypted) in a central database requires extreme security measures and careful consideration of privacy regulations. For a voting app aiming for anonymity, this might be counterproductive unless strictly for initial identity proofing decoupled from the vote itself.
-   **Internet Dependency:** May require internet connectivity for biometric operations.

## Recommendation for PoC

For the Proof of Concept (PoC) phase of the Biometric Voting App, focusing on leveraging **on-device native biometric capabilities** is the most straightforward and secure approach.

**Recommended Library:**
-   **`react-native-biometrics`**: It directly addresses the need to prompt for biometrics and, crucially, manage cryptographic keys protected by biometrics. This key management feature is essential for building a secure link between on-device biometric authentication and server-side verification without the app handling raw biometric data.

**Alternative if using Expo Managed Workflow:**
-   **`expo-local-authentication`**: Provides similar on-device authentication capabilities.

**Why these are preferred for PoC:**
-   **Security:** Leverages strong, hardware-backed security on the device.
-   **Privacy:** Raw biometric data does not leave the device and is not handled by the application code.
-   **Cost:** Open-source and free to use.
-   **Simplicity for PoC:** Allows demonstrating the core flow of biometric authentication.

The PoC should aim to:
1.  During/after initial username/password registration, prompt the user to set up biometric authentication.
2.  If they agree, use `react-native-biometrics` to generate a public/private key pair. The private key will be stored securely on the device and require biometrics for access.
3.  The public key will be sent to the app's backend and associated with the user's account.
4.  For subsequent biometric logins (or biometric confirmation for voting), the app would use the library to prompt for biometrics to access the private key, sign a challenge from the server, and the server would verify this signature.

This approach avoids storing any biometric templates on the server, relying instead on cryptographic verification tied to successful on-device biometric authentication.
