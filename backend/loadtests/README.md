# Backend Load Tests (k6)

This directory contains k6 scripts for basic load testing of the Biometric Voting App backend API.

## Prerequisites

1.  **k6 Installation:**
    You need to have k6 installed on your system. Follow the official installation guide:
    [https://k6.io/docs/getting-started/installation/](https://k6.io/docs/getting-started/installation/)

2.  **Running Backend Server:**
    The backend server (`server.js`) must be running locally, typically on `http://localhost:3000`. Refer to the main backend `README.md` for instructions on starting the server. Ensure the database is also running and accessible.

## Available Test Scripts

The following scripts are available:

1.  **`elections-test.k6.js`**:
    *   Tests the `GET /api/v1/elections` endpoint.
    *   Simulates a small number of users requesting the list of elections.

2.  **`register-test.k6.js`**:
    *   Tests the `POST /api/v1/register` endpoint.
    *   Simulates new users registering with unique (generated) anonymized voter IDs.

3.  **`submitvote-test.k6.js`**:
    *   Tests the `POST /api/v1/submitVote` endpoint.
    *   Includes a setup phase to register test users and fetch active elections.
    *   Simulates registered users submitting votes for available elections.

## How to Run Tests

Navigate to this `backend/loadtests/` directory in your terminal.

**To run a specific test script:**

*   **Elections Test:**
    ```bash
    k6 run elections-test.k6.js
    ```

*   **Registration Test:**
    ```bash
    k6 run register-test.k6.js
    ```

*   **Vote Submission Test:**
    ```bash
    k6 run submitvote-test.k6.js
    ```

## Understanding the Scripts & Output

*   **Options:** Each script has an `options` block where you can adjust parameters like:
    *   `vus`: Number of virtual users.
    *   `duration`: Total duration of the test.
    *   `thresholds`: Define pass/fail criteria for the test (e.g., error rates, request duration percentiles).
*   **Checks:** `check()` functions are used to assert conditions (e.g., HTTP status code).
*   **Metrics:** k6 outputs various metrics upon completion, including request rates, durations, error rates, and any custom metrics defined (like `vote_submission_success_rate` in `submitvote-test.k6.js`).
*   **Output:** Review the console output from k6 for detailed statistics and any error messages (e.g., from `console.log` statements in the scripts or failed checks).

## Notes

*   These scripts are designed for basic load testing and baseline performance assessment against a local development environment.
*   For more extensive load testing (e.g., against a staging or production-like environment), you will need to adjust the `BASE_URL` in the scripts and significantly increase `vus` and `duration` in the `options`.
*   The `submitvote-test.k6.js` script's setup phase relies on the backend having some active elections. The default `initializeDatabase()` function in `server.js` creates sample elections.
```
