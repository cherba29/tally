import {
  buildTransactionStatementTable,
  Transaction,
  TransactionStatement,
  Type as TransactionType
} from '@tally/lib/statement/transaction';
import { buildSummaryStatementTable, SummaryStatement } from '@tally/lib/statement/summary';
import { Type as BalanceType } from '@tally/lib/core/balance';
import { Month } from '@tally/lib/core/month';
import { listFiles, loadBudget } from '@tally/lib/data/loader';
import { GraphQLScalarType, Kind, ValueNode } from 'graphql';
import {
  GqlAccount,
  GqlBudget,
  GqlStatement,
  GqlSummaryStatement,
  GqlSummaryData,
  GqlTable,
  GqlTableCell,
  GqlTableRow,
  QueryTableArgs,
  QuerySummaryArgs
} from './types';
import { Account } from '@tally/lib';

function toGqlAccount(account: Account): GqlAccount {
  return {
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
}

function toGqlStatement(statement: TransactionStatement): GqlStatement {
  return {
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
      statement.percentChange && Math.round((statement.percentChange + Number.EPSILON) * 100) / 100,
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
      description: t.description
    })),
    isClosed:
      statement.account.closedOn?.isLess(statement.month) ||
      (statement.account.openedOn && statement.month.isLess(statement.account.openedOn)) ||
      false
  };
}

function toGqlSummaryStatement(summary: SummaryStatement): GqlSummaryStatement {
  return {
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
  };
}

async function buildGqlBudget(): Promise<GqlBudget> {
  const startTimeMs: number = Date.now();
  const budget = await loadBudget();

  const accounts: GqlAccount[] = budget.findActiveAccounts().map(toGqlAccount);
  accounts.sort((a, b) => (a.name == b.name ? 0 : (a.name ?? '') < (b.name ?? '') ? -1 : 1));

  const transactionStatementTable = buildTransactionStatementTable(budget);
  const statements: GqlStatement[] = [];
  for (const statement of transactionStatementTable) {
    statements.push(toGqlStatement(statement));
  }

  const summaryStatementTable = buildSummaryStatementTable(transactionStatementTable);
  const summaries: GqlSummaryStatement[] = [];
  for (const summary of summaryStatementTable) {
    summaries.push(toGqlSummaryStatement(summary));
  }
  console.log(`gql budget in ${Date.now() - startTimeMs}ms`);
  return {
    accounts,
    months: budget.months.sort((a: Month, b: Month) => -a.compareTo(b)),
    statements,
    summaries
  };
}

async function buildGqlTable(_: any, args: QueryTableArgs): Promise<GqlTable> {
  const startTimeMs: number = Date.now();
  const budget = await loadBudget();
  const months = budget.months.sort((a: Month, b: Month) => -a.compareTo(b));
  const activeAccounts = budget.findActiveAccounts();
  const owners = [...new Set(activeAccounts.map((account) => account.owners).flat())].sort();
  const owner = args.owner || owners[0];
  const transactionStatementTable: TransactionStatement[] = buildTransactionStatementTable(
    budget,
    owner
  );
  const accoutToMonthToTransactionStatement = new Map<string, Map<string, TransactionStatement>>();
  for (const stmt of transactionStatementTable) {
    let monthToStatement = accoutToMonthToTransactionStatement.get(stmt.account.name);
    if (!monthToStatement) {
      monthToStatement = new Map<string, TransactionStatement>();
      accoutToMonthToTransactionStatement.set(stmt.account.name, monthToStatement);
    }
    monthToStatement.set(stmt.month.toString(), stmt);
  }
  const summaryStatementTable = buildSummaryStatementTable(transactionStatementTable, owner);
  const summaryNameMonthMap = new Map<string, Map<string, SummaryStatement>>();
  for (const summary of summaryStatementTable) {
    let monthToSummary = summaryNameMonthMap.get(summary.name);
    if (!monthToSummary) {
      monthToSummary = new Map();
      summaryNameMonthMap.set(summary.name, monthToSummary);
    }
    monthToSummary.set(summary.month.toString(), summary);
  }
  const rows: GqlTableRow[] = [];
  // Insert Total summary row
  {
    const cells: GqlTableCell[] = [];
    const summaryMonthMap = summaryNameMonthMap.get(owner + ' SUMMARY');
    for (const month of months) {
      const summary = summaryMonthMap?.get(month.toString());
      if (summary) {
        cells.push({
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
      for (const month of months) {
        const stmt = accoutToMonthToTransactionStatement.get(account.name)?.get(month.toString());
        cells.push({
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
      rows.push({ title: account.name, account: toGqlAccount(account), isNormal: true, cells });
    }
    // Summary for each account type.
    const summaryMonthMap = summaryNameMonthMap.get(owner + ' ' + accountType);
    const cells: GqlTableCell[] = [];
    for (const month of months) {
      const stmt = summaryMonthMap?.get(month.toString());
      cells.push({
        isClosed: stmt?.isClosed,
        addSub: stmt?.addSub,
        balance: stmt?.endBalance?.amount,
        isProjected: stmt?.endBalance?.type !== BalanceType.CONFIRMED,
        percentChange:
          stmt?.percentChange && Math.round((stmt.percentChange + Number.EPSILON) * 100) / 100,
        unaccounted: stmt?.unaccounted,
        balanced: !stmt?.unaccounted
      });
    }
    rows.push({ title: accountType, isTotal: true, cells });
  }
  console.log(`gql table in ${Date.now() - startTimeMs}ms`);
  return {
    currentOwner: owner,
    owners,
    months,
    rows
  };
}

async function buildSummaryData(_: any, args: QuerySummaryArgs): Promise<GqlSummaryData> {
  const startTimeMs: number = Date.now();
  const budget = await loadBudget();
  const transactionStatementTable: TransactionStatement[] = buildTransactionStatementTable(
    budget,
    args.owner
  );
  const summaryStatementTable = buildSummaryStatementTable(transactionStatementTable, args.owner);
  const summaryName =
    args.owner + ' ' + (args.accountType === args.owner ? 'SUMMARY' : args.accountType);
  let summary = undefined;
  for (const stmt of summaryStatementTable) {
    // console.log(`### '${stmt.name}' and '${stmt.month}'`);
    if (stmt.name === summaryName && stmt.month.toString() === args.month.toString()) {
      summary = stmt;
      break;
    }
  }
  if (!summary) {
    throw new Error(
      `Summary ${args.accountType} for ${args.owner} for month ${args.month} not found.`
    );
  }
  const payload = {
    statements: summary.statements.map((stmt) => toGqlStatement(stmt as TransactionStatement)),
    total: toGqlSummaryStatement(summary)
  };
  console.log(`gql table in ${Date.now() - startTimeMs}ms`);
  return payload;
}

export default {
  Query: {
    files: listFiles,
    budget: buildGqlBudget,
    table: buildGqlTable,
    summary: buildSummaryData
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
