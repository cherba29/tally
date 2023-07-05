import dotenv from 'dotenv';
import { ApolloServer } from '@apollo/server';
import { ApolloServerPluginCacheControl } from '@apollo/server/plugin/cacheControl';
import { expressMiddleware } from '@apollo/server/express4';
import express from 'express';
import http from 'http';
import cors from 'cors';
import { json } from 'body-parser';
import { loadBudget } from '@tally/lib/data/loader';

import resolvers from './resolvers';
import typeDefs from './type-defs';

interface MyContext {
  token?: String;
}

// Load settings from .env into process.env
dotenv.config();

if (!process.env.TALLY_FILES) {
  throw Error('Process environment variable "TALLY_FILES" has not been specified.');
}

console.log('Serving data from: ', process.env.TALLY_FILES);

if (!process.env.CLIENT_BUNDLE) {
  throw Error('Process environment variable "CLIENT_BUNDLE" has not been specified.');
}

process.on('uncaughtException', (e) => {
  console.log(e);
  process.exit(1);
});

process.on('unhandledRejection', (e) => {
  console.log(e);
  process.exit(1);
});

const server = new ApolloServer<MyContext>({
  resolvers,
  typeDefs,
  plugins: [ApolloServerPluginCacheControl({ defaultMaxAge: 5, calculateHttpHeaders: false })]
});

await server.start();

const app = express();
const httpServer = http.createServer(app);
// Seting up connection takes a few seconds on the client.
// https://nodejs.org/api/http.html#serverkeepalivetimeout
httpServer.keepAliveTimeout = (60 * 1000) + 1000;
// https://nodejs.org/api/http.html#serverheaderstimeout
httpServer.headersTimeout = (60 * 1000) + 2000;
app.use(
  '/graphql',
  cors<cors.CorsRequest>(),
  json(),
  expressMiddleware(server, {
    context: async ({ req }) => ({ token: req.headers.token })
  })
);
console.log('Serving web UI from: ', process.env.CLIENT_BUNDLE);
app.use('/app', express.static(process.env.CLIENT_BUNDLE));

await new Promise<void>((resolve) => httpServer.listen({ port: 4000 }, resolve));
// Preload the budget on server start.
await loadBudget();
console.log(`🚀 UI ready at http://localhost:4000/app`);
console.log(`🚀 GraphQl ready at http://localhost:4000/graphql`);
