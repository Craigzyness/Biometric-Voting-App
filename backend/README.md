# Biometric Voting App - Backend

This directory contains the Node.js (Express) backend server for the Biometric Voting App. It handles API requests, interacts with the PostgreSQL database, and manages the core logic for voter registration, election information, and vote submission.

## Configuration

The backend server requires a connection to a PostgreSQL database. Connection parameters are configured via environment variables. If these variables are not set, the application will attempt to use the default values specified below.

**Environment Variables:**

*   `DB_HOST`: Hostname of the PostgreSQL server.
    *   Default: `localhost`
*   `DB_PORT`: Port on which the PostgreSQL server is running.
    *   Default: `5432`
*   `DB_USER`: Username for connecting to the PostgreSQL database.
    *   Default: `your_db_user`
    *   **Note:** It is strongly recommended to change this default for any non-local or shared development environment.
*   `DB_PASSWORD`: Password for the specified PostgreSQL user.
    *   Default: `your_db_password`
    *   **Note:** This should be kept secret and changed from the default. For production, use a secure method for managing secrets.
*   `DB_NAME`: The name of the PostgreSQL database to connect to.
    *   Default: `biometric_voting_app_db`
*   `ANDROID_PACKAGE_NAME`: The package name of your Android application (e.g., `com.example.biometricvotingapp`). Used by the Play Integrity verifier.
    *   Default: `com.example.biometricvotingapp` (as set in `play_integrity_verifier.js`)
*   `GOOGLE_APPLICATION_CREDENTIALS`: Absolute path to your Google Cloud service account key JSON file. Required for Play Integrity token verification by the backend.
    *   Example: `/path/to/your/service-account-key.json`
*   `LOG_LEVEL`: Sets the logging level for Winston. Can be `error`, `warn`, `info`, `http`, `verbose`, `debug`, `silly`.
    *   Default for production: `info`
    *   Default for development: `debug`
*   `PERFORM_PLAY_INTEGRITY_CHECK`: Controls Play Integrity checks. Set to `false` to bypass checks in development/testing if needed.
    *   Default in production: `true` (checks are performed)
    *   Default in non-production: `false` (checks are bypassed)

**Using a `.env` file for Local Development (Recommended):**

For local development, you can use a `.env` file in the `backend/` directory to manage these environment variables. Create a file named `.env` and add your configuration like this:

```
# Example .env file
DB_HOST=localhost
DB_PORT=5432
DB_USER=my_actual_user
DB_PASSWORD=my_secure_password
DB_NAME=biometric_voting_app_db

# For Play Integrity (optional for local dev if PERFORM_PLAY_INTEGRITY_CHECK is false or NODE_ENV is not 'production')
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json
# ANDROID_PACKAGE_NAME=com.your.app.package.name
# PERFORM_PLAY_INTEGRITY_CHECK=false
# LOG_LEVEL=debug
```

To load these variables from a `.env` file when running `node server.js`, you would typically use a library like `dotenv`. To do this:
1. Install `dotenv`: `npm install dotenv`
2. Add `require('dotenv').config();` at the very beginning of `server.js`.
(Note: The `dotenv` library is not currently a dependency in this project's `package.json`. This is a suggestion for developers.)

### Play Integrity API Backend Verification Setup

To enable the backend to verify Play Integrity tokens received from the Android client, you need to set up a Google Cloud service account and provide its credentials to the backend server.

1.  **Create a Service Account:**
    *   In the Google Cloud Console, navigate to your linked Google Cloud Project.
    *   Go to **IAM & Admin > Service Accounts**.
    *   Click **+ CREATE SERVICE ACCOUNT**.
    *   Give it a name (e.g., "play-integrity-verifier") and an optional description.
    *   Click **CREATE AND CONTINUE**.

2.  **Grant Permissions:**
    *   In the "Grant this service account access to project" step, assign the role **"Play Integrity API Viewer"** (or `roles/playintegrity.viewer`) to the service account. This role allows the service account to call the Play Integrity API to decode tokens.
    *   Click **CONTINUE**, then **DONE**.

3.  **Create a Service Account Key:**
    *   Find the newly created service account in the list.
    *   Click on the three dots (Actions) next to it and select **Manage keys**.
    *   Click **ADD KEY > Create new key**.
    *   Choose **JSON** as the key type and click **CREATE**.
    *   A JSON file containing the service account key will be downloaded. **Treat this file as highly sensitive.**

4.  **Set Environment Variable:**
    *   The backend server uses the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to find this JSON key file.
    *   Set this environment variable in your deployment environment (e.g., server, Docker container, cloud service configuration) to the **absolute path** of the downloaded JSON key file.
    *   Example: `export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account-key.json"`

5.  **Security Warning:**
    *   **DO NOT commit the service account key JSON file to your Git repository.**
    *   Ensure this file is kept secure and access to it is strictly controlled. Add its filename pattern to your `.gitignore` file if it's stored locally during development (though it's better to not store it in the project directory at all).

