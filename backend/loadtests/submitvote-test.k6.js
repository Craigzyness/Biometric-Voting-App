import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics'; // Import Rate

export const options = {
  vus: 2, // Number of virtual users. Should be <= numSetupVoters in setup for unique voters per VU.
  duration: '20s', // Test duration
  thresholds: {
    'http_req_failed': ['rate<0.01'],         // http errors should be less than 1%
    'http_req_duration': ['p(95)<1000'],     // 95% of requests should be below 1000ms
    'vote_submission_success_rate': ['rate>0.90'], // Custom metric: at least 90% success for vote submissions
                                                 // (allowing for some conflicts if VUs pick same election & voter by chance,
                                                 // or if setup doesn't provide enough unique vote opportunities)
  },
};

const BASE_URL = 'http://localhost:3000/api/v1';
const voteSubmissionSuccessRate = new Rate('vote_submission_success_rate'); // Define Rate metric

// Helper function to generate a unique SHA-256 like hex string (64 chars)
function generateTestVoterId(vu, iter, prefix = 'loadtest-user') {
  const randomSuffix = Math.random().toString(36).substring(2, 10);
  const baseString = `${prefix}-vu${vu}-iter${iter}-${Date.now()}-${randomSuffix}`;
  let pseudoHex = '';
  for (let i = 0; i < baseString.length; i++) {
    pseudoHex += (baseString.charCodeAt(i) % 256).toString(16).padStart(2, '0');
  }
  return (pseudoHex + 'a'.repeat(128)).substring(0, 64); // Ensure enough padding then truncate
}

// Setup function: runs once before the test VUs start
export function setup() {
  console.log('Setting up data for vote submission test...');
  const registeredVoters = [];
  const electionsToUse = []; // Store { electionId, optionToVote }

  // 1. Register a few voters for the test
  // Ensure numSetupVoters is at least equal to options.vus if each VU needs a unique voter.
  const numSetupVoters = Math.max(options.vus || 2, 2); // Ensure at least 2, or enough for VUs
  for (let i = 0; i < numSetupVoters; i++) {
    const voterId = generateTestVoterId(0, i, 'setupvoter'); // VU 0 for setup phase
    const regPayload = JSON.stringify({ anonymizedVoterId: voterId });
    const regParams = { headers: { 'Content-Type': 'application/json' } };
    const regRes = http.post(`${BASE_URL}/register`, regPayload, regParams);
    if (regRes.status === 201) {
      registeredVoters.push(voterId);
      console.log(`Setup: Registered voter ${voterId}`);
    } else {
      console.error(`Setup: Failed to register voter ${voterId}. Status: ${regRes.status}, Body: ${regRes.body}`);
    }
  }

  if (registeredVoters.length === 0) {
    console.error('Setup: No voters registered, aborting test setup. VUs will likely fail.');
    return { registeredVoters: new SharedArray("voters", () => []), electionsToUse: new SharedArray("elections", () => []) };
  }

  // 2. Fetch elections to get valid electionId and option
  // Use one of the registered voters to check their 'hasVoted' status if API supports it well
  const voterForElectionFetch = registeredVoters[0];
  const electionsRes = http.get(`${BASE_URL}/elections?anonymizedVoterId=${voterForElectionFetch}`);

  if (electionsRes.status === 200 && electionsRes.json() && electionsRes.json().elections && electionsRes.json().elections.length > 0) {
    const fetchedElections = electionsRes.json().elections;
    fetchedElections.forEach(election => {
      // Ensure election has options and is active, and this setup voter hasn't voted (if hasVoted is reliable)
      if (election.options && election.options.length > 0 && election.status === 'ACTIVE' && !election.hasVoted) {
        electionsToUse.push({
          electionId: election.id,
          // Select the first option for simplicity. Assumes options are strings.
          // If options are objects {id, name}, use election.options[0].id or election.options[0].name
          selectedOption: election.options[0],
        });
      }
    });
    console.log(`Setup: Found ${electionsToUse.length} usable (active, with options, not voted by setup voter) elections.`);
  } else {
    console.error(`Setup: Failed to fetch elections or no usable elections available. Status: ${electionsRes.status}, Body: ${electionsRes.body}`);
  }

  if (electionsToUse.length === 0) {
     console.error('Setup: No usable elections found for VUs to vote on. VUs will likely skip voting.');
  }

  // Wrap data in SharedArray for VUs
  return {
    registeredVoters: new SharedArray("voters", () => registeredVoters),
    electionsToUse: new SharedArray("elections", () => electionsToUse)
  };
}

export default function (data) {
  // Check if setup provided necessary data
  if (!data.registeredVoters || data.registeredVoters.length === 0 || !data.electionsToUse || data.electionsToUse.length === 0) {
    console.log(`VU ${__VU}: Skipping vote due to missing or inadequate setup data (not enough voters or usable elections).`);
    voteSubmissionSuccessRate.add(false); // Record as failure if cannot proceed
    sleep(1);
    return;
  }

  // Each VU picks a voter ID based on its number (round-robin)
  const voterId = data.registeredVoters[__VU % data.registeredVoters.length];

  // Pick an election and option (e.g., round-robin or random from available)
  const electionInfo = data.electionsToUse[__ITER % data.electionsToUse.length]; // Simple round-robin for iterations over usable elections

  if (!electionInfo || !electionInfo.electionId || !electionInfo.selectedOption) {
    console.log(`VU ${__VU} iter ${__ITER}: No valid election info available for voter ${voterId}. Skipping vote.`);
    voteSubmissionSuccessRate.add(false);
    sleep(1);
    return;
  }

  const { electionId, selectedOption } = electionInfo;

  const votePayload = JSON.stringify({
    anonymizedVoterId: voterId,
    electionId: electionId,
    selectedOption: selectedOption,
    encryptedProof: `dummyEncryptedProof_vu${__VU}_iter${__ITER}`,
    iv: `dummyIV_vu${__VU}_iter${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'SubmitVote' } // Tag for filtering results
  };

  const voteRes = http.post(`${BASE_URL}/submitVote`, votePayload, params);

  const isSuccess = voteRes.status === 201; // Vote submission should be 201 Created
  voteSubmissionSuccessRate.add(isSuccess);

  check(voteRes, {
    'POST /submitVote status is 201': (r) => r.status === 201,
  });

  if (!isSuccess && voteRes.status !== 409 && voteRes.status !== 429) { // Don't log expected conflicts or rate limits as errors here
    console.log(`Vote API call for ${voterId} on election ${electionId} (option ${selectedOption}) failed: ${voteRes.status} - ${voteRes.body}`);
  } else if (voteRes.status === 409) {
    console.log(`Vote API call for ${voterId} on election ${electionId} resulted in a conflict (409) - likely already voted.`);
    // This is a valid scenario in load tests if VUs contend for same voter/election pair
    // and the setup doesn't guarantee unique vote attempts per VU.
    // For the custom metric, this is still a "failed submission" if we expect 201.
  }

  sleep(1 + Math.random()); // Wait for 1-2 seconds between iterations with some jitter
}

// Teardown function (optional): runs once after all VUs have finished
export function teardown(data) {
  console.log('Vote submission load test finished.');
  // console.log(`Registered voters used: ${JSON.stringify(data.registeredVoters.toArray())}`);
  // console.log(`Elections used: ${JSON.stringify(data.electionsToUse.toArray())}`);
}
