// backend/jest.config.js
module.exports = {
  testEnvironment: 'node', // Specifies the test environment for Node.js
  verbose: true, // Indicates whether each individual test should be reported during the run
  // setupFilesAfterEnv: ['./tests/setupTests.js'], // Uncomment if you create a setup file for tests
  // globalTeardown: './tests/teardownTests.js', // Uncomment if you create a global teardown file (e.g., to close DB pool)
  // testMatch: ['**/tests/**/*.test.js?(x)', '**/?(*.)+(spec|test).js?(x)'], // Default pattern, usually fine
  // collectCoverage: true, // Uncomment to enable coverage collection
  // coverageDirectory: "coverage", // Directory where coverage reports will be saved
  // coverageReporters: ["json", "lcov", "text", "clover"], // Formats for coverage reports
};
