import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 2, // Number of virtual users
  duration: '15s', // Test duration
  thresholds: {
    'http_req_failed': ['rate<0.01'], // http errors should be less than 1%
    'http_req_duration': ['p(95)<500'], // 95% of requests should be below 500ms
  },
};

const BASE_URL = 'http://localhost:3000/api/v1'; // Assuming backend runs on port 3000

export default function () {
  // Test GET /api/v1/elections
  const electionsRes = http.get(`${BASE_URL}/elections`);
  check(electionsRes, {
    'GET /elections status is 200': (r) => r.status === 200,
    'GET /elections response is not empty': (r) => r.body && r.body.length > 0,
  });
  sleep(1); // Wait for 1 second between iterations

  // Optionally, add a voterId to the request to test that path
  // This requires a known/valid voterId. For a generic test, we can skip this
  // or use a placeholder if the endpoint handles non-existent voterIds gracefully.
  // Example:
  // const voterId = 'some-test-voter-id-for-loadtest'; // Replace with a valid or test ID
  // const electionsWithVoterRes = http.get(`${BASE_URL}/elections?voterId=${voterId}`);
  // check(electionsWithVoterRes, {
  //   'GET /elections?voterId status is 200': (r) => r.status === 200,
  // });
  // sleep(1);
}
