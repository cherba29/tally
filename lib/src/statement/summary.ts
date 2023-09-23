import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { Statement } from './statement';
import { TransactionStatement } from './transaction';

export class SummaryStatement extends Statement {
  statements: Statement[] = [];
  startMonth: Month;

  constructor(name: string, month: Month, startMonth: Month|undefined = undefined) {
    super(name, month);
    this.startMonth = startMonth ?? month;
  }

  get isClosed(): boolean {
    return this.statements.every((statement) => statement.isClosed);
  }

  addStatement(statement: Statement): void {
    if (statement.month.compareTo(this.month) !== 0) {
      throw new Error(
        `${statement.name} statement for month ${statement.month} is being added to summary for month ${this.month}`
      );
    }
    if (statement.isClosed) {
      // Does not contribute to the summary.
      return;
    }
    if (!this.startBalance) {
      this.startBalance = statement.startBalance;
    } else if (statement.startBalance) {
      this.startBalance = Balance.add(this.startBalance, statement.startBalance);
    }
    if (!this.endBalance) {
      this.endBalance = statement.endBalance;
    } else if (statement.endBalance) {
      this.endBalance = Balance.add(this.endBalance, statement.endBalance);
    }
    this.addInFlow(statement.inFlows);
    this.addOutFlow(statement.outFlows);
    this.totalTransfers += statement.totalTransfers;
    this.totalPayments += statement.totalPayments;
    this.income += statement.income;
    this.statements.push(statement);
  }
}

export function* buildSummaryStatementTable(
  statements: TransactionStatement[],
  selectedOwner?: string
): Generator<SummaryStatement> {
  // Map of 'summary name' -> month -> 'summary statement'.
  const summaryStatements = new Map<string, Map<string, SummaryStatement>>();

  const updateSummaryStatement = (name: string, statement: TransactionStatement) => {
    let accountSummaryStatements = summaryStatements.get(name);
    if (!accountSummaryStatements) {
      accountSummaryStatements = new Map<string, SummaryStatement>();
      summaryStatements.set(name, accountSummaryStatements);
    }
    let accountMonthSummaryStatement = accountSummaryStatements.get(statement.month.toString());
    if (!accountMonthSummaryStatement) {
      accountMonthSummaryStatement = new SummaryStatement(name, statement.month);
      accountSummaryStatements.set(statement.month.toString(), accountMonthSummaryStatement);
    }
    accountMonthSummaryStatement.addStatement(statement);
  };

  for (const statement of statements) {
    for (const owner of statement.account.owners) {
      if (selectedOwner && owner !== selectedOwner) {
        continue;
      }
      for (const summaryName of [owner + ' ' + statement.account.typeIdName, owner + ' SUMMARY']) {
        if (statement.account.isExternal && summaryName.includes('SUMMARY')) {
          continue;
        }
        // Aggregate statements by name and month.
        updateSummaryStatement(summaryName, statement);
      }
    }
  }

  for (const monthStatements of summaryStatements.values()) {
    for (const statement of monthStatements.values()) {
      yield statement;
    }
  }
}



export function combineSummaryStatements(summaryStatements: SummaryStatement[]): SummaryStatement {
  if (!summaryStatements.length) {
    throw new Error(`Cant combine empty list of summary statements`);
  }
  let stmtName: string|undefined = undefined;
  let minMonth: Month|undefined = undefined;
  let maxMonth: Month|undefined = undefined;
  // Map of 'account name' -> month -> 'summary statement'.
  const accountStatements = new Map<string, Map<string, Statement>>();

  for (const summaryStmt of summaryStatements) {
    if (!stmtName) {
      stmtName = summaryStmt.name;
    } else if (stmtName !== summaryStmt.name) {
      throw new Error(`Cant combine different summary statements ${stmtName} and ${summaryStmt.name}`);
    }
    if (!minMonth || summaryStmt.month.isLess(minMonth)) {
      minMonth = summaryStmt.month;
    }
    if (!maxMonth || maxMonth.isLess(summaryStmt.month)) {
      maxMonth = summaryStmt.month;
    }
    for (const stmt of summaryStmt.statements) {
      let accountMontlyStatements = accountStatements.get(stmt.name);
      if (!accountMontlyStatements) {
        accountMontlyStatements = new Map<string, Statement>();
        accountStatements.set(stmt.name, accountMontlyStatements);
      }
      let monthStatement = accountMontlyStatements.get(stmt.month.toString());
      if (monthStatement) {
        throw new Error(`Duplicate month statement for ${stmt.name} for ${stmt.month}`);
      }
      accountMontlyStatements.set(stmt.month.toString(), stmt);
    }
  }
  if (!stmtName) {
    throw new Error(`Statements do not have name set.`);
  }
  if (!minMonth) {
    throw new Error(`Could not determine start month`);
  }
  if (!maxMonth) {
    throw new Error(`Could not determine end month`);
  }
  const combined = new SummaryStatement(stmtName, maxMonth, minMonth);
  for (const [acctName, acctStatements] of accountStatements) {
    // Combine all statements for a given account over all months in the range.
    const stmt = combineAccountStatements(acctName, minMonth, maxMonth, acctStatements);
    combined.addStatement(stmt);
  }
  return combined;
}

export class CombinedStatement extends Statement {
  startMonth: Month;

  constructor(name: string, month: Month, startMonth: Month|undefined = undefined) {
    super(name, month);
    this.startMonth = startMonth ?? month;
  }

  get isClosed(): boolean {
    return false;
  }
};

export class EmptyStatement extends Statement {
  constructor(name: string, month: Month) {
    super(name, month);
  }

  get isClosed(): boolean {
    return false;
  }
};

function makeProxyStatement(name: string, month: Month, currStmt: Statement|undefined, prevStmt: Statement|undefined, nextStmt: Statement|undefined): Statement {
  const stmt = currStmt ?? new EmptyStatement(name, month);
  if (!stmt.startBalance) {
    stmt.startBalance = prevStmt?.endBalance ?? new Balance(0, new Date(month.year, month.month, 1), BalanceType.PROJECTED);
  }
  if (!stmt.endBalance) {
    stmt.endBalance = nextStmt?.startBalance ?? new Balance(0, new Date(month.year, month.month + 1, 1), BalanceType.PROJECTED);
  }
  return stmt;
}

function combineAccountStatements(name: string, startMonth: Month, endMonth: Month, stmts: Map<string,Statement>): Statement {
  const combined = new CombinedStatement(name, endMonth, startMonth);
  for (let currentMonth = startMonth; !endMonth.isLess(currentMonth); currentMonth = currentMonth.next()) {
    const stmt: Statement = makeProxyStatement(
      name,
      currentMonth, 
      stmts.get(currentMonth.toString()),
      stmts.get(currentMonth.previous().toString()), 
      stmts.get(currentMonth.next().toString())
    );
    if (!combined.startBalance || stmt.startBalance && combined.startBalance.date.getTime() > stmt.startBalance.date.getTime()) {
      combined.startBalance = stmt.startBalance;
    }
    if (!combined.endBalance || stmt.endBalance &&  combined.endBalance.date.getTime() < stmt.endBalance.date.getTime()) {
      combined.endBalance = stmt.endBalance;
    }
    combined.addInFlow(stmt.inFlows);
    combined.addOutFlow(stmt.outFlows);
    combined.totalTransfers += stmt.totalTransfers;
    combined.totalPayments += stmt.totalPayments;
    combined.income += stmt.income;
  }
  return combined;
}