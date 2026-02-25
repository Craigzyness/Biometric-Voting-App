// backend/server.js
const express = require('express');
const { Pool } = require('pg');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const logger = require('./logger');
const playIntegrityVerifier = require('./play_integrity_verifier'); // Added Play Integrity Verifier

const app = express();
const PORT = process.env.PORT || 3000;

// Regex for validations
const uuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
const base64Regex = /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/;
const sha256HexRegex = /^[a-f0-9]{64}$/i;

app.use(helmet());
app.use(express.json());
app.use(morgan(process.env.NODE_ENV === 'development' ? 'dev' : 'short'));


// Request logging middleware (morgan) - will log to console independently of Winston for now
// For production, consider using a format that doesn't log PII or using morgan with winston stream.
app.use(morgan(process.env.NODE_ENV === 'development' ? 'dev' : 'short'));


// Database Configuration from Environment Variables with Defaults
const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = process.env.DB_PORT || 5432;
const DB_USER = process.env.DB_USER || 'your_db_user';
const DB_PASSWORD = process.env.DB_PASSWORD || 'your_db_password';
const DB_NAME_DEFAULT = 'biometric_voting_app_db';
const DB_NAME_TEST = process.env.DB_TEST_NAME || 'biometric_voting_app_test_db';
const DB_NAME = process.env.NODE_ENV === 'test' ? DB_NAME_TEST : (process.env.DB_NAME || DB_NAME_DEFAULT);

const pool = new Pool({
    user: DB_USER,
    host: DB_HOST,
    database: DB_NAME,
    password: DB_PASSWORD,
    port: parseInt(DB_PORT),
});

// Helper function to get a loggable version of an ID
function getLoggableId(id) {
  const isProduction = process.env.NODE_ENV === 'production';
  // Check if current effective log level is verbose enough to show full ID in prod
  const productionLogLevelIsVerbose = () => {
    // Use the logger's actual effective level for comparison
    const currentProdLogLevel = logger.level;
    const verboseLevels = ['http', 'verbose', 'debug', 'silly'];
    return verboseLevels.includes(currentProdLogLevel);
  };

  if (!isProduction || (isProduction && productionLogLevelIsVerbose())) {
    return id;
  }
  // If id is undefined or null, handle that gracefully
  if (id === undefined || id === null) {
    return '[ID_NOT_PROVIDED]';
  }
  // Ensure id is a string before calling substring
  const idStr = String(id);
  return `${idStr.substring(0, 8)}...[REDACTED_FOR_PROD_INFO_LOG]`;
}

async function initializeDatabase() {
    logger.info(`Attempting to connect to database: ${DB_NAME} on ${DB_HOST}:${DB_PORT} as user ${DB_USER}`);
    try {
        const client = await pool.connect();
        logger.info('Connected to PostgreSQL database successfully!');
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
        logger.info('Table "Voters" checked/created successfully.');
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
        logger.info('Table "Elections" checked/created successfully.');
        await client.query(`
            CREATE INDEX IF NOT EXISTS idx_elections_status_start_end ON Elections (status, start_timestamp, end_timestamp);
        `);
        logger.info('Index "idx_elections_status_start_end" on "Elections" table checked/created successfully.');
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
                UNIQUE (voter_id, election_id)
            );
        `);
        logger.info('Table "Votes" checked/created successfully.');
        const { rows } = await client.query('SELECT COUNT(*) AS count FROM Elections');
        if (rows[0].count === '0') {
            logger.info('No elections found, inserting sample data...');
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
                    start_timestamp: "2024-07-20T00:00:00Z", end_timestamp: "2024-09-05T23:59:59Z", status: "ACTIVE"
                }
            ];
            for (const election of sampleElections) {
                await client.query(
                    `INSERT INTO Elections (election_code, title, description, options, start_timestamp, end_timestamp, status)
                     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
                    [election.election_code, election.title, election.description, election.options, election.start_timestamp, election.end_timestamp, election.status]
                );
            }
            logger.info('Sample elections inserted.');
        }
        client.release();
    } catch (err) {
        logger.error('Failed to initialize database or create tables:', err);
    }
}

