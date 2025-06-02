const request = require('supertest');
const { app, pool } = require('../server'); // Import app and main pool
const { clearAllTables, closeTestPool, getTestPool } = require('./db_test_helper');

// Helper function to seed election data
async function seedElection(dbPool, electionData) {
    const {
        electionCode, title, description, options,
        startTimestamp, endTimestamp, status
    } = electionData;
    const optionsJson = Array.isArray(options) ? JSON.stringify(options) : options;
    const result = await dbPool.query(
        `INSERT INTO "Elections" (election_code, title, description, options, start_timestamp, end_timestamp, status)
         VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`, // Return the generated id
        [electionCode, title, description, optionsJson, startTimestamp, endTimestamp, status]
    );
    return result.rows[0].id; // Return the election ID
}

// Helper function to seed voter data
async function seedVoter(dbPool, voterData) {
    const { anonymizedVoterId, isEligible = true } = voterData;
    const result = await dbPool.query(
        `INSERT INTO "Voters" (anonymized_voter_id, is_eligible)
         VALUES ($1, $2) RETURNING id, anonymized_voter_id, is_eligible`, // Return relevant data
        [anonymizedVoterId, isEligible]
    );
    return result.rows[0]; // Return the voter object including DB id
}


describe('/api/v1/submitVote', () => {
    let dbPool;

    beforeAll(() => {
        dbPool = getTestPool();
    });

    beforeEach(async () => {
        await clearAllTables();
    });

    afterAll(async () => {
        if (pool) { // main app pool
            await pool.end();
        }
        await closeTestPool(); // test helper pool
    });

    const createActiveElectionPayload = (code = 'ACTV001') => {
        const now = new Date();
        return {
            electionCode: code, title: `Active Election ${code}`, description: 'This election is currently active.',
            options: ['Option A', 'Option B', 'Option C'],
            startTimestamp: new Date(now.getTime() - 1 * 60 * 60 * 1000).toISOString(), // Started 1 hour ago
            endTimestamp: new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString(), // Ends in 1 day
            status: 'ACTIVE'
        };
    };

    it('should submit a vote successfully for a registered voter and active election', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_valid_1' });
        const electionId = await seedElection(dbPool, createActiveElectionPayload());

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(201);
        expect(response.body).toHaveProperty('message', 'Vote submitted successfully.');
        expect(response.body).toHaveProperty('vote');
        expect(response.body.vote).toHaveProperty('voteId'); // UUID
        expect(response.body.vote.voteId).toMatch(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/);
        expect(response.body.vote).toHaveProperty('electionId', electionId);
        expect(response.body.vote).toHaveProperty('selectedOption', 'Option A');
        expect(response.body.vote).toHaveProperty('castAtTimestamp');

        // Verify in DB
        const dbResult = await dbPool.query('SELECT * FROM "Votes" WHERE election_id = $1 AND voter_id = $2', [electionId, voter.id]);
        expect(dbResult.rows.length).toBe(1);
        expect(dbResult.rows[0].selected_option_value).toBe('Option A');
    });

    it('should return 409 Conflict when a voter tries to vote twice for the same election', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_double_1' });
        const electionId = await seedElection(dbPool, createActiveElectionPayload());
        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option B'
        };

        // First vote
        await request(app).post('/api/v1/submitVote').send(votePayload);

        // Attempt second vote
        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(409);
        expect(response.body).toHaveProperty('error', 'Already voted in this election.');
    });

    it('should return 403 Forbidden when using an unregistered anonymizedVoterId', async () => {
        const electionId = await seedElection(dbPool, createActiveElectionPayload());
        const votePayload = {
            anonymizedVoterId: 'voter_unregistered_1',
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(403);
        expect(response.body).toHaveProperty('error', 'Voter not registered.');
    });

    it('should return 404 Not Found when voting for a non-existent electionId', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_no_election_1' });
        const nonExistentElectionId = '00000000-0000-0000-0000-000000000000'; // Valid UUID format, but not in DB
        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: nonExistentElectionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(404);
        expect(response.body).toHaveProperty('error', 'Election not found.');
    });

    it('should return 403 Forbidden when voting for an inactive (past) election', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_past_election_1' });
        const now = new Date();
        const pastElectionPayload = {
            ...createActiveElectionPayload('PAST001'),
            startTimestamp: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString(), // 2 days ago
            endTimestamp: new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000).toISOString(),   // 1 day ago
        };
        const electionId = await seedElection(dbPool, pastElectionPayload);

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(403);
        expect(response.body).toHaveProperty('error', 'Election is not currently active or open for voting.');
    });

    it('should return 403 Forbidden when voting for an election that has not yet started', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_future_election_1' });
        const now = new Date();
        const futureElectionPayload = {
            ...createActiveElectionPayload('FUTR001'),
            startTimestamp: new Date(now.getTime() + 1 * 24 * 60 * 60 * 1000).toISOString(), // Tomorrow
            endTimestamp: new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000).toISOString(),   // Day after tomorrow
        };
        const electionId = await seedElection(dbPool, futureElectionPayload);

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(403);
        expect(response.body).toHaveProperty('error', 'Election is not currently active or open for voting.');
    });

    it('should return 403 Forbidden when voting for an election with status not ACTIVE', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_inactive_status_1' });
        const inactiveElectionPayload = {
            ...createActiveElectionPayload('INACT001'),
            status: 'PENDING' // Or 'CLOSED', 'CANCELLED' etc.
        };
        const electionId = await seedElection(dbPool, inactiveElectionPayload);

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(403);
        expect(response.body).toHaveProperty('error', 'Election is not currently active or open for voting.');
    });

    it('should return 400 Bad Request when selectedOption is not valid for the election', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_invalid_option_1' });
        const electionId = await seedElection(dbPool, createActiveElectionPayload('OPTTEST')); // Options: ['Option A', 'Option B', 'Option C']

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option D_Invalid' // This option does not exist
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(400);
        expect(response.body).toHaveProperty('error', 'Invalid option selected for this election.');
    });

    it('should return 403 Forbidden when the voter is not eligible', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_ineligible_1', isEligible: false });
        const electionId = await seedElection(dbPool, createActiveElectionPayload());

        const votePayload = {
            anonymizedVoterId: voter.anonymized_voter_id,
            electionId: electionId,
            selectedOption: 'Option A'
        };

        const response = await request(app)
            .post('/api/v1/submitVote')
            .send(votePayload);

        expect(response.statusCode).toBe(403);
        expect(response.body).toHaveProperty('error', 'Voter is not eligible to vote.');
    });

    it('should return 400 Bad Request if anonymizedVoterId is missing', async () => {
        const electionId = await seedElection(dbPool, createActiveElectionPayload());
        const response = await request(app)
            .post('/api/v1/submitVote')
            .send({ electionId: electionId, selectedOption: 'Option A' });
        expect(response.statusCode).toBe(400);
        expect(response.body).toHaveProperty('error', 'Missing or invalid fields: anonymizedVoterId, electionId, selectedOption.');
    });

    it('should return 400 Bad Request if electionId is missing', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_missing_eid_1' });
        const response = await request(app)
            .post('/api/v1/submitVote')
            .send({ anonymizedVoterId: voter.anonymized_voter_id, selectedOption: 'Option A' });
        expect(response.statusCode).toBe(400);
        expect(response.body).toHaveProperty('error', 'Missing or invalid fields: anonymizedVoterId, electionId, selectedOption.');
    });

    it('should return 400 Bad Request if selectedOption is missing', async () => {
        const voter = await seedVoter(dbPool, { anonymizedVoterId: 'voter_missing_opt_1' });
        const electionId = await seedElection(dbPool, createActiveElectionPayload());
        const response = await request(app)
            .post('/api/v1/submitVote')
            .send({ anonymizedVoterId: voter.anonymized_voter_id, electionId: electionId });
        expect(response.statusCode).toBe(400);
        // The actual error message might be a join of multiple messages if using the array approach for errors
        // For this specific case, it's one of the first checks.
        expect(response.body.error).toMatch(/Missing or invalid fields|anonymizedVoterId is required/);
    });

    // --- BEGIN New Input Validation Tests for /submitVote ---
    describe('Input Validation for /submitVote', () => {
        let voter;
        let electionId;

        beforeEach(async () => {
            // Seed a valid voter and election for these tests
            voter = await seedVoter(dbPool, { anonymizedVoterId: `voter_input_val_${Date.now()}` });
            electionId = await seedElection(dbPool, createActiveElectionPayload(`EIV${Date.now()}`));
        });

        it('should return 400 for invalid anonymizedVoterId (too long)', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: 'a'.repeat(257), electionId, selectedOption: 'Option A'
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('anonymizedVoterId must not exceed 255 characters.');
        });

        it('should return 400 for invalid anonymizedVoterId (bad format)', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: 'invalid-hex-id-123', electionId, selectedOption: 'Option A'
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('anonymizedVoterId must be a valid 64-character hex string.');
        });

        it('should return 400 for invalid electionId (not UUID)', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId: 'not-a-uuid', selectedOption: 'Option A'
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('electionId must be a valid UUID.');
        });

        it('should return 400 for invalid selectedOption (too long)', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'A'.repeat(257)
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('selectedOption must not exceed 255 characters.');
        });

        it('should return 400 if encryptedProof is not Base64', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                encryptedProof: 'not base64!', iv: 'validBase64=='
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('encryptedProof must be a valid Base64 encoded string.');
        });

        it('should return 400 if iv is not Base64', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                encryptedProof: 'validBase64==', iv: 'not base64!'
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('iv must be a valid Base64 encoded string.');
        });
        
        it('should return 400 if encryptedProof is provided as non-empty string but iv is missing or empty', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                encryptedProof: 'validBase64==' // iv is missing
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('encryptedProof and iv must be provided together');
            
            const response2 = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                encryptedProof: 'validBase64==', iv: '   ' // iv is empty after trim
            });
            expect(response2.statusCode).toBe(400);
            expect(response2.body.error).toContain('encryptedProof and iv must be provided together');
        });

        it('should return 400 if iv is provided as non-empty string but encryptedProof is missing or empty', async () => {
            const response = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                iv: 'validBase64==' // encryptedProof is missing
            });
            expect(response.statusCode).toBe(400);
            expect(response.body.error).toContain('encryptedProof and iv must be provided together');

            const response2 = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A',
                iv: 'validBase64==', encryptedProof: '   ' // encryptedProof is empty after trim
            });
            expect(response2.statusCode).toBe(400);
            expect(response2.body.error).toContain('encryptedProof and iv must be provided together');
        });

        it('should succeed if encryptedProof and iv are both validly provided or both omitted', async () => {
            // Both omitted (already tested in primary success case)
            const responseOmitted = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option A'
            });
            expect(responseOmitted.statusCode).toBe(201);

            // Both provided and valid
            const responseProvided = await request(app).post('/api/v1/submitVote').send({
                anonymizedVoterId: voter.anonymized_voter_id, electionId, selectedOption: 'Option B', // Use different option to avoid double voting
                encryptedProof: 'Zm9vYmFy', iv: 'YmF6cXV4' // "foobar" and "bazqux" in base64
            });
            expect(responseProvided.statusCode).toBe(201);
        });
    });
    // --- END New Input Validation Tests for /submitVote ---
});
