# CI/CD Setup Guide (GitHub Actions)

This document outlines the Continuous Integration (CI) setup for the Biometric Voting App using GitHub Actions. These workflows automate building and testing for both the backend and Android applications.

## Overview

We use GitHub Actions to automatically build and test our code whenever changes are pushed to key branches (`main`, `develop`) or when pull requests are made to these branches. This helps ensure code quality and integration consistency.

Two separate workflow files are defined:
*   `.github/workflows/backend-ci.yml` for the Node.js backend.
*   `.github/workflows/android-ci.yml` for the Android application.

These files are automatically detected and executed by GitHub Actions.

## Backend CI Workflow (`.github/workflows/backend-ci.yml`)

This workflow handles the Node.js backend application.

**Triggers:**
*   Push to `main` or `develop` branches (if changes are in `backend/**` or the workflow file).
*   Pull request to `main` or `develop` branches (if changes are in `backend/**` or the workflow file).

**Key Steps:**
1.  **Checkout Code:** Fetches the latest code from the repository.
2.  **Set up Node.js:** Configures the environment with specified Node.js versions (currently tests on 18.x and 20.x). It also caches npm dependencies for faster builds.
3.  **Install Dependencies:** Runs `npm ci` in the `backend/` directory to install dependencies reliably.
4.  **Run Tests:** Executes `npm test` in the `backend/` directory.
    *   **Database Service for Testing:** To ensure reliable testing of database interactions, this workflow now includes a **PostgreSQL service container**.
        *   **Image:** `postgres:15-alpine` is used.
        *   **Service Configuration:** The service is automatically configured with the following environment variables:
            *   `POSTGRES_USER: testuser`
            *   `POSTGRES_PASSWORD: testpassword`
            *   `POSTGRES_DB: biometric_voting_app_test_db` (This database is created and ready for use by the tests).
        *   **Health Check:** A health check (`pg_isready`) is configured for the service to ensure PostgreSQL is operational before tests proceed. An additional short `sleep` step is also included in the workflow for extra readiness assurance.
    *   **Application Environment Variables for Tests:** The test execution step sets necessary environment variables for the Node.js application:
        *   `NODE_ENV: test`
        *   `DB_HOST: localhost` (as services are mapped to localhost on the GitHub Actions runner).
        *   `DB_PORT: 5432` (matching the service's exposed port).
        *   `DB_USER: testuser` (matching `POSTGRES_USER` of the service).
        *   `DB_PASSWORD: testpassword` (matching `POSTGRES_PASSWORD` of the service).
        *   `DB_TEST_NAME: biometric_voting_app_test_db` (ensuring the application connects to the database created by the service).

## Android CI Workflow (`.github/workflows/android-ci.yml`)

This workflow handles the Android application.

**Triggers:**
*   Push to `main` or `develop` branches (if changes are in `app/**`, `gradle/**`, root Gradle files, or the workflow file).
*   Pull request to `main` or `develop` branches (with similar path filters).

**Key Steps:**
1.  **Checkout Code:** Fetches the latest code.
2.  **Set up JDK:** Configures the Java environment (currently JDK 17) and caches Gradle dependencies.
3.  **Grant Execute Permissions:** Makes the `./gradlew` wrapper script executable.
4.  **Build Debug APK:** Runs `./gradlew :app:assembleDebug` to compile the debug version of the app.
5.  **Run Unit Tests:** Executes `./gradlew :app:testDebugUnitTest`.
6.  **Run Android Lint:** Runs `./gradlew :app:lintDebug` to check for code quality issues.
7.  **Artifacts (Optional):** The workflow includes commented-out steps for uploading lint and test reports.

### Handling Release Builds and Signing

For distributing the Android application (e.g., via the Google Play Store), a signed release build (typically an AAB - Android App Bundle) is required. This involves signing the app with a private release key.

**Secure Key Management:**
The private key (usually in a `.jks` or `.keystore` file) and its associated passwords must be kept secure. They should **not** be committed to the version control system.

**Recommended Approach using GitHub Actions Secrets:**
The CI workflow (`.github/workflows/android-ci.yml`) is set up to handle release builds and signing using GitHub Actions secrets. Here's how it's conceptually designed:

1.  **Keystore Preparation:**
    *   Your Java Keystore (`.jks`) file needs to be encoded into a Base64 string.
    *   Example command (Linux/macOS): `base64 -w0 your-release-key.jks > your-release-key.jks.b64`
    *   The content of this `.b64` file is what will be stored as a secret.

2.  **Store Credentials as GitHub Secrets:**
    Navigate to your GitHub repository's `Settings > Secrets and variables > Actions` and add the following secrets:
    *   `ANDROID_RELEASE_KEYSTORE_B64`: The Base64 encoded string of your `.jks` keystore file.
    *   `ANDROID_RELEASE_STORE_PASSWORD`: The password for the keystore.
    *   `ANDROID_RELEASE_KEY_ALIAS`: The alias for your release key within the keystore.
    *   `ANDROID_RELEASE_KEY_PASSWORD`: The password for the key alias.

3.  **Workflow Steps for Signing:**
    The `android-ci.yml` workflow includes conditional steps that typically run on pushes to specific branches (e.g., `main` or `release/*`). These steps perform the following:
    *   **Decode Keystore:**
        *   The Base64 encoded keystore string (`secrets.ANDROID_RELEASE_KEYSTORE_B64`) is retrieved.
        *   A script within the workflow decodes this string back into a `.jks` file (e.g., `app/release.keystore.jks`) on the CI runner.
        *   A check is included to gracefully skip these release steps if the `ANDROID_RELEASE_KEYSTORE_B64` secret is not found (e.g., for PRs from forks).
        ```yaml
        # Conceptual snippet from the workflow:
        # - name: Decode Keystore and Create keystore.properties (Release)
        #   env:
        #     ANDROID_RELEASE_KEYSTORE_B64: ${{ secrets.ANDROID_RELEASE_KEYSTORE_B64 }}
        #     # ... other secrets ...
        #   run: |
        #     echo $ANDROID_RELEASE_KEYSTORE_B64 | base64 --decode > app/release.keystore.jks
        ```
    *   **Create `keystore.properties`:**
        *   The workflow dynamically creates the `app/keystore.properties` file on the CI runner using the other secrets (`ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, `ANDROID_RELEASE_KEY_PASSWORD`) to populate it. This file is configured in `app/build.gradle.kts` to provide signing information to Gradle.
        ```yaml
        # Conceptual snippet from the workflow:
        #     echo "storeFile=release.keystore.jks" > app/keystore.properties
        #     echo "storePassword=$ANDROID_RELEASE_STORE_PASSWORD" >> app/keystore.properties
        #     # ... and so on for keyAlias and keyPassword
        ```
        *   The `app/keystore.properties` file itself should be listed in the project's `.gitignore` file (and it is).
    *   **Build Release AAB:**
        *   The command `./gradlew :app:bundleRelease` is run to build the signed Android App Bundle.
    *   **Upload AAB Artifact:**
        *   The generated AAB file (e.g., `app/build/outputs/bundle/release/app-release.aab`) is uploaded as a build artifact for download or further deployment steps.

**Security Emphasis:**
*   These GitHub Actions secrets must be configured in the repository settings by someone with administrative privileges.
*   They are encrypted by GitHub and only exposed to the workflow run.
*   Never hardcode your release keystore details or passwords directly in the workflow file or any other version-controlled file.

This setup allows for automated, secure signing of release builds within the CI environment.

## General CI/CD Considerations & Future Enhancements

While the current setup provides essential CI capabilities (automated testing and building), a more comprehensive CI/CD pipeline for production would include:

*   **Secrets Management:**
    *   **Backend:** Database credentials (`DB_USER`, `DB_PASSWORD`), API keys for external services, and any deployment credentials should be stored as encrypted secrets in GitHub repository settings (`Settings > Secrets and variables > Actions`).
    *   **Android:** The keystore file (`.jks`) for signing release builds, along with its alias and passwords, needs to be securely managed. This can be done by:
        1.  Base64 encoding the keystore file and storing it as a secret.
        2.  Storing keystore passwords and alias as secrets.
        3.  Adding steps in the CI workflow to decode the keystore file and use the secrets during the signing process for release builds.
*   **Branching Strategy:**
    *   A consistent branching strategy (e.g., Gitflow - `feature` branches, `develop`, `release`, `main`, `hotfix`) is recommended to manage development and releases effectively.
*   **Caching:**
    *   Dependency caching (npm packages, Gradle dependencies) is already implemented to speed up builds. Further caching for build outputs could be explored if beneficial.
*   **Deployment (CD - Continuous Deployment/Delivery):**
    *   **Backend:**
        *   Automated deployment to staging and production environments after successful tests on `develop` or `main` branches.
        *   This could involve building a Docker image, pushing it to a container registry (e.g., Docker Hub, GitHub Container Registry, AWS ECR), and then deploying to a cloud platform (e.g., AWS, Google Cloud, Azure) or a virtual server using SSH, Ansible, or platform-specific CLI tools.
    *   **Android:**
        *   Automated signing of release builds (AABs).
        *   Automated deployment to Google Play Console testing tracks (Internal, Alpha, Beta) or directly to production. Tools like `fastlane` or Gradle Play Publisher plugin can facilitate this.
*   **Notifications:**
    *   Configure notifications (e.g., via Slack, email) for build successes or failures.
*   **Code Quality Gates:**
    *   Integrate tools like SonarQube for more in-depth static analysis and code quality tracking.
*   **Service Containers (for Backend Testing):**
    *   A PostgreSQL service container **has been implemented** in the backend CI workflow (`.github/workflows/backend-ci.yml`) to provide a dedicated database for running integration tests. This ensures a clean, consistent, and ephemeral database environment for each CI run.
    *   This approach (using service containers) is highly recommended for any backend tests requiring external services (databases, message queues, etc.) to improve reliability and avoid dependency on external, persistent test environments.

This guide provides a starting point for understanding and expanding the CI/CD capabilities of the project.
```
