#!/usr/bin/env node

import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import { generate } from './commands/generate';

yargs(hideBin(process.argv))
  // Use the commands directory to scaffold.
  // .commandDir('commands')
  // Enable strict mode.
  .command(generate)
  .strict()
  // Useful aliases.
  .alias({ h: 'help' })
  .argv;
