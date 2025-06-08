# Biometric Voting App - Threat Modeling Guide

## 0. Scope and Objectives (This Document)

*   **Purpose:** This document serves as an introductory guide and framework to assist the Biometric Voting App project team in conducting a formal threat modeling exercise.
*   **Not a Threat Model Itself:** It is crucial to understand that this document does *not* constitute an exhaustive threat model for the application. Instead, it provides a starting point, suggests methodologies, and offers illustrative examples to facilitate the actual threat modeling process.
*   **Objectives of this Guide:**
    *   To introduce the core concepts and the importance of threat modeling in the context of the Biometric Voting App.
    *   To identify key architectural areas, components, and data flows of the application that should be the focus of the threat modeling analysis.
    *   To suggest a structured approach, such as STRIDE, for brainstorming and categorizing potential threats.
    *   To provide illustrative examples of threats and probing questions relevant to the application's specific domain (biometrics, voting, data anonymization, API security).
    *   To encourage and cultivate a proactive security-aware mindset within the development team throughout the project lifecycle.
*   **Goal of the Actual Exercise:** The ultimate goal for the project team, when performing the threat modeling exercise using this guide, is to systematically identify, analyze, and prioritize potential security threats and vulnerabilities. This process should directly inform the design and implementation of appropriate and effective mitigation strategies, thereby enhancing the overall security posture of the Biometric Voting App.

---

## 1. Introduction to Threat Modeling

### 1.1. What is Threat Modeling?
Threat modeling is a proactive and structured process used to identify potential threats, vulnerabilities, and attack vectors that could affect an application or system. The primary purpose is to understand the system's security exposure from an attacker's perspective and to define countermeasures to prevent, mitigate, or respond to these threats *before* they can be exploited. It involves thinking about what could go wrong and how to prevent it.

### 1.2. Why is it Important for this App?
The Biometric Voting App is inherently security-sensitive due to several factors:
*   **Voting Integrity:** The core function involves recording votes, where accuracy, tamper-resistance, and prevention of fraudulent activities (like double voting) are paramount.
*   **User Anonymity & Privacy:** While using biometrics for verification, the system is designed to create anonymized voter IDs. Protecting this anonymization and any associated (even non-PII) data is critical.
*   **Handling of Sensitive Operations:** Biometric authentication gates critical operations like ID generation, login, and vote submission. The mechanisms protecting these operations must be robust.
*   **Trust:** Users need to trust the system for it to be adopted. A proactive approach to security, like threat modeling, helps build this trust.

Proactively identifying threats helps in building a more secure and resilient application from the ground up, rather than reacting to vulnerabilities after they are discovered or exploited.

### 1.3. Suggested Methodology: STRIDE
STRIDE is a popular threat modeling methodology developed by Microsoft that helps in systematically identifying and categorizing threats. It's an acronym for:

*   **S**poofing: Illegitimately accessing and using another user's identity or credentials (e.g., faking an anonymized voter ID, unauthorized API access).
*   **T**ampering: Maliciously modifying data or code (e.g., altering vote data in transit or at rest, modifying app behavior).
*   **R**epudiation: Denying having performed an action when others can prove otherwise, or conversely, being unable to prove an action was taken (e.g., a voter denying they cast a vote, or the system being unable to prove a vote was validly recorded without compromising anonymity). This is complex in anonymous systems.
*   **I**nformation Disclosure: Exposing sensitive information to individuals who are not authorized to access it (e.g., leaking anonymized ID linkage, exposing database contents, revealing encryption keys).
*   **D**enial of Service (DoS): Preventing legitimate users from accessing or using the system (e.g., overwhelming the backend API, making the database unresponsive, locking out users via biometric failures).
*   **E**levation of Privilege: Gaining capabilities or access beyond what is authorized (e.g., a regular user gaining administrative access, a compromised app bypassing security controls).

---

## 2. System Overview for Analysis

