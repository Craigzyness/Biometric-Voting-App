// backend/server.js
const express = require('express');
const { Pool } = require('pg');
const helmet = require('helmet'); // Import helmet
const morgan = require('morgan'); // Added for request logging
const rateLimit = require('express-rate-limit'); // Added for rate limiting
const app = express();
const PORT = process.env.PORT || 3000;

// Regex for validations
const uuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
const base64Regex = /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/;
const sha256HexRegex = /^[a-f0-9]{64}$/i;

// Apply Helmet middleware for security headers
app.use(helmet());

// Middleware to parse JSON bodies
app.use(express.json());

// Request logging middleware (morgan)
app.use(morgan('dev'));

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
        console.log('Checking/Creating "Voters" table...');
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
        console.log('Checking/Creating "Elections" table...');
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

        // Add Composite Index for Elections table
        console.log('Checking/Creating index "idx_elections_status_start_end" on "Elections" table...');
        await client.query(`
            CREATE INDEX IF NOT EXISTS idx_elections_status_start_end ON Elections (status, start_timestamp, end_timestamp);
        `);
        console.log('Index "idx_elections_status_start_end" on "Elections" table checked/created successfully.');

        // Create Votes Table
        console.log('Checking/Creating "Votes" table...');
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
        console.log('Checking if "Elections" table needs sample data...');
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

// Configure Rate Limiters
const generalApiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
    message: { error: "Too many requests from this IP, please try again after 15 minutes." },
    standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
    legacyHeaders: false, // Disable the `X-RateLimit-*` headers
});

const registrationLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 5, // limit each IP to 5 registration attempts per windowMs
    message: { error: "Too many registration attempts from this IP, please try again after 15 minutes." },
    standardHeaders: true,
    legacyHeaders: false,
});

const voteSubmissionLimiter = rateLimit({
    windowMs: 10 * 60 * 1000, // 10 minutes
    max: 10, // limit each IP to 10 vote submission attempts per windowMs
    message: { error: "Too many vote submission attempts from this IP, please try again after 10 minutes." },
    standardHeaders: true,
    legacyHeaders: false,
});

// Apply general limiter to all routes on apiRouter first
apiRouter.use(generalApiLimiter);

