import * as esbuild from 'esbuild';

const buildOptions = {
  entryPoints: ['src/main.ts'],
  outfile: 'dist/main.mjs',
  format: 'esm',
  target: 'esnext',
  platform: 'node',
  banner: {
    // See discussion in https://github.com/evanw/esbuild/pull/2067.
    js: "import { createRequire } from 'module'; const require = createRequire(import.meta.url);"
  },
  bundle: true
};
await esbuild.build(buildOptions);
