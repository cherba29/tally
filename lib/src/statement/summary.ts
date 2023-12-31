import { Account, Type as AccountType } from '../core/account';
import { Balance, Type as BalanceType } from '../core/balance';
import { Month } from '../core/month';
import { Statement } from './statement';
import { TransactionStatement } from './transaction';
import { Map2, Map3 } from '../utils';

export class SummaryStatement extends Statement {
  statements: Statement[] = [];
  startMonth: Month;

  constructor(account: Account, month: Month, startMonth: Month | undefined = undefined) {
    super(account, month);
    this.startMonth = startMonth ?? month;
  }

  get isClosed(): boolean {
    return this.statements.every((statement) => statement.isClosed);
  }

  get annualizedPercentChange(): number | undefined {
    const prctChange = this.percentChange;
    if (prctChange === undefined) {
      return undefined;
    }
    const numberOfMonths = this.month.distance(this.startMonth) + 1;
    const annualFrequency = 12.0 / numberOfMonths;
    const result = Math.pow(1 + Math.abs(prctChange) / 100, annualFrequency) - 1;
    // Annualized percentage change is not that meaningful if large.
    return result < 10 ? 100 * Math.sign(prctChange) * result : undefined;
  }

  addStatement(statement: Statement): void {
    if (statement.month.compareTo(this.month) !== 0) {
      throw new Error(
        `${statement.account.name} statement for month ${statement.month} is being added to summary for month ${this.month}`
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

  mergeStatement(statement: SummaryStatement) {
    if (statement.month.compareTo(this.month) !== 0) {
      throw new Error(
        `${statement.account.name} statement for month ${statement.month} is being added to summary for month ${this.month}`
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
    this.statements = [...this.statements, ...statement.statements];
  }
}

class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  summaryStatements = new Map3<SummaryStatement>();
  private summaryAccounts = new Map<string, Account>();

  addStatement(summaryName: string, owner: string, statement: Statement) {
    const summaryAccount = this.getAccount(summaryName, owner, statement.account.path);

    const accountMonthSummaryStatement = this.summaryStatements.getDefault(
      owner, summaryAccount.name, statement.month.toString(), 
      () => new SummaryStatement(summaryAccount, statement.month));
    accountMonthSummaryStatement.addStatement(statement);
  }

  private getAccount(name: string, owner: string, path: string[]): Account {
    const key = owner + ' - ' + name;
    let account = this.summaryAccounts.get(key);
    if (!account) {
      account = new Account({name, type: AccountType.SUMMARY, owners: [owner], path: path.slice(0, -1)});
      this.summaryAccounts.set(key, account);
    }
    return account
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  // propogateUpThePath() {
  //   let nextLevelSummaries = new SummaryStatementAggregator();
  //   nextLevelSummaries.summaryAccounts = this.summaryAccounts;
  //   nextLevelSummaries.summaryStatements = this.summaryStatements;
  //   while (!nextLevelSummaries.summaryStatements.isEmpty()) {
  //     console.log('### processing ', nextLevelSummaries.summaryStatements.size, 'summary statements');
  //     let currentLevelSummaries = nextLevelSummaries.summaryStatements;
  //     nextLevelSummaries.summaryStatements = new Map3<SummaryStatement>();
  //     for (const [owner, , , statement] of currentLevelSummaries) {
  //       // Break recursion stopping at the root, and exclude other legacy accounts in totals.
  //       if (statement.account.name !== '/' && statement.account.name.startsWith('/')) {
  //         const summaryName = '/' + statement.account.path.join('/');
  //         if (summaryName.startsWith('/external') && statement.month.toString() === 'Dec2023') {
  //           console.log('### adding statement to', summaryName, owner, statement.account.name, statement.endBalance?.amount);
  //         }
  //         nextLevelSummaries.addStatement(summaryName, owner, statement);
  //       }
  //     }
  //     this.merge(nextLevelSummaries.summaryStatements);
  //   }
  //   console.log('### Final ', this.summaryStatements.size, 'summary statements');
  // }

  // private merge(summaries: Map3<SummaryStatement>) {
  //   for (const [owner, summaryName, month, statement] of summaries) {
  //     const existingStatement = this.summaryStatements.get(owner, summaryName, month);
  //     if (existingStatement) {
  //       existingStatement.mergeStatement(statement);
  //     } else {
  //       this.summaryStatements.set(owner, summaryName, month, statement);
  //     }
  //   }
  // }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  propogateUpThePath2() {
      // Build a multi-root tree based on account paths for each owner.
    const tree = new Map<string, Set<string>>();  // node -> set of children.
    const owners = new Set<string>();
    for (const [, account] of this.summaryAccounts) {
      for (const owner of account.owners) {
        owners.add(owner);
        if (!account.name.startsWith('/')) continue;
        const path = account.path;
        let entry = '/' + owner + account.name;
        for (let sub = path.length; sub >= 0 ; sub--) {
          const subPath = path.slice(0, sub);
          const subPathId = '/' + owner + '/' + subPath.join('/');
          let subTreeEntry = tree.get(subPathId);
          if (!subTreeEntry) {
            subTreeEntry = new Set<string>();
            tree.set(subPathId, subTreeEntry);
          }
          if (subPathId !== entry) {  // Make sure root does not reference itself.
            subTreeEntry.add(entry);
          }
          entry = subPathId;
        }
      }
    }
    // For each owner bottom up build up summaries.
    for (const owner of owners) {
      const ownerRoot = '/' + owner + '/';
      for (const node of traverseBottomUp(ownerRoot, tree)) {
        const fullPath = node.split('/');
        const summaryId = '/' + fullPath.slice(2).join('/');
        // skip this is root node it does not need to be added to anything.
        if (summaryId === '/') continue;
        const monthlyStatements = this.summaryStatements.get2(owner, summaryId);
        if (!monthlyStatements) {  // Should never happen.
          throw new Error(`${node} has no monthly statements.`);
        }
        const parrentSummaryId = '/' + fullPath.slice(2, -1).join('/');
        for (const [, monthlyStatement] of monthlyStatements) {
          this.addStatement(parrentSummaryId, owner, monthlyStatement);
        }
      }
    }
  }
}

function *traverseBottomUp(root: string, tree: Map<string, Set<string>>): Generator<string> {
  const children = tree.get(root) ?? new Set<string>();
  for (const child of children) {
    for (const node of traverseBottomUp(child, tree)) {
      yield node;
    }
  }
  yield root;
}

export function buildSummaryStatementTable(
  statements: TransactionStatement[],
  selectedOwner?: string
): Map3<SummaryStatement> {
  const statementsAggregator = new SummaryStatementAggregator();
  for (const statement of statements) {
    for (const owner of statement.account.owners) {
      if (selectedOwner && owner !== selectedOwner || statement.isEmpty()) {
        continue;
      }
      const summariesToAddTo = [owner + ' ' + statement.account.typeIdName, owner + ' SUMMARY'];
      if (statement.account.path.length) {
        summariesToAddTo.push('/' + statement.account.path.join('/'));
      }
      for (const summaryName of summariesToAddTo) {
        if (statement.account.isExternal && summaryName.includes('SUMMARY')) {
          continue;
        }
        statementsAggregator.addStatement(summaryName, owner, statement);
      }
    }
  }
  // statementsAggregator.propogateUpThePath();
  statementsAggregator.propogateUpThePath2();
  return statementsAggregator.summaryStatements;
}

export function combineSummaryStatements(summaryStatements: SummaryStatement[]): SummaryStatement {
  if (!summaryStatements.length) {
    throw new Error(`Cant combine empty list of summary statements`);
  }
  let stmtName: string | undefined = undefined;
  let owners: string[] = [];
  let minMonth: Month | undefined = undefined;
  let maxMonth: Month | undefined = undefined;
  // Map of 'account name' -> month -> 'summary statement'.
  const accountStatements = new Map<string, Map<string, Statement>>();

  for (const summaryStmt of summaryStatements) {
    if (!stmtName) {
      stmtName = summaryStmt.account.name;
      owners = summaryStmt.account.owners;
    } else if (stmtName !== summaryStmt.account.name) {
      throw new Error(
        `Cant combine different summary statements ${stmtName} and ${summaryStmt.account.name}`
      );
    }
    if (!minMonth || summaryStmt.month.isLess(minMonth)) {
      minMonth = summaryStmt.month;
    }
    if (!maxMonth || maxMonth.isLess(summaryStmt.month)) {
      maxMonth = summaryStmt.month;
    }
    for (const stmt of summaryStmt.statements) {
      let accountMontlyStatements = accountStatements.get(stmt.account.name);
      if (!accountMontlyStatements) {
        accountMontlyStatements = new Map<string, Statement>();
        accountStatements.set(stmt.account.name, accountMontlyStatements);
      }
      let monthStatement = accountMontlyStatements.get(stmt.month.toString());
      if (monthStatement) {
        throw new Error(`Duplicate month statement for ${stmt.account.name} for ${stmt.month}`);
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
  const summaryAccount = new Account({name: stmtName, type: AccountType.SUMMARY, owners});
  const combined = new SummaryStatement(summaryAccount, maxMonth, minMonth);
  for (const [acctName, acctStatements] of accountStatements) {
    // Combine all statements for a given account over all months in the range.
    const stmt = combineAccountStatements(
        new Account({name: acctName, type: AccountType.SUMMARY, owners: []}),
        minMonth,
        maxMonth,
        acctStatements);
    combined.addStatement(stmt);
  }
  return combined;
}

export class CombinedStatement extends Statement {
  startMonth: Month;

  constructor(account: Account, month: Month, startMonth: Month | undefined = undefined) {
    super(account, month);
    this.startMonth = startMonth ?? month;
  }

  get annualizedPercentChange(): number | undefined {
    const prctChange = this.percentChange;
    if (prctChange === undefined) {
      return undefined;
    }
    const numberOfMonths = this.month.distance(this.startMonth) + 1;
    const annualFrequency = 12.0 / numberOfMonths;
    const result = Math.pow(1 + Math.abs(prctChange) / 100, annualFrequency) - 1;
    // Annualized percentage change is not that meaningful if large.
    return result < 10 ? 100 * Math.sign(prctChange) * result : undefined;
  }

  get isClosed(): boolean {
    return false;
  }
}

export class EmptyStatement extends Statement {
  constructor(account: Account, month: Month) {
    super(account, month);
  }

  get isClosed(): boolean {
    return false;
  }
}

function makeProxyStatement(
  account: Account,
  month: Month,
  currStmt: Statement | undefined,
  prevStmt: Statement | undefined,
  nextStmt: Statement | undefined
): Statement {
  const stmt = currStmt ?? new EmptyStatement(account, month);
  if (!stmt.startBalance) {
    stmt.startBalance =
      prevStmt?.endBalance ??
      new Balance(0, new Date(month.year, month.month, 1), BalanceType.PROJECTED);
  }
  if (!stmt.endBalance) {
    stmt.endBalance =
      nextStmt?.startBalance ??
      new Balance(0, new Date(month.year, month.month + 1, 1), BalanceType.PROJECTED);
  }
  return stmt;
}

function combineAccountStatements(
  account: Account,
  startMonth: Month,
  endMonth: Month,
  stmts: Map<string, Statement>
): Statement {
  const combined = new CombinedStatement(account, endMonth, startMonth);
  for (
    let currentMonth = startMonth;
    !endMonth.isLess(currentMonth);
    currentMonth = currentMonth.next()
  ) {
    const stmt: Statement = makeProxyStatement(
      account,
      currentMonth,
      stmts.get(currentMonth.toString()),
      stmts.get(currentMonth.previous().toString()),
      stmts.get(currentMonth.next().toString())
    );
    if (
      !combined.startBalance ||
      (stmt.startBalance && combined.startBalance.date.getTime() > stmt.startBalance.date.getTime())
    ) {
      combined.startBalance = stmt.startBalance;
    }
    if (
      !combined.endBalance ||
      (stmt.endBalance && combined.endBalance.date.getTime() < stmt.endBalance.date.getTime())
    ) {
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
