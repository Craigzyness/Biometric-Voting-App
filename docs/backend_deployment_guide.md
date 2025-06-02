# Backend Deployment Guide - Biometric Voting App

## 1. Introduction

This guide provides instructions for deploying the Node.js backend server for the Biometric Voting App. The backend handles API requests for voter registration, fetching election data, and submitting votes. It interacts with a PostgreSQL database for data persistence.

## 2. Prerequisites

Before deploying the backend, ensure the following are installed and configured in your deployment environment:

*   **Node.js:** Version 14.x or later is recommended (as per `backend/README.md`). You can check your version with `node -v`.
*   **PostgreSQL Server:** A running PostgreSQL instance must be accessible by the backend server.
*   **npm or yarn:** For managing Node.js packages. `npm` usually comes with Node.js. `yarn` can be installed separately.

## 3. Configuration

The backend server is configured primarily through environment variables.

### Environment Variables

The following environment variables need to be set in your deployment environment:

*   **`PORT`**: The port on which the Node.js server will listen.
    *   Default (if not set): `3000`
*   **`DB_HOST`**: Hostname of the PostgreSQL server.
    *   Default (if not set): `localhost`
*   **`DB_PORT`**: Port on which the PostgreSQL server is running.
    *   Default (if not set): `5432`
*   **`DB_USER`**: Username for connecting to the PostgreSQL database.
    *   Default (if not set): `your_db_user` (Placeholder: **MUST be changed for production**)
*   **`DB_PASSWORD`**: Password for the specified PostgreSQL user.
    *   Default (if not set): `your_db_password` (Placeholder: **MUST be changed for production and kept secret**)
*   **`DB_NAME`**: The name of the PostgreSQL database to connect to.
    *   Default (if not set): `biometric_voting_app_db`
*   **`NODE_ENV`**: The runtime environment.
    *   Impact:
        *   `test`: The server uses `DB_TEST_NAME` (or `biometric_voting_app_test_db`) for the database name and does not automatically start listening (to allow tests to control the server).
        *   `development` (or any other value except `test`): Uses `DB_NAME` (or `biometric_voting_app_db`). Server starts listening on `PORT`.
        *   `production`: Recommended for deployed environments. Ensures production optimizations if any are added to the code based on this variable. It currently behaves like `development` regarding DB naming and server start.

### `.env` File (for Local Development)

*   For local development, you can create a `.env` file in the `backend/` directory to store these environment variables. Example:
    ```env
    PORT=3001
    DB_HOST=localhost
    DB_USER=my_local_user
    DB_PASSWORD=my_secret_local_password
    DB_NAME=biometric_voting_app_dev_db
    NODE_ENV=development
    ```
*   **IMPORTANT:** Ensure the `.env` file is added to your `.gitignore` file and **NEVER** committed to version control, especially if it contains sensitive credentials.
*   To use `.env` files, the `dotenv` package would typically be required and initialized at the start of `server.js`. The current `server.js` does not explicitly include `dotenv` usage, so environment variables must be set directly in the deployment environment or via a process manager.

## 4. Deployment Steps

1.  **Clone Repository:**
    ```bash
    git clone <your-repository-url>
    cd <repository-name>/backend
    ```

2.  **Navigate to Backend Directory:**
    ```bash
    cd backend 
    ```
    (If you cloned directly into a directory named `backend`, you might already be there).

3.  **Install Dependencies:**
    Install only production dependencies to minimize the package size and potential vulnerabilities from development tools.
    Using npm:
    ```bash
    npm install --production
    ```
    Or using yarn:
    ```bash
    yarn install --production
    ```

4.  **Set Environment Variables:**
    Configure all the environment variables listed in the "Configuration" section. The method depends on your deployment platform:
    *   **Hosting Providers (Heroku, AWS Elastic Beanstalk, Azure App Service, etc.):** Use the provider's dashboard or CLI tools to set config/environment variables.
    *   **Virtual Private Servers (VPS) / Bare Metal:**
        *   Set them in the shell environment before running the app (e.g., in `~/.bash_profile`, `~/.zshrc`, or an environment script).
        *   Use systemd unit files with `Environment=` or `EnvironmentFile=`.
        *   Use environment files with Docker containers.
    *   **PM2:** Use an ecosystem file (`ecosystem.config.js`) to define environment variables per application.