### 2.1. Key Components & Technologies
*   **Android Client Application:**
    *   Language: Kotlin
    *   UI: Jetpack Compose
    *   Biometric Authentication: `androidx.biometric.BiometricPrompt`
    *   Secure Storage: Android Keystore (for encryption keys), `androidx.security.crypto.EncryptedSharedPreferences` (for salt, stable ID).
    *   Device Attestation (Planned): Play Integrity API (client-side token request).
    *   Networking: Retrofit, OkHttp, Gson.
*   **Backend API Server:**
    *   Framework: Node.js with Express.js
    *   Database Client: `pg` (node-postgres)
    *   Security Middleware: `helmet`, `express-rate-limit`
    *   Logging: `winston`, `morgan`
    *   Device Attestation (Planned): Google Play Integrity API (backend token verification using `googleapis`).
*   **Database Server:**
    *   Type: PostgreSQL
*   **Communication Channels:**
    *   Client <-> Backend: HTTPS (TLS)
    *   Backend <-> Google Play Integrity API Servers: HTTPS (TLS)

### 2.2. Key Data Flows & Assets
*   **User Registration:**
    *   Client: Biometric prompt -> On success, trigger ID generation.
    *   Client: `AnonymizedIdGenerator` uses `SecureSaltProvider` and `StableIdentifierProvider` (data stored in EncryptedSharedPreferences) to create a unique, anonymized ID.
    *   Client -> Backend: Send anonymized ID via HTTPS.
    *   Backend: Validates ID, stores it in `Voters` table.
*   **User Login/Verification:**
    *   Client: Biometric prompt -> On success, `AnonymizedIdGenerator.getRegisteredAnonymizedId()` re-derives ID.
    *   (Future: This ID might be used to fetch user-specific state or a session token from backend, though current login is primarily local for ID access).
*   **Fetching Election Data:**
    *   Client: Requests list of active elections, optionally sending its anonymized ID.
    *   Backend: Queries `Elections` table, and if anonymized ID provided, queries `Votes` table to determine `hasVoted` status for each election.
    *   Backend -> Client: Sends list of elections (DTOs) via HTTPS.
*   **Vote Submission:**
    *   Client: User selects option, confirms with biometrics.
    *   Client: `SecurityUtil` uses Keystore-backed key (unlocked by biometrics) to encrypt a "vote proof payload".
    *   Client (Planned): `PlayIntegrityService` generates nonce, requests Play Integrity token.
    *   Client -> Backend: Sends `anonymizedVoterId`, `electionId`, `selectedOption`, `encryptedProof` (Base64), `iv` (Base64), `playIntegrityToken`, `playIntegrityNonce` via HTTPS.
    *   Backend (Planned): `play_integrity_verifier` decodes and verifies `playIntegrityToken` against `playIntegrityNonce`.
    *   Backend: Validates voter, election status, option validity.
    *   Backend: Stores vote details (including encrypted proof, IV) in `Votes` table, ensuring unique (`voter_id`, `election_id`) constraint.
*   **Sensitive Data Assets:**
    *   **Client-Side:**
        *   Raw Biometric Data: Handled by Android OS within Secure Hardware; not directly accessed by the app.
        *   Android Keystore Keys: Keys used by `SecurityUtil` for vote proof, MasterKeys for `EncryptedSharedPreferences`.
        *   Salt & Stable ID: Stored in `EncryptedSharedPreferences`.
        *   Derived Anonymized ID: Stored in memory during session, potentially cached (though current implementation re-derives it).
    *   **In Transit:**
        *   Anonymized ID, Vote Data, Encrypted Proof, IV, Play Integrity Token & Nonce (all over HTTPS).
    *   **Backend-Side:**
        *   Anonymized IDs: Stored in `Voters` table.
        *   Vote Data (`selected_option_value`, `encrypted_proof`, `iv`): Stored in `Votes` table.
        *   `GOOGLE_APPLICATION_CREDENTIALS` (Service Account Key for Play Integrity): Stored on the server filesystem or environment, path configured via env var.
        *   Database Credentials: Configured via environment variables.
        *   Session Tokens (if implemented in future for admin panel).

