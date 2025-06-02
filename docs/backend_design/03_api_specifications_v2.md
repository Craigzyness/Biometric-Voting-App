# API Endpoint Specifications (V2 - Real Backend)

Date: (Refer to Git history for the last update date of this document)

This document refines the API endpoint specifications for the Biometric Voting App, assuming interaction with a persistent relational database (e.g., PostgreSQL) as defined in `02_database_schema.md`.

## Common Considerations:

*   **Base URL:** `/api/v1` (Example)
*   **Content Type:** `application/json` for requests and responses.
*   **Authentication:** For MVP, the primary authentication is the `anonymized_voter_id` itself, which is generated on the client. The backend verifies if this ID is registered for certain actions. Future versions might introduce session tokens or other mechanisms if direct user-specific (but still anonymous) data needs to be accessed beyond just voting.
*   **Error Responses:**
    *   `400 Bad Request`: Invalid input, missing fields.
        ```json
        { "error": "Descriptive error message about what was wrong." }
        ```
    *   `401 Unauthorized`: Authentication credentials missing or invalid (less relevant for MVP if `anonymized_voter_id` is the main identifier and its absence is a 400 or 403).
    *   `403 Forbidden`: Authenticated (ID known) but not permitted to perform action (e.g., voter not eligible, or trying to access admin functions).
        ```json
        { "error": "User does not have permission for this action." }
        ```
    *   `404 Not Found`: Resource not found (e.g., specific election).
        ```json
        { "error": "Resource not found." }
        ```
    *   `409 Conflict`: Action conflicts with current state (e.g., ID already registered, already voted).
        ```json
        { "error": "Conflict with existing resource or state." }
        ```
    *   `500 Internal Server Error`: Unexpected server-side error.
        ```json
        { "error": "An unexpected error occurred on the server." }
        ```

---

## 1. Voter Registration

### `POST /api/v1/register`

Registers a new voter based on their client-generated anonymized ID.

*   **Request Body:**
    ```json
    {
      "anonymizedVoterId": "string (sha256_hex_representation)"
    }
    ```
*   **Success Response (201 Created):**
    ```json
    {
      "message": "Voter registered successfully.",
      "voter": {
        "id": "uuid_or_bigint_from_db",
        "anonymizedVoterId": "string",
        "registrationTimestamp": "iso_timestamp_string",
        "isEligible": true
      }
    }
    ```
*   **Error Responses:**
    *   `400 Bad Request`: If `anonymizedVoterId` is missing, empty, or invalid format.
    *   `409 Conflict`: If `anonymizedVoterId` already exists in the `Voters` table.
    *   `500 Internal Server Error`: If database interaction fails.
*   **Logic:**
    1.  Validate `anonymizedVoterId` from request body (must be present, non-empty string).
    2.  Check if `anonymizedVoterId` already exists in the `Voters` table.
        *   If yes, return `409 Conflict`.
    3.  If not, insert a new record into the `Voters` table:
        *   `anonymized_voter_id` = provided `anonymizedVoterId`.
        *   `registration_timestamp` = current server timestamp.
        *   `is_eligible` = `TRUE` (default).
    4.  Return `201 Created` with the newly created voter record (excluding sensitive internal details if any, but `id` from DB is fine).

---

## 2. Get Available Elections

### `GET /api/v1/elections`

Retrieves a list of currently available/active elections.

*   **Request Body:** None.
*   **Query Parameters (Optional for future expansion):**
    *   `status=active` (e.g., to filter only active elections)
    *   `limit=10`
    *   `offset=0`
*   **Success Response (200 OK):**
    ```json
    {
      "elections": [
        {
          "id": "uuid_or_bigint_from_db",
          "electionCode": "string",
          "title": "string",
          "description": "string (nullable)",
          "options": ["string", "string", ...], // or [{"id": "opt_id", "text": "Option Text"}, ...]
          "startTimestamp": "iso_timestamp_string",
          "endTimestamp": "iso_timestamp_string",
          "status": "string (e.g., 'ACTIVE')"
        }
        // ... more elections
      ]
    }
    ```
