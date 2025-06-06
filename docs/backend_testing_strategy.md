# Backend API Testing Strategy

## Introduction

The purpose of this document is to outline a comprehensive strategy for automated API testing of the Node.js (Express) backend for the Biometric Voting App. Automated API tests are crucial for:

*   **Ensuring Reliability:** Verifying that API endpoints function as expected under various conditions.
*   **Preventing Regressions:** Detecting unintended side effects or bugs introduced by new code changes.
*   **Validating Business Logic:** Confirming that the application's rules and logic are correctly implemented at the API level.
*   **Facilitating CI/CD:** Enabling automated testing as part of continuous integration and deployment pipelines.

## Recommended Tools

*   **Jest:** A delightful JavaScript Testing Framework with a focus on simplicity. Jest will serve as the primary testing framework, providing the test runner, assertion library, and mocking capabilities.
*   **Supertest:** An HTTP assertion library that allows for testing Node.js HTTP servers. It provides a high-level abstraction for testing Express applications by making requests to the app and asserting responses.
*   **Database Management (Choose one or a combination):**
    *   **Dedicated Test Database Instance:** Using a separate PostgreSQL database instance specifically for testing. This offers the highest fidelity to the production environment.
    *   **`pg-mem`:** An in-memory PostgreSQL emulator. This can lead to faster tests as it avoids disk I/O and can be easily reset. However, it might not be 100% compatible with all PostgreSQL features, so complex queries or specific constraints should be tested against a real PostgreSQL instance if issues arise.
    *   **Testcontainers:** A library that allows spinning up Docker containers (e.g., a PostgreSQL container) on demand for tests. This provides a clean, isolated database environment for each test run or suite.

## Test Environment Setup

### Database

A critical aspect of backend API testing is managing the database state to ensure tests are independent and repeatable.

