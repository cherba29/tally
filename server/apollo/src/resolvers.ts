import { Month } from '@tally/lib/core/month';
import { listFiles } from '@tally/lib/data/loader';
import { GraphQLScalarType, Kind, ValueNode } from 'graphql';
import { buildSummaryData } from './query-summary';
import { buildGqlTable } from './query-table';
import { buildStatement } from './query-statement';

export default {
  Query: {
    files: listFiles,
    table: buildGqlTable,
    summary: buildSummaryData,
    statement: buildStatement
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
