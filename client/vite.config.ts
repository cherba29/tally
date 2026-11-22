import {defineConfig} from 'vite';

const backendPort = process.env.TALLY_BACKEND_PORT ?? '8080';
const backendHttpTarget = `http://127.0.0.1:${backendPort}`;
const backendWsTarget = `ws://127.0.0.1:${backendPort}`;

export default defineConfig({
  publicDir: false,
  server: {
    port: Number(process.env.TALLY_DEV_PORT ?? '5173'),
    strictPort: false,
    proxy: {
      '/graphql': {
        target: backendHttpTarget,
        changeOrigin: true,
      },
      '/subscriptions': {
        target: backendWsTarget,
        ws: true,
        changeOrigin: true,
      },
      '/graphiql': {
        target: backendHttpTarget,
        changeOrigin: true,
      },
      '/sdl': {
        target: backendHttpTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    target: 'es2022',
    sourcemap: true,
  },
});
