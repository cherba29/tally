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
      target: 'auto', 
      sourceMap: true 
    })
  ],
};
