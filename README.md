# tally
Financial ledger based on simple text files 

```
gradle eclipse
```

## Conventions
Style guide: https://google.github.io/styleguide/javaguide.html

Line wrapper: https://google.github.io/styleguide/javaguide.html#s4.4-column-limit

## Setup

Build client side:

```
npm run webpack
```

Start server with

gradle -p server/jetty runT --offline

Load page for example by navigating to
http://localhost:8080/files/js_summary.html#data

where data is path to yaml files to be parsed. It can be relative to current 
server directory which will be in this case `./server/jetty`.
