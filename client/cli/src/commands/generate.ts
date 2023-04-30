import type { Arguments, CommandBuilder, CommandModule, Options, ArgumentsCamelCase } from 'yargs';
//import type {Account} from '@tally/lib';
// import { loadBudget } from '@tally/backend/data/loader';

interface GenerateOptions extends Options {
  account: string;
  upper: boolean | undefined;
};

const command: string = 'generate <account>';
const desc: string = 'Generate balances record based on transfers.';

const builder: CommandBuilder<unknown, Arguments> = (yargs) =>
  yargs
    .options({
      upper: { type: 'boolean' },
    })
    .positional('account', { type: 'string', demandOption: true });

export const generate: CommandModule<unknown, Arguments> = {
  command,
  describe: desc,
  builder,
  handler:  (args: ArgumentsCamelCase<Arguments>): void => {
    const { account, upper } = args;
    //const budget = loadBudget();
    //console.log(budget);
    const greeting = `Generating balances for account ${account}!\n`;
    process.stdout.write(upper ? greeting.toUpperCase() : greeting);
    process.exit(0);
 
  }
}