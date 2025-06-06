# MVP Security & Anonymity Review Notes (Thought Experiments)

Date: (Refer to Git history for the last update date of this document)

This document summarizes key discussion points and potential areas for future hardening based on a conceptual review of the Biometric Voting App MVP's security and anonymity design. The current MVP relies heavily on on-device security and placeholder backend components.

## Key Discussion Points & Scenarios:

1.  **Compromise of Android Keystore / MasterKey:**
    *   **Scenario:** A severe OS vulnerability allows extraction of MasterKeys.
    *   **Potential Impact:** Decryption of `EncryptedSharedPreferences` containing salt and stable app UUID, allowing re-generation of the anonymized ID.
    *   **Assessment:** Relies on Android OS security. The app uses recommended best practices (`MasterKey`). The anonymized ID itself still lacks direct PII. This is a systemic risk.

2.  **Anonymized ID Correlation (with a Future Real Backend):**
    *   **Scenario:** A real backend logs the anonymized ID with extensive metadata (IP, device info).
    *   **Potential Impact:** Pattern analysis could lead to de-anonymization risks.
    *   **Mitigation (Future):** Strict data minimization on the backend. Avoid logging excessive metadata alongside anonymized IDs or votes.

3.  **Stability & Uniqueness of "Stable App UUID":**
    *   **Scenario:** App data clear, or complex backup/restore scenarios.
    *   **Potential Impact:** Loss of existing salt/stable UUID leading to a new anonymized ID upon re-registration. User might lose their "voting identity" for the app.
    *   **Assessment:** Current UUID approach (generated once, stored in EncryptedPrefs) is good for app-install uniqueness. Data clear leading to a new ID is generally expected. True ID stability across device transfers without compromising privacy is very complex and beyond MVP.

4.  **Biometric Sensor/OS Vulnerabilities:**
    *   **Scenario:** Theoretical attacks against fingerprint hardware or OS biometric stack.
    *   **Potential Impact:** Authentication bypass.
    *   **Assessment:** App relies on the security of the underlying platform and `BiometricPrompt` API. User OS updates are crucial.

5.  **Placeholder Backend Security (`server.js`):**
    *   **Scenario:** If the current MVP backend were deployed.
    *   **Potential Impact:** Highly vulnerable (no HTTPS, no rate limiting, no robust input sanitization, data loss on restart).
    *   **Assessment:** Acceptable *only* as a local, non-deployed placeholder for app development. A production backend needs full security hardening.

6.  **Vote Privacy on (Simulated) Blockchain:**
    *   **Scenario:** The `selectedOption` is logged directly in the simulated blockchain record.
    *   **Potential Impact:** If the anonymized ID were deanonymized and the log public, vote choice is revealed.
    *   **Mitigation (Future):** Consider hashing/encrypting vote content on a real blockchain or using advanced privacy-preserving techniques (complex). `docs/backend_logic.md` notes this. Current MVP simulation (private console log) is fine.

7.  **Cryptographic Implementation Risks (Human Error):**
    *   **Scenario:** Bugs in salt/UUID generation, storage, or hashing logic.
    *   **Potential Impact:** Weakened security or non-unique IDs.
    *   **Mitigation:** Use of standard libraries is good. **Crucially, all cryptographic code requires thorough review by human security experts before any production consideration.**

## Overall MVP Security Posture:

For its defined scope (on-device focus, placeholder backend), the MVP's conceptual security design is strong. It correctly isolates raw biometrics and relies on OS-level security for key storage.

## Key Recommendations for Future Phases:

*   **Mandatory Expert Security Review:** Before moving towards any pilot or production, a full security audit by external experts is essential, especially for `AnonymizedIdGenerator`, `SecureSaltProvider`, `StableIdentifierProvider`, and any cryptographic elements.
*   **Secure Backend Development:** When building the real backend, apply all standard security best practices (HTTPS, input validation, ORM/parameterized queries, rate limiting, secure authentication for admin functions, etc.).
*   **Blockchain Design for Anonymity:** If/when moving to a real blockchain, carefully design the smart contracts and data stored on-chain to preserve voter anonymity as much as possible, balancing with auditability needs.

This review is conceptual and based on the code and documentation. Actual on-device testing and penetration testing are vital next steps for security assurance.
