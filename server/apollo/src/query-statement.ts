import { loadBudget } from '@tally/lib/data/loader';
import {
  GqlStatement,
  QueryStatementArgs
} from './types';
import { toGqlStatement } from './to-gql';

export async function buildStatement(_: any, args: QueryStatementArgs): Promise<GqlStatement> {
  const startTimeMs: number = Date.now();
  const payload = await loadBudget();
  const statement = payload.statements.get(args.account)?.get(args.month.toString());
  if (!statement) {
    throw new Error(
      `Did not find statement for ${args.owner} ${args.account} ${args.month}`
    );
  }
  const result = toGqlStatement(statement);
  console.log(`gql statement in ${Date.now() - startTimeMs}ms`);
  return result;
}
