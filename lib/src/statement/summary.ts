import { Balance } from '../core/balance';
import { Month } from '../core/month';
import { Statement } from './statement';
import { TransactionStatement } from './transaction';

export class SummaryStatement extends Statement {
  statements: Statement[] = [];

  constructor(name: string, month: Month) {
    super(name, month);
  }

  get isClosed(): boolean {
    return this.statements.every((statement) => statement.isClosed);
  }

  addStatement(statement: Statement): void {
    if (statement.month.compareTo(this.month) !== 0) {
      throw new Error(
        `Statement for month ${statement.month} is being added to summary for month ${this.month}`
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
