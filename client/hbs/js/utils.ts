import { GqlBudget } from '@backend/types';
import { Account as TallyAccount, AccountType, Balance, BalanceType, Month } from '@tally-lib';
import { Statement, SummaryStatement} from './base';
import {Cell} from './cell';
import {Row} from './row';
import {gqlToAccount, gqlToBalance} from './gql_utils'


/**
 *  Create map of owner->type->accountname, it will be rendered in this format.
 * @param {Object<string, TallyAccount>} accountNameToAccount
 *   account name->account map
 * @return {Object<string, Object<string, string>>} map of
 *   owner->type->accountname.
 */
function getOwnerTypeAccountMap(
    accountNameToAccount: {[accountType: string]: TallyAccount}):
        {[ownerType:string]: {[accountType:string]: string[]}} {
  const ownerTypeAccountNames: {
    [ownerType:string]: {[accountType:string]: string[]}} = {};

  for (const accountName of Object.keys(accountNameToAccount)) {
    const account: TallyAccount = accountNameToAccount[accountName];
    for (const owner of account.owners) {
      let typeAccounts: {[accountName:string]: string[]};
      if (owner in ownerTypeAccountNames) {
        typeAccounts = ownerTypeAccountNames[owner];
      } else {
        ownerTypeAccountNames[owner] = typeAccounts = {};
      }

      let accountNames: string[];
      if (account.type in typeAccounts) {
        accountNames = typeAccounts[account.type];
      } else {
        typeAccounts[account.type] = accountNames = [];
      }
      accountNames.push(account.name);
    }
  }
  return ownerTypeAccountNames;
}

interface PopupMonthSummaryData {
  id: string,
  accountName: string,
  month: string,
  summary?: SummaryStatement,
  statements?: StatementEntry[]
}

interface StatementEntry {
  name: string,
  stmt: Statement
}

interface PopupMonthData {
  id: string,
  accountName: string,
  month: string,
  stmt: Statement
}

interface HeadingPopupData {
  id: string,
  account: TallyAccount
}

export type PopupData =
    PopupMonthSummaryData | PopupMonthData | HeadingPopupData;

interface CellData {
  cells: Cell[],
  popups: PopupData[],
}

/**
 * Computes data for summary cells.
 * @param {string} owner the id/name for the owner.
 * @param {string} name account type id
 * @param {Array<string>} months list of month names to compute summary for.
 * @param {Object<string, Object<string, Statement>>} statements
 *   mapping of statements by account and month.
 * @param {Object<String, SummaryStatement>} summaries
 *   mapping of summaries by month.
 * @return {CellData} list of cells and popus.
 */
function getSummaryCells(
    owner: string, name: string, months: string[],
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[month:string]: SummaryStatement}): CellData {
  const cellData: CellData = {
    cells: [],
    popups: [],
  };
  for (const month of months) {
    const summaryStmt: SummaryStatement = summaries[month];
    const id = owner + '_' + name + '_' + month;
    cellData.cells.push(new Cell(id, summaryStmt));
    const accounts = ('accounts' in summaryStmt) ? summaryStmt.accounts : [];
    cellData.popups.push({
      id,
      accountName: owner + ' ' + name,
      month,
      summary: summaryStmt,
      statements: accounts.map((accountName: string): StatementEntry => ({
        name: accountName, stmt: statements[accountName][month]})),
    });
  }
  return cellData;
}

interface MatrixDataView {
  months: string[],
  rows: Row[],
  popupCells: PopupData[]
}

/**
 * Builds dataView structure with months, rows and popup cell data.
 *
 * @param {Array<string>} months - descending, continuous list of months to show
 * @param {Object<string, TallyAccount>} accountNameToAccount
 *   account name to account map
 * @param {Object<string, Object<string, Statement>>} statements
 *   table indexed by account name and month of statements
 * @param {Object<string, Object<string, SummaryStatement>>} summaries
 *   table indexed by owner name + account type and month of
 *   statement summaries.
 * @return {MatrixDataView} dataview structure.
 */