### 2.3. Trust Boundaries
*   **User <-> Client Device:** Physical access to device, biometric sensor.
*   **Client Application <-> Android OS:** BiometricPrompt, Keystore, EncryptedSharedPreferences rely on OS security.
*   **Client Device <-> Network <-> Backend API Server:** Data transmitted over HTTPS.
*   **Backend API Server <-> Database Server:** Typically within a private network, but still a boundary.
*   **Backend API Server <-> Google Play Integrity API Servers:** External API call over HTTPS.

---

## 3. Threat Elicitation Guidance (Using STRIDE Categories)

This section provides questions and example threats to consider for each major component or interaction. The team should brainstorm additional threats.

### 3.1. Android Client Application

*   **Spoofing:**
    *   Could a malicious app installed on the same device attempt to intercept or mimic communication with `BiometricPrompt`? (Likely hard due to OS protections, but consider).
    *   If device credentials (PIN/Pattern/Password) are compromised, and if they are allowed as a fallback or part of biometric strength, how does this impact the perceived security of biometric authentication?
    *   Can an attacker with physical access to an unlocked device (e.g., screen lock disabled after initial auth) re-trigger sensitive operations without re-authentication?
*   **Tampering:**
    *   Can the vote payload (`VoteRequest`) be altered after biometric confirmation and encryption of proof, but before it's sent over the network? (e.g., via Man-in-the-Middle on a compromised local network if not exclusively HTTPS, or malware on device).
    *   How is the integrity of the app itself (preventing repackaging with malicious code) addressed beyond what Play Integrity's app recognition might offer?
    *   Can the salt or stable ID stored in `EncryptedSharedPreferences` be tampered with on a rooted device, leading to predictable or controlled anonymized ID generation?
*   **Repudiation:**
    *   (Difficult in an anonymous system by design). Are there any client-side logs or states that a malicious user could manipulate to falsely claim an action was or wasn't taken, if such client-side proof were ever considered?
    *   Does the "vote proof" provide non-repudiation from the client's perspective that a specific biometric event authorized *some* data encryption?
*   **Information Disclosure:**
    *   On a rooted or compromised device, what is the risk of extracting keys from Android Keystore or data from `EncryptedSharedPreferences`? (Assume strong compromise).
    *   Are there any scenarios (e.g., verbose debug logging accidentally left in, side-channel attacks via other apps) where the anonymized ID could be linked back to a user or device identifier?
    *   Could any part of the `encrypted_proof` or `iv` leak information about the vote if the encryption were flawed (though AES-CBC is standard)?
*   **Denial of Service:**
    *   Can repeated failed biometric attempts (e.g., by a user struggling, or someone else) permanently lock out the legitimate user from the app's biometric features without a clear recovery path within the app?
    *   Can the app be crashed or made unresponsive by malformed data received from the backend API?
    *   Is there a risk of excessive calls to `PlayIntegrityService.requestIntegrityToken` leading to rate limiting from Google's side?
*   **Elevation of Privilege:**
    *   On a rooted device, can a malicious actor bypass the `BiometricPrompt` system call and still make the app believe authentication was successful, thereby gaining access to ID generation or use of Keystore-protected keys for vote proofing?
    *   Are there any debug features or developer backdoors accidentally left in release builds that could grant elevated access?

### 3.2. Backend API Server

*   **Spoofing:**
    *   If an administrative API is added in the future, how will admin users be authenticated and authorized? What protects against spoofing of admin credentials?
    *   Can API keys (if the backend uses them to communicate with other third-party services) be stolen from the server environment?
    *   Can a client successfully register or vote with a malformed (but correctly formatted type) `anonymizedVoterId` if validation is not strict enough on its cryptographic properties (though current check is SHA-256 format)?
*   **Tampering:**
    *   What if a valid Play Integrity token is received, but the associated vote payload (`electionId`, `selectedOption`) is inconsistent, malformed, or refers to an invalid/inactive election? (Current validation checks should cover this).
    *   Are all input parameters (headers, query params, body fields) rigorously validated for type, format, length, and range beyond basic presence checks?
    *   Is there any risk of HTTP parameter pollution or verb tampering if not using strict routing?
