import { defineConfig } from 'eslint/config';
import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';


export default defineConfig([
  {
    ignores: [
      "**/build/**",
      "**/dist/**",
      "**/node_modules/**",
      "**/public/**",
      "**/*.js",
      "src/gql_types.ts",
    ],
  },
  {
    rules: {
      // Note: you must disable the base rule as it can report incorrect errors
      "no-unused-vars": "off",
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          "argsIgnorePattern": "^_",
          "varsIgnorePattern": "^_",
          "caughtErrorsIgnorePattern": "^_",
          "ignoreRestSiblings": true
        }
      ],
    },
  },
  eslint.configs.recommended,
  tseslint.configs.recommended,
]);
