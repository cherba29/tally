import type { Argv, CommandBuilder, CommandModule } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import { TransactionStatement, buildTransactionStatementTable } from '@tally/lib/statement/transaction';

type Options = {
  account: string|undefined;
  startMonth: Month|undefined;
  endMonth: Month|undefined;
  limit: number|undefined;
};

type UnaccountedEntry = {
  account: Account;
  statement: TransactionStatement;
}

const command: string = 'unaccounted';
const desc: string = 'List of periods with unaccounted balances.';

const builder: CommandBuilder<unknown, Options> = (yargs: Argv<unknown>) =>
  yargs
    .options({
      'start-month': { type: 'string' },
      'end-month': { type: 'string' },
      'limit': { type: 'number', default: 20 },
    })
    .positional('account', { type: 'string' })
    .coerce('start-month', Month.fromString)
    .coerce('end-month', Month.fromString) as unknown as Argv<Options>;

export const commandModule: CommandModule<unknown, Options> = {
  command,
  describe: desc,
  builder,
  handler:  ({ account, startMonth, endMonth, limit }): void => {
    const budget: Budget = loadBudget(startMonth, endMonth);
    const statementTable: TransactionStatement[] = buildTransactionStatementTable(budget);
    const unaccountedEntries: UnaccountedEntry[] = [];
    for (const transactionStatement of statementTable) {
      const stmtAccount: Account = transactionStatement.account;
      if (account && stmtAccount.name !== account) { continue; }
      const unaccounted = transactionStatement?.unaccounted;
      if (unaccounted) {
        unaccountedEntries.push({
          account: stmtAccount,
          statement: transactionStatement
        });
      }
    }
    unaccountedEntries.sort((a, b) => (a.statement.unaccounted ?? 0) - (b.statement.unaccounted ?? 0) );
    for (const entry of unaccountedEntries.slice(0, limit)) {
      process.stdout.write(
        `${entry.statement.month} ${((entry.statement.unaccounted??0)/100).toFixed(2).padStart(10)} ${entry.account.name}\n`);
    }
    process.exit(0);
  }
}
