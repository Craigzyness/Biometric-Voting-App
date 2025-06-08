const request = require('supertest');
const { app, pool } = require('../server'); // Assuming server.js exports the app for testing
const dbTestHelper = require('../tests/db_test_helper'); // For pool cleanup and schema setup

describe('Security Middleware Tests', () => {
  let dbPool;

  beforeAll(async () => {
    dbPool = dbTestHelper.getTestPool();
    await dbTestHelper.setupTestDatabaseSchema(dbPool);
  });

  afterAll(async () => {
    if (pool) { // Main app pool
      await pool.end();
    }
    await dbTestHelper.closeTestPool();
  });

  beforeEach(async () => {
    // Clear tables before each test if any test makes DB modifications
    // For middleware tests just checking headers or rate limits on non-mutating endpoints,
    // this might not be strictly necessary for every test, but good for consistency.
    await dbTestHelper.clearAllTables();
  });

  describe('Helmet Security Headers', () => {
    it('should set X-DNS-Prefetch-Control header to "off"', async () => {
      const response = await request(app).get('/api/v1/elections'); // Any valid endpoint
      expect(response.headers['x-dns-prefetch-control']).toEqual('off');
    });

    it('should set X-Frame-Options header to "SAMEORIGIN"', async () => {
      const response = await request(app).get('/api/v1/elections');
      expect(response.headers['x-frame-options']).toEqual('SAMEORIGIN');
    });

    it('should set Strict-Transport-Security header', async () => {
      // HSTS is often enabled only when req.secure is true or in production.
      // Helmet's default might not send it if it detects not being over HTTPS.
      // Supertest usually uses HTTP. This test checks for its presence.
      const response = await request(app).get('/api/v1/elections');
      expect(response.headers['strict-transport-security']).toBeDefined();
    });

    it('should set X-Content-Type-Options header to "nosniff"', async () => {
      const response = await request(app).get('/api/v1/elections');
      expect(response.headers['x-content-type-options']).toEqual('nosniff');
    });

    // Common Helmet headers to check (some might be defaults, others need explicit config)
    it('should set X-Download-Options header to "noopen"', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['x-download-options']).toEqual('noopen');
    });

    it('should set X-Permitted-Cross-Domain-Policies header to "none"', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['x-permitted-cross-domain-policies']).toEqual('none');
    });

    it('should set Referrer-Policy header to "no-referrer"', async () => {
        // Default for helmet is 'no-referrer'
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['referrer-policy']).toEqual('no-referrer');
    });

    it('should set Cross-Origin-Embedder-Policy header to "require-corp"', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['cross-origin-embedder-policy']).toEqual('require-corp');
    });

    it('should set Cross-Origin-Opener-Policy header to "same-origin"', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['cross-origin-opener-policy']).toEqual('same-origin');
    });

    it('should set Cross-Origin-Resource-Policy header to "same-origin"', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['cross-origin-resource-policy']).toEqual('same-origin');
    });

    it('should set Origin-Agent-Cluster header to "?1" (true)', async () => {
        const response = await request(app).get('/api/v1/elections');
        expect(response.headers['origin-agent-cluster']).toEqual('?1');
    });


  });

  describe('Express Rate Limiter', () => {
    const registerEndpoint = '/api/v1/register';

    // Test for registrationLimiter (max: 5 requests per 15 minutes from server.js)
    it('should block POST requests to /api/v1/register after 5 attempts within window', async () => {
      const voterIdBase = `rate-limit-reg-test-${Date.now()}`;
      const maxRequests = 5; // From registrationLimiter in server.js

      for (let i = 0; i < maxRequests; i++) {
        const voterId = `${voterIdBase}-${i}-sha256hexvalue0123456789abcdef0123456789abcdef012345`;
        const response = await request(app).post(registerEndpoint).send({ anonymizedVoterId: voterId });
        // Expect success (201) or conflict (409) or bad request (400) for valid attempts within limit
        expect([201, 409, 400]).toContain(response.status);
      }

      const voterIdOverLimit = `${voterIdBase}-${maxRequests}-sha256hexvalue0123456789abcdef0123456789abcdef012345`;
      const responseOverLimit = await request(app).post(registerEndpoint).send({ anonymizedVoterId: voterIdOverLimit });
      expect(responseOverLimit.status).toBe(429); // Too Many Requests
      expect(responseOverLimit.body.error).toContain('Too many registration attempts');
    }, 30000); // Increased timeout for multiple requests

    // Test for voteSubmissionLimiter (max: 10 requests per 10 minutes from server.js)
    // This requires a valid voter and election to exist to get past initial checks.
    describe('Vote Submission Rate Limiter', () => {
        let validVoter;
        let validElectionId;
        const voteEndpoint = '/api/v1/submitVote';

        beforeEach(async () => {
            // Seed a voter and an election for vote submission tests
            const voterRes = await dbPool.query(
                `INSERT INTO "Voters" (anonymized_voter_id) VALUES ($1) RETURNING id, anonymized_voter_id`,
                [`voterratelimit-${Date.now()}-sha256hexvalue0123456789abcdef0123456789abcdef012345`]
            );
            validVoter = voterRes.rows[0];

            const electionRes = await dbPool.query(
                `INSERT INTO "Elections" (election_code, title, options, start_timestamp, end_timestamp, status)
                 VALUES ($1, $2, $3, $4, $5, $6) RETURNING id`,
                [`EL-RATE-${Date.now()}`, 'Rate Limit Test Election', '["Opt1","Opt2"]', new Date(Date.now() - 3600000).toISOString(), new Date(Date.now() + 3600000).toISOString(), 'ACTIVE']
            );
            validElectionId = electionRes.rows[0].id;
        });

        it('should block POST requests to /api/v1/submitVote after 10 attempts within window', async () => {
            const maxRequests = 10; // From voteSubmissionLimiter in server.js

            for (let i = 0; i < maxRequests; i++) {
                // To avoid hitting unique constraint (voter_id, election_id) for actual vote insertion,
                // we can make the selectedOption slightly different, or use different elections,
                // or expect 409 after the first successful one.
                // For testing rate limiter, it's often easier if the underlying logic always passes or fails consistently
                // before the rate limit is hit. Here, the double-vote check might interfere.
                // A simpler way is to have the first vote succeed, and subsequent ones (2 to maxRequests) fail with 409 (double vote).
                // The rate limiter should still count these.

                const selectedOption = `Opt1_attempt_${i}`; // Make option unique to bypass simple re-vote on same option if logic changes
                                                        // but DB unique constraint is on (voter_id, election_id)

                const votePayload = {
                    anonymizedVoterId: validVoter.anonymized_voter_id,
                    electionId: validElectionId,
                    selectedOption: "Opt1" // Keep option same, expect 409 after first
                };
                const response = await request(app).post(voteEndpoint).send(votePayload);

                if (i === 0) {
                    expect(response.status).toBe(201); // First vote
                } else {
                    expect(response.status).toBe(409); // Subsequent votes are double votes
                }
            }

            const responseOverLimit = await request(app).post(voteEndpoint).send({
                anonymizedVoterId: validVoter.anonymized_voter_id,
                electionId: validElectionId,
                selectedOption: "Opt1_over_limit"
            });
            expect(responseOverLimit.status).toBe(429); // Too Many Requests
            expect(responseOverLimit.body.error).toContain('Too many vote submission attempts');
        }, 45000); // Increased timeout
    });
  });
});
