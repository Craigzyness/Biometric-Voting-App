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

**Using a `.env` file for Local Development (Recommended):**

For local development, you can use a `.env` file in the `backend/` directory to manage these environment variables. Create a file named `.env` and add your configuration like this:

```
# Example .env file
DB_HOST=localhost
DB_PORT=5432
DB_USER=my_actual_user
DB_PASSWORD=my_secure_password
DB_NAME=biometric_voting_app_db
```

To load these variables from a `.env` file when running `node server.js`, you would typically use a library like `dotenv`. To do this:
1. Install `dotenv`: `npm install dotenv`
2. Add `require('dotenv').config();` at the very beginning of `server.js`.
(Note: The `dotenv` library is not currently a dependency in this project's `package.json`. This is a suggestion for developers.)

## Prerequisites

*   Node.js (version 14.x or later recommended) and npm (or yarn).
*   A running PostgreSQL server.
*   A PostgreSQL database created (e.g., `biometric_voting_app_db`).
*   A PostgreSQL user with permissions to connect to the database, create tables, and perform CRUD operations on those tables. The credentials should match the configuration (either environment variables or defaults).

## Setup and Running the Server

1.  **Navigate to the backend directory:**
    ```sh
    cd backend
    ```

2.  **Install dependencies:**
    ```sh
    npm install
    ```

3.  **Configure Database:**
    *   Ensure your PostgreSQL server is running.
    *   Create the database (e.g., `biometric_voting_app_db`) and a user with appropriate permissions if you haven't already.
    *   Set the environment variables (e.g., `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`) in your environment or by creating a `.env` file as described in the "Configuration" section (if you choose to add and use the `dotenv` library). If you do not set these, the application will attempt to use the hardcoded default values, which may not be suitable for your environment (especially `DB_USER` and `DB_PASSWORD`).

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
The current logging in `server.js` may include identifiers like `anonymizedVoterId` for debugging purposes during development. For a production environment, review and adjust log verbosity to ensure that no sensitive or personally identifiable information (even anonymized IDs in high-frequency logs) is excessively logged, adhering to privacy best practices and minimizing information exposure. Consider using different log levels for development vs. production.
