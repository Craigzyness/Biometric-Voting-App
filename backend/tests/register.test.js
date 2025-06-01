const request = require('supertest');
const { app, pool } = require('../server'); // Import app and pool for potential direct DB checks if needed & closing pool
const { clearAllTables, closeTestPool, getTestPool } = require('./db_test_helper');

describe('/api/v1/register', () => {
    // No specific one-time setup needed beyond what server.js does for test env
    // beforeAll(async () => { });

    beforeEach(async () => {
        // Ensure the main app's pool (which points to test DB in NODE_ENV=test) has initialized the schema
        // This relies on server.js's initializeDatabase() having run or being callable for test DB.
        // For now, we assume initializeDatabase in server.js handles the test DB schema.
        // Then clear tables using the test helper's dedicated pool.
        await clearAllTables();
    });

    afterAll(async () => {
        // Close the main app's pool and the test helper's pool
        if (pool) { // pool exported from server.js
            await pool.end();
        }
        await closeTestPool(); // Closes the pool used by db_test_helper
    });

    it('should register a new voter successfully', async () => {
        const anonymizedVoterId = `testVoter_${Date.now()}`;
        const response = await request(app)
            .post('/api/v1/register')
            .send({ anonymizedVoterId });

        expect(response.statusCode).toBe(201);
        expect(response.body).toHaveProperty('message', 'Voter registered successfully.');
        expect(response.body).toHaveProperty('voter');
        expect(response.body.voter).toHaveProperty('id'); // UUID string
        expect(response.body.voter.id).toMatch(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/); // Basic UUID format check
        expect(response.body.voter).toHaveProperty('anonymizedVoterId', anonymizedVoterId);
        expect(response.body.voter).toHaveProperty('isEligible', true);
        expect(response.body.voter).toHaveProperty('registrationTimestamp'); // ISO String

        // Optional: Verify in DB directly using getTestPool()
        const dbPool = getTestPool();
        const dbResult = await dbPool.query('SELECT * FROM "Voters" WHERE anonymized_voter_id = $1', [anonymizedVoterId]);
        expect(dbResult.rows.length).toBe(1);
        expect(dbResult.rows[0].anonymized_voter_id).toBe(anonymizedVoterId);
    });

    it('should return 409 Conflict when trying to register an existing voter', async () => {
        const anonymizedVoterId = `existingVoter_${Date.now()}`;

        // First registration
        await request(app)
            .post('/api/v1/register')
            .send({ anonymizedVoterId });

        // Attempt to register the same ID again
        const response = await request(app)
            .post('/api/v1/register')
            .send({ anonymizedVoterId });

        expect(response.statusCode).toBe(409);
        expect(response.body).toHaveProperty('error', 'This anonymizedVoterId is already registered.');
    });

    it('should return 400 Bad Request if anonymizedVoterId is missing', async () => {
        const response = await request(app)
            .post('/api/v1/register')
            .send({}); // Empty body

        expect(response.statusCode).toBe(400);
        // The backend server.js currently has a generic message for missing/invalid
        expect(response.body).toHaveProperty('error', 'Invalid or missing anonymizedVoterId.');
    });

    it('should return 400 Bad Request if anonymizedVoterId is an empty string', async () => {
        const response = await request(app)
            .post('/api/v1/register')
            .send({ anonymizedVoterId: "" });

        expect(response.statusCode).toBe(400);
        // The backend server.js currently has a generic message for missing/invalid
        expect(response.body).toHaveProperty('error', 'Invalid or missing anonymizedVoterId.');
    });

    it('should return 400 Bad Request if anonymizedVoterId is not a string', async () => {
        const response = await request(app)
            .post('/api/v1/register')
            .send({ anonymizedVoterId: 12345 }); // Send number instead of string

        expect(response.statusCode).toBe(400);
        expect(response.body).toHaveProperty('error', 'Invalid or missing anonymizedVoterId.');
    });

});
