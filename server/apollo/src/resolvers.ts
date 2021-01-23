import {buildTransactionStatementTable} from './statement/transaction';
import {buildSummaryStatementTable} from './statement/summary';
import { Month } from './core/month';
import { listFiles, loadBudget } from './data/loader'
import { GraphQLScalarType, Kind } from 'graphql';
import { GqlAccount, GqlBudget, GqlStatement, GqlSummaryStatement } from './types';

function buildGqlBudget(): GqlBudget {
  const budget = loadBudget();
  
  const accounts: GqlAccount[] = [];
  for (const account of budget.accounts.values()) {
    const gqlAccount: GqlAccount = {
      name: account.name,
      ...(account.description && {description: account.description}),
      type: account.type,
      ...(account.number && {number: account.number}),
      ...(account.openedOn && {openedOn: account.openedOn}),
      ...(account.closedOn && {closedOn: account.closedOn}),
    };
    accounts.push(gqlAccount);
  }

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
      isClosed: statement.account.closedOn?.isLess(statement.month) ||
          statement.account.openedOn && statement.month.isLess(statement.account.openedOn) || false,
    });
  }
  const summaryStatementTable = buildSummaryStatementTable(transactionStatementTable);
  const summaries: GqlSummaryStatement[] = [];
  for (const summary of summaryStatementTable) {
    summaries.push({
      name: summary.name,
      month: summary.month,
      accounts: summary.statements.map(statement=>statement.name),
      addSub: summary.addSub,
      change: summary.change,
      inFlows: summary.inFlows,
      outFlows: summary.outFlows,
      percentChange: summary.percentChange,
      totalPayments: summary.totalPayments,
      totalTransfers: summary.totalTransfers,
      unaccounted: summary.unaccounted,
      ...(summary.startBalance && { startBalance: {
         amount: summary.startBalance.amount,
         date: summary.startBalance.date,
      }}),
      ...(summary.endBalance && { endBalance: {
        amount: summary.endBalance.amount,
        date: summary.endBalance.date,
     }})
    });
  }
  return {
    accounts,
    months: budget.months,
    statements,
    summaries,
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
      return new Date(value);  // value from the client
    },
    serialize(value: Date): string {
      return value.toISOString().split('T')[0];
    },
    parseLiteral(ast): Date | null {
      if (ast.kind === Kind.STRING) {
        return new Date(ast.value);
      }
      return null;
    },
  }),

  GqlMonth: new GraphQLScalarType({
    name: 'GqlMonth',
    description: 'Month representation in XxxYYYY format.',
    parseValue(value: string): Month {
      return Month.fromString(value);  // value from the client
    },
    serialize(value: Month): string {
      return value.toString();
    },
    parseLiteral(ast): Month | null {
      if (ast.kind === Kind.STRING) {
        return Month.fromString(ast.value);
      }
      return null;
    },
  }),
};
