# NodeJs based express server

This will eventually replace Java/Jetty server. Since both client and server will be written with same language this will allow sharing of types and code between the two. Also reduce learning overhead and maintenance costs of multiple technologies.

Based on https://github.com/alexpermiakov/node-rest-api/tree/step.5.1
https://itnext.io/production-ready-node-js-rest-apis-setup-using-typescript-postgresql-and-redis-a9525871407

TODO:
  1. Testing
  1. Logging

## Run

```
npm run start
```

Open http://localhost:8001/api/v1/search?q=Berlin

Swagger http://localhost:8001/api-docs/