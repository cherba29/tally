import {Account, Type as AccountType} from '@tally/lib/core/account';
import {Balance, Type as BalanceType} from '@tally/lib/core/balance';
import {Month} from '@tally/lib/core/month';
import {Statement, SummaryStatement, Transaction} from './base';
import {
  GqlBalance,
  GqlAccount,
  GqlBudget,
  GqlStatement,
  GqlTransaction,
  GqlSummaryStatement,
} from './gql_types';
import {transformBudgetData, MatrixDataView} from './utils';

/**
 * Convert gqlType Account type.
 * @param gqlAccount - backend object with GqlAccount fields.
 * @return Account object.
 */
function gqlToAccount(gqlAccount: GqlAccount): Account {
  return new Account({
    name: gqlAccount.name || '',
    description: gqlAccount.description || '',
    openedOn: gqlAccount.openedOn ? Month.fromString(gqlAccount.openedOn) : undefined,
    closedOn: gqlAccount.closedOn ? Month.fromString(gqlAccount.closedOn) : undefined,
    owners: (gqlAccount.owners || []).filter((item) => item).map((item) => item as string),
    url: gqlAccount.url || '',
    type: (gqlAccount.type || AccountType.EXTERNAL) as AccountType,
    address: gqlAccount.address || undefined,
    userName: gqlAccount.userName || undefined,
    number: gqlAccount.number || undefined,
    phone: gqlAccount.phone || undefined,
    password: gqlAccount.password || undefined,
  });
}

/**
 * Convert gqlType Balance type.
 * @param gqlBalance - backend object with GqlBalance fields.
 * @return Balance object.
 */
export function gqlToBalance(gqlBalance: GqlBalance | null | undefined): Balance | undefined {
  if (!gqlBalance || gqlBalance.amount === null) {
    return undefined;
  }
  return new Balance(
    gqlBalance?.amount ?? 0,
    new Date(gqlBalance.date),
    gqlBalance.type as BalanceType
  );
}

/**
 * Convert gqlType Statement type.
 * @param tran - backend object with GqlTransaction fields.
 * @return Transaction object.
 */
function gqlToTransaction(tran: GqlTransaction | null): Transaction {
  if (!tran) {
    throw new Error(`Undefined transaction`);
  }
  const balance = gqlToBalance(tran.balance ?? null);
  if (!balance) {
    throw new Error(`Undefined balance in transaction to ${tran.toAccountName}`);
  }
  return {
    toAccountName: tran.toAccountName || '',
    isExpense: tran.isExpense ?? false,
    isIncome: tran.isIncome ?? false,
    balance,
    balanceFromStart: tran.balanceFromStart || 0,
    balanceFromEnd: tran.balanceFromEnd || 0,
    description: tran.description || '',
  };
}

/**
 * Convert gqlType Statement type.
 * @param stmt - backend object with GqlStatement fields.
 * @return Statement object.
 */
function gqlToStatement(stmt: GqlStatement): Statement {
  if (stmt.isClosed) {
    return {
      isClosed: true,
    };
  }
  return {
    inFlows: stmt.inFlows ?? undefined,
    outFlows: stmt.outFlows ?? undefined,
    isClosed: stmt.isClosed ?? undefined,
    isCovered: stmt.isCovered ?? undefined,
    isProjectedCovered: stmt.isProjectedCovered ?? undefined,
    hasProjectedTransfer: stmt.hasProjectedTransfer ?? undefined,
    income: stmt.income ?? undefined,
    totalPayments: stmt.totalPayments ?? undefined,
    totalTransfers: stmt.totalTransfers ?? undefined,
    ...(stmt.change == null ? {} : {change: stmt.change}),
    ...(stmt.percentChange == null ? {} : {percentChange: stmt.percentChange}),
    ...(stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
    startBalance: gqlToBalance(stmt.startBalance),
    endBalance: gqlToBalance(stmt.endBalance),
    addSub: (stmt.inFlows || 0) + (stmt.outFlows || 0),
    transactions: (stmt.transactions || []).map((t) => gqlToTransaction(t)) ?? [],
  };
}

/**
 * Convert gqlType SummaryStatement type.
 * @param stmt - backend object with GqlSummaryStatement fields.
 * @return SummaryStatement object.
 */
function gqlToSummaryStatement(stmt: GqlSummaryStatement): SummaryStatement {
  return {
    accounts: (stmt.accounts || []).filter((a) => a).map((a) => a as string),
    inFlows: stmt.inFlows ?? 0,
    outFlows: stmt.outFlows ?? 0,
    income: stmt.income ?? 0,
    totalPayments: stmt.totalPayments ?? 0,
    totalTransfers: stmt.totalTransfers ?? 0,
    addSub: stmt.addSub ?? 0,
    startBalance: gqlToBalance(stmt.startBalance),
    endBalance: gqlToBalance(stmt.endBalance),
    ...(stmt.change == null ? {} : {change: stmt.change}),
    ...(stmt.percentChange == null ? {} : {percentChange: stmt.percentChange}),
    ...(stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
  };
}

/**
 * Builds dataView structure with months, rows and popup cell data.
 *
 * @param data - Server returnred GqlBudget structure.
 * @return MatrixDataView dataview structure.
 */
export function transformGqlBudgetData(data: GqlBudget | undefined): MatrixDataView {
  const accountNameToAccount: {[accountName: string]: Account} = {};
  for (const gqlAccount of data?.accounts ?? []) {
    if (!gqlAccount) continue;
    const account = gqlToAccount(gqlAccount);
    accountNameToAccount[account.name] = account;
  }
  // account name => month => statement map.
  const statements: {[accountName: string]: {[month: string]: Statement}} = {};
  for (const stmt of data?.statements ?? []) {
    if (!stmt || !stmt.name) continue;
    const entry = statements[stmt.name] || (statements[stmt.name] = {});
    entry[stmt.month] = gqlToStatement(stmt);
  }
  // owner + accout type => month => summary statement map.
  const summaries: {
    [ownerAccountType: string]: {[month: string]: SummaryStatement};
  } = {};
  for (const stmt of data?.summaries ?? []) {
    if (!stmt || !stmt.name) continue;
    const entry = summaries[stmt.name] || (summaries[stmt.name] = {});
    entry[stmt.month] = gqlToSummaryStatement(stmt);
  }
  return transformBudgetData(data?.months || [], accountNameToAccount, statements, summaries);

  // return {
  //   months: data.months,
  //   accountNameToAccount,
  //   statements,
  //   summaries
  // };
}