*   **Error Responses:**
    *   `404 Not Found`: If no elections are available matching criteria.
    *   `500 Internal Server Error`: If database interaction fails.
*   **Logic:**
    1.  Query the `Elections` table.
    2.  For MVP, initially, might return all elections.
    3.  **Refinement:** Filter elections where `start_timestamp` <= current server time AND `end_timestamp` >= current server time AND `status` = 'ACTIVE'.
    4.  Order results (e.g., by `start_timestamp` or `title`).
    5.  Format and return the list of elections. If no elections match, return `200 OK` with an empty list or `404 Not Found` (consistency needed). Let's go with `200 OK` and an empty list.

---

## 3. Submit Vote

### `POST /api/v1/submitVote`

Submits a vote for a specific election by a registered voter.

*   **Request Body:**
    ```json
    {
      "anonymizedVoterId": "string",
      "electionId": "uuid_or_bigint_or_election_code_from_db", // Assuming direct UUID for electionId as implemented
      "selectedOption": "string", // The exact text of the chosen option
      "encryptedProof": "string (nullable, base64 encoded)",
      "iv": "string (nullable, base64 encoded)"
    }
    ```
*   **Success Response (201 Created):**
    ```json
    {
      "message": "Vote submitted successfully.",
      "vote": {
        "voteId": "uuid_or_bigint_from_db",
        "electionId": "uuid_or_bigint_or_election_code_from_db",
        "selectedOption": "string", // Or hash, or index, confirming what was recorded
        "castAtTimestamp": "iso_timestamp_string"
      }
    }
    ```
*   **Error Responses:**
    *   `400 Bad Request`: Missing/invalid fields, or `selectedOption` not valid for the given `electionId`.
    *   `403 Forbidden`: If `anonymizedVoterId` is not found in the `Voters` table (i.e., not registered) or if `Voters.is_eligible` is false.
    *   `404 Not Found`: If `electionId` does not correspond to a valid, active election (or an election that is currently open for voting).
    *   `409 Conflict`: If the `anonymizedVoterId` has already voted in this `electionId` (double voting attempt).
    *   `500 Internal Server Error`: If database interaction fails.
*   **Logic:**
    1.  Validate request body: `anonymizedVoterId`, `electionId`, `selectedOption` must be present and valid types.
    2.  Query `Voters` table to find the internal `voter_id` based on `anonymizedVoterId`.
        *   If not found or `is_eligible` is false, return `403 Forbidden`.
    3.  Query `Elections` table to find the election by `electionId` (or `election_code`).
        *   If not found, return `404 Not Found`.
        *   Verify the election is currently active for voting (current server time is between `start_timestamp` and `end_timestamp` AND `status` is 'ACTIVE'). If not, return `403 Forbidden` or `400 Bad Request` (e.g., "Election is not currently open for voting").
        *   Verify `selectedOption` is a valid option within `Elections.options` for this election. If not, return `400 Bad Request`.
    4.  Check `Votes` table for an existing record with the internal `voter_id` and internal `election_id`.
        *   If a record exists, return `409 Conflict` (double voting).
    5.  If all checks pass:
        *   Insert a new record into the `Votes` table:
            *   `voter_id` = internal `voter_id`.
            *   `election_id` = internal `election_id`.
            *   `selected_option_value` = `selectedOption`.
            *   `encrypted_proof` = `encryptedProof` from request (if provided, store as base64 text or after decoding if DB stores bytes).
            *   `iv` = `iv` from request (if provided, store as base64 text or after decoding).
            *   `cast_at_timestamp` = current server timestamp.
        *   **(Simulated Blockchain Interaction):** Log the vote details (excluding sensitive proof details from direct log output for security). Example: `console.log("SIMULATING BLOCKCHAIN RECORD: VoteID X for Election Y by Voter Z, Proof Stored")`.
        *   Return `201 Created` with details of the submitted vote (again, excluding proof from response).

---

This V2 specification provides a more robust definition for backend development against a persistent database.
