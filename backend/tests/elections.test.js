const request = require('supertest');
const { app, pool } = require('../server'); // Import app and pool
const { clearAllTables, closeTestPool, getTestPool } = require('./db_test_helper');

// Helper function to seed election data
async function seedElection(dbPool, electionData) {
    const {
        electionCode, title, description, options,
        startTimestamp, endTimestamp, status
    } = electionData;

    // Ensure options are stringified if your DB expects JSON string for JSONB
    const optionsJson = Array.isArray(options) ? JSON.stringify(options) : options;

    await dbPool.query(
        `INSERT INTO "Elections" (election_code, title, description, options, start_timestamp, end_timestamp, status)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [electionCode, title, description, optionsJson, startTimestamp, endTimestamp, status]
    );
}


describe('/api/v1/elections', () => {
    let dbPool;

    beforeAll(() => {
        dbPool = getTestPool(); // Get the test pool instance
    });

    beforeEach(async () => {
        await clearAllTables();
    });

    afterAll(async () => {
        if (pool) { // pool exported from server.js
            await pool.end();
        }
        await closeTestPool(); // Closes the pool used by db_test_helper
    });

    it('should return active elections if they exist', async () => {
        const now = new Date();
        const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000).toISOString();
        const oneHourHence = new Date(now.getTime() + 60 * 60 * 1000).toISOString();
        const twoHoursHence = new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString();

        await seedElection(dbPool, {
            electionCode: 'ACTIVE001', title: 'Active Election 1', description: 'This is active',
            options: ['Yes', 'No'], startTimestamp: oneHourAgo, endTimestamp: oneHourHence, status: 'ACTIVE'
        });
        await seedElection(dbPool, {
            electionCode: 'PAST001', title: 'Past Election', description: 'This was in the past',
            options: ['A', 'B'], startTimestamp: new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString(),
            endTimestamp: oneHourAgo, status: 'ACTIVE' // Status is active but time is past
        });
        await seedElection(dbPool, {
            electionCode: 'FUTURE001', title: 'Future Election', description: 'This is in the future',
            options: ['Go', 'Stop'], startTimestamp: oneHourHence,
            endTimestamp: twoHoursHence, status: 'ACTIVE' // Status is active but time is future
        });
        await seedElection(dbPool, {
            electionCode: 'INACTIVE001', title: 'Inactive Election', description: 'This is inactive',
            options: ['X', 'Y'], startTimestamp: oneHourAgo, endTimestamp: oneHourHence, status: 'INACTIVE'
        });


        const response = await request(app).get('/api/v1/elections');

        expect(response.statusCode).toBe(200);
        expect(response.body).toHaveProperty('elections');
        expect(Array.isArray(response.body.elections)).toBe(true);
        expect(response.body.elections.length).toBe(1); // Only ACTIVE001 should be returned

        const election = response.body.elections[0];
        expect(election).toHaveProperty('id');
        expect(election.electionCode).toBe('ACTIVE001');
        expect(election.title).toBe('Active Election 1');
        expect(Array.isArray(election.options)).toBe(true);
        expect(election.options).toEqual(['Yes', 'No']);
        expect(election).toHaveProperty('startTimestamp');
        expect(election).toHaveProperty('endTimestamp');
        expect(election.status).toBe('ACTIVE');
    });

    it('should return an empty array if no active elections exist', async () => {
        const now = new Date();
        const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000).toISOString();

        await seedElection(dbPool, {
            electionCode: 'INACTIVE002', title: 'Inactive Election 2', description: 'This is also inactive',
            options: ['Old', 'New'], startTimestamp: oneHourAgo,
            endTimestamp: new Date(now.getTime() + 60 * 60 * 1000).toISOString(), status: 'INACTIVE'
        });

        const response = await request(app).get('/api/v1/elections');

        expect(response.statusCode).toBe(200);
        expect(response.body).toHaveProperty('elections');
        expect(Array.isArray(response.body.elections)).toBe(true);
        expect(response.body.elections.length).toBe(0);
    });

    it('should return an empty array if database is empty', async () => {
        // No elections seeded
        const response = await request(app).get('/api/v1/elections');

        expect(response.statusCode).toBe(200);
        expect(response.body).toHaveProperty('elections');
        expect(Array.isArray(response.body.elections)).toBe(true);
        expect(response.body.elections.length).toBe(0);
    });

    it('should return elections with the correct object structure', async () => {
        const now = new Date();
        const electionData = {
            electionCode: 'STRUCT001', title: 'Structure Test Election', description: 'Description here',
            options: ['Opt 1', 'Opt 2', 'Opt 3'],
            startTimestamp: new Date(now.getTime() - 60 * 60 * 1000).toISOString(), // started 1 hour ago
            endTimestamp: new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString(), // ends in 1 day
            status: 'ACTIVE'
        };
        await seedElection(dbPool, electionData);

        const response = await request(app).get('/api/v1/elections');

        expect(response.statusCode).toBe(200);
        expect(response.body.elections.length).toBe(1);
        const election = response.body.elections[0];

        expect(election).toHaveProperty('id'); // UUID string
        expect(election.id).toMatch(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/);
        expect(election).toHaveProperty('electionCode', electionData.electionCode);
        expect(election).toHaveProperty('title', electionData.title);
        expect(election).toHaveProperty('description', electionData.description);
        expect(election).toHaveProperty('options', electionData.options); // Assumes JSONB from DB is parsed to array by pg driver
        expect(election).toHaveProperty('startTimestamp'); // ISO String
        expect(new Date(election.startTimestamp).toISOString()).toBe(electionData.startTimestamp);
        expect(election).toHaveProperty('endTimestamp'); // ISO String
        expect(new Date(election.endTimestamp).toISOString()).toBe(electionData.endTimestamp);
        expect(election).toHaveProperty('status', electionData.status);
    });
});
