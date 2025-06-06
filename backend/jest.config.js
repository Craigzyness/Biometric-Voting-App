// backend/jest.config.js
module.exports = {
  testEnvironment: 'node', // Specifies the test environment for Node.js
  verbose: true, // Indicates whether each individual test should be reported during the run
  // setupFilesAfterEnv: ['./tests/setupTests.js'], // Uncomment if you create a setup file for tests
  // globalTeardown: './tests/teardownTests.js', // Uncomment if you create a global teardown file (e.g., to close DB pool)
  // testMatch: ['**/tests/**/*.test.js?(x)', '**/?(*.)+(spec|test).js?(x)'], // Default pattern, usually fine

  collectCoverage: true,
  coverageDirectory: "coverage",
  coverageProvider: "v8", // Using v8, common for Node.js
  collectCoverageFrom: [
    "**/*.js", // Collect from all JS files in the backend directory
    "!**/node_modules/**",
    "!**/tests/**", // Exclude test files themselves
    "!**/coverage/**", // Exclude the coverage output directory
    "!jest.config.js", // Exclude this config file
    // server.js contains route logic that should be covered.
    // If specific parts of server.js are hard to cover (e.g. direct app.listen),
    // consider istanbul ignore comments or further refactoring.
    // "!server.js", // Potentially exclude if it causes issues or if its coverage is mostly about startup
  ],
  // coverageReporters: ["json", "lcov", "text", "clover"], // Formats for coverage reports (can be customized)
};
