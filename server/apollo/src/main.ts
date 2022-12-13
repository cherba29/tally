import dotenv from 'dotenv';
import { ApolloServer } from 'apollo-server-express';
import express from 'express';

import resolvers from './resolvers';
import typeDefs from './type-defs';

// Load settings from .env into process.env
dotenv.config();

process.on('uncaughtException', (e) => {
  console.log(e);
  process.exit(1);
});

process.on('unhandledRejection', (e) => {
  console.log(e);
  process.exit(1);
});

const server = new ApolloServer({
  resolvers,
  typeDefs,
  cacheControl: {
    defaultMaxAge: 0,
    calculateHttpHeaders: false
  }
});

const app = express();
server.applyMiddleware({app});

app.listen({port: 4000}, () => 
  console.log(`Server ready at http://localhost:4000${server.graphqlPath}.`)
);

if (module.hot) {
  module.hot.accept();
  module.hot.dispose(() => server.stop());
}