export function transformBudgetData(
    months: string[],
    accountNameToAccount: {[accountName:string]: TallyAccount},
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {
      [ownerAccountType:string]: {[month:string]: SummaryStatement}}) {
  const dataView: MatrixDataView = {
    months,
    rows: [],
    popupCells: [],
  };
  const ownerTypeAccountNames =
      getOwnerTypeAccountMap(accountNameToAccount);
  const ownersSorted = getKeysSorted(ownerTypeAccountNames);

  for (const owner of ownersSorted) {
    const typeAccountNames = ownerTypeAccountNames[owner];
    dataView.rows.push(new Row(owner, 'SPACE', []));
    const summaryName = owner + ' SUMMARY';
    if (summaryName in summaries) {
      const cellData = getSummaryCells(
          owner, 'SUMMARY', months, statements, summaries[summaryName]);
      dataView.rows.push(new Row(owner, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
    const accountTypes = getKeysSorted(typeAccountNames);

    for (const accountType of accountTypes) {
      dataView.rows.push(new Row(
          '*** ' + owner + ' - ' + accountType + ' *** accounts',
          'SPACE', []));
      const accountNames = typeAccountNames[accountType].sort(
          (a, b) => a < b ? -1 : (a > b ? 1 : 0));
      for (const accountName of accountNames) {
        const accountStatements = statements[accountName];
        const cells = [];
        for (const month of months) {
          const stmt = accountStatements[month];
          const id = owner + '_' + accountName + '_' + month;
          cells.push(new Cell(id, stmt));
          const popupMonthData: PopupMonthData = {
            id,
            accountName,
            month,
            stmt,
          };
          dataView.popupCells.push(popupMonthData);
        }
        const account = accountNameToAccount[accountName];
        dataView.rows.push(new Row(account, 'NORMAL', cells));
        dataView.popupCells.push({
          id: account.name,
          account,
        });
      }
      const name = owner + ' ' + accountType;
      const cellData = getSummaryCells(
          owner, accountType, months, statements, summaries[name]);
      dataView.rows.push(new Row(name, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
  }
  return dataView;
}

/**
 * Builds dataView structure with months, rows and popup cell data.
 *
 * @param {GqlBudget} data - Server returnred GqlBudget structure.
 * @return {MatrixDataView} dataview structure.
 */
export function transformGqlBudgetData(data: GqlBudget): MatrixDataView {
  const accountNameToAccount: {[accountName:string]: TallyAccount} = {};
  for (const account of data.accounts) {
    accountNameToAccount[account.name] = gqlToAccount(account);
  }
  // account name => month => statement map.
  const statements: {[accountName:string]: {[month:string]: Statement}} = {};
  for (const stmt of data.statements) {
    const entry = statements[stmt.name] || (statements[stmt.name] = {});
    if (stmt.isClosed) {
      entry[stmt.month] = {
        isClosed: true,
      };
    } else {
      entry[stmt.month] = {
        inFlows: stmt.inFlows,
        outFlows: stmt.outFlows,
        isClosed: stmt.isClosed || undefined,
        isCovered: stmt.isCovered,
        isProjectedCovered: stmt.isProjectedCovered,
        hasProjectedTransfer: stmt.hasProjectedTransfer,
        income: stmt.income,
        totalPayments: stmt.totalPayments,
        totalTransfers: stmt.totalTransfers,
        ...(stmt.change == null ? {} : {change: stmt.change}),
        ...(stmt.percentChange == null ?
                {} : {percentChange: stmt.percentChange}),
        ...(stmt.unaccounted == null ?
                {} : {unaccounted: stmt.unaccounted}),
        startBalance: gqlToBalance(stmt.startBalance),
        endBalance: gqlToBalance(stmt.endBalance),
        addSub: stmt.inFlows + stmt.outFlows,
        transactions: stmt.transactions?.map((t)=>({
          toAccountName: t.toAccountName,
          isExpense: t.isExpense,
          isIncome: t.isIncome,
          balance: gqlToBalance(t.balance),
          ...(t.balanceFromStart != null &&
                  {balanceFromStart: t.balanceFromStart}),
          ...(t.balanceFromEnd != null && {balanceFromEnd: t.balanceFromEnd}),
          description: t.description || '',
        })) ?? [],
      };
    }
  }
  // owner + accout type => month => summary statement map.
  const summaries: {
    [ownerAccountType:string]: {[month:string]: SummaryStatement}} = {};
  for (const stmt of data.summaries) {
    const entry = summaries[stmt.name] || (summaries[stmt.name] = {});
    entry[stmt.month] = {
      accounts: stmt.accounts,
      inFlows: stmt.inFlows,
      outFlows: stmt.outFlows,
      income: stmt.income,
      totalPayments: stmt.totalPayments,
      totalTransfers: stmt.totalTransfers,
      addSub: stmt.addSub,
      startBalance: gqlToBalance(stmt.startBalance),
      endBalance: gqlToBalance(stmt.endBalance),
      ...(stmt.change == null ? {} : {change: stmt.change}),
      ...(stmt.percentChange == null ?
              {} : {percentChange: stmt.percentChange}),
      ...(stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
    };
  }
  return transformBudgetData(
      data.months, accountNameToAccount, statements, summaries);

  // return {
  //   months: data.months,
  //   accountNameToAccount,
  //   statements,
  //   summaries
  // };
}

/**
 * Get sorted list of object keys.
 * @param {Object} obj any object
 * @return {Array<string>} list of keys in sorted order.
 */
function getKeysSorted(obj: {[key:string]: any;}): string[] {
  return Object.keys(obj).sort((a, b) => a < b ? -1 : (a > b ? 1 : 0));
}
