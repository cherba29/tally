interface TallyAccount {
  name: string;
  description: string;

  // Month when account was opened and possibly closed.
  openedOn: string;
  closedOn: string | null;

  // Is this an external/bookeeping account. External accounts are not
  // considered to be part of any owner.
  external: boolean;

  // Is this synthetic summary account.
  summary: boolean;

  // List of account owner ids.
  owners: string[];

  // Account type, for example 'CREDIT_CARD'.
  type: string;

  // Url to the account.
  url: string;

  // Physical address for the account.
  address: string;

  // Phone number for customer support.
  phone: string;

  // Username/password to use to login to the account.
  userName: string;
  password: string;

  // Real account number associated with this account.
  number: string;
}

/**
 *  Create map of owner->type->accountname, it will be rendered in this format.
 * @param accountNameToAccount - account name->account map
 */
function getOwnerTypeAccountMap(
    accountNameToAccount: {[accountType: string]: TallyAccount}):
        {[ownerType:string]: {[accountType:string]: string[]}} {
  console.log("name to account", accountNameToAccount);
  let ownerTypeAccountNames: {
    [ownerType:string]: {[accountType:string]: string[]}} = {};

  for (let accountName in accountNameToAccount) {
    let account: TallyAccount = accountNameToAccount[accountName]
    for (let owner of account.owners) {
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

// Data for rendering given row.
class Row {
  title: string|TallyAccount;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: any[];

  constructor(title: string|TallyAccount, type: string, cells: any[]) {
    this.title = title;
    this.isSpace = ('SPACE' == type);
    this.isTotal = ('TOTAL' == type);
    this.isNormal = ('NORMAL' == type);
    this.cells = cells
  }
}

interface Balance {
  amount: number;
  type: string;
}

interface Transaction {
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
  balance: Balance;
}

interface Statement {
  isClosed: boolean;
  addSub: number;
  endBalance: Balance;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
}

// Data for rendering given cell.
class Cell {
  id: string;
  isClosed: boolean;
  addSub: number | null;
  balance: number | null;
  isProjected: boolean;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number | null;
  unaccounted: number | null;
  balanced: boolean;

  constructor(id: string, stmt: Statement) {
    this.isClosed = stmt.isClosed;
    this.id = id;
    this.addSub = ('addSub' in stmt) ? stmt.addSub : null;
    if ('endBalance' in stmt && stmt.endBalance !== null) {
      this.balance = stmt.endBalance.amount;
      this.isProjected = stmt.endBalance.type != 'CONFIRMED';
    } else {
      this.balance = null;
      this.isProjected = false;
    }
    this.isCovered = stmt.isCovered;
    this.isProjectedCovered = stmt.isProjectedCovered;
    this.hasProjectedTransfer = stmt.hasProjectedTransfer;
    this.isProjected = this.isProjected || this.hasProjectedTransfer;
    this.percentChange = ('percentChange' in stmt) ? stmt.percentChange : null;
    if ('unaccounted' in stmt) {
      this.unaccounted = stmt.unaccounted;
      this.balanced = !this.unaccounted;
    } else {
      this.unaccounted = null;
      this.balanced = true;
    }
  }
}

interface SummaryStatement {
  isClosed: boolean;
  accounts: string[];
  addSub: number;
  endBalance: Balance;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
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

type PopupData = PopupMonthSummaryData | PopupMonthData | HeadingPopupData;


interface CellData {
  cells: Cell[],
  popups: PopupData[],
  // summary?: SummaryStatement,
  // statements?: StatementEntry[],
}

// Computes data for summary cells.
function getSummaryCells(
    owner: string, name: string, months: string[],
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[month:string]: SummaryStatement}): CellData {
  
  let cellData: CellData = {
    cells: [],
    popups: []
  };
  for (let month of months) {
    let summaryStmt: SummaryStatement = summaries[month];
    let id = owner + "_" + name + "_" + month;
    cellData.cells.push(new Cell(id, summaryStmt));
    let accounts = ('accounts' in summaryStmt) ? summaryStmt.accounts : [];
    cellData.popups.push({
      id: id,
      accountName: owner + " " + name,
      month: month,
      summary: summaryStmt,
      statements: accounts.map(
          function(name: string): StatementEntry {
            return { name: name, stmt: statements[name][month] };
          })
    });
  }
  return cellData;
}

function getPopupData(
  id: string, owner: string, name: string,
  month: string, stmt: Statement): PopupMonthData {
  return {
    id: id,
    accountName: name,
    month: month,
    stmt: stmt
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
  let dataView: MatrixDataView = {
    months: months,
    rows: [],
    popupCells: []
  };
  let ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  console.log("ownerTypeAccountNames", ownerTypeAccountNames);
  let ownersSorted = getKeysSorted(ownerTypeAccountNames);
  console.log("ownersSorted", ownersSorted);
  for (let owner of ownersSorted) {
    let typeAccountNames = ownerTypeAccountNames[owner];
    dataView.rows.push(new Row(owner, 'SPACE', []));
    let summaryName = owner + " SUMMARY";
    //console.log("summaryName", summaryName);
    if (summaryName in summaries) {
      let cellData = getSummaryCells(
          owner, "SUMMARY", months, statements, summaries[summaryName]);
      dataView.rows.push(new Row(owner, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
    let accountTypes = getKeysSorted(typeAccountNames);

    for (let accountType of accountTypes) {
      dataView.rows.push(new Row(
          "*** " + owner + " - " + accountType + " *** accounts", 'SPACE', []));
      let accountNames = typeAccountNames[accountType].sort(function(a, b) {
        return a < b ? -1 : (a > b ? 1 : 0)
      });
      for (let accountName of accountNames) {
        let accountStatements = statements[accountName];
        let cells = [];
        for (let month of months) {
          let stmt = accountStatements[month];
          let id = owner + "_" + accountName + "_" + month;
          //console.log("stmt", stmt, "id", id);
          cells.push(new Cell(id, stmt));
          dataView.popupCells.push(
              getPopupData(id, owner, accountName, month, stmt))
        }
        let account = accountNameToAccount[accountName];
        dataView.rows.push(new Row(account, 'NORMAL', cells));
        dataView.popupCells.push({
          id: account.name,
          account: account
        });
      }
      let name = owner + " " + accountType;
      let cellData = getSummaryCells(
        owner, accountType, months, statements, summaries[name]);
      dataView.rows.push(new Row(name, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
  }
  console.log("dataView", dataView);
  return dataView;
}

function getKeysSorted(obj:  {[key:string]: any;}): string[] {
  return Object.keys(obj).sort(function(a, b) {
    return a < b ? -1 : (a > b ? 1 : 0)
  });
}

export { transformBudgetData };
