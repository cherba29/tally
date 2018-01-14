interface Account {
  name: string;
  address: string;
  closedOn: string;
  description: string;
  external: boolean;
  number: string;
  openedOn: string;
  owners: string[];
  password: string;
  phone: string;
  summary: boolean;
  type: string;
  url: string;
  userName: string;
}

// Create map of owner->type->accountname, it will be rendered in this format.
// @param accountNameToAccount - account name->account map
function getOwnerTypeAccountMap(accountNameToAccount: {[accountType: string]: Account}):
    {[ownerType:string]: {[accountType:string]: string[]}} {
  console.log("name to account", accountNameToAccount);
  let ownerTypeAccountNames: {[ownerType:string]: {[accountType:string]: string[]}} = {};

  for (let accountName in accountNameToAccount) {
    let account = accountNameToAccount[accountName]
    for (let i = 0; i < account.owners.length; i++) {
      let owner = account.owners[i];
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
  title: string|Account;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: any[];

  constructor(title: string|Account, type: string, cells: any[]) {
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
  addSub: number;
  balance: number;
  isProjected: boolean;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  balanced: boolean;

  constructor(id: string, stmt: Statement) {
    this.isClosed = stmt.isClosed;
    if (!this.isClosed) {
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

interface PopupData {
  id: string;
  accountName: string;
  month: string;
  summary: any;
  transfer_matrix: any;
  statements: {
    name: string;
    stmt: Statement;
  }[];
}

// Computes data for summary cells.
function getSummaryCells(
    owner: string, name: string, months: string[],
    statements: {[accountName:string]: {[month:string]: Statement}},
    summaries: {[month:string]: SummaryStatement}): {cells: Cell[], popups: PopupData[]} {
  let cellData = {
    cells: [],
    popups: []
  };
  for (let m = 0, mm = months.length; m < mm; m++) {
    let month = months[m];
    let summaryStmt = summaries[month];
    let id = owner + "_" + name + "_" + month;
    cellData.cells.push(new Cell(id, summaryStmt));
    let accounts = ('accounts' in summaryStmt) ? summaryStmt.accounts : [];
    let transfer_matrix = buildMonthTransferMatrix(month, statements);
    console.log("transfer matrix", id, transfer_matrix);
    cellData.popups.push({
      id: id,
      accountName: owner + " " + name,
      month: month,
      summary: summaryStmt,
      transfer_matrix: transfer_matrix,
      statements: accounts.map(
          function(name: string) {
            return { name: name, stmt: statements[name][month] };
          })
    });
  }
  return cellData;
}

function getPopupData(id: string, owner: string, name: string, month: string, stmt: Statement) {
  return {
    id: id,
    accountName: name,
    month: month,
    stmt: stmt
  };
}

// Builds a list of accounts with name, map of account name to
// total transfers to that account, and total transfers for the whole account.
// @param statements - table indexed by account name and month of statements
function buildMonthTransferMatrix(
    month: string,
    statements: {[accountName:string]: {[month:string]: Statement}}): 
        {accounts: {name: string; transfers: {[accountName:string]: number}, total:number}[], total: number} {
  let accounts: {name: string; transfers: {[accountName:string]: number}, total:number}[] = [];
  let overall_total = 0.0;
  //console.log("transfer matrix month ", month);
  for (let accountName in statements) {
    //console.log("transfer matrix accountName ", accountName);
    let stmt = statements[accountName][month];
    //console.log("transfer matrix stmt ", stmt);
    if (stmt.isClosed) {
      continue;
    }
    let transfers: {[accountName:string]: number} = {};
    let total = 0.0;
    for (let i = 0, ii = stmt.transactions.length; i < ii; i++) {
      let transaction = stmt.transactions[i];
      if (!transaction.isExpense && !transaction.isIncome) {
        transfers[transaction.toAccountName] = transaction.balance.amount;
        total += transaction.balance.amount
      }
    }
    overall_total += total;
    accounts.push({
      name: accountName,
      transfers: transfers,
      total: total
    });
  }
  accounts.sort(function(a, b) {
    return a.name < b.name ? -1 : (a.name > b.name ? 1 : 0)
  });
  return {
    accounts: accounts,
    total: overall_total
  };
}

// Builds dataView structure with months, rows and popup cell data.
// @param months - descending, continuous list of months to show
// @param accountNameToAccount - account name to account map
// @param statements - table indexed by account name and month of statements
// @param summaries - table indexed by owner name + account type and month
//                    of statement summaries.
function transformBudgetData(
    months: string[], accountNameToAccount: {[accountName:string]: Account}, 
    statements: {[accountName:string]: {[month:string]: Statement}}, 
    summaries: {[ownerAccountType:string]: {[month:string]: SummaryStatement}}) {
  let dataView = {
    months: months,
    rows: [],
    popupCells: []
  };
  let ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  console.log("ownerTypeAccountNames", ownerTypeAccountNames);
  let ownersSorted = getKeysSorted(ownerTypeAccountNames);
  console.log("ownersSorted", ownersSorted);
  for (let i = 0, ii = ownersSorted.length; i < ii; i++) {
    let owner = ownersSorted[i];
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

    for (let j = 0, jj = accountTypes.length; j < jj; j++) {
      let accountType = accountTypes[j];
      dataView.rows.push(new Row(
          "*** " + owner + " - " + accountType + " *** accounts", 'SPACE', []));
      let accountNames = typeAccountNames[accountType].sort(function(a, b) {
        return a < b ? -1 : (a > b ? 1 : 0)
      });
      for (let k = 0, kk = accountNames.length; k < kk; k++) {
        let accountName = accountNames[k];
        let accountStatements = statements[accountName];
        let cells = [];
        for (let m = 0, mm = months.length; m < mm; m++) {
          let month = months[m];
          let stmt = accountStatements[month];
          let id = owner + "_" + accountName + "_" + month;
          //console.log("stmt", stmt, "id", id);
          cells.push(new Cell(id, stmt));
          dataView.popupCells.push(getPopupData(id, owner, accountName, month, stmt))
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
