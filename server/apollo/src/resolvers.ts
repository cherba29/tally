import {
  buildTransactionStatementTable,
  Transaction,
  Type as TransactionType
} from '@tally/lib/statement/transaction';
import { buildSummaryStatementTable } from '@tally/lib/statement/summary';
import { Type as BalanceType } from '@tally/lib/core/balance';
import { Month } from '@tally/lib/core/month';
import { listFiles, loadBudget } from '@tally/lib/data/loader';
import { GraphQLScalarType, Kind, ValueNode } from 'graphql';
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
        type: BalanceType[(statement.startBalance?.type ?? BalanceType.UNKNOWN) as BalanceType]
      },
      endBalance: {
        amount: statement.endBalance?.amount,
        date: statement.endBalance?.date,
        type: BalanceType[(statement.endBalance?.type ?? BalanceType.UNKNOWN) as BalanceType]
      },
      isCovered: statement.isCovered,
      isProjectedCovered: statement.isProjectedCovered,
      hasProjectedTransfer: statement.hasProjectedTransfer,
      transactions: statement.transactions.map((t: Transaction) => ({
        toAccountName: t.account.name,
        isExpense: t.type == TransactionType.EXPENSE,
        isIncome: t.type == TransactionType.INCOME,
        balance: {
          amount: t.balance.amount,
          date: t.balance.date,
          type: BalanceType[t.balance.type as BalanceType]
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
          type: BalanceType[summary.startBalance.type as BalanceType]
        }
      }),
      ...(summary.endBalance && {
        endBalance: {
          amount: summary.endBalance.amount,
          date: summary.endBalance.date,
          type: BalanceType[summary.endBalance.type as BalanceType]
        }
      })
    });
  }
  return {
    accounts,
    months: budget.months.sort((a: Month, b: Month) => -a.compareTo(b)),
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
    parseValue(value: unknown): Date {
      if (typeof value === 'string') {
        return new Date(value); // value from the client
      }
      throw new Error('GraphQL Date Scalar parser expected a `string`');
    },
    serialize(value: unknown): string {
      if (value instanceof Date) {
        return value.toISOString().split('T')[0];
      }
      throw Error('GraphQL Date Scalar serializer expected a `Date` object');
    },
    parseLiteral(ast: ValueNode): Date | null {
      if (ast.kind === Kind.STRING) {
        return new Date(ast.value);
      }
      return null;
    }
  }),

  GqlMonth: new GraphQLScalarType({
    name: 'GqlMonth',
    description: 'Month representation in XxxYYYY format.',
    parseValue(value: unknown): Month {
      if (typeof value === 'string') {
        return Month.fromString(value); // value from the client
      }
      throw new Error('GraphQL GqlMonth Scalar parser expected a `string`');
    },
    serialize(value: unknown): string {
      if (value instanceof Month) {
        return value.toString();
      }
      throw Error('GraphQL GqlMonth Scalar serializer expected a `Month` object');
    },
    parseLiteral(ast: ValueNode): Month | null {
      if (ast.kind === Kind.STRING) {
        return Month.fromString(ast.value);
      }
      return null;
    }
  })
};
