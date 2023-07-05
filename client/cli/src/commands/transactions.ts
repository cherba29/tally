import type { Argv, CommandBuilder, CommandModule } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import { Transaction } from '@tally/lib/statement/transaction';
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

const command: string = 'transactions';
const desc: string = 'List of transactions within given period.';

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
  handler: async ({ owner, account, startMonth, endMonth, limit }): Promise<void> => {
    const budget: Budget = (await loadBudget(startMonth, endMonth)).budget;
    const statementTable: TransactionStatement[] = buildTransactionStatementTable(budget);
    let entries: Transaction[] = [];
    for (const transactionStatement of statementTable) {
      const stmtAccount: Account = transactionStatement.account;
      if (owner && !stmtAccount.owners.includes(owner)) {
        continue;
      }
      if (account && stmtAccount.name !== account) {
        continue;
      }

      entries = entries.concat(transactionStatement.transactions);
    }
    entries.sort((a, b) => {
      const timeDiff = a.balance.date.getTime() - b.balance.date.getTime();
      if (timeDiff !== 0) {
        return timeDiff;
      }
      return Math.abs(a.balance.amount) - Math.abs(b.balance.amount);
    });
    for (const t of entries.slice(0, limit)) {
      process.stdout.write(
        `${t.balance.date.toISOString().slice(0, 10)} ${(t.balance.amount / 100)
          .toFixed(2)
          .padStart(8)}  ${t.account.name.padEnd(20)} ${t.description}\n`
      );
    }
    process.exit(0);
  },
};
