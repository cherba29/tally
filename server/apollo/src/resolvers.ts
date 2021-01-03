import { Month } from './core/month';
import { listFiles, loadBudget } from './data/loader'
import { GraphQLScalarType, Kind } from 'graphql';
import { GqlAccount, GqlBudget, GqlStatement } from './types';

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

  const statements: GqlStatement[] = [];
  for (const account of budget.accounts.values()) {
    for (const month of budget.months) {
      statements.push({
        name: account.name,
        month,
        inFlows: 0,
        outFlows: 0,
        income: 0,
        totalPayments: 0,
        totalTransfers: 0,
      });
    }
  }
 
  return {
    accounts,
    months: budget.months,
    statements,
    summaries: []
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
