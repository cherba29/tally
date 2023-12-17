import { TransactionStatement } from '@tally/lib/statement/transaction';
import { combineSummaryStatements } from '@tally/lib/statement/summary';
import { Type as BalanceType } from '@tally/lib/core/balance';
import { Month } from '@tally/lib/core/month';
import { listFiles, loadBudget } from '@tally/lib/data/loader';
import { GraphQLScalarType, Kind, ValueNode } from 'graphql';
import {
  GqlStatement,
  GqlSummaryData,
  GqlTable,
  GqlTableCell,
  GqlTableRow,
  QueryTableArgs,
  QuerySummaryArgs,
  QueryStatementArgs
} from './types';
import { Account } from '@tally/lib/core/account';
import { toGqlAccount, toGqlStatement, toGqlSummaryStatement } from './to-gql';

async function buildGqlTable(_: any, args: QueryTableArgs): Promise<GqlTable> {
  const startTimeMs: number = Date.now();
  const startMonth = args.startMonth.previous();
  const endMonth = args.endMonth.next();
  const payload = await loadBudget();
  const months = payload.budget.months
    .filter((m) => m.isLess(endMonth) && startMonth.isLess(m))
    .sort((a: Month, b: Month) => -a.compareTo(b));
  const activeAccounts = payload.budget.findActiveAccounts();
  const owners = [...new Set(activeAccounts.map((account) => account.owners).flat())].sort();
  const owner = args.owner || owners[0];

  const rows: GqlTableRow[] = [];
  // Insert Total summary row
  {
    const cells: GqlTableCell[] = [];
    const summaryMonthMap = payload.summaries.get(owner + ' SUMMARY');
    if (!summaryMonthMap) {
      throw new Error(`Not able to find total summary ${owner + ' SUMMARY'}`);
    }
    for (const month of months) {
      const summary = summaryMonthMap?.get(month.toString());
      if (summary) {
        cells.push({
          month,
          addSub: summary.addSub,
          balance: summary.endBalance?.amount,
          percentChange:
            summary.percentChange &&
            Math.round((summary.percentChange + Number.EPSILON) * 100) / 100,
          unaccounted: summary.unaccounted,
          isProjected: summary.endBalance?.type !== BalanceType.CONFIRMED,
          balanced: !summary.unaccounted
        });
      } else {
        cells.push({});
      }
    }
    rows.push({ title: owner, isTotal: true, cells });
  }

  // Group accounts per type
  const accounts = activeAccounts.filter((a) => a.owners.includes(owner));
  const accountTypesToAccounts = new Map<string, Account[]>();
  for (const account of accounts) {
    const accountTypeId = account.typeIdName;
    let accountsOfType = accountTypesToAccounts.get(accountTypeId);
    if (!accountsOfType) {
      accountsOfType = [account];
      accountTypesToAccounts.set(accountTypeId, accountsOfType);
    } else {
      accountsOfType.push(account);
    }
  }
  for (const accountType of [...accountTypesToAccounts.keys()].sort()) {
    rows.push({ title: accountType + ' accounts', isSpace: true, cells: [] });
    const groupedAccounts =
      accountTypesToAccounts.get(accountType)?.sort((a, b) => (a.name > b.name ? 1 : -1)) ?? [];
    for (const account of groupedAccounts) {
      const cells: GqlTableCell[] = [];
      let isClosed = true;
      for (const month of months) {
        const stmt = payload.statements.get(account.name)?.get(month.toString());
        isClosed = isClosed && (stmt?.isClosed ?? false);
        cells.push({
          month,
          isClosed: stmt?.isClosed,
          addSub: stmt?.addSub,
          balance: stmt?.endBalance?.amount,
          isProjected:
            (stmt?.endBalance && stmt?.endBalance.type !== BalanceType.CONFIRMED) ||
            stmt?.hasProjectedTransfer,
          isCovered: stmt?.isCovered,
          isProjectedCovered: stmt?.isProjectedCovered,
          hasProjectedTransfer: stmt?.hasProjectedTransfer,
          percentChange:
            stmt?.percentChange && Math.round((stmt.percentChange + Number.EPSILON) * 100) / 100,
          unaccounted: stmt?.unaccounted,
          balanced: !stmt?.unaccounted
        });
      }
      if (!isClosed) {
        // Dont add accounts which are closed over selected timeframe.
        rows.push({ title: account.name, account: toGqlAccount(account), isNormal: true, cells });
      }
    }
    // Summary for each account type.
    const summaryMonthMap = payload.summaries.get(owner + ' ' + accountType);
    const cells: GqlTableCell[] = [];
    let isClosed = true;
    for (const month of months) {
      const stmt = summaryMonthMap?.get(month.toString());
      isClosed = isClosed && (stmt?.isClosed ?? false);
      cells.push({
        month,
        isClosed: stmt?.isClosed,
        addSub: stmt?.addSub,
        balance: stmt?.endBalance?.amount,
        isProjected: stmt?.endBalance?.type !== BalanceType.CONFIRMED,
        percentChange:
          stmt?.percentChange && Math.round((stmt.percentChange + Number.EPSILON) * 100) / 100,
        annualizedPercentChange:
          stmt?.annualizedPercentChange &&
          Math.round((stmt.annualizedPercentChange + Number.EPSILON) * 100) / 100,
        unaccounted: stmt?.unaccounted,
        balanced: !stmt?.unaccounted
      });
    }
    if (!isClosed) {
      // Dont add accounts which are closed over selected timeframe.
      rows.push({ title: accountType, isTotal: true, cells });
    }
  }
  console.log(`gql table ${args.endMonth}--${args.startMonth} in ${Date.now() - startTimeMs}ms`);
  return {
    currentOwner: owner,
    owners,
    months,
    rows
  };
}

async function buildSummaryData(_: any, args: QuerySummaryArgs): Promise<GqlSummaryData> {
  const startTimeMs: number = Date.now();
  const payload = await loadBudget();
  const summaryName =
    args.owner + ' ' + (args.accountType === args.owner ? 'SUMMARY' : args.accountType);
  const monthSummaries = payload.summaries.get(summaryName);
  if (!monthSummaries) {
    throw new Error(`Summary ${args.accountType} for ${args.owner}  not found.`);
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
    `gql summary data in ${Date.now() - startTimeMs}ms for [${args.startMonth}, ${args.endMonth}]`
  );
  return result;
}

async function buildStatement(_: any, args: QueryStatementArgs): Promise<GqlStatement> {
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
