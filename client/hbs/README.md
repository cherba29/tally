# Tally client side

WARNING: This app is not secure, it allows one to load yaml financial data
from any location. Do not expose it online.

## Setup

To build js bundle use.

```
npm run build
```

It is loaded by `js_summary.html`. Java `jetty` server is also a
file server, opening this file will start the app.

`js_summary.html` requires query hash parameter, for example

```
http://localhost:8080/files/client/hbs/js_summary.html#../../../data/yaml/files
```

## Development

```
npm run test
```