1.  **Isolation:** Each test run (or at least each test suite focusing on a specific module) should operate on a known database state. Data created by one test should not interfere with another.
2.  **Schema Initialization:** Before tests run, the database schema (tables, constraints, etc.) must be created. The `initializeDatabase()` function in `backend/server.js` (which creates tables if they don't exist) can be leveraged or adapted for this purpose in the test setup.
3.  **Data Seeding & Teardown:**
    *   For some tests, specific seed data might be required in the database.
    *   Between tests or test suites, tables should be cleaned (e.g., using `TRUNCATE TABLE ... RESTART IDENTITY CASCADE` or by dropping and recreating tables) to ensure a consistent starting point. `pg-mem` can be reset easily. Testcontainers provide a fresh instance.

### Environment Variables for Testing

The backend should be configurable to use a separate test database. This can be achieved by setting specific environment variables before running the tests:

*   `DB_HOST_TEST` (or use existing `DB_HOST` if the test runner environment is isolated)
*   `DB_PORT_TEST`
*   `DB_USER_TEST`
*   `DB_PASSWORD_TEST`
*   `DB_NAME_TEST`

The test setup script or Jest's global setup would ensure the application uses these test-specific database credentials.

### Test Script

A script should be added to `backend/package.json` to execute the tests:
```json
"scripts": {
  "start": "node server.js",
  "test": "jest"
}
```
This allows running tests via `npm test`.

## Test Structure

*   **Directory:** Test files should typically reside in a `tests/` subdirectory within `backend/` or be co-located with the modules they test (e.g., `feature.test.js` alongside `feature.js`). For API tests, grouping by endpoint is common.
*   **Naming Convention:** Use a consistent naming convention, such as `[endpointName].test.js` (e.g., `register.api.test.js`, `elections.api.test.js`).
*   **Jest Globals:**
    *   `describe(name, fn)`: Creates a block that groups together several related tests.
    *   `it(name, fn)` or `test(name, fn)`: Defines an individual test case.
    *   `beforeAll(fn)`, `afterAll(fn)`: Hooks to run setup/teardown logic once before/after all tests in a `describe` block. Useful for database schema initialization or final cleanup.
    *   `beforeEach(fn)`, `afterEach(fn)`: Hooks to run setup/teardown logic before/after each individual test case. Ideal for cleaning database tables between tests.

## Test Cases for Endpoints

The following outlines conceptual test cases for each API endpoint. These tests will use Supertest to make HTTP requests to the running Express app instance (or a test instance) and Jest for assertions.

### `POST /api/v1/register`

*   **`describe('POST /api/v1/register', () => { ... });`**
    *   `it('should register a new voter successfully with valid ID', async () => { ... })`:
        *   Send a unique `anonymizedVoterId`.
        *   Assert 201 Created status.
        *   Assert response body structure matches V2 spec (contains `message` and `voter` object with `id`, `anonymizedVoterId`, `registrationTimestamp`, `isEligible`).
        *   Verify the voter is actually created in the test database.
    *   `it('should return 409 Conflict for duplicate anonymizedVoterId', async () => { ... })`:
        *   Register a voter.
        *   Attempt to register the same `anonymizedVoterId` again.
        *   Assert 409 Conflict status.
        *   Assert error response body structure (e.g., `{ "error": "This anonymizedVoterId is already registered." }`).
    *   `it('should return 400 Bad Request if anonymizedVoterId is missing', async () => { ... })`:
        *   Send request with no `anonymizedVoterId` in the body.
        *   Assert 400 Bad Request status.
        *   Assert error response body.
    *   `it('should return 400 Bad Request if anonymizedVoterId is empty or invalid format', async () => { ... })`:
        *   Send request with `anonymizedVoterId: ""`.
        *   (Optional) Send with an ID that doesn't match an expected format if such validation is added.
        *   Assert 400 Bad Request status.
        *   Assert error response body.

### `GET /api/v1/elections`

*   **`describe('GET /api/v1/elections', () => { ... });`**
    *   `beforeEach(async () => { /* Seed database with sample elections, some active, some not */ })`
    *   `it('should return a list of active elections', async () => { ... })`:
        *   Assert 200 OK status.
        *   Assert response body has an `elections` array.
        *   Assert each election object in the array matches the V2 spec structure (`id`, `electionCode`, `title`, `description`, `options`, `startTimestamp`, `endTimestamp`, `status`).
        *   Verify that only elections currently active (based on current time vs. `startTimestamp`/`endTimestamp` and `status='ACTIVE'`) are returned.
    *   `it('should return an empty list if no active elections are found', async () => { ... })`:
        *   Ensure no elections in the DB are currently active.
        *   Assert 200 OK status.
        *   Assert `elections` array in response is empty.
    *   `// (Future) it('should support pagination if implemented', async () => { ... });`

### `POST /api/v1/submitVote`

*   **`describe('POST /api/v1/submitVote', () => { ... });`**
    *   `let registeredVoterId, activeElectionId;`
    *   `beforeEach(async () => { /* Register a voter, create an active election, store their IDs */ })`
    *   `it('should successfully submit a vote for a registered voter and active election', async () => { ... })`:
        *   Send valid `anonymizedVoterId`, `electionId`, and `selectedOption`.
        *   Assert 201 Created status.
        *   Assert response body structure matches V2 spec (contains `message` and `vote` object).
        *   Verify the vote is recorded in the `Votes` table in the test database.
    *   `it('should return 409 Conflict when submitting a duplicate vote', async () => { ... })`:
        *   Submit a valid vote.
        *   Attempt to submit the same vote again (same voter, same election).
        *   Assert 409 Conflict status.
        *   Assert error response body.
    *   `it('should return 403 Forbidden if voter is not registered', async () => { ... })`:
        *   Send a non-existent `anonymizedVoterId`.
        *   Assert 403 Forbidden status.
        *   Assert error response body.
    *   `it('should return 403 Forbidden if voter is not eligible', async () => { ... })`:
        *   Seed a voter with `is_eligible = false`.
        *   Attempt to vote with this voter's ID.
        *   Assert 403 Forbidden status.
    *   `it('should return 404 Not Found if electionId does not exist', async () => { ... })`:
        *   Send a non-existent `electionId`.
        *   Assert 404 Not Found status.
    *   `it('should return 403 Forbidden if election is not active (e.g., past end_timestamp)', async () => { ... })`:
        *   Seed an election that is not currently active.
        *   Attempt to vote in this election.
        *   Assert 403 Forbidden status (or as per specific error for "election not open").
    *   `it('should return 400 Bad Request if selectedOption is invalid for the election', async () => { ... })`:
        *   Send a `selectedOption` that is not in the election's `options` array.
        *   Assert 400 Bad Request status.
    *   `it('should return 400 Bad Request for missing fields in request body', async () => { ... })`:
        *   Test with missing `anonymizedVoterId`, `electionId`, or `selectedOption`.
        *   Assert 400 Bad Request status for each case.

## Running Tests

To execute the test suite:

1.  Ensure your test database is configured and accessible (either via environment variables or defaults if using a tool like `pg-mem` that doesn't require external setup).
2.  Run the command from the `backend/` directory:
    ```sh
    npm test
    ```

This strategy provides a solid foundation for ensuring the backend API's quality and correctness. Tests should be regularly updated as new features are added or existing ones are modified.
