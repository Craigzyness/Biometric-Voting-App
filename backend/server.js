// backend/server.js
const express = require('express');
const { Pool } = require('pg');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware to parse JSON bodies
app.use(express.json());

// Database Configuration from Environment Variables with Defaults
const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = process.env.DB_PORT || 5432;
const DB_USER = process.env.DB_USER || 'your_db_user'; // Default placeholder
const DB_PASSWORD = process.env.DB_PASSWORD || 'your_db_password'; // Default placeholder

const DB_NAME_DEFAULT = 'biometric_voting_app_db';
const DB_NAME_TEST = process.env.DB_TEST_NAME || 'biometric_voting_app_test_db'; // Test specific DB name
const DB_NAME = process.env.NODE_ENV === 'test' ? DB_NAME_TEST : (process.env.DB_NAME || DB_NAME_DEFAULT);

// Database Connection Pool
const pool = new Pool({
    user: DB_USER,
    host: DB_HOST,
    database: DB_NAME,
    password: DB_PASSWORD,
    port: parseInt(DB_PORT), // Ensure port is an integer
});

// Test DB Connection and Create Tables
async function initializeDatabase() {
    console.log(`Attempting to connect to database: ${DB_NAME} on ${DB_HOST}:${DB_PORT} as user ${DB_USER}`);
    try {
        const client = await pool.connect();
        console.log('Connected to PostgreSQL database successfully!');

        // Create Voters Table
        await client.query(`
            CREATE TABLE IF NOT EXISTS Voters (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                anonymized_voter_id VARCHAR(255) NOT NULL UNIQUE,
                registration_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        `);
        console.log('Table "Voters" checked/created successfully.');

        // Create Elections Table
        await client.query(`
            CREATE TABLE IF NOT EXISTS Elections (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                election_code VARCHAR(64) NOT NULL UNIQUE,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                options JSONB NOT NULL,
                start_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                end_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT check_election_dates CHECK (start_timestamp < end_timestamp)
            );
        `);
        console.log('Table "Elections" checked/created successfully.');

        // Create Votes Table
        await client.query(`
            CREATE TABLE IF NOT EXISTS Votes (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                voter_id UUID NOT NULL REFERENCES Voters(id),
                election_id UUID NOT NULL REFERENCES Elections(id),
                selected_option_value TEXT NOT NULL,
                encrypted_proof TEXT NULL,
                iv TEXT NULL,
                cast_at_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_valid BOOLEAN NOT NULL DEFAULT TRUE,
                UNIQUE (voter_id, election_id) -- Critical constraint
            );
        `);
        console.log('Table "Votes" checked/created successfully.');

        // Add sample election data if table is empty
        const { rows } = await client.query('SELECT COUNT(*) AS count FROM Elections');
        if (rows[0].count === '0') {
            console.log('No elections found, inserting sample data...');
            const sampleElections = [
                {
                    election_code: "PRES2024", title: "Student Council President 2024", description: "Vote for the next Student Council President.",
                    options: JSON.stringify(["Alice Wonderland", "Bob The Builder", "Charlie Brown"]),
                    start_timestamp: "2024-08-01T00:00:00Z", end_timestamp: "2024-08-15T23:59:59Z", status: "ACTIVE"
                },
                {
                    election_code: "CAFE2024", title: "Referendum: New Cafeteria Menu", description: "Should the cafeteria menu be updated?",
                    options: JSON.stringify(["Yes, update the menu", "No, keep the current menu"]),
                    start_timestamp: "2024-08-05T00:00:00Z", end_timestamp: "2024-08-10T23:59:59Z", status: "ACTIVE"
                },
                {
                    election_code: "MASCOT2024", title: "Mascot Naming Contest", description: "Choose the new official mascot name.",
                    options: JSON.stringify(["Sparky the Dragon", "Captain Comet", "Wally the Wombat"]),
                    start_timestamp: "2024-07-20T00:00:00Z", end_timestamp: "2024-09-05T23:59:59Z", status: "ACTIVE" // Extended end date for testing
                }
            ];
            for (const election of sampleElections) {
                await client.query(
                    `INSERT INTO Elections (election_code, title, description, options, start_timestamp, end_timestamp, status)
                     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
                    [election.election_code, election.title, election.description, election.options, election.start_timestamp, election.end_timestamp, election.status]
                );
            }
            console.log('Sample elections inserted.');
        }

        client.release();
    } catch (err) {
        console.error('Failed to initialize database or create tables:', err.stack);
        // process.exit(1); // Exit if DB init fails, or handle more gracefully
    }
}


// --- API Routes ---
const apiRouter = express.Router(); // Create a router for /api/v1

// POST /api/v1/register
apiRouter.post('/register', async (req, res) => {
    const { anonymizedVoterId } = req.body;

    if (!anonymizedVoterId || typeof anonymizedVoterId !== 'string' || anonymizedVoterId.trim() === '') {
        return res.status(400).json({ error: "Invalid or missing anonymizedVoterId." });
    }

    try {
        // Check if voter already exists
        let result = await pool.query('SELECT * FROM Voters WHERE anonymized_voter_id = $1', [anonymizedVoterId.trim()]);
        if (result.rows.length > 0) {
            return res.status(409).json({ error: "This anonymizedVoterId is already registered." });
        }

        // Insert new voter
        result = await pool.query(
            'INSERT INTO Voters (anonymized_voter_id) VALUES ($1) RETURNING id, anonymized_voter_id, registration_timestamp, is_eligible',
            [anonymizedVoterId.trim()]
        );
        const newVoter = result.rows[0];

        console.log(`New voter registered: ${newVoter.anonymized_voter_id}`);
        res.status(201).json({
            message: "Voter registered successfully.",
            voter: {
                id: newVoter.id,
                anonymizedVoterId: newVoter.anonymized_voter_id,
                registrationTimestamp: newVoter.registration_timestamp,
                isEligible: newVoter.is_eligible
            }
        });
    } catch (err) {
        console.error('Error during /register:', err);
        res.status(500).json({ error: "An unexpected error occurred on the server." });
    }
});

// GET /api/v1/elections
apiRouter.get('/elections', async (req, res) => {
    try {
        const now = new Date();
        // Filter for active elections based on time and status
        const result = await pool.query(
            "SELECT id, election_code, title, description, options, start_timestamp, end_timestamp, status FROM Elections WHERE start_timestamp <= $1 AND end_timestamp >= $1 AND status = 'ACTIVE' ORDER BY start_timestamp DESC",
            [now]
        );

        if (result.rows.length === 0) {
            // Consistent with spec: 200 OK with empty list
            return res.status(200).json({ elections: [] });
        }

        // Map database row names to API spec names (e.g., start_timestamp to startTimestamp)
        const electionsForApi = result.rows.map(election => ({
            id: election.id,
            electionCode: election.election_code,
            title: election.title,
            description: election.description,
            options: election.options, // Assuming options are stored correctly as JSON array strings
            startTimestamp: election.start_timestamp,
            endTimestamp: election.end_timestamp,
            status: election.status
        }));

        res.status(200).json({ elections: electionsForApi });
    } catch (err) {
        console.error('Error during /elections:', err);
        res.status(500).json({ error: "An unexpected error occurred on the server." });
    }
});

// POST /api/v1/submitVote
apiRouter.post('/submitVote', async (req, res) => {
    const { anonymizedVoterId, electionId, selectedOption } = req.body;

    if (!anonymizedVoterId || !electionId || !selectedOption ||
        typeof anonymizedVoterId !== 'string' || typeof electionId !== 'string' || typeof selectedOption !== 'string') {
        return res.status(400).json({ error: "Missing or invalid fields: anonymizedVoterId, electionId, selectedOption." });
    }

    try {
        // 1. Get internal voter_id and check eligibility
        const voterResult = await pool.query('SELECT id, is_eligible FROM Voters WHERE anonymized_voter_id = $1', [anonymizedVoterId]);
        if (voterResult.rows.length === 0) {
            return res.status(403).json({ error: "Voter not registered." });
        }
        const voter = voterResult.rows[0];
        if (!voter.is_eligible) {
            return res.status(403).json({ error: "Voter is not eligible to vote." });
        }
        const internalVoterId = voter.id;

        // 2. Get election details and check if active and option is valid
        // Assuming electionId from request is the UUID 'id' from Elections table.
        // If election_code is used by client, query by election_code instead.
        const electionResult = await pool.query('SELECT id, options, start_timestamp, end_timestamp, status FROM Elections WHERE id = $1', [electionId]);
        if (electionResult.rows.length === 0) {
            return res.status(404).json({ error: "Election not found." });
        }
        const election = electionResult.rows[0];
        const now = new Date();
        if (now < election.start_timestamp || now > election.end_timestamp || election.status !== 'ACTIVE') {
            return res.status(403).json({ error: "Election is not currently active or open for voting." });
        }
        // Ensure options is an array before checking includes. DB stores JSONB, which pg driver parses.
        if (!Array.isArray(election.options) || !election.options.includes(selectedOption)) {
            return res.status(400).json({ error: "Invalid option selected for this election." });
        }
        const internalElectionId = election.id;

        // 3. Attempt to insert vote (double voting check by DB unique constraint)
        try {
            const { encryptedProof, iv } = req.body; // Get new fields from request body
            const voteInsertResult = await pool.query(
                'INSERT INTO Votes (voter_id, election_id, selected_option_value, encrypted_proof, iv) VALUES ($1, $2, $3, $4, $5) RETURNING id, election_id, selected_option_value, cast_at_timestamp',
                [internalVoterId, internalElectionId, selectedOption, encryptedProof || null, iv || null]
            );
            const newVote = voteInsertResult.rows[0];

            // Log more info for simulation, including proof if present
            const logDataForBlockchain = {
                voteId: newVote.id,
                anonymizedVoterId: anonymizedVoterId,
                electionId: newVote.election_id,
                selectedOption: newVote.selected_option_value,
                castAtTimestamp: newVote.cast_at_timestamp,
            };
            if (encryptedProof && iv) {
                logDataForBlockchain.encryptedProof = encryptedProof; // For simulation only
                logDataForBlockchain.iv = iv; // For simulation only
            }
            console.log(`SIMULATING BLOCKCHAIN RECORD (Append-Only Log Entry): ${JSON.stringify(logDataForBlockchain)}`);

            res.status(201).json({
                message: "Vote submitted successfully.",
                vote: {
                    voteId: newVote.id,
                    electionId: newVote.election_id, // This is the internal UUID
                    selectedOption: newVote.selected_option_value,
                    castAtTimestamp: newVote.cast_at_timestamp
                }
            });
        } catch (dbErr) {
            if (dbErr.code === '23505') { // Unique violation (double voting)
                return res.status(409).json({ error: "Already voted in this election." });
            }
            throw dbErr; // Re-throw other DB errors to be caught by outer try-catch
        }
    } catch (err) {
        console.error('Error during /submitVote:', err);
        res.status(500).json({ error: "An unexpected error occurred on the server." });
    }
});

// Mount the API router under /api/v1
app.use('/api/v1', apiRouter);

// Basic route for testing if the server is up (outside /api/v1)
app.get('/', (req, res) => {
    res.send('Biometric Voting App Backend is running with PostgreSQL!');
});


// --- End of API Routes ---

// Start server only if not in test environment
if (process.env.NODE_ENV !== 'test') {
    app.listen(PORT, async () => {
        await initializeDatabase(); // Initialize DB and tables before starting server
        console.log(`Backend server listening on port ${PORT}`);
        console.log('Using PostgreSQL for data persistence.');
        console.log(`Using database: ${DB_NAME}`);
        console.log('API endpoints are available under /api/v1');
    });
}

module.exports = { app, pool }; // Export app and pool for testing
