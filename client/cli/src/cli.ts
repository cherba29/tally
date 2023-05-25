#!/usr/bin/env node

import dotenv from 'dotenv';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import { generate } from './commands/generate';
import { report } from './commands/report';
import { commandModule as unaccounted } from './commands/unaccounted';
import { commandModule as transactions } from './commands/transactions';

// Load settings from .env into process.env
dotenv.config();

yargs(hideBin(process.argv))
  // Use the commands directory to scaffold.
  // Enable strict mode.
  .command(generate)
  .command(report)
  .command(unaccounted)
  .command(transactions)
  .strict()
  // Useful aliases.
  .alias({ h: 'help' }).argv;
