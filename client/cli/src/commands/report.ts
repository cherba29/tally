import type { Argv, CommandBuilder, CommandModule } from 'yargs';
import { loadBudget } from '@tally/lib/data/loader';
import { Account } from '@tally/lib/core/account';
import { Type as BalanceType } from '@tally/lib/core/balance';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import {
  TransactionStatement,
  buildTransactionStatementTable,
} from '@tally/lib/statement/transaction';

type ReportOptions = {
  account: string;
  startMonth: Month;
  endMonth: Month;
};

const HEADER_ROW = [
  'Account',
  'Type',
  'OpenedOn',
  'ClosedOn',
  'External',
  'Closed',
  'Year',
  'Month',
  'Start Amount',
  'Start Projected',
  'End Amount',
  'End Projected',
  'Inflows',
  'OutFlows',
  'Income',
  'Expense',
  'Transfers',
  'Unaccounted',
];

const command: string = 'report <account>';
const desc: string = 'Full report';

const builder: CommandBuilder<unknown, ReportOptions> = (yargs: Argv<unknown>) =>
  yargs
    .options({
      'start-month': { type: 'string', demandOption: true },
      'end-month': { type: 'string', demandOption: true },
    })
    .positional('account', { type: 'string', demandOption: true })
    .coerce('start-month', Month.fromString)
    .coerce('end-month', Month.fromString) as unknown as Argv<ReportOptions>;

export const report: CommandModule<unknown, ReportOptions> = {
  command,
  describe: desc,
  builder,
  handler: ({ account, startMonth, endMonth }): void => {
    const budget: Budget = loadBudget(startMonth, endMonth);
    process.stdout.write(HEADER_ROW.join(',') + '\n');
    const statementTable: TransactionStatement[] = buildTransactionStatementTable(budget);
    for (const transactionStatement of statementTable) {
      const stmtAccount: Account = transactionStatement.account;
      if (stmtAccount.name !== account) {
        continue;
      }
      const row: string[] = [
        stmtAccount.name,
        stmtAccount.type,
        stmtAccount.openedOn?.toString() ?? '',
        stmtAccount.closedOn?.toString() ?? '',
        stmtAccount.isExternal ? 'T' : 'F',
        !stmtAccount.isClosed(transactionStatement.month) ? 'T' : 'F',
        transactionStatement.month.year.toString(),
        (transactionStatement.month.month + 1).toString(),
        transactionStatement.startBalance
          ? (transactionStatement.startBalance?.amount / 100).toFixed(2)
          : '',
        transactionStatement.startBalance
          ? transactionStatement.startBalance.type === BalanceType.PROJECTED
            ? 'P'
            : 'C'
          : '',
        transactionStatement.endBalance
          ? (transactionStatement.endBalance.amount / 100).toFixed(2)
          : '',
        transactionStatement.endBalance
          ? transactionStatement.endBalance.type === BalanceType.PROJECTED
            ? 'P'
            : 'C'
          : '',
        (transactionStatement.inFlows / 100).toFixed(2),
        (transactionStatement.outFlows / 100).toFixed(2),
        (transactionStatement.income / 100).toFixed(2),
        (transactionStatement.totalPayments / 100).toFixed(2),
        (transactionStatement.totalTransfers / 100).toFixed(2),
        transactionStatement?.unaccounted
          ? (transactionStatement.unaccounted / 100).toFixed(2)
          : '',
      ];

      process.stdout.write(row.join(',') + '\n');
    }
    process.exit(0);
  },
};