*   **Repudiation:**
    *   (Primarily for future admin actions). What audit trails are in place for administrative actions?
    *   For voting, the system is designed for anonymity. However, are logs sufficient to trace system errors without compromising voter anonymity?
*   **Information Disclosure:**
    *   Are error messages returned to the client overly descriptive, potentially revealing internal system paths, library versions, or database states? (Current generic 500 errors are good).
    *   How is the `GOOGLE_APPLICATION_CREDENTIALS` JSON file protected on the server filesystem and in backups?
    *   Are there any database query patterns that might inadvertently leak data through side channels (e.g., timing attacks on ID lookups - likely low risk for this app)?
*   **Denial of Service:**
    *   Are there any API endpoints that are not covered by rate limiting or have limits that are too generous, which could be abused to exhaust server resources (CPU, memory, DB connections)?
    *   What is the impact of database connection pool exhaustion? Does the application handle this gracefully?
    *   Can large, unexpected payloads in requests (e.g., very long `selectedOption` string) cause issues even if validated for type?
*   **Elevation of Privilege:**
    *   Are there any input parameters or sequences of API calls that could lead to unintended higher privileges if an authorization flaw exists (more relevant if different user roles/permissions are introduced)?
    *   Is there any possibility of command injection if any external processes are invoked based on user input (unlikely for this app but a general concern)?

### 3.3. Database (PostgreSQL)

*   **Spoofing (Unauthorized Access):**
    *   How are database credentials managed and secured? Are they hardcoded, in config files, or injected via environment variables? (Currently env vars, which is good).
    *   Are different database users used for different services/apps accessing the same DB instance, each with minimal necessary permissions? (For this app, likely one user).
*   **Tampering:**
    *   SQL Injection is largely mitigated by using parameterized queries. Are there *any* exceptions to this rule anywhere in the codebase?
    *   If multiple applications/services shared this database with different users, could a less privileged user find a way to tamper with data they shouldn't have access to?
*   **Information Disclosure:**
    *   Are database backups encrypted at rest?
    *   Are connection strings (including credentials) logged or exposed anywhere insecurely?
    *   Is the database port exposed to the public internet, or only accessible from the backend server's private network?
*   **Denial of Service:**
    *   What is the system's behavior if the database runs out of disk space or reaches its maximum number of connections?
    *   Are there any queries that could be particularly long-running or resource-intensive on large datasets, potentially impacting overall DB performance?
*   **Elevation of Privilege:**
    *   Could a vulnerability in the application (e.g., SQLi if parameterized queries were missed somewhere) allow an attacker to execute commands or modify database structures with the privileges of the application's database user?
    *   Does the application's database user have more privileges than necessary (e.g., superuser)?

### 3.4. Communication Channels

*   **Information Disclosure/Tampering (Client <-> Backend):**
    *   HTTPS is planned for production. Is certificate pinning being considered for the Android client to mitigate sophisticated Man-in-the-Middle (MitM) attacks?
    *   Are appropriate TLS versions and strong cipher suites enforced on the server?
*   **Information Disclosure/Tampering (Backend <-> Google Play Integrity Servers):**
    *   These calls are made using Google's client libraries over HTTPS. Is the backend environment validating SSL certificates for these outgoing connections correctly? (Usually handled by Node.js HTTPS module / `googleapis` library).

### 3.5. Play Integrity Interactions

*   **Bypassing/Tampering (Client-Side Nonce & Token):**
    *   How robust is the client-generated nonce? Could it be predictable or reused if an attacker can control the client environment to some extent?
    *   Can an attacker capture a valid token and nonce pair and replay it for a different vote if the backend nonce validation against the token's `requestDetails.nonce` is flawed or if server-side nonce binding isn't also used?
*   **Policy for Device Verdicts:**
    *   What is the defined policy if `deviceIntegrity.deviceRecognitionVerdict` includes `MEETS_VIRTUAL_INTEGRITY`? Is this acceptable for voting?
    *   What if it only meets `MEETS_BASIC_INTEGRITY` but not `MEETS_DEVICE_INTEGRITY` or `MEETS_STRONG_INTEGRITY`?
