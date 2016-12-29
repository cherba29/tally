"use strict";

// Create map of owner->type->accountname, it will be rendered in this format.
// @param accountNameToAccount - account name->account map
function getOwnerTypeAccountMap(accountNameToAccount) {
  console.log("name to account", accountNameToAccount);
  var ownerTypeAccountNames = {};

  for (var accountName in accountNameToAccount) {
    var account = accountNameToAccount[accountName]
    for (var i = 0; i < account.owners.length; i++) {
      var owner = account.owners[i];
      var typeAccounts;
      if (owner in ownerTypeAccountNames) {
        typeAccounts = ownerTypeAccountNames[owner];
      } else {
        ownerTypeAccountNames[owner] = typeAccounts = {}
      }

      var accountNames;
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
function Row(title, type, cells) {
  this.title = title;
  this.isSpace = ('SPACE' == type);
  this.isTotal = ('TOTAL' == type);
  this.isNormal = ('NORMAL' == type);
  this.cells = cells
}

// Data for rendering given cell.
function Cell(id, stmt) {
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

// Computes data for summary cells.
function getSummaryCells(owner, name, months, statements, summaries) {
  var cellData = {
    cells: [],
    popups: []
  };
  for (var m = 0, mm = months.length; m < mm; m++) {
    var month = months[m];
    var summaryStmt = summaries[month];
    var id = owner + "_" + name + "_" + month;
    cellData.cells.push(new Cell(id, summaryStmt));
    var accounts = ('accounts' in summaryStmt) ? summaryStmt.accounts : [];
    var transfer_matrix = buildMonthTransferMatrix(month, statements);
    console.log("transfer matrix", id, transfer_matrix);
    cellData.popups.push({
      id: id,
      accountName: owner + " " + name,
      month: month,
      summary: summaryStmt,
      transfer_matrix: transfer_matrix,
      statements: accounts.map(
          function(name) { return { name: name, stmt: statements[name][month] };})
    });
  }
  return cellData;
}

function getPopupData(id, owner, name, month, stmt) {
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
function buildMonthTransferMatrix(month, statements) {
  var accounts = [];
  var overall_total = 0.0;
  //console.log("transfer matrix month ", month);
  for (var accountName in statements) {
    //console.log("transfer matrix accountName ", accountName);
    var stmt = statements[accountName][month];
    //console.log("transfer matrix stmt ", stmt);
    if (stmt.isClosed) {
      continue;
    }
    var transfers = [];
    var total = 0.0;
    for (var i = 0, ii = stmt.transactions.length; i < ii; i++) {
      var transaction = stmt.transactions[i];
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
    months, accountNameToAccount, statements, summaries) {
  var dataView = {
    months: months,
    rows: [],
    popupCells: []
  };
  var ownerTypeAccountNames = getOwnerTypeAccountMap(accountNameToAccount);
  console.log("ownerTypeAccountNames", ownerTypeAccountNames);
  var ownersSorted = getKeysSorted(ownerTypeAccountNames);
  console.log("ownersSorted", ownersSorted);
  for (var i = 0, ii = ownersSorted.length; i < ii; i++) {
    var owner = ownersSorted[i];
    var typeAccountNames = ownerTypeAccountNames[owner];
    dataView.rows.push(new Row(owner, 'SPACE', []));
    var summaryName = owner + " SUMMARY";
    //console.log("summaryName", summaryName);
    if (summaryName in summaries) {
      var cellData = getSummaryCells(
          owner, "SUMMARY", months, statements, summaries[summaryName]);
      dataView.rows.push(new Row(owner, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
    var accountTypes = getKeysSorted(typeAccountNames);

    for (var j = 0, jj = accountTypes.length; j < jj; j++) {
      var accountType = accountTypes[j];
      dataView.rows.push(new Row(
          "*** " + owner + " - " + accountType + " *** accounts", 'SPACE', []));
      var accountNames = typeAccountNames[accountType].sort(function(a, b) {
        return a < b ? -1 : (a > b ? 1 : 0)
      });
      for (var k = 0, kk = accountNames.length; k < kk; k++) {
        var accountName = accountNames[k];
        var accountStatements = statements[accountName];
        var cells = [];
        for (var m = 0, mm = months.length; m < mm; m++) {
          var month = months[m];
          var stmt = accountStatements[month];
          var id = owner + "_" + accountName + "_" + month;
          //console.log("stmt", stmt, "id", id);
          cells.push(new Cell(id, stmt));
          dataView.popupCells.push(getPopupData(id, owner, accountName, month, stmt))
        }
        var account = accountNameToAccount[accountName];
        dataView.rows.push(new Row(account, 'NORMAL', cells));
        dataView.popupCells.push({
          id: account.name,
          account: account
        });
      }
      var name = owner + " " + accountType;
      var cellData = getSummaryCells(
        owner, accountType, months, statements, summaries[name]);
      dataView.rows.push(new Row(name, 'TOTAL', cellData.cells));
      Array.prototype.push.apply(dataView.popupCells, cellData.popups);
    }
  }
  console.log("dataView", dataView);
  return dataView;
}

function getKeysSorted(obj) {
  return Object.keys(obj).sort(function(a, b) {
    return a < b ? -1 : (a > b ? 1 : 0)
  });
}
