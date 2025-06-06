# Biometric Voting App

A secure, privacy-respecting voting platform that leverages biometric authentication to ensure election integrity. Designed for extensibility, developer productivity, and robust code quality.

---

## Vision

Create a modern, user-friendly voting system that uses biometrics (fingerprint, facial recognition, etc.) to guarantee one person, one vote—securely and transparently.

---

## Key Features

- **Biometric Authentication:** Integrate device-level biometrics for voter verification.
- **One Voter, One Vote:** Prevent duplicate votes; ensure anonymity and auditability.
- **End-to-End Security:** Encryption, secure storage, and privacy by design.
- **Extensible:** Built for adding new features (e.g., multi-factor auth, real-time tally).
- **AI-Augmented Development:** Leverage AI coding partner for productivity and code quality.

---

## Recommended Stack

- **Frontend:** React (Web) or React Native (Mobile)
- **Backend:** Python (FastAPI) or Node.js (Express)
- **Biometric Integration:** Platform-specific APIs (WebAuthn, mobile SDKs)
- **Database:** PostgreSQL
- **Testing:** Pytest/Jest, CI with GitHub Actions

---

## Project Overview

This project implements a biometric voting application with an Android client and a Node.js (Express) backend using a PostgreSQL database. Key features include biometric authentication for voting, generation of an anonymized voter ID, and secure submission of votes with cryptographic proof.

## Key Features (Current Implementation)

- **Biometric Authentication:** Android app uses device-level biometrics for critical actions like registration and vote confirmation.
- **Anonymized Voter ID:** Client-side generation of a unique, anonymized ID based on a persisted salt and stable application identifier.
- **Persistent Backend:** Node.js backend with PostgreSQL database for storing voter registrations, election data, and votes.
- **Secure Vote Proofing:** Android app encrypts a proof payload using a biometric-bound key from Android Keystore during vote submission.
- **API Security:** Backend includes basic security headers via Helmet.js and guidance for HTTPS in production.
- **ViewModel Architecture:** Android app utilizes ViewModels for UI logic separation in key screens.
- **Automated Testing:** Includes unit tests for Android ViewModels and backend API endpoints (Jest/Supertest).

## Directory Structure

```
biometric-voting-app/
│
├── backend/                # API, business logic, DB models
│   └── tests/              # Backend tests (e.g., Jest, Pytest)
├── app/                    # Android mobile client
│   ├── src/main/           # Main application source code
│   ├── src/test/           # Unit tests
│   └── src/androidTest/    # Instrumented tests
├── docs/                   # Project documentation (architecture, design, guides)
├── gradle/                 # Gradle wrapper files & version catalog
├── .gitignore
├── README.md               # This file: Project overview, setup
├── PROJECT_REMITS.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
└── CODE_OF_CONDUCT.md
```

---

## AI Coding Partner

This project is AI-augmented. See [PROJECT_REMITS.md](PROJECT_REMITS.md) for goals and remit of the AI assistant.

---

## Building and Running the Project

This project consists of two main components: an Android application (`app/`) and a backend server (`backend/`).

### 1. Backend Server (`backend/`)

The backend is a Node.js application that uses Express.js and connects to a PostgreSQL database.

*   **Prerequisites:** Node.js, npm (or yarn), and a running PostgreSQL instance.
*   **Setup and Configuration:**
    *   Navigate to the `backend/` directory.
    *   Detailed instructions for installing dependencies (`npm install`), configuring database connection parameters (via environment variables like `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`), and running the server (`npm start`) are provided in `backend/README.md`.
    *   The server will attempt to create the necessary database tables if they don't exist on startup.

### 2. Android Application (`app/`)

The Android client is located in the `app/` directory.

*   **Prerequisites:** Android Studio (latest stable version recommended) or Android SDK tools with a compatible JDK.
*   **Building:**
    *   Import the project into Android Studio.
    *   The project can be built using Android Studio's "Build" menu or via Gradle commands (e.g., `./gradlew :app:assembleDebug` or `./gradlew :app:assembleRelease`).
*   **Running:**
    *   Run the app on an Android emulator or a physical device through Android Studio.
    *   Ensure the backend server is running and accessible from the Android client (e.g., emulator typically accesses host machine's localhost via `10.0.2.2`). The Android app's `ApiService.kt` is configured to use this for development.
*   **Generating a Signed APK for Release:**
    *   For release builds, you must configure signing credentials. The `app/build.gradle.kts` file contains a placeholder `signingConfigs { releasePlaceholder { ... } }` block.
    *   Follow standard Android procedures to:
        1.  Generate a Java Keystore (JKS) file if you don't have one (using `keytool`).
        2.  Securely store this keystore file.
        3.  Update `app/build.gradle.kts` or (preferably) create a `keystore.properties` file in the `app/` directory to provide the `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` to the Gradle build process. Comments within `app/build.gradle.kts` provide guidance on this.
        4.  Add `keystore.properties` to your `.gitignore` file.
    *   Once configured, a signed release APK can be generated using `./gradlew :app:assembleRelease`.

## Key Documentation

*   **`README.md`** (this file): Project overview, high-level setup.
*   **`backend/README.md`**: Detailed setup and configuration instructions for the backend server.
*   **`docs/android_implementation_guide.md`**: Served as the initial setup guide for the Android app. Note that some implementation details have evolved; the current codebase is the source of truth.
*   **`docs/backend_testing_strategy.md`**: Strategy and examples for testing the backend API.
*   **`docs/security_requirements.md`**: Core security principles and requirements for the application.
*   **`docs/mvp_features.md`**: Description of the Minimum Viable Product features.
*   **`docs/ui_ux_flow.md`**: Describes the user interface screens and user experience flow.
*   **`docs/backend_design/`**: Contains detailed backend design documents including API specifications and database schema.

## Considerations & Best Practices

- **Security:** Always use encrypted channels (HTTPS for production) and secure credential storage.
- **Privacy:** Minimize collection and logging of sensitive data. Raw biometric data is never stored.
- **Testing:** Prioritize automated testing for all critical paths.
- **Accessibility:** Ensure usability for all potential voters.
- **Transparency:** Document design and decisions in `docs/`.

---

## License

See [LICENSE](LICENSE) for details.
