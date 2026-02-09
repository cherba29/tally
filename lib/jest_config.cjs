module.exports = {
  verbose: true,
  "testEnvironment": "node",
  preset: 'ts-jest/presets/default-esm',
  "clearMocks": true,
  "collectCoverage": true,
  "coverageDirectory": "coverage",
  "coverageProvider": "v8",
  "testRegex": [".*\\.test.ts$"],
}
