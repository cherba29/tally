import type { Arguments, CommandBuilder } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Budget } from '@tally/lib/core/budget';

type Options = {
  account: string;
  upper: boolean | undefined;
};

export const command: string = 'generate <account>';
export const desc: string = 'Generate <account> balance record based on transfers.';

export const builder: CommandBuilder<Options, Options> = (yargs) =>
  yargs
    .options({
      upper: { type: 'boolean' },
    })
    .positional('account', { type: 'string', demandOption: true });

export const handler = (argv: Arguments<Options>): void => {
  const { account, upper } = argv;
  const greeting = `Generating balances for ${account}!\n`;
  process.stdout.write(upper ? greeting.toUpperCase() : greeting);
  const budget: Budget = loadBudget();
  for (const [accountName, ] of budget.accounts) {
    console.log(accountName);
  }

  process.exit(0);
};
