# tally
Financial ledger based on simple text files 

```
gradle eclipse
```

## Conventions
Style guide: https://google.github.io/styleguide/javaguide.html

Line wrapper: https://google.github.io/styleguide/javaguide.html#s4.4-column-limit

## Setup

```
npm install -g grunt-cli
```

```
npm install grunt-contrib-handlebars --save-dev
```	

Run unit tests as

```
gradle tests
```
	
If any templates are changes run grunt to regenerate 'templates.js' file.

grunt

Start server with

gradle runT --offline

Load page for example by navigating to
http://localhost:8080/files/js_summary.html#data


## typescript

```
grunt ts
```

```
npm run test
```