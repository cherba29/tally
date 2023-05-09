import { Account } from '@tally/lib/core/account';
import { Balance, Type as BalanceType } from '@tally/lib/core/balance';
import { Budget } from '@tally/lib/core/budget';
import { Month } from '@tally/lib/core/month';
import { Transfer } from '@tally/lib/core/transfer';
import { Statement } from './statement';

export enum Type {
  UNKNOWN,
  TRANSFER,
  INCOME,
  EXPENSE
}

export interface Transaction {
  account: Account;
  balance: Balance;
  description?: string;
  type: Type;
  balanceFromStart?: number;
  balanceFromEnd?: number;
}

// Extension of Statement for transactions over an account.
export class TransactionStatement extends Statement {
  // Account to which this transaction statements belongs.
  account: Account;

  // List of transcation in this statement.
  transactions: Transaction[] = [];

  // True if any transactions in this statement "cover" previous statement.
  coversPrevious = false;

  // True if any projected transactions in this statement "cover"
  // previous statement.
  coversProjectedPrevious = false;

  // True if any of the transcations are projects.
  hasProjectedTransfer = false;

  // True if this statement is covered by next.
  isCovered = false;

  // True if this statement is covered by any projected transactions in next
  // statement.
  isProjectedCovered = false;

  // Is this statement for closed account for given month.
  get isClosed(): boolean {
    return this.account.isClosed(this.month);
  }

  constructor(account: Account, month: Month) {
    super(account.name, month);
    this.account = account;
  }
}

function getTransactionType(fromAccount: Account, toAccount: Account, amount: number): Type {
  if (toAccount.hasCommonOwner(fromAccount) && !toAccount.isExternal && !fromAccount.isExternal) {
    return Type.TRANSFER;
  } else {
    return amount > 0 ? Type.INCOME : Type.EXPENSE;
  }
}

function makeTranscationStatement(
  account: Account,
  month: Month,
  transfers?: Set<Transfer>,
  startBalance?: Balance
): TransactionStatement {
  const statement = new TransactionStatement(account, month);
  statement.startBalance = startBalance;
  const attributeTransfer = (fromAccount: Account, toAccount: Account, amount: number): Type => {
    if (amount > 0) {
      statement.inFlows += amount;
    } else {
      statement.outFlows += amount;
    }
    const transactionType = getTransactionType(fromAccount, toAccount, amount);
    switch (transactionType) {
      case Type.EXPENSE:
        statement.totalPayments += amount;
        break;
      case Type.INCOME:
        statement.income += amount;
        break;
      case Type.UNKNOWN:
        break;
      case Type.TRANSFER:
        statement.totalTransfers += amount;
        break;
    }
    return transactionType;
  };
  const descTransfers = Array.from(transfers || []).sort((b, a) => {
    let eq = a.balance.compareTo(b.balance);
    if (eq != 0) return eq;
    eq = a.fromMonth.compareTo(b.fromMonth);
    if (eq != 0) return eq;
    eq = a.toMonth.compareTo(b.toMonth);
    if (eq != 0) return eq;
    if (a.fromAccount.name != b.fromAccount.name) {
      return a.fromAccount.name < b.fromAccount.name ? -1 : 1;
    }
    if (a.toAccount.name != b.toAccount.name) {
      return a.toAccount.name < b.toAccount.name ? -1 : 1;
    }
    if (a.description != b.description) {
      return (a.description || '') < (b.description || '') ? -1 : 1;
    }
    return 0;
  });

  const firstTransfer: Transfer|undefined = descTransfers[descTransfers.length - 1];
  if (firstTransfer && startBalance && firstTransfer.balance.date.getTime() < startBalance.date.getTime()) {
    throw new Error(
      `Balance ${month} ${startBalance} for account ${account.name} ` + 
      `starts after transaction ${firstTransfer.fromAccount.name} --> ${firstTransfer.toAccount.name}/${firstTransfer.balance} ` +
      `desc "${firstTransfer.description}"`);
  }

  for (const t of descTransfers) {
    statement.hasProjectedTransfer =
      statement.hasProjectedTransfer || t.balance.type == BalanceType.PROJECTED;
    let otherAccount: Account | undefined;
    let balance: Balance | undefined;
    let transactionType = Type.UNKNOWN;
    if (t.toAccount.name === account.name) {
      balance = t.balance;
      otherAccount = t.fromAccount;
      transactionType = attributeTransfer(otherAccount, account, balance.amount);
    } else if (t.fromAccount.name === account.name) {
      balance = Balance.negated(t.balance);
      otherAccount = t.toAccount;
      transactionType = attributeTransfer(account, otherAccount, balance.amount);
    } else {
      // This should never occur since budget should have been validated by now.
      throw new Error(
        `Setting transfer (${t.fromAccount.name} to ${t.toAccount.name}) for ${account.name} account statement!`
      );
    }
    if (!statement.coversPrevious && balance.amount > 0 && t.fromAccount.hasCommonOwner(account)) {
      statement.coversProjectedPrevious = true;
      if (balance.type != BalanceType.PROJECTED) {
        statement.coversPrevious = true;
      }
    }
    statement.transactions.push({
      account: otherAccount,
      description: t.description,
      balance,
      type: transactionType
    });
  }
  return statement;
}

export function buildTransactionStatementTable(budget: Budget): TransactionStatement[] {
  const statementTable: TransactionStatement[] = [];

  const makeStatement = (account: Account, month: Month): TransactionStatement => {
    return makeTranscationStatement(
      account,
      month,
      budget.transfers.get(account.name)?.get(month.toString()),
      budget.balances.get(account.name)?.get(month.toString())
    );
  };

  // Working backwards.
  const months = budget.months.concat().sort((a, b) => b.compareTo(a));
  if (months.length < 1) {
    throw new Error('Budget must have at least one month.');
  }

  for (const account of budget.accounts.values()) {
    const accountStatements: TransactionStatement[] = [];
    // Make statement outside range so that its attributes relating to previous can be used.
    let nextMonthStatement = makeStatement(account, months[0]!.next());
    for (const month of months) {
      const statement = makeStatement(account, month);
      statement.endBalance = nextMonthStatement.startBalance;
      statement.isCovered =
        statement.endBalance === undefined ||
        statement.endBalance.amount >= 0 ||
        nextMonthStatement.coversPrevious;
      statement.isProjectedCovered =
        statement.isCovered || nextMonthStatement.coversProjectedPrevious;
      nextMonthStatement = statement;
      if (statement.startBalance) {
        statement.transactions.reduceRight((prevBalance, t) => {
          return (t.balanceFromStart = prevBalance + t.balance.amount);
        }, statement.startBalance.amount);
      }
      if (statement.endBalance) {
        statement.transactions.reduce((prevBalance, t) => {
          return (t.balanceFromEnd = prevBalance - t.balance.amount);
        }, statement.endBalance.amount);
      }
      accountStatements.push(statement);
    }
    // Do not include account if for all months it was closed.
    if (accountStatements.some((stmt) => !stmt.isClosed)) {
      statementTable.push(...accountStatements);
    }
  }
  return statementTable;
}
