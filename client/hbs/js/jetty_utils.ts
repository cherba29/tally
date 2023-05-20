import {Account, Type as AccountType} from '@tally/lib/core/account';
import {Balance, Type as BalanceType} from '@tally/lib/core/balance';
import {Month} from '@tally/lib/core/month';
import {Statement, SummaryStatement, Transaction} from './base';
import {transformBudgetData, MatrixDataView} from './utils';

interface JettyAccount {
  name: string;
  type: string;
  description?: string;
  number?: string;
  owners: string[];
  openedOn: string | null;
  closedOn: string | null;
  url: string | null;
  username: string | null;
  password: string | null;
  phone: string | null;
  address: string | null;
  external: boolean;
  summary: boolean;
}

interface JettyBalance {
  amount: number;
  date: string;
  type: string;
}

interface JettyTransaction {
  balance: JettyBalance;
  balanceFromEnd: number;
  balanceFromStart: number;
  description: string;
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
}

interface JettyStatement {
  addSub: number;
  change: number;
  endBalance: JettyBalance;
  hasProjectedTransfer: boolean;
  inFlows: number;
  income: number;
  isClosed: boolean;
  isCovered: boolean;
  isProjectedCovered: boolean;
  outFlows: number;
  percentChange: number;
  startBalance: JettyBalance;
  totalPayments: number;
  totalTransfers: number;
  transactions: JettyTransaction[];
  unaccounted: number;
}

interface JettySummaryStatement {
  accounts: string[];
  addSub: number;
  change: number;
  endBalance: JettyBalance;
  inFlows: number;
  income: number;
  outFlows: number;
  percentChange: number;
  startBalance: JettyBalance;
  totalPayments: number;
  totalTransfers: number;
  unaccounted: number;
}

interface JettyResponseData {
  months: string[];
  accountNameToAccount: {[accountName: string]: JettyAccount};
  statements: {[key: string]: JettyStatement};
  summaries: {
    [ownerAccountType: string]: {
      [month: string]: JettySummaryStatement;
    };
  };
}

/** Response returned by Jetty server. */
export interface JettyResponse {
  data: JettyResponseData;
  success: boolean;
  message: string;
}

/**
 * Convert jetty json representation to Account.
 * @param acc - jetty json for account.
 * @returns Account object.
 */
function jettyToAccount(acc: JettyAccount): Account {
  return new Account({
    name: acc.name || '',
    description: acc.description || '',
    openedOn: acc.openedOn ? Month.fromString(acc.openedOn) : undefined,
    closedOn: acc.closedOn ? Month.fromString(acc.closedOn) : undefined,
    owners: (acc.owners || []).filter((item) => item).map((item) => item as string),
    url: acc.url || '',
    type: (acc.type || AccountType.EXTERNAL) as AccountType,
    address: acc.address || undefined,
    userName: acc.username || undefined,
    number: acc.number || undefined,
    phone: acc.phone || undefined,
    password: acc.password || undefined,
  });
}

/**
 * Convert jettyBalance to Balance type.
 * @param jettyBalance - jetty json for Balance.
 * @returns Balance object.
 */
function jettyToBalance(jettyBalance: JettyBalance | null | undefined): Balance | undefined {
  if (!jettyBalance || jettyBalance.amount === null) {
    return undefined;
  }
  return new Balance(
    jettyBalance.amount,
    new Date(jettyBalance.date),
    jettyBalance.type as BalanceType
  );
}

/**
 * Convert jettyType transaction type.
 * @param tran jetty json for transaction.
 * @returns Transaction object.
 */
function jettyToTransaction(tran: JettyTransaction | null): Transaction {
  if (!tran) {
    throw new Error(`Undefined transaction`);
  }
  const balance = jettyToBalance(tran.balance ?? null);
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
 * Convert jettyType Statement type.
 * @param stmt - jetty json for statement.
 * @returns Statement object.
 */
function jettyToStatement(stmt: JettyStatement): Statement {
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
    startBalance: jettyToBalance(stmt.startBalance),
    endBalance: jettyToBalance(stmt.endBalance),
    addSub: (stmt.inFlows || 0) + (stmt.outFlows || 0),
    transactions: (stmt.transactions || []).map((t) => jettyToTransaction(t)) ?? [],
  };
}

/**
 * Convert jettyType SummaryStatement type.
 * @param stmt - jetty json for SummaryStatement.
 * @returns SummaryStatement object.
 */
function jettyToSummaryStatement(stmt: JettySummaryStatement): SummaryStatement {
  return {
    accounts: (stmt.accounts || []).filter((a) => a).map((a) => a as string),
    inFlows: stmt.inFlows ?? 0,
    outFlows: stmt.outFlows ?? 0,
    income: stmt.income ?? 0,
    totalPayments: stmt.totalPayments ?? 0,
    totalTransfers: stmt.totalTransfers ?? 0,
    addSub: stmt.addSub ?? 0,
    startBalance: jettyToBalance(stmt.startBalance),
    endBalance: jettyToBalance(stmt.endBalance),
    ...(stmt.change == null ? {} : {change: stmt.change}),
    ...(stmt.percentChange == null ? {} : {percentChange: stmt.percentChange}),
    ...(stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
  };
}

/**
 * Transform jetty json response data to matrix data view.
 * @param data - jetty json response data.
 * @returns MatrixDataView.
 */
export function transformJettyBudgetData(data: JettyResponseData): MatrixDataView {
  const accountNameToAccount: {[accountToName: string]: Account} = {};
  for (const [name, jettyAccount] of Object.entries(data.accountNameToAccount)) {
    accountNameToAccount[name] = jettyToAccount(jettyAccount);
  }

  const statements: {[accountName: string]: {[month: string]: Statement}} = {};
  for (const [name, jettyMonthToStatement] of Object.entries(data.statements)) {
    const monthStatements: {[month: string]: Statement} = (statements[name] = {});
    for (const [jettyMonth, jettyStatement] of Object.entries(jettyMonthToStatement)) {
      monthStatements[jettyMonth] = jettyToStatement(jettyStatement);
    }
  }

  const summaries: {[ownerAccountType: string]: {[month: string]: SummaryStatement}} = {};
  for (const [ownerAccountType, monthToJettySummaryStatement] of Object.entries(data.summaries)) {
    const monthSummaryStatements: {[month: string]: SummaryStatement} = (summaries[
      ownerAccountType
    ] = {});
    for (const [month, jettySummaryStatement] of Object.entries(monthToJettySummaryStatement)) {
      monthSummaryStatements[month] = jettyToSummaryStatement(jettySummaryStatement);
    }
  }
  return transformBudgetData(data.months, accountNameToAccount, statements, summaries);
}