const apiRouter = express.Router();

const generalApiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // Limit each IP to 100 requests per `window` (here, per 15 minutes)
    standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
    legacyHeaders: false, // Disable the `X-RateLimit-*` headers
});

const registrationLimiter = rateLimit({
    windowMs: 60 * 60 * 1000, // 1 hour
    max: 5,
    message: "Too many accounts created from this IP, please try again after an hour"
});

const voteSubmissionLimiter = rateLimit({
    windowMs: 60 * 60 * 1000, // 1 hour
    max: 5,
    message: "Too many votes submitted from this IP, please try again after an hour"
});

// Middleware to handle validation errors
const handleValidationErrors = (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        const errorMessages = errors.array().map(err => err.msg).join(' ');
        logger.warn('Validation error:', { errors: errors.array(), body: req.body });
        return res.status(400).json({ error: errorMessages });
    }
    next();
};

apiRouter.use(generalApiLimiter);

// Validation rules for /register
const registerValidation = [
    body('anonymizedVoterId')
        .notEmpty().withMessage('anonymizedVoterId is required and must be a non-empty string.')
        .isString().withMessage('anonymizedVoterId must be a string.')
        .trim()
        .isLength({ max: 255 }).withMessage('anonymizedVoterId must not exceed 255 characters.')
        .matches(sha256HexRegex).withMessage('anonymizedVoterId must be a valid 64-character hex string.'),
];

apiRouter.post('/register', registrationLimiter, registerValidation, handleValidationErrors, async (req, res) => {
    const { anonymizedVoterId } = req.body;
    const finalNormalizedVoterId = anonymizedVoterId.trim().toLowerCase();

    try {
        let result = await pool.query('SELECT * FROM Voters WHERE anonymized_voter_id = $1', [finalNormalizedVoterId]);
        if (result.rows.length > 0) {
            logger.warn(`Attempt to register existing voter: ${getLoggableId(finalNormalizedVoterId)}`);
            return res.status(409).json({ error: "This anonymizedVoterId is already registered." });
        }
        result = await pool.query(
            'INSERT INTO Voters (anonymized_voter_id) VALUES ($1) RETURNING id, anonymized_voter_id, registration_timestamp, is_eligible',
            [finalNormalizedVoterId]
        );
        const newVoter = result.rows[0];
        logger.info(`New voter registered: ${getLoggableId(newVoter.anonymized_voter_id)} (DB ID: ${newVoter.id})`);
        res.status(201).json({
            message: "Voter registered successfully.",
            voter: {
                id: newVoter.id,
                anonymizedVoterId: newVoter.anonymized_voter_id, // Return full ID in response
                registrationTimestamp: newVoter.registration_timestamp,
                isEligible: newVoter.is_eligible
            }
        });
    } catch (err) {
        logger.error('Error during /register', { anonymizedVoterId: getLoggableId(finalNormalizedVoterId), error: err.message, stack: err.stack, detail: err.detail, code: err.code });
        res.status(500).json({ error: "An unexpected error occurred on the server." });
    }
});

apiRouter.get('/elections', async (req, res) => {
    const { anonymizedVoterId: queryAnonymizedVoterId } = req.query;
    let internalVoterId = null;
    const votedElectionIds = new Set();

    if (queryAnonymizedVoterId) {
        if (typeof queryAnonymizedVoterId !== 'string' || queryAnonymizedVoterId.trim() === '') {
            logger.warn('Invalid /elections request: anonymizedVoterId provided but empty or not a string.', { query: req.query });
            return res.status(400).json({ error: "anonymizedVoterId must be a non-empty string if provided." });
        }
        const trimmedQueryAnonymizedVoterId = queryAnonymizedVoterId.trim();
        if (trimmedQueryAnonymizedVoterId.length > 255) {
            logger.warn(`Invalid /elections request: anonymizedVoterId too long. ID: ${getLoggableId(trimmedQueryAnonymizedVoterId)}`);
            return res.status(400).json({ error: "anonymizedVoterId must not exceed 255 characters." });
        }
        if (!sha256HexRegex.test(trimmedQueryAnonymizedVoterId)) {
            logger.warn(`Invalid /elections request: anonymizedVoterId invalid format. ID: ${getLoggableId(trimmedQueryAnonymizedVoterId)}`);
            return res.status(400).json({ error: "anonymizedVoterId must be a valid 64-character hex string if provided." });
        }
        const normalizedQueryVoterId = trimmedQueryAnonymizedVoterId.toLowerCase();
        try {
            const voterResult = await pool.query('SELECT id FROM Voters WHERE anonymized_voter_id = $1', [normalizedQueryVoterId]);
            if (voterResult.rows.length > 0) {
                internalVoterId = voterResult.rows[0].id;
                logger.debug(`Fetching votes for voterId (DB ID): ${internalVoterId} (anonymized: ${getLoggableId(normalizedQueryVoterId)}) for /elections endpoint.`);
                const votesResult = await pool.query('SELECT election_id FROM Votes WHERE voter_id = $1', [internalVoterId]);
                votesResult.rows.forEach(row => votedElectionIds.add(row.election_id));
                logger.debug(`Voter (DB ID): ${internalVoterId} has voted in ${votedElectionIds.size} elections.`);
            } else {
                logger.info(`No voter found for anonymizedVoterId: ${getLoggableId(normalizedQueryVoterId)} in /elections query.`);
            }
        } catch (err) {
            logger.error('Error fetching voter or votes status for /elections (non-critical for election listing)', { anonymizedVoterId: getLoggableId(normalizedQueryVoterId), error: err.message, stack: err.stack });
            internalVoterId = null;
            votedElectionIds.clear();
        }
    }
    try {
        const now = new Date();
        logger.debug('Fetching active elections from database.');
        const electionsResult = await pool.query(
            "SELECT id, election_code, title, description, options, start_timestamp, end_timestamp, status FROM Elections WHERE start_timestamp <= $1 AND end_timestamp >= $1 AND status = 'ACTIVE' ORDER BY start_timestamp DESC",
            [now]
        );
        if (electionsResult.rows.length === 0) {
            logger.info('No active elections found.');
            return res.status(200).json({ elections: [] });
        }
        const electionsForApi = electionsResult.rows.map(election => ({
            id: election.id,
            electionCode: election.election_code,
            title: election.title,
            description: election.description,
            options: election.options,
            startTimestamp: election.start_timestamp,
            endTimestamp: election.end_timestamp,
            status: election.status,
            hasVoted: internalVoterId ? votedElectionIds.has(election.id) : false,
        }));
        logger.info(`Returning ${electionsForApi.length} active elections. Requested by: ${queryAnonymizedVoterId ? getLoggableId(queryAnonymizedVoterId) : 'anonymous'}`);
        res.status(200).json({ elections: electionsForApi });
    } catch (err) {
        logger.error('Error during /elections main query', { error: err.message, stack: err.stack, detail: err.detail, code: err.code });
        res.status(500).json({ error: "An unexpected error occurred on the server while fetching elections." });
    }
});

