import { TransactionStatement } from '@tally/lib/statement/transaction';
import { combineSummaryStatements } from '@tally/lib/statement/summary';
import { toGqlStatement, toGqlSummaryStatement } from './to-gql';
import { loadBudget } from '@tally/lib/data/loader';
import {
  GqlSummaryData,
  QuerySummaryArgs,
} from './types';


export async function buildSummaryData(_: any, args: QuerySummaryArgs): Promise<GqlSummaryData> {
  const startTimeMs: number = Date.now();
  const payload = await loadBudget();
  const summaryName = args.accountType.startsWith('/') ? args.accountType :
    (args.owner + ' ' + (args.accountType === args.owner ? 'SUMMARY' : args.accountType));
  const monthSummaries = payload.summaries.get2(args.owner, summaryName);
  if (!monthSummaries) {
    throw new Error(`Summary ${args.accountType} for ${args.owner} not found.`);
  }
  const summaryStatements = [...monthSummaries.values()].filter(
    (stmt) => stmt.month.isBetween(args.startMonth, args.endMonth)
  );
  if (!summaryStatements.length) {
    throw new Error(
      `Summary ${args.accountType} for ${args.owner} for months [${args.startMonth}, ${args.endMonth}] not found.`
    );
  }
  const summary =
    summaryStatements.length === 1
      ? summaryStatements[0]
      : combineSummaryStatements(summaryStatements);
  const result = {
    statements: summary.statements
      .sort((a, b) => (a.account.name < b.account.name ? -1 : 1))
      .map((stmt) => toGqlStatement(stmt as TransactionStatement)),
    total: toGqlSummaryStatement(summary)
  };
  console.log(
    `gql "${summaryName}" summary data in ${Date.now() - startTimeMs}ms for [${args.startMonth}, ${args.endMonth}]`
  );
  return result;
}
