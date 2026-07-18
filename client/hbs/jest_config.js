/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  "preset": "ts-jest",
  // Use a browser-like DOM environment for Web Components
  "testEnvironment": "jsdom",
  "clearMocks": true,
  "collectCoverage": true,
  "coverageDirectory": "coverage",
  "coverageProvider": "v8",
  "transform": {
    // Compile TypeScript using ts-jest
    '^.+\\.tsx?$': ['ts-jest', { useESM: true }],
  },
  // Force Jest to process Lit ESM files inside node_modules
  "transformIgnorePatterns": [
    "node_modules/(?!(lit|lit-html|lit-element|@lit|@lit-labs)/)",
  ],
}
