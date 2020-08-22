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
  console.log("name to account", accountNameToAccount);
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
function transformBudgetData(
    months: string[], accountNameToAccount: {[accountName:string]: TallyAccount},
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[ownerAccountType:string]: {[month:string]: SummaryStatement}}) {
  const dataView: MatrixDataView = {
    months,
    rows: [],
    popupCells: []
  };
  const ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  console.log("ownerTypeAccountNames", ownerTypeAccountNames);
  const ownersSorted = getKeysSorted(ownerTypeAccountNames);
  console.log("ownersSorted", ownersSorted);
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
  console.log("dataView", dataView);
  return dataView;
}

function getKeysSorted(obj:  {[key:string]: any;}): string[] {
  return Object.keys(obj).sort((a, b) => a < b ? -1 : (a > b ? 1 : 0));
}

export { transformBudgetData };
