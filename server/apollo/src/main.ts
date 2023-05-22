import dotenv from 'dotenv';
import { ApolloServer } from '@apollo/server';
import { ApolloServerPluginCacheControl } from '@apollo/server/plugin/cacheControl';
import { expressMiddleware } from '@apollo/server/express4';
import express from 'express';
import http from 'http';
import cors from 'cors';
import { json } from 'body-parser';

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
app.use(
  '/graphql',
  cors<cors.CorsRequest>(),
  json(),
  expressMiddleware(server, {
    context: async ({ req }) => ({ token: req.headers.token })
  })
);
app.use('/app', express.static(__dirname + '/../../../client/hbs'));

await new Promise<void>((resolve) => httpServer.listen({ port: 4000 }, resolve));
console.log(`🚀 Server ready at http://localhost:4000/graphql`);
