import { GqlBudget } from '@backend/types';
import {TallyAccount, Statement, SummaryStatement} from './base';
import {Cell} from './cell';
import {Row} from './row';

/**
 *  Create map of owner->type->accountname, it will be rendered in this format.
 * @param accountNameToAccount - account name->account map
 */
function getOwnerTypeAccountMap(
    accountNameToAccount: {[accountType: string]: TallyAccount}):
        {[ownerType:string]: {[accountType:string]: string[]}} {
  const ownerTypeAccountNames: {
    [ownerType:string]: {[accountType:string]: string[]}} = {};

  for (const accountName of Object.keys(accountNameToAccount)) {
    const account: TallyAccount = accountNameToAccount[accountName]
    for (const owner of account.owners) {
      let typeAccounts: {[accountName:string]: string[]};
      if (owner in ownerTypeAccountNames) {
        typeAccounts = ownerTypeAccountNames[owner];
      } else {
        ownerTypeAccountNames[owner] = typeAccounts = {}
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

interface MonthTransferMatrix {
  accounts: {
    name: string;
    transfers: {[accountName:string]: number},
    total:number
  }[],
  total: number
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

export type PopupData = PopupMonthSummaryData | PopupMonthData | HeadingPopupData;


interface CellData {
  cells: Cell[],
  popups: PopupData[],
}

// Computes data for summary cells.
function getSummaryCells(
    owner: string, name: string, months: string[],
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[month:string]: SummaryStatement}): CellData {

  const cellData: CellData = {
    cells: [],
    popups: []
  };
  for (const month of months) {
    const summaryStmt: SummaryStatement = summaries[month];
    const id = owner + "_" + name + "_" + month;
    cellData.cells.push(new Cell(id, summaryStmt));
    const accounts = ('accounts' in summaryStmt) ? summaryStmt.accounts : [];
    cellData.popups.push({
      id,
      accountName: owner + " " + name,
      month,
      summary: summaryStmt,
      statements: accounts.map((accountName: string): StatementEntry => ({
          name: accountName, stmt: statements[accountName][month] }))
    });
  }
  return cellData;
}

function getPopupData(
  id: string, owner: string, name: string,
  month: string, stmt: Statement): PopupMonthData {
  return {
    id,
    accountName: name,
    month,
    stmt
  };
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
 * @param accountNameToAccount - account name to account map
 * @param statements - table indexed by account name and month of statements
 * @param summaries - table indexed by owner name + account type and month
 *                    of statement summaries.
 */
export function transformBudgetData(
    months: string[], accountNameToAccount: {[accountName:string]: TallyAccount},
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[ownerAccountType:string]: {[month:string]: SummaryStatement}}) {
  const dataView: MatrixDataView = {
    months,
    rows: [],
    popupCells: []
  };
  const ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  const ownersSorted = getKeysSorted(ownerTypeAccountNames);

  for (const owner of ownersSorted) {
    const typeAccountNames = ownerTypeAccountNames[owner];
    dataView.rows.push(new Row(owner, 'SPACE', []));
    const summaryName = owner + " SUMMARY";
    if (summaryName in summaries) {
      const cellData = getSummaryCells(
          owner, "SUMMARY", months, statements, summaries[summaryName]);
      dataView.rows.push(new Row(owner, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
    const accountTypes = getKeysSorted(typeAccountNames);

    for (const accountType of accountTypes) {
      dataView.rows.push(new Row(
          "*** " + owner + " - " + accountType + " *** accounts", 'SPACE', []));
      const accountNames = typeAccountNames[accountType].sort(
          (a, b) => a < b ? -1 : (a > b ? 1 : 0));
      for (const accountName of accountNames) {
        const accountStatements = statements[accountName];
        const cells = [];
        for (const month of months) {
          const stmt = accountStatements[month];
          const id = owner + "_" + accountName + "_" + month;
          cells.push(new Cell(id, stmt));
          dataView.popupCells.push(
              getPopupData(id, owner, accountName, month, stmt))
        }
        const account = accountNameToAccount[accountName];
        dataView.rows.push(new Row(account, 'NORMAL', cells));
        dataView.popupCells.push({
          id: account.name,
          account
        });
      }
      const name = owner + " " + accountType;
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
 * @param data - Server returnred GqlBudget structure.
 */
export function transformGqlBudgetData(data: GqlBudget) {
  const accountNameToAccount: {[accountName:string]: TallyAccount} = {};
  // Object.assign({}, ...data.accounts.map((x) => ({[x.name]: x})));;
  for (const account of data.accounts) {
    accountNameToAccount[account.name] = {
      name: account.name,
      description: account.description,
      openedOn: account.openedOn,
      closedOn: account.closedOn,
      owners: account.owners ?? [],
      url: account.url,
      type: account.type,
      address: account.address,
      userName: account.userName,
      number: account.number,
      phone: account.phone,
      password: account.password,
      summary: account.summary,
      external: account.external,
    };
  }
  // account name => month => statement map.
  const statements: {[accountName:string]: {[month:string]: Statement}} = {};
  for (const stmt of data.statements) {
    const entry = statements[stmt.name] || (statements[stmt.name] = {});
    if (stmt.isClosed) {
      entry[stmt.month] = {
        isClosed: true
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
        ... (stmt.change == null ? {} : {change: stmt.change}),
        ... (stmt.percentChange == null ? {} : {percentChange: stmt.percentChange}),
        ... (stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
        startBalance: (stmt.startBalance?.date == null ? null : {
          amount: stmt.startBalance.amount,
          date: stmt.startBalance.date,
          type: stmt.startBalance.type,
        }),
        endBalance: (stmt.endBalance?.date == null ? null : {
          amount: stmt.endBalance.amount,
          date: stmt.endBalance.date,
          type: stmt.endBalance.type,
        }),
        addSub: stmt.inFlows + stmt.outFlows,
        transactions: stmt.transactions?.map(t=>({
          toAccountName: t.toAccountName,
          isExpense: t.isExpense,
          isIncome: t.isIncome,
          balance: {
            amount: t.balance.amount,
            date: t.balance.date,
            type: t.balance.type
          },
          ...(t.balanceFromStart != null && {balanceFromStart: t.balanceFromStart}),
          ...(t.balanceFromEnd != null && {balanceFromEnd: t.balanceFromEnd}),
          description: t.description || ''
        })) ?? [],
      };
    }
  }
  // owner + accout type => month => summary statement map.
  const summaries: {[ownerAccountType:string]: {[month:string]: SummaryStatement}} = {};
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
      startBalance: stmt.startBalance && {
        amount: stmt.startBalance.amount,
        date: stmt.startBalance.date,
        type: stmt.startBalance.type,
      },
      endBalance: stmt.endBalance && {
        amount: stmt.endBalance.amount,
        date: stmt.endBalance.date,
        type: stmt.endBalance.type,
      },
      ... (stmt.change == null ? {} : {change: stmt.change}),
      ... (stmt.percentChange == null ? {} : {percentChange: stmt.percentChange}),
      ... (stmt.unaccounted == null ? {} : {unaccounted: stmt.unaccounted}),
    };
  }
  return transformBudgetData(data.months, accountNameToAccount, statements, summaries);

  // return {
  //   months: data.months,
  //   accountNameToAccount,
  //   statements,
  //   summaries
  // };
}

function getKeysSorted(obj:  {[key:string]: any;}): string[] {
  return Object.keys(obj).sort((a, b) => a < b ? -1 : (a > b ? 1 : 0));
}