5.  **Database Setup:**
    *   Ensure your PostgreSQL server is running and accessible from where the backend application will run.
    *   Create the database specified by the `DB_NAME` environment variable if it doesn't already exist.
    *   The database user specified by `DB_USER` must have the necessary permissions:
        *   Connect to the database.
        *   Create tables (as the application attempts this on startup: `CREATE TABLE IF NOT EXISTS ...`).
        *   Perform CRUD (SELECT, INSERT, UPDATE, DELETE) operations on these tables.
        *   Create indexes (as the application attempts this on startup: `CREATE INDEX IF NOT EXISTS ...`).

6.  **Running the Application:**
    *   You can start the application using:
        ```bash
        npm start 
        ```
        (This typically executes `node server.js` as defined in `package.json`).
    *   **Production Recommendation:** Use a process manager like PM2, systemd, or run the application within a Docker container managed by an orchestrator. This ensures the application restarts automatically if it crashes, manages logs, and can help with scaling.
        *   **Example with PM2:**
            1.  Install PM2 globally: `sudo npm install pm2 -g`
            2.  Start the application: `pm2 start server.js --name biometric-voting-backend`
            3.  To define environment variables with PM2, use an `ecosystem.config.js` file. Example:
                ```javascript
                // ecosystem.config.js
                module.exports = {
                  apps : [{
                    name   : "biometric-voting-backend",
                    script : "./server.js",
                    env_production: {
                       NODE_ENV: "production",
                       PORT: 3000, // Or your desired port
                       DB_HOST: "your_db_host",
                       // ... other DB variables
                    }
                  }]
                }
                ```
                Then start with `pm2 start ecosystem.config.js --env production`.

## 5. Security Considerations for Production

*   **HTTPS (Critical):**
    *   The Node.js application (`server.js`) runs as an HTTP server.
    *   **It MUST be deployed behind a reverse proxy (e.g., Nginx, Apache, Caddy, or a load balancer like AWS ALB/ELB) that handles HTTPS termination and manages SSL/TLS certificates.**
    *   Configure the reverse proxy to forward requests to the Node.js server's HTTP port.
    *   Do not expose the Node.js HTTP port directly to the internet.

*   **Database Security:**
    *   Use strong, unique passwords for the database user (`DB_PASSWORD`).
    *   Limit the database user's permissions to only what is necessary for the application.
    *   Restrict network access to your PostgreSQL server (e.g., firewall rules allowing connections only from your application server IPs).

*   **Environment Variables:**
    *   Manage all sensitive configuration (DB credentials, secret keys if any are added later) exclusively through environment variables.
    *   Ensure these variables are set securely in the deployment environment and not exposed publicly.

*   **Logging:**
    *   The application uses `morgan('dev')` for request logging, which is suitable for development. For production, consider a more structured logging format (e.g., JSON) that can be easily ingested by log management systems. Libraries like Winston or Pino can be used for this.
    *   If using PM2 or Docker, configure them to manage log files, including rotation and size limits.
    *   Ensure no sensitive data (like full raw `encryptedProof` or `iv` if they were ever to be logged, though current code avoids this) is logged.

*   **Rate Limiting:**
    *   The application has implemented rate limiting (`express-rate-limit`) for general API requests and stricter limits for registration and vote submission endpoints. This helps protect against brute-force attacks and API abuse. Monitor these limits and adjust as necessary based on traffic patterns.

*   **Regular Updates:**
    *   Keep Node.js, npm packages (especially Express, pg, helmet, etc.), and the underlying operating system patched and up-to-date to mitigate known vulnerabilities. Regularly run `npm audit` in your development environment and address reported issues.

## 6. Monitoring (Basic Recommendations)

*   **Application Health/Uptime:**
    *   If using PM2, `pm2 monit` or `pm2 list` can provide basic process status.
    *   Set up health check endpoints that your hosting provider or monitoring system can ping.
*   **Server Resources:**
    *   Monitor CPU, memory, disk space, and network I/O of the server running the backend.
*   **Log Analysis:**
    *   Regularly review application logs, especially errors reported by `console.error` or your structured logging system. This helps identify issues, potential security events, and performance bottlenecks.
    *   Pay attention to rate limit messages in logs or from the rate limiting middleware if it supports event emission or logging.

This guide provides a baseline for deploying the backend. Specific configurations will vary based on your chosen hosting environment and infrastructure. Always prioritize security best practices.
