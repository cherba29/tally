import type { Arguments, CommandBuilder } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';

type Options = {
  account: string;
  startMonth: Month;
};

export const command: string = 'generate <account>';
export const desc: string = 'Generate <account> balance record based on transfers.';

export const builder: CommandBuilder<Options, Options> = (yargs) =>
  yargs
    .options({
      'start-month': { type: 'string', demandOption: true },
    })
    .positional('account', { type: 'string', demandOption: true })
    .coerce('start-month', Month.fromString);

export const handler = (argv: Arguments<Options>): void => {
  const { account, startMonth } = argv;
  const budget: Budget = loadBudget();
  process.stdout.write(`Generating balances for ${account} for month ${startMonth?.toString()}!\n`);
  const acct: Account|undefined = budget.accounts.get(account);
  if (!acct) {
    process.stderr.write(`The account "${account}" does not exist.\n`);
    process.exit(1);
  }
  if (acct.isClosed(startMonth)) {
    process.stderr.write(`The account "${account}" is closed as of ${startMonth} since ${acct.closedOn}.\n`);
    process.exit(1);
  }
  process.exit(0);
};