The `play_integrity_verifier.js` service in the backend uses these credentials to communicate with Google's Play Integrity API. If this environment variable is not set correctly, the Play Integrity token verification will fail (or be bypassed in non-production environments if `PERFORM_PLAY_INTEGRITY_CHECK` is not explicitly set to `true`).

## Prerequisites

*   Node.js (version 14.x or later recommended) and npm (or yarn).
*   A running PostgreSQL server.
*   A PostgreSQL database created (e.g., `biometric_voting_app_db`).
*   A PostgreSQL user with permissions to connect to the database, create tables, and perform CRUD operations on those tables. The credentials should match the configuration (either environment variables or defaults).
*   For Play Integrity verification: A configured Google Cloud service account with the Play Integrity API enabled and credentials provided via `GOOGLE_APPLICATION_CREDENTIALS`.

## Setup and Running the Server

1.  **Navigate to the backend directory:**
    ```sh
    cd backend
    ```

2.  **Install dependencies:**
    ```sh
    npm install
    ```

3.  **Configure Database & Environment:**
    *   Ensure your PostgreSQL server is running.
    *   Create the database (e.g., `biometric_voting_app_db`) and a user with appropriate permissions if you haven't already.
    *   Set the environment variables (e.g., `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`, `GOOGLE_APPLICATION_CREDENTIALS`, etc.) in your environment or by creating a `.env` file as described in the "Configuration" section.

4.  **Run the server:**
    ```sh
    npm start
    ```
    This command executes `node server.js` as defined in `package.json`.
    Upon starting, the server will attempt to connect to the PostgreSQL database and automatically create the necessary tables (`Voters`, `Elections`, `Votes`) if they do not already exist. It will also populate the `Elections` table with sample data if it's empty.

The server will listen on the port specified by the `PORT` environment variable, or `3000` by default. API endpoints are available under the `/api/v1` prefix.

## Security Considerations

### HTTPS
For production deployments, it is **critical** to run this backend server behind a reverse proxy (such as Nginx or Apache) that handles HTTPS termination and manages SSL/TLS certificates. The Node.js server itself runs as an HTTP server and relies on the reverse proxy to provide secure HTTPS communication to clients. Do not expose the plain HTTP port directly to the internet in production.

### Logging
The current logging in `server.js` (using Winston and Morgan) is configured to be more verbose in development and less so in production by default. The `getLoggableId` function attempts to redact parts of voter IDs in production 'info' logs. Review `LOG_LEVEL` environment variable for production to ensure an appropriate level of detail (e.g., 'info' or 'warn') that minimizes exposure of sensitive data while still providing necessary operational insights. Morgan's 'short' format is used for production request logging.

### Play Integrity
Ensure `GOOGLE_APPLICATION_CREDENTIALS` is properly secured and not exposed. The `PERFORM_PLAY_INTEGRITY_CHECK` environment variable can be used to control the enforcement of Play Integrity checks, which is useful for local development or testing environments where setting up Play Integrity might be cumbersome or not desired. It defaults to ON for production.
