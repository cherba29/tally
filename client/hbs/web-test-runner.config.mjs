import { esbuildPlugin } from '@web/dev-server-esbuild';

export default {
  // Target your TypeScript test files
  files: ['src/**/*.test.ts', 'src/**/*.spec.ts'],
  
  // Resolve bare module imports in the browser
  nodeResolve: true,

  // On-the-fly compilation for TS files
  plugins: [
    esbuildPlugin({ 
      ts: true, 
      target: 'es2022', 
      sourceMap: true,
      tsconfig: './tsconfig.json' 
    })
  ],

  coverage: true,
  coverageConfig: {
    report: true,
    reportDir: 'coverage',
    reporters: ['lcov', 'text-summary'],
    include: ['src/**/*.ts'],
    exclude: ['src/**/*.test.ts', 'src/**/*.spec.ts'],
    threshold: {
      statements: 60,
      branches: 30,
      functions: 60,
      lines: 60,
    },
  },
};