// POST /api/v1/register
// Apply specific stricter limiter for registration
apiRouter.post('/register', registrationLimiter, async (req, res) => {
    const { anonymizedVoterId } = req.body;

    // Validation for anonymizedVoterId
    if (!anonymizedVoterId || typeof anonymizedVoterId !== 'string' || anonymizedVoterId.trim() === '') {
        return res.status(400).json({ error: "Invalid or missing anonymizedVoterId." });
    }
    const trimmedAnonymizedVoterId = anonymizedVoterId.trim();
    if (trimmedAnonymizedVoterId.length > 255) {
        return res.status(400).json({ error: "anonymizedVoterId must not exceed 255 characters." });
    }
    if (!sha256HexRegex.test(trimmedAnonymizedVoterId)) {
        return res.status(400).json({ error: "anonymizedVoterId must be a valid 64-character hex string." });
    }

    try {
        // Check if voter already exists
        let result = await pool.query('SELECT * FROM Voters WHERE anonymized_voter_id = $1', [trimmedAnonymizedVoterId]);
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
// Apply specific stricter limiter for vote submission
apiRouter.post('/submitVote', voteSubmissionLimiter, async (req, res) => {
    const { anonymizedVoterId, electionId, selectedOption, encryptedProof, iv } = req.body;

    // --- Input Validation ---
    const errors = [];

    // anonymizedVoterId Validation
    if (!anonymizedVoterId || typeof anonymizedVoterId !== 'string' || anonymizedVoterId.trim() === '') {
        errors.push("anonymizedVoterId is required and must be a non-empty string.");
    } else {
        const trimmedVoterId = anonymizedVoterId.trim();
        if (trimmedVoterId.length > 255) {
            errors.push("anonymizedVoterId must not exceed 255 characters.");
        }
        if (!sha256HexRegex.test(trimmedVoterId)) {
            errors.push("anonymizedVoterId must be a valid 64-character hex string.");
        }
    }

    // electionId Validation
    if (!electionId || typeof electionId !== 'string' || electionId.trim() === '') {
        errors.push("electionId is required and must be a non-empty string.");
    } else {
        const trimmedElectionId = electionId.trim();
        if (!uuidRegex.test(trimmedElectionId)) {
            errors.push("electionId must be a valid UUID.");
        }
    }

    // selectedOption Validation
    if (!selectedOption || typeof selectedOption !== 'string' || selectedOption.trim() === '') {
        errors.push("selectedOption is required and must be a non-empty string.");
    } else {
        const trimmedSelectedOption = selectedOption.trim();
        if (trimmedSelectedOption.length > 255) {
            errors.push("selectedOption must not exceed 255 characters.");
        }
    }

    // encryptedProof Validation (optional)
    const hasEncryptedProof = encryptedProof !== undefined && encryptedProof !== null;
    let proofIsNonEmptyString = false;
    if (hasEncryptedProof) {
        if (typeof encryptedProof !== 'string') {
            errors.push("If provided, encryptedProof must be a string.");
        } else if (encryptedProof.trim() === '') {
            errors.push("If provided as a string, encryptedProof must not be empty.");
        } else {
            proofIsNonEmptyString = true;
            if (!base64Regex.test(encryptedProof.trim())) {
                errors.push("If provided, encryptedProof must be a valid Base64 encoded string.");
            }
        }
    }

    // iv Validation (optional)
    const hasIv = iv !== undefined && iv !== null;
    let ivIsNonEmptyString = false;
    if (hasIv) {
        if (typeof iv !== 'string') {
            errors.push("If provided, iv must be a string.");
        } else if (iv.trim() === '') {
            errors.push("If provided as a string, iv must not be empty.");
        } else {
            ivIsNonEmptyString = true;
            if (!base64Regex.test(iv.trim())) {
                errors.push("If provided, iv must be a valid Base64 encoded string.");
            }
        }
    }

    // Conditional Presence for encryptedProof and iv
    if (proofIsNonEmptyString !== ivIsNonEmptyString) {
        errors.push("encryptedProof and iv must be provided together as non-empty strings, or not at all.");
    }

    if (errors.length > 0) {
        return res.status(400).json({ error: errors.join(" ") });
    }
    // --- End of Input Validation ---

    const finalAnonymizedVoterId = anonymizedVoterId.trim();
    const finalElectionId = electionId.trim();
    const finalSelectedOption = selectedOption.trim();
    const finalEncryptedProof = proofIsNonEmptyString ? encryptedProof.trim() : null;
    const finalIv = ivIsNonEmptyString ? iv.trim() : null;

    try {
        // 1. Get internal voter_id and check eligibility
        const voterResult = await pool.query('SELECT id, is_eligible FROM Voters WHERE anonymized_voter_id = $1', [finalAnonymizedVoterId]);
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
        const electionResult = await pool.query('SELECT id, options, start_timestamp, end_timestamp, status FROM Elections WHERE id = $1', [finalElectionId]);
        if (electionResult.rows.length === 0) {
            return res.status(404).json({ error: "Election not found." });
        }
        const election = electionResult.rows[0];
        const now = new Date();
        if (now < election.start_timestamp || now > election.end_timestamp || election.status !== 'ACTIVE') {
            return res.status(403).json({ error: "Election is not currently active or open for voting." });
        }
        // Ensure options is an array before checking includes. DB stores JSONB, which pg driver parses.
        if (!Array.isArray(election.options) || !election.options.includes(finalSelectedOption)) {
            return res.status(400).json({ error: "Invalid option selected for this election." });
        }
        const internalElectionId = election.id;

        // 3. Attempt to insert vote (double voting check by DB unique constraint)
        try {
            // Use validated and trimmed values: finalSelectedOption, finalEncryptedProof, finalIv
            const voteInsertResult = await pool.query(
                'INSERT INTO Votes (voter_id, election_id, selected_option_value, encrypted_proof, iv) VALUES ($1, $2, $3, $4, $5) RETURNING id, election_id, selected_option_value, cast_at_timestamp',
                [internalVoterId, internalElectionId, finalSelectedOption, finalEncryptedProof, finalIv]
            );
            const newVote = voteInsertResult.rows[0];

            // Log core vote info for simulation, excluding sensitive cryptographic proof details
            const logDataForBlockchain = {
                voteId: newVote.id, // Internal DB id for the vote
                anonymizedVoterId: finalAnonymizedVoterId, // Original anonymized ID from request
                electionId: newVote.election_id, // Internal election ID
                selectedOption: newVote.selected_option_value,
                castAtTimestamp: newVote.cast_at_timestamp,
                // encryptedProof and iv are intentionally NOT logged here for security, even in simulation.
                // Their presence in the DB is the important part.
            };
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
    // PRODUCTION NOTE:
    // In a production environment, this Node.js server should run behind a
    // reverse proxy (e.g., Nginx, Apache) that handles HTTPS termination,
    // SSL certificates, and potentially load balancing.
    // The following app.listen is suitable for development and
    // for when the reverse proxy forwards plain HTTP requests to this server internally.
    app.listen(PORT, async () => {
        await initializeDatabase(); // Initialize DB and tables before starting server
        console.log(`Backend server listening on port ${PORT}`);
        console.log('Using PostgreSQL for data persistence.');
        console.log(`Using database: ${DB_NAME}`);
        console.log('API endpoints are available under /api/v1');
    });
}

module.exports = { app, pool }; // Export app and pool for testing
