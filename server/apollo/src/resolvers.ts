import {Month} from './core/month';
import { loadTallyConfig } from './data/config'
import { listFiles, loadStatements, loadAccounts } from './data/loader'
import { GraphQLScalarType, Kind } from 'graphql';

export default {
  Query: {
    accounts: loadAccounts,
    files: listFiles,
    statements: loadStatements,
    months: (): Month[] => {
      const config = loadTallyConfig();
      const start = Month.fromString(config.budget_period.start);
      const end = Month.fromString(config.budget_period.end).next();
      return Array.from(Month.generate(start, end));
    },
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
