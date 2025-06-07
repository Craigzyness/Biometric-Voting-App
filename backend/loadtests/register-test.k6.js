import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 2,
  duration: '15s',
  thresholds: {
    'http_req_failed': ['rate<0.01'],         // http errors should be less than 1%
    'http_req_duration': ['p(95)<500'],      // 95% of requests should be below 500ms
    // Example for check based threshold (name format might vary based on k6 version or if a group is used):
    // 'checks{check_name:POST /register status is 201}': ['rate>0.95'],
    // Note: k6 automatically creates metrics for checks. The tag for the check name
    // might be just "POST /register status is 201" or include default scenario/group.
    // A common way to refer to it is by its name if it's unique.
    // For example, if your check is named 'status is 201', it could be:
    // 'checks{my_check_tag_or_name:status is 201}': ['rate>0.95']
    // Or more simply, if only one check:
    'checks': ['rate>0.95'], // At least 95% of all checks should pass
  },
};

const BASE_URL = 'http://localhost:3000/api/v1';

// Helper to generate a unique SHA-256 like hex string (64 chars)
function generateTestVoterId(vu, iter) {
  // Create a base string that's likely to be unique per iteration and VU
  // Using Date.now() and Math.random() for more uniqueness within the short test duration
  const randomSuffix = Math.random().toString(36).substring(2, 10); // 8 random chars
  const baseString = `vu${vu}iter${iter}${Date.now()}${randomSuffix}`;

  // Simple non-cryptographic hash-like function to produce varied hex characters
  let pseudoHash = '';
  for (let i = 0; i < baseString.length; i++) {
    pseudoHash += (baseString.charCodeAt(i) % 256).toString(16).padStart(2, '0');
  }

  // Ensure it's 64 chars by padding with 'a' or truncating
  // If pseudoHash is too short, pad it. If too long, truncate.
  if (pseudoHash.length >= 64) {
    return pseudoHash.substring(0, 64);
  } else {
    return (pseudoHash + 'a'.repeat(64)).substring(0, 64);
  }
}

export default function () {
  const testVoterId = generateTestVoterId(__VU, __ITER);

  const payload = JSON.stringify({
    anonymizedVoterId: testVoterId,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const registerRes = http.post(`${BASE_URL}/register`, payload, params);

  const isSuccess = check(registerRes, {
    'POST /register status is 201': (r) => r.status === 201,
  });

  // If status is not 201 (registration failed for some reason other than rate limit, like conflict or bad format)
  // it's useful to log this during test development/debugging.
  // The http_req_failed threshold will catch these as errors if they are > 400.
  // A 409 (conflict) is not an http_req_failed but would fail the 'status is 201' check.
  if (!isSuccess && registerRes.status !== 429) { // Don't log for expected rate limit errors
    console.log(`Registration attempt for ${testVoterId} failed with status ${registerRes.status}: ${registerRes.body}`);
  }

  sleep(1); // Wait for 1 second between iterations
}
