# Backend Logic Outline

This document outlines the necessary backend server functionalities to support the Minimum Viable Product (MVP) of the Android Biometric Voting App. The backend will primarily manage user registration (via anonymized IDs), election data, vote reception, and simulated blockchain interaction.

## Core Backend Functionalities (MVP):

1.  **Anonymized Voter ID Registration:**
    *   **Endpoint:** `POST /register`
    *   **Request Body:**
        *   `anonymizedVoterId`: The unique, anonymized identifier generated on the Android device.
    *   **Logic:**
        *   Receive the `anonymizedVoterId`.
        *   Validate the format of the ID (basic validation).
        *   Check if the `anonymizedVoterId` already exists in the database.
            *   If yes, return an appropriate error/message (e.g., "ID already registered").
            *   If no, store the `anonymizedVoterId` in a secure database (e.g., marking it as an eligible voter).
        *   Return a success or failure response to the app.
    *   **Database Table (Example - `Voters`):**
        *   `id` (Primary Key)
        *   `anonymized_voter_id` (Unique, Indexed)
        *   `registration_timestamp`

2.  **Provide List of Active Elections:**
    *   **Endpoint:** `GET /elections`
    *   **Request Body:** None (potentially parameters for pagination in the future).
    *   **Logic:**
        *   Retrieve the list of currently active elections from a database or a configuration file (for MVP, this can be hardcoded or from a simple DB table).
        *   Format the election data (e.g., election ID, title, description, options).
        *   Return the list of elections as a JSON response.
    *   **Database Table (Example - `Elections`):**
        *   `id` (Primary Key)
        *   `title`
        *   `description`
        *   `start_date`
        *   `end_date`
        *   `options` (JSON array or link to separate options table)

3.  **Submit Vote:**
    *   **Endpoint:** `POST /submitVote`
    *   **Request Body:**
        *   `anonymizedVoterId`: The voter's anonymized ID.
        *   `electionId`: The unique identifier for the election.
        *   `voteOption`: The option chosen by the voter (this might be an ID or the text of the option; consider how to store this consistently and potentially how to prevent analysis of vote distribution if options are too unique).
    *   **Logic:**
        1.  **Authentication/Authorization (Basic):** Verify that the `anonymizedVoterId` is a registered/valid ID (exists in the `Voters` table).
        2.  **Election Validity:** Check if `electionId` corresponds to an active and valid election.
        3.  **Prevent Double Voting:**
            *   Query the vote log/database to see if `anonymizedVoterId` has already submitted a vote for `electionId`.
            *   If a vote already exists, return an error (e.g., "Already voted in this election").
        4.  **Record Vote (Simulated Blockchain):**
            *   If the vote is valid (no double vote), record the vote details. For MVP, this means writing to a secure, append-only log file or a specific database table (`SimulatedBlockchainVotes`) that mimics an immutable ledger.
            *   The record should include: `anonymizedVoterId`, `electionId`, `voteOption` (or a hash of it for privacy), and a `timestamp`.
        5.  Return a success response to the app (e.g., "Vote recorded successfully").
    *   **Database Table (Example - `SimulatedBlockchainVotes`):**
        *   `id` (Primary Key)
        *   `anonymized_voter_id`
        *   `election_id`
        *   `vote_option_hash` (Hash of the chosen option for privacy)
        *   `submission_timestamp`
        *   (Constraint: Unique combination of `anonymized_voter_id` and `election_id`)

4.  **Login (Optional Backend Validation):**
    *   **Endpoint:** `POST /login` (Potentially)
    *   **Request Body:** `anonymizedVoterId`
    *   **Logic:**
        *   For MVP, primary authentication is on-device via fingerprint.
        *   This endpoint could serve as a secondary check to confirm if the `anonymizedVoterId` is known/registered in the backend system. This might be useful if the app needs to fetch user-specific settings or confirm eligibility status from the backend post-local authentication.
        *   However, for a pure MVP focused on on-device auth, this backend endpoint might be deferred. If implemented, it would simply check for the existence of the `anonymizedVoterId`.

## Technology Stack Considerations (Examples):

*   **Language/Framework:** Node.js (with Express.js), Python (with FastAPI or Flask), Java (with Spring Boot), Kotlin (with Ktor).
*   **Database:** PostgreSQL, MySQL, MongoDB. For MVP's simple needs, even SQLite could be used for the simulated parts if the backend is a single instance.
*   **Security:** Ensure all endpoints are protected (e.g., input validation, rate limiting, HTTPS).

## Future Considerations (Post-MVP):

*   Real blockchain integration: Interacting with a chosen blockchain platform (e.g., Ethereum, Polygon) via smart contracts.
*   More robust user management.
*   Administrator roles for managing elections.
*   Scalability and load balancing.
*   Advanced security measures (e.g., JWTs for session management if needed).
```
