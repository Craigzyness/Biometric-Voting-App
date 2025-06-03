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
    *   **Environment Variables for Tests:** The workflow sets `NODE_ENV: test`. For tests requiring a database, it includes placeholder environment variables (`DB_HOST`, `DB_USER`, etc.). In a full production CI setup, these would be securely provided (e.g., via GitHub secrets) to connect to a dedicated test database instance or a service container (like PostgreSQL running in Docker).

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
7.  **Artifacts (Optional):** The workflow includes commented-out steps for uploading lint and test reports as build artifacts, which can be useful for debugging CI failures.

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
    *   For more reliable backend testing, especially for database interactions, use service containers (e.g., a PostgreSQL Docker container) directly within the GitHub Actions workflow. This ensures a clean and consistent test database environment for each run. The `DB_HOST` would then be `localhost` or the service name defined in the workflow.

This guide provides a starting point for understanding and expanding the CI/CD capabilities of the project.
```
