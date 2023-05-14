```
npm init -y
npm i typescript @types/node -D
npx tsc --init
npm i yargs
npm i @types/yargs -D # for TypeScript
```


### Via typescript
```
npx tsc
./build/cli.js generate Alice 
```

### After packaging
```
npm i pkg -D
npm run package
./tally generate Alice
```

### Via pnpm
```
pnpm cli run exec generate External --start-month Dec2022
```

### Via bazel (preferred)
```
 bazel run //client/cli:tally_cli --  generate External --start-month Dec2022 --use-transfers
 ```
 