// Validation rules for /submitVote
const submitVoteValidation = [
    body('anonymizedVoterId')
        .notEmpty().withMessage('anonymizedVoterId is required and must be a non-empty string.')
        .isString().withMessage('anonymizedVoterId must be a string.')
        .trim()
        .isLength({ max: 255 }).withMessage('anonymizedVoterId must not exceed 255 characters.')
        .matches(sha256HexRegex).withMessage('anonymizedVoterId must be a valid 64-character hex string.'),

    body('electionId')
        .notEmpty().withMessage('electionId is required and must be a non-empty string.')
        .isString().withMessage('electionId must be a string.')
        .trim()
        .matches(uuidRegex).withMessage('electionId must be a valid UUID.'),

    body('selectedOption')
        .notEmpty().withMessage('selectedOption is required and must be a non-empty string.')
        .isString().withMessage('selectedOption must be a string.')
        .trim()
        .isLength({ max: 255 }).withMessage('selectedOption must not exceed 255 characters.'),

    body('playIntegrityToken')
        .notEmpty().withMessage('playIntegrityToken is required and must be a non-empty string.')
        .isString().withMessage('playIntegrityToken must be a string.')
        .trim(),

    body('playIntegrityNonce')
        .notEmpty().withMessage('playIntegrityNonce is required and must be a non-empty string.')
        .isString().withMessage('playIntegrityNonce must be a string.')
        .trim(),

    body('encryptedProof')
        .optional()
        .isString().withMessage('If provided, encryptedProof must be a string.')
        .trim()
        .notEmpty().withMessage('If provided as a string, encryptedProof must not be empty.')
        .matches(base64Regex).withMessage('If provided, encryptedProof must be a valid Base64 encoded string.'),

    body('iv')
        .optional()
        .isString().withMessage('If provided, iv must be a string.')
        .trim()
        .notEmpty().withMessage('If provided as a string, iv must not be empty.')
        .matches(base64Regex).withMessage('If provided, iv must be a valid Base64 encoded string.'),

    // Custom validator to ensure encryptedProof and iv are present together or not at all
    body().custom((value) => {
        const hasEncryptedProof = !!value.encryptedProof;
        const hasIv = !!value.iv;
        if (hasEncryptedProof !== hasIv) {
            throw new Error('encryptedProof and iv must be provided together as non-empty strings, or not at all.');
        }
        return true;
    }),
];

