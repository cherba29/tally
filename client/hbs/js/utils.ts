import {Account} from '@tally-lib';
import {Statement, SummaryStatement} from './base';
import {Cell} from './cell';
import {Row, Type as RowType} from './row';

/**
 *  Create map of owner->type->accountname, it will be rendered in this format.
 * @param {Object<string, Account>} accountNameToAccount
 *   account name->account map
 * @return {Object<string, Object<string, string>>} map of
 *   owner->type->accountname.
 */
function getOwnerTypeAccountMap(accountNameToAccount: {[accountType: string]: Account}): {
  [ownerType: string]: {[accountType: string]: string[]};
} {
  const ownerTypeAccountNames: {
    [ownerType: string]: {[accountType: string]: string[]};
  } = {};

  for (const accountName of Object.keys(accountNameToAccount)) {
    const account: Account = accountNameToAccount[accountName];
    for (const owner of account.owners) {
      let typeAccounts: {[accountName: string]: string[]};
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
  id: string;
  accountName: string;
  month: string;
  summary?: SummaryStatement;
  statements?: StatementEntry[];
}

interface StatementEntry {
  name: string;
  stmt: Statement;
}

interface PopupMonthData {
  id: string;
  accountName: string;
  month: string;
  stmt: Statement;
}

interface HeadingPopupData {
  id: string;
  account: Account;
}

export type PopupData = PopupMonthSummaryData | PopupMonthData | HeadingPopupData;

interface CellData {
  cells: Cell[];
  popups: PopupData[];
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
    owner: string,
    name: string,
    months: string[],
    statements: {[accountName: string]: {[month: string]: Statement}},
    summaries: {[month: string]: SummaryStatement},
): CellData {
  const cellData: CellData = {
    cells: [],
    popups: [],
  };
  for (const month of months) {
    const summaryStmt: SummaryStatement = summaries[month];
    const cell = new Cell(owner, name, month, summaryStmt);
    cellData.cells.push(cell);
    const accounts = summaryStmt?.accounts ?? [];
    cellData.popups.push({
      id: cell.id,
      accountName: owner + ' ' + name,
      month,
      summary: summaryStmt,
      statements: accounts.map(
          (accountName: string): StatementEntry => ({
            name: accountName,
            stmt: statements[accountName][month],
          }),
      ),
    });
  }
  return cellData;
}

export interface MatrixDataView {
  months: string[];
  rows: Row[];
  popupCells: PopupData[];
}

/**
 * Builds dataView structure with months, rows and popup cell data.
 *
 * @param {Array<string>} months - descending, continuous list of months to show
 * @param {Object<string, Account>} accountNameToAccount
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
    accountNameToAccount: {[accountName: string]: Account},
    statements: {[accountName: string]: {[month: string]: Statement}},
    summaries: {
    [ownerAccountType: string]: {[month: string]: SummaryStatement};
  },
): MatrixDataView {
  const dataView: MatrixDataView = {
    months,
    rows: [],
    popupCells: [],
  };
  const ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  const ownersSorted = getKeysSorted(ownerTypeAccountNames);

  for (const owner of ownersSorted) {
    const typeAccountNames = ownerTypeAccountNames[owner];
    dataView.rows.push(new Row(owner, RowType.SPACE, []));
    const summaryName = owner + ' SUMMARY';
    if (summaryName in summaries) {
      const cellData = getSummaryCells(
          owner,
          'SUMMARY',
          months,
          statements,
          summaries[summaryName],
      );
      dataView.rows.push(new Row(owner, RowType.TOTAL, cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
    const accountTypes = getKeysSorted(typeAccountNames);

    for (const accountType of accountTypes) {
      dataView.rows.push(
          new Row('*** ' + owner + ' - ' + accountType + ' *** accounts', RowType.SPACE, []),
      );
      const accountNames: string[] = typeAccountNames[accountType].sort();
      for (const accountName of accountNames) {
        const accountStatements: {[month: string]: Statement; } = statements[accountName];
        const cells: Cell[] = [];
        for (const month of months) {
          const stmt: Statement = accountStatements[month];
          const cell = new Cell(owner, accountName, month, stmt);
          cells.push(cell);
          const popupMonthData: PopupMonthData = {
            id: cell.id,
            accountName,
            month,
            stmt,
          };
          dataView.popupCells.push(popupMonthData);
        }
        const account = accountNameToAccount[accountName];
        dataView.rows.push(new Row(account, RowType.NORMAL, cells));
        dataView.popupCells.push({
          id: account.name,
          account,
        });
      }
      const name = owner + ' ' + accountType;
      const cellData = getSummaryCells(owner, accountType, months, statements, summaries[name]);
      dataView.rows.push(new Row(name, RowType.TOTAL, cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
  }
  return dataView;
}

/**
 * Get sorted list of object keys.
 * @param {Object} obj any object
 * @return {Array<string>} list of keys in sorted order.
 */
function getKeysSorted(obj: {[key: string]: any}): string[] {
  return Object.keys(obj).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
}
