import { buildTransactionStatementTable, Type as TransactionType } from './statement/transaction';
import { buildSummaryStatementTable } from './statement/summary';
import { BalanceType, Month } from '@tally/lib';
import { listFiles, loadBudget } from './data/loader';
import { GraphQLScalarType, Kind } from 'graphql';
import { GqlAccount, GqlBudget, GqlStatement, GqlSummaryStatement } from './types';

function buildGqlBudget(): GqlBudget {
  const budget = loadBudget();

  const accounts: GqlAccount[] = [];
  for (const account of budget.findActiveAccounts()) {
    const gqlAccount: GqlAccount = {
      name: account.name,
      ...(account.description && { description: account.description }),
      type: account.typeIdName,
      number: account.number,
      ...(account.openedOn && { openedOn: account.openedOn }),
      ...(account.closedOn && { closedOn: account.closedOn }),
      owners: account.owners,
      url: account.url,
      phone: account.phone,
      address: account.address,
      userName: account.userName,
      password: account.password,
      external: account.isExternal,
      summary: account.isSummary
    };
    accounts.push(gqlAccount);
  }
  accounts.sort((a, b) => (a.name == b.name ? 0 : (a.name ?? '') < (b.name ?? '') ? -1 : 1));

  const transactionStatementTable = buildTransactionStatementTable(budget);
  const statements: GqlStatement[] = [];
  for (const statement of transactionStatementTable) {
    statements.push({
      name: statement.name,
      month: statement.month,
      inFlows: statement.inFlows,
      outFlows: statement.outFlows,
      income: statement.income,
      totalPayments: statement.totalPayments,
      totalTransfers: statement.totalTransfers,
      change: statement.change,
      addSub: statement.addSub,
      percentChange:
        statement.percentChange &&
        Math.round((statement.percentChange + Number.EPSILON) * 100) / 100,
      unaccounted: statement.unaccounted,
      startBalance: {
        amount: statement.startBalance?.amount ?? 0,
        date: statement.startBalance?.date,
        type: BalanceType[statement.startBalance?.type ?? BalanceType.UNKNOWN]
      },
      endBalance: {
        amount: statement.endBalance?.amount,
        date: statement.endBalance?.date,
        type: BalanceType[statement.endBalance?.type ?? BalanceType.UNKNOWN]
      },
      isCovered: statement.isCovered,
      isProjectedCovered: statement.isProjectedCovered,
      hasProjectedTransfer: statement.hasProjectedTransfer,
      transactions: statement.transactions.map((t) => ({
        toAccountName: t.account.name,
        isExpense: t.type == TransactionType.EXPENSE,
        isIncome: t.type == TransactionType.INCOME,
        balance: {
          amount: t.balance.amount,
          date: t.balance.date,
          type: BalanceType[t.balance.type]
        },
        balanceFromStart: t.balanceFromStart,
        balanceFromEnd: t.balanceFromEnd,
        description: t.description
      })),
      isClosed:
        statement.account.closedOn?.isLess(statement.month) ||
        (statement.account.openedOn && statement.month.isLess(statement.account.openedOn)) ||
        false
    });
  }

  const summaryStatementTable = buildSummaryStatementTable(transactionStatementTable);
  const summaries: GqlSummaryStatement[] = [];
  for (const summary of summaryStatementTable) {
    summaries.push({
      name: summary.name,
      month: summary.month,
      accounts: summary.statements.map((statement) => statement.name).sort(),
      addSub: summary.addSub,
      change: summary.change,
      income: summary.income,
      inFlows: summary.inFlows,
      outFlows: summary.outFlows,
      percentChange:
        summary.percentChange && Math.round((summary.percentChange + Number.EPSILON) * 100) / 100,
      totalPayments: summary.totalPayments,
      totalTransfers: summary.totalTransfers,
      unaccounted: summary.unaccounted,
      ...(summary.startBalance && {
        startBalance: {
          amount: summary.startBalance.amount,
          date: summary.startBalance.date,
          type: BalanceType[summary.startBalance.type]
        }
      }),
      ...(summary.endBalance && {
        endBalance: {
          amount: summary.endBalance.amount,
          date: summary.endBalance.date,
          type: BalanceType[summary.endBalance.type]
        }
      })
    });
  }
  return {
    accounts,
    months: budget.months.sort((a, b) => -a.compareTo(b)),
    statements,
    summaries
  };
}

export default {
  Query: {
    files: listFiles,
    budget: buildGqlBudget
  },

  Date: new GraphQLScalarType({
    name: 'Date',
    description: 'Date representation in YYYY-MM-DD format.',
    parseValue(value: string): Date {
      return new Date(value); // value from the client
    },
    serialize(value: Date): string {
      return value.toISOString().split('T')[0];
    },
    parseLiteral(ast): Date | null {
      if (ast.kind === Kind.STRING) {
        return new Date(ast.value);
      }
      return null;
    }
  }),

  GqlMonth: new GraphQLScalarType({
    name: 'GqlMonth',
    description: 'Month representation in XxxYYYY format.',
    parseValue(value: string): Month {
      return Month.fromString(value); // value from the client
    },
    serialize(value: Month): string {
      return value.toString();
    },
    parseLiteral(ast): Month | null {
      if (ast.kind === Kind.STRING) {
        return Month.fromString(ast.value);
      }
      return null;
    }
  })
};
