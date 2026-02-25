# Biometric Voting App

backend-db-frontend-setup
## 1. Project Vision
A secure and anonymous voting application utilizing biometric authentication and blockchain technology to ensure the integrity and privacy of the voting process.

## 2. Overview
This project aims to develop a comprehensive solution for biometric voting. Key documents:
- [Project Requirements](./docs/project_requirements.md)
- [System Architecture](./docs/system_architecture.md)
- [Development Roadmap](./docs/development_roadmap.md)

## 3. Project Structure
- **/app**: Android mobile application source code.
- **/backend**: Node.js Backend API services.
- **/legacy**: Legacy or prototype code (e.g., previous Python backend, React frontend).
- **/docs**: Project documentation, including requirements, architecture, and roadmap.
- **/tests**: Unit, integration, and end-to-end tests.

## 4. Getting Started (Placeholder)
Detailed setup instructions will be added here as the components are developed. This will typically include:
- Cloning the repository.
- Installing dependencies for frontend and backend.
- Setting up database connections.
- Configuring environment variables.
- Instructions for running the application and tests.

## 5. Contributing
Contribution guidelines will be added here.

## 6. License
To be determined.

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
│   ├── build.gradle.kts    # Android app build configuration
│   ├── proguard-rules.pro  # Proguard rules for release builds
│   ├── src/main/           # Main application source code
│   ├── src/test/           # Unit tests
│   └── src/androidTest/    # Instrumented tests
├── docs/                   # Project documentation (architecture, design, guides)
├── gradle/                 # Gradle wrapper files & version catalog
│   └── libs.versions.toml
├── .gitignore
├── PROJECT_REMITS.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
├── CODE_OF_CONDUCT.md
└── settings.gradle.kts
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
    *   For local development, you can create a `.env` file in the `backend/` directory to manage these variables (you might need to add the `dotenv` package: `npm install dotenv` and require it in `server.js`).
    *   **Caution:** It is strongly recommended to change the default `DB_USER` and `DB_PASSWORD` for any non-local or shared development environment, as noted in `backend/README.md`.
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
*   **`docs/mvp_security_review_notes.md`**: Notes from security review of MVP features.
*   **`docs/ui_ux_flow.md`**: Describes the user interface screens and user experience flow.
*   **`docs/user_stories.md`**: User stories for the application.
*   **`docs/backend_design/`**: Contains detailed backend design documents including API specifications and database schema.
*   **`docs/ai_coding_assistants_guide.md`**: Guide for working with AI coding assistants on this project.
*   **`docs/backend_logic.md`**: Detailed explanation of backend business logic.
*   **`docs/human_developer_engagement_plan.md`**: Plan for human developer engagement and collaboration with AI.
*   **`docs/research_ai_platforms_strategy.md`**: Strategy for researching AI platforms and tools.

## Considerations & Best Practices

- **Security:** Always use encrypted channels (HTTPS for production) and secure credential storage.
- **Privacy:** Minimize collection and logging of sensitive data. Raw biometric data is never stored.
- **Testing:** Prioritize automated testing for all critical paths.
- **Accessibility:** Ensure usability for all potential voters.
- **Transparency:** Document design and decisions in `docs/`.

---

## Known Issues & Future Work

This section outlines known limitations of the current MVP and potential areas for future development.

### Known Issues/Limitations (MVP Scope)

*   **Error Reporting (Android):** Firebase Crashlytics has been integrated for crash reporting. Its full efficacy across various scenarios will become clearer with more extensive real-world or simulated testing.
*   **Scalability (Backend):** The current backend setup (`Node.js/Express` with a single PostgreSQL instance) is designed for MVP loads. For very large-scale elections (e.g., hundreds of thousands to millions of voters/votes), further database optimization (e.g., connection pool tuning, more advanced indexing strategies), potential use of read replicas, horizontal scaling of the application layer, and robust load balancing strategies would be necessary.
*   **Offline Support (Android):** The Android app currently requires a stable network connection for all primary operations: registration, login (which re-verifies ID components implicitly via `AnonymizedIdGenerator` if it were to hit an API, though current login is local), fetching elections, and submitting votes. It does not support offline voting or extensive caching of election data.
*   **Accessibility (Android):** While standard Jetpack Compose components are used (which have some built-in accessibility support), a dedicated accessibility review and testing phase (e.g., for improved TalkBack support across all custom UI interactions, comprehensive keyboard navigation, and ensuring high contrast ratios beyond defaults) has not been performed.
*   **Biometric Security & Testing:**
    *   The security of on-device ID generation (`AnonymizedIdGenerator.kt`) relies heavily on the secure implementation of `SecureSaltProvider.kt` and `StableIdentifierProvider.kt`, which are noted as needing expert cryptographic review for production deployment.
    *   Testing biometric authentication features, particularly those involving `CryptoObject` operations (like vote proofing via `SecurityUtil.kt`), can be challenging to fully automate and verify on emulators and may require extensive testing on a diverse range of physical devices.
*   **Database Schema Evolution:** The current `initializeDatabase()` function in `backend/server.js` creates tables and indexes `IF NOT EXISTS`. For future schema changes in production, a proper migration tool (e.g., `node-pg-migrate`, `Flyway`, `Liquibase`) would be essential instead of relying on conditional DDL in application startup.
*   **Configuration Management (Backend):** While environment variables are used, a more robust configuration management solution might be needed for complex production deployments, including validation of environment variable settings at startup.

### Future Work & Potential Enhancements

*   **User Acceptance Testing (UAT):** Conduct thorough UAT with a diverse group of target users to gather feedback on usability, clarity, and overall experience.
*   **CI/CD Pipeline:** Implement a Continuous Integration/Continuous Deployment (CI/CD) pipeline for automated builds, comprehensive testing (unit, integration, UI), and streamlined deployments for both Android and backend components.
*   **Advanced Election Features:**
    *   Display more detailed election information (e.g., candidate profiles, links to external resources, PDF attachments).
    *   Support for different question types (e.g., ranked choice, multiple selections) or more complex voting mechanisms.
    *   Real-time election results display (if applicable and designed to maintain anonymity).
*   **Enhanced Security & Auditing:**
    *   **Backend:** Implement Security Information and Event Management (SIEM) capabilities for monitoring and alerting on suspicious activities. Conduct professional, third-party penetration testing and security audits.
    *   **Android & Backend:** Explore advanced cryptographic techniques for end-to-end verifiability of votes if required by specific election integrity standards.
*   **Administrative Interface:** Develop a secure administrative web interface for:
    *   Managing elections (creation, scheduling, status changes).
    *   Monitoring system health and basic statistics (e.g., number of registered users, votes per election - while preserving anonymity).
    *   Managing application content or configurations.
*   **Comprehensive Internationalization (i18n) and Localization (l10n):**
    *   Translate all user-facing strings in the Android app into multiple languages.
    *   Ensure backend messages (if any become user-facing) are localizable.
*   **Improved Test Coverage:**
    *   Continuously expand unit, integration, and end-to-end (E2E) test suites for both Android and backend.
    *   Develop more sophisticated UI tests for the Android app (e.g., using Espresso or UI Automator for complex flows).
    *   Implement performance and load testing for the backend API.
*   **Push Notifications (Android):** Notify users about upcoming elections or important announcements.
*   **Data Backup and Recovery (Backend):** Implement and regularly test robust backup and disaster recovery plans for the PostgreSQL database.

---

## License

See [LICENSE](LICENSE) for details.
Biometric-Voting-App