apiRouter.post('/submitVote', voteSubmissionLimiter, submitVoteValidation, handleValidationErrors, async (req, res) => {
    const {
        anonymizedVoterId,
        electionId,
        selectedOption,
        encryptedProof,
        iv,
        playIntegrityToken,
        playIntegrityNonce
    } = req.body;

    const finalAnonymizedVoterId = anonymizedVoterId.trim().toLowerCase();
    const loggableVoterId = getLoggableId(finalAnonymizedVoterId);

    const finalElectionId = electionId.trim();
    const finalSelectedOption = selectedOption.trim();
    const finalEncryptedProof = encryptedProof ? encryptedProof.trim() : null;
    const finalIv = iv ? iv.trim() : null;

    try {
        logger.info(`Initiating Play Integrity check for submitVote from voter: ${loggableVoterId}`);

        let integrityResult = { isValid: process.env.NODE_ENV !== 'production' }; // Bypass in non-prod for now
        if (process.env.PERFORM_PLAY_INTEGRITY_CHECK === 'true') { // Allow forcing check via env var
             integrityResult = await playIntegrityVerifier.verifyToken(playIntegrityToken.trim(), playIntegrityNonce.trim());
        } else if (process.env.NODE_ENV === 'production' && process.env.PERFORM_PLAY_INTEGRITY_CHECK !== 'false') {
            // In production, if not explicitly told to skip, perform the check.
             integrityResult = await playIntegrityVerifier.verifyToken(playIntegrityToken.trim(), playIntegrityNonce.trim());
        }

        if (!integrityResult.isValid) {
            logger.warn(`Play Integrity check failed for voter ${loggableVoterId}: ${integrityResult.error || 'Unknown integrity failure'}`);
            return res.status(403).json({
                message: "Device integrity check failed or token invalid.",
                error: integrityResult.error || 'Integrity verification failed.',
            });
        }
        logger.info(`Play Integrity check passed for voter: ${loggableVoterId}`);

        // Proceed with existing logic if integrity check passes
        const voterResult = await pool.query('SELECT id, is_eligible FROM Voters WHERE anonymized_voter_id = $1', [finalAnonymizedVoterId]);
        if (voterResult.rows.length === 0) {
            logger.warn(`Vote attempt by unregistered voter: ${loggableVoterId}`);
            return res.status(403).json({ error: "Voter not registered." });
        }
        const voter = voterResult.rows[0];
        if (!voter.is_eligible) {
            logger.warn(`Vote attempt by ineligible voter: ${loggableVoterId} (DB ID: ${voter.id})`);
            return res.status(403).json({ error: "Voter is not eligible to vote." });
        }
        const internalVoterId = voter.id;

        const electionResult = await pool.query('SELECT id, options, start_timestamp, end_timestamp, status FROM Elections WHERE id = $1', [finalElectionId]);
        if (electionResult.rows.length === 0) {
            logger.warn(`Vote attempt for non-existent election: ${finalElectionId} by voter: ${loggableVoterId}`);
            return res.status(404).json({ error: "Election not found." });
        }
        const election = electionResult.rows[0];
        const now = new Date();
        if (now < election.start_timestamp || now > election.end_timestamp || election.status !== 'ACTIVE') {
            logger.warn(`Vote attempt for inactive/non-open election: ${finalElectionId} by voter ${loggableVoterId}. Current status: ${election.status}, Start: ${election.start_timestamp}, End: ${election.end_timestamp}`);
            return res.status(403).json({ error: "Election is not currently active or open for voting." });
        }
        if (!Array.isArray(election.options) || !election.options.includes(finalSelectedOption)) {
            logger.warn(`Vote attempt with invalid option "${finalSelectedOption}" for election ${finalElectionId} by voter ${loggableVoterId}`);
            return res.status(400).json({ error: "Invalid option selected for this election." });
        }
        const internalElectionId = election.id;

        try {
            const voteInsertResult = await pool.query(
                'INSERT INTO Votes (voter_id, election_id, selected_option_value, encrypted_proof, iv) VALUES ($1, $2, $3, $4, $5) RETURNING id, election_id, selected_option_value, cast_at_timestamp',
                [internalVoterId, internalElectionId, finalSelectedOption, finalEncryptedProof, finalIv]
            );
            const newVote = voteInsertResult.rows[0];
            const logDataForBlockchain = {
                voteId: newVote.id,
                anonymizedVoterId: loggableVoterId,
                electionId: newVote.election_id,
                selectedOption: newVote.selected_option_value, // Option itself is not PII
                castAtTimestamp: newVote.cast_at_timestamp,
            };

            // Log full ID for blockchain simulation at debug, otherwise loggable version for info
            logger.debug(`Full data for blockchain simulation: ${JSON.stringify({ ...logDataForBlockchain, anonymizedVoterId: finalAnonymizedVoterId })}`);
            logger.info(`SIMULATING BLOCKCHAIN RECORD (Append-Only Log Entry): ${JSON.stringify(logDataForBlockchain)}`);


            res.status(201).json({
                message: "Vote submitted successfully and recorded anonymously!",
                vote: {
                    voteId: newVote.id,
                    electionId: newVote.election_id,
                    selectedOption: newVote.selected_option_value,
                    castAtTimestamp: newVote.cast_at_timestamp
                }
            });
        } catch (dbErr) {
            if (dbErr.code === '23505') { // Unique violation (double voting)
                logger.warn(`Double voting attempt by voter ${loggableVoterId} (DB ID: ${internalVoterId}) for election ${finalElectionId}`);
                return res.status(409).json({ error: "Already voted in this election." });
            }
            throw dbErr; // Re-throw other DB errors
        }

    } catch (err) { // This outer catch now primarily catches errors from Play Integrity or if it re-throws.
        logger.error('Error during /submitVote (potentially Play Integrity or subsequent logic)', { anonymizedVoterId: loggableVoterId, electionId: finalElectionId, error: err.message, stack: err.stack, detail: err.detail, code: err.code });
        // If it's an error from playIntegrityVerifier.verifyToken that wasn't caught as a structured {isValid:false}
        if (err.message && err.message.startsWith('PlayIntegrityClientInitError')) {
             return res.status(503).json({ message: "Service temporarily unavailable due to integrity client error." });
        }
        return res.status(500).json({ error: "An unexpected error occurred on the server." });
    }
});

app.use('/api/v1', apiRouter);

app.get('/', (req, res) => {
    res.send('Biometric Voting App Backend is running with PostgreSQL!');
});

if (process.env.NODE_ENV !== 'test') {
    app.listen(PORT, async () => {
        await initializeDatabase();
        logger.info(`Backend server listening on port ${PORT}`);
        logger.info('Using PostgreSQL for data persistence.');
        logger.info(`Using database: ${DB_NAME}`);
        logger.info('API endpoints are available under /api/v1');
    });
}

module.exports = { app, pool };
