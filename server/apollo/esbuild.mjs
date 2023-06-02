import * as esbuild from 'esbuild';
import buildOptions from './esbuild.config.mjs';

await esbuild.build(buildOptions);