*   **Token Expiry & Freshness:**
    *   How is token expiry handled or considered? (Play Integrity tokens have a limited validity period).
    *   The backend currently relies on the client-provided nonce to match the one inside the token. For stronger replay protection in future, the backend might generate a nonce, send it to the client, which then uses it for the token request.

---

## 4. General Mitigation Strategies & Best Practices Checklist

This is a non-exhaustive checklist of common security best practices and mitigation strategies relevant to this application. The team should ensure these are considered and implemented where appropriate.

*   **Input Validation (Client & Server-Side):** Validate all external input for type, length, format, and range.
*   **Principle of Least Privilege:** Users, processes, and API clients should only have the permissions necessary to perform their tasks.
*   **Secure Defaults:** Default configurations should be secure.
*   **Defense in Depth:** Employ multiple layers of security controls.
*   **HTTPS Everywhere:** Encrypt all data in transit.
*   **Parameterized Queries (Backend):** Use exclusively to prevent SQL injection.
*   **Secure Credential Storage & Management:**
    *   Client: Android Keystore for cryptographic keys, EncryptedSharedPreferences for sensitive data.
    *   Backend: Securely manage database credentials and Google service account keys (e.g., using environment variables, secrets management solutions).
*   **Regular Dependency Updates & Vulnerability Scanning:** Keep libraries and frameworks up-to-date. Use tools like `npm audit` or Snyk.
*   **Robust Error Handling:** Handle errors gracefully without leaking sensitive information.
*   **Security Logging & Monitoring:** Log security-relevant events. Set up monitoring and alerting for suspicious activities.
*   **Code Obfuscation (Android):** Use Proguard/R8 to make reverse engineering more difficult.
*   **Regular Security Reviews & Testing:** Conduct manual code reviews, automated security scans, and periodic penetration testing.
*   **Rate Limiting (Backend):** Protect against brute-force and DoS attacks.
*   **Security Headers (Backend):** Use `helmet` or similar to set appropriate HTTP security headers.

---

## 5. Suggested Process for Conducting the Threat Model

The following steps outline a suggested process for the team to conduct the actual threat modeling exercise:

1.  **Preparation:**
    *   Assemble a cross-functional team (developers from Android/backend, QA, potentially a security champion or external consultant if available).
    *   Gather all relevant documentation: this guide, architecture diagrams, data flow diagrams, API specifications, `security_requirements.md`, `mvp_features.md`.
    *   Define the scope of the threat model (e.g., focus on MVP features first, then expand).
2.  **Decomposition:**
    *   Break down the application into its key components (as listed in Section 2.1).
    *   Identify key data flows (Section 2.2) and draw/review data flow diagrams (DFDs).
    *   Define trust boundaries (Section 2.3).
3.  **Threat Identification (Brainstorming):**
    *   For each component and data flow, systematically brainstorm potential threats using the STRIDE methodology (Section 3) as a guide.
    *   Ask "What could go wrong here?" for each STRIDE category.
    *   Document all identified threats, even if they seem minor initially.
4.  **Risk Assessment (Optional for this initial guide, but recommended for prioritization):**
    *   For each identified threat, assess its potential impact and likelihood.
    *   Methodologies like DREAD (Damage, Reproducibility, Exploitability, Affected Users, Discoverability) can be used, or a simpler High/Medium/Low rating.
    *   This helps prioritize which threats require immediate attention.
5.  **Mitigation Planning:**
    *   For each prioritized threat, identify existing security controls that might mitigate it.
    *   If no sufficient controls exist, brainstorm and define new mitigation strategies or design changes.
    *   Assign owners and timelines for implementing new mitigations.
6.  **Documentation & Iteration:**
    *   Document the entire threat modeling process: identified threats, risk assessments (if done), planned mitigations, and any outstanding concerns. This becomes the actual "Threat Model Document."
    *   Threat modeling is not a one-time activity. Revisit and update the threat model periodically, especially when new features are added, significant architectural changes occur, or new vulnerabilities are discovered in the technologies used.

By following this guide and process, the Biometric Voting App team can significantly improve the application's security posture.
