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
};
