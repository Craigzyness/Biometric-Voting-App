// backend/tests/db_test_helper.js
const { Pool } = require('pg');

// Test Database Configuration (should match what server.js uses in 'test' mode)
const TEST_DB_CONFIG = {
    user: process.env.DB_USER || 'your_db_user', // Ensure these match server.js defaults for test or are set in test env
    host: process.env.DB_HOST || 'localhost',
    database: process.env.NODE_ENV === 'test'
        ? (process.env.DB_TEST_NAME || 'biometric_voting_app_test_db')
        : (process.env.DB_NAME || 'biometric_voting_app_db'), // Fallback to main DB if not in test, though this helper is for tests.
    password: process.env.DB_PASSWORD || 'your_db_password',
    port: parseInt(process.env.DB_PORT || 5432),
};

let testPool;

const getTestPool = () => {
    if (!testPool) {
        console.log(`Test helper connecting to DB: ${TEST_DB_CONFIG.database} on ${TEST_DB_CONFIG.host}:${TEST_DB_CONFIG.port}`);
        testPool = new Pool(TEST_DB_CONFIG);
    }
    return testPool;
};

/**
 * Sets up the database schema for tests.
 * Ensures pgcrypto extension is available and creates tables.
 * @param {Pool | Client} poolOrClient - The pg Pool or Client object to use for queries.
 */
async function setupTestDatabaseSchema(poolOrClient) {
    console.log('Setting up test database schema...');
    try {
        await poolOrClient.query('CREATE EXTENSION IF NOT EXISTS pgcrypto;');
        console.log('Extension "pgcrypto" checked/created successfully.');

        await poolOrClient.query(`
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

        await poolOrClient.query(`
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

        await poolOrClient.query(`
            CREATE INDEX IF NOT EXISTS idx_elections_status_start_end ON Elections (status, start_timestamp, end_timestamp);
        `);
        console.log('Index "idx_elections_status_start_end" on "Elections" table checked/created successfully.');

        await poolOrClient.query(`
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
        console.log('Test database schema setup complete.');
    } catch (error) {
        console.error('Error setting up test database schema:', error);
        throw error; // Re-throw to fail tests if schema setup fails
    }
}

/**
 * Clears all relevant tables in the test database.
 * Ensures tests start with a clean slate.
 */
const clearAllTables = async () => {
    const poolInstance = getTestPool();
    try {
        // The order matters due to foreign key constraints. Votes depends on Voters and Elections.
        await poolInstance.query('TRUNCATE TABLE "Votes" RESTART IDENTITY CASCADE');
        await poolInstance.query('TRUNCATE TABLE "Voters" RESTART IDENTITY CASCADE');
        await poolInstance.query('TRUNCATE TABLE "Elections" RESTART IDENTITY CASCADE');
        console.log('Test database tables truncated successfully.');
    } catch (error) {
        console.error('Error truncating test database tables:', error);
        throw error; // Re-throw to fail tests if cleanup fails
    }
};

/**
 * Closes the test database connection pool.
 * Should be called after all tests have run (e.g., in Jest's globalTeardown or afterAll).
 */
const closeTestPool = async () => {
    if (testPool) {
        try {
            await testPool.end();
            testPool = null; // Reset for any potential subsequent runs within the same test process
            console.log('Test database pool closed.');
        } catch (error) {
            console.error('Error closing test database pool:', error);
        }
    }
};

module.exports = {
    getTestPool,
    clearAllTables,
    closeTestPool,
    setupTestDatabaseSchema, // Export the new function
};
