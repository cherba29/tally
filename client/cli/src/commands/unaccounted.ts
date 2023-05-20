import type { Argv, CommandBuilder, CommandModule } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import {
  TransactionStatement,
  buildTransactionStatementTable,
} from '@tally/lib/statement/transaction';

type Options = {
  owner: string | undefined;
  account: string | undefined;
  startMonth: Month | undefined;
  endMonth: Month | undefined;
  limit: number | undefined;
};

type UnaccountedEntry = {
  account: Account;
  statement: TransactionStatement;
};

const command: string = 'unaccounted';
const desc: string = 'List of periods with unaccounted balances.';

const builder: CommandBuilder<unknown, Options> = (yargs: Argv<unknown>) =>
  yargs
    .options({
      owner: { type: 'string' },
      'start-month': { type: 'string' },
      'end-month': { type: 'string' },
      limit: { type: 'number', default: 20 },
    })
    .positional('account', { type: 'string' })
    .coerce('start-month', Month.fromString)
    .coerce('end-month', Month.fromString) as unknown as Argv<Options>;

export const commandModule: CommandModule<unknown, Options> = {
  command,
  describe: desc,
  builder,
  handler: ({ owner, account, startMonth, endMonth, limit }): void => {
    const budget: Budget = loadBudget(startMonth, endMonth);
    const statementTable: TransactionStatement[] = buildTransactionStatementTable(budget);
    const unaccountedEntries: UnaccountedEntry[] = [];
    for (const transactionStatement of statementTable) {
      const stmtAccount: Account = transactionStatement.account;
      if (transactionStatement.isClosed) {
        continue;
      }
      if (owner && !stmtAccount.owners.includes(owner)) {
        continue;
      }
      if (account && stmtAccount.name !== account) {
        continue;
      }
      const unaccounted = transactionStatement?.unaccounted;
      if (unaccounted !== 0) {
        unaccountedEntries.push({
          account: stmtAccount,
          statement: transactionStatement,
        });
      }
    }
    unaccountedEntries.sort((a, b) => {
      if (a.statement.unaccounted !== undefined && b.statement.unaccounted !== undefined) {
        return Math.abs(b.statement.unaccounted) - Math.abs(a.statement.unaccounted);
      }
      if (a.statement.unaccounted === undefined) {
        if (b.statement.unaccounted === undefined) {
          return b.statement.transactions.length - a.statement.transactions.length;
        }
        return Math.abs(b.statement.unaccounted / 100) - a.statement.transactions.length;
      }
      return b.statement.transactions.length - Math.abs(a.statement.unaccounted / 100);
    });
    for (const entry of unaccountedEntries.slice(0, limit)) {
      const unnacountedValue = entry.statement.unaccounted
        ? (entry.statement.unaccounted / 100).toFixed(2).padStart(10)
        : '---';
      const numTransfers = entry.statement.transactions.length;
      process.stdout.write(
        `${entry.statement.month} ${unnacountedValue} ${entry.account.name} ${numTransfers} transfers\n`
      );
    }
    process.exit(0);
  },
};
