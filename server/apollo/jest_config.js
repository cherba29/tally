module.exports = {
  "testEnvironment": "node",
  "clearMocks": true,
  "collectCoverage": true,
  "coverageDirectory": "coverage",
  "coverageProvider": "v8",
  "testRegex": [".*\\.test.ts$"],
  "transform": {
    "^.+\\.(ts|tsx)$": "ts-jest"
  }
}
