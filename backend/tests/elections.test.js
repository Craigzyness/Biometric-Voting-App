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
        expect(election).toHaveProperty('hasVoted', false); // Default for no user ID
    });

    // --- BEGIN New Tests for hasVoted logic ---
    describe('/api/v1/elections with anonymizedVoterId for hasVoted status', () => {
        let voter1, voter2;
        let election1Id, election2Id, election3Id;

        const createActiveElectionPayload = (code, title = 'Active Election') => {
            const now = new Date();
            return {
                electionCode: code, title: `${title} ${code}`, description: 'This is an active election.',
                options: ['Yes', 'No'],
                startTimestamp: new Date(now.getTime() - 1 * 60 * 60 * 1000).toISOString(),
                endTimestamp: new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString(),
                status: 'ACTIVE'
            };
        };

        async function seedVoter(anonymizedVoterId) {
            const result = await dbPool.query(
                `INSERT INTO "Voters" (anonymized_voter_id, is_eligible) VALUES ($1, TRUE) RETURNING id, anonymized_voter_id`,
                [anonymizedVoterId]
            );
            return result.rows[0];
        }

        async function seedVote(voterId, electionId, selectedOption = 'Yes') {
            await dbPool.query(
                `INSERT INTO "Votes" (voter_id, election_id, selected_option_value) VALUES ($1, $2, $3)`,
                [voterId, electionId, selectedOption]
            );
        }

        beforeEach(async () => {
            // Seed voters
            voter1 = await seedVoter('voter1_hasvoted_test_sha256_hex_id_001'); // Valid SHA256 format
            voter2 = await seedVoter('voter2_hasvoted_test_sha256_hex_id_002'); // Valid SHA256 format

            // Seed elections
            election1Id = (await dbPool.query(`INSERT INTO "Elections" (election_code, title, description, options, start_timestamp, end_timestamp, status) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`, Object.values(createActiveElectionPayload('E01')))).rows[0].id;
            election2Id = (await dbPool.query(`INSERT INTO "Elections" (election_code, title, description, options, start_timestamp, end_timestamp, status) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`, Object.values(createActiveElectionPayload('E02')))).rows[0].id;
            election3Id = (await dbPool.query(`INSERT INTO "Elections" (election_code, title, description, options, start_timestamp, end_timestamp, status) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`, Object.values(createActiveElectionPayload('E03')))).rows[0].id;
        
            // Seed votes: voter1 voted in E01 and E03
            await seedVote(voter1.id, election1Id);
            await seedVote(voter1.id, election3Id);
        });

        it('should return all elections with hasVoted = false if no anonymizedVoterId is provided', async () => {
            const response = await request(app).get('/api/v1/elections');
            expect(response.statusCode).toBe(200);
            expect(response.body.elections.length).toBe(3);
            response.body.elections.forEach(election => {
                expect(election.hasVoted).toBe(false);
            });
        });
        
        it('should return 400 if provided anonymizedVoterId is invalid format', async () => {
            const response = await request(app).get('/api/v1/elections?anonymizedVoterId=invalid-id-format');
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('anonymizedVoterId must be a valid 64-character hex string');
        });

        it('should return hasVoted = false for all elections if voter is not found', async () => {
            const nonExistentVoterId = '00000000000000000000000000000000000000000000000000000000deadbeef'; // Valid format, not in DB
            const response = await request(app).get(`/api/v1/elections?anonymizedVoterId=${nonExistentVoterId}`);
            expect(response.statusCode).toBe(200);
            expect(response.body.elections.length).toBe(3);
            response.body.elections.forEach(election => {
                expect(election.hasVoted).toBe(false);
            });
        });

        it('should return correct hasVoted status for a voter who has voted in some elections', async () => {
            const response = await request(app).get(`/api/v1/elections?anonymizedVoterId=${voter1.anonymized_voter_id}`);
            expect(response.statusCode).toBe(200);
            expect(response.body.elections.length).toBe(3); // All active elections still returned

            const electionMap = new Map(response.body.elections.map(e => [e.id, e]));
            expect(electionMap.get(election1Id).hasVoted).toBe(true);
            expect(electionMap.get(election2Id).hasVoted).toBe(false);
            expect(electionMap.get(election3Id).hasVoted).toBe(true);
        });

        it('should return hasVoted = false for all elections for a voter who has not voted in any', async () => {
            const response = await request(app).get(`/api/v1/elections?anonymizedVoterId=${voter2.anonymized_voter_id}`);
            expect(response.statusCode).toBe(200);
            expect(response.body.elections.length).toBe(3);
            response.body.elections.forEach(election => {
                expect(election.hasVoted).toBe(false);
            });
        });
    });
    // --- END New Tests for hasVoted logic ---

    // --- BEGIN Test for Index Creation ---
    describe('Database Initialization Checks for Elections', () => {
        it('should have created the idx_elections_status_start_end index on the Elections table', async () => {
            // This test relies on initializeDatabase() having been run by the time the app starts
            // or specifically called if needed for a test setup.
            // The server.js already calls initializeDatabase when not in 'test' mode for app.listen,
            // but for tests, the schema setup is implicit via the main pool or explicit calls.
            // We assume the schema is initialized for the test DB.
            const dbPool = getTestPool();
            const indexCheckQuery = `
                SELECT 1
                FROM pg_indexes
                WHERE tablename = 'Elections' AND indexname = 'idx_elections_status_start_end';
            `;
            const { rows } = await dbPool.query(indexCheckQuery);
            expect(rows.length).toBe(1); // Expect the index to exist
        });
    });
    // --- END Test for Index Creation ---
});
