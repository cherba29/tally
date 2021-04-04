import dotenv from 'dotenv';
import { ApolloServer } from 'apollo-server';

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

server.listen().then(({ url }) => console.log(`Server ready at ${url}. `));

if (module.hot) {
  module.hot.accept();
  module.hot.dispose(() => server.stop());
}
