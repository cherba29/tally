import {
  Transaction,
  TransactionStatement,
  Type as TransactionType
} from '@tally/lib/statement/transaction';
import { SummaryStatement } from '@tally/lib/statement/summary';
import { Type as BalanceType } from '@tally/lib/core/balance';
import {
  GqlAccount,
  GqlStatement,
  GqlSummaryStatement,
} from './types';
import { Account } from '@tally/lib/core/account';

export function toGqlAccount(account: Account): GqlAccount {
  return {
    name: account.name,
    ...(account.description && { description: account.description }),
    path: account.path,
    type: account.typeIdName,
    number: account.number,
    ...(account.openedOn && { openedOn: account.openedOn }),
    ...(account.closedOn && { closedOn: account.closedOn }),
    owners: account.owners,
    url: account.url,
    phone: account.phone,
    address: account.address,
    userName: account.userName,
    password: account.password,
    external: account.isExternal,
    summary: account.isSummary
  };
}

export function toGqlStatement(statement: TransactionStatement): GqlStatement {
  return {
    name: statement.account.name,
    month: statement.month,
    inFlows: statement.inFlows,
    outFlows: statement.outFlows,
    income: statement.income,
    totalPayments: statement.totalPayments,
    totalTransfers: statement.totalTransfers,
    change: statement.change,
    addSub: statement.addSub,
    percentChange:
      statement.percentChange && Math.round((statement.percentChange + Number.EPSILON) * 100) / 100,
    annualizedPercentChange:
      statement.annualizedPercentChange &&
      Math.round((statement.annualizedPercentChange + Number.EPSILON) * 100) / 100,
    unaccounted: statement.unaccounted,
    startBalance: {
      amount: statement.startBalance?.amount ?? 0,
      date: statement.startBalance?.date,
      type: BalanceType[(statement.startBalance?.type ?? BalanceType.UNKNOWN) as BalanceType]
    },
    endBalance: {
      amount: statement.endBalance?.amount,
      date: statement.endBalance?.date,
      type: BalanceType[(statement.endBalance?.type ?? BalanceType.UNKNOWN) as BalanceType]
    },
    isCovered: statement.isCovered,
    isProjectedCovered: statement.isProjectedCovered,
    hasProjectedTransfer: statement.hasProjectedTransfer,
    transactions: statement.transactions?.map((t: Transaction) => ({
      toAccountName: t.account.name,
      isExpense: t.type == TransactionType.EXPENSE,
      isIncome: t.type == TransactionType.INCOME,
      balance: {
        amount: t.balance.amount,
        date: t.balance.date,
        type: BalanceType[t.balance.type as BalanceType]
      },
      balanceFromStart: t.balanceFromStart,
      description: t.description
    })),
    isClosed:
      statement.account?.closedOn?.isLess(statement.month) ||
      (statement.account?.openedOn && statement.month.isLess(statement.account.openedOn)) ||
      false
  };
}

export function toGqlSummaryStatement(summary: SummaryStatement): GqlSummaryStatement {
  return {
    name: summary.account.name,
    month: summary.month,
    accounts: summary.statements.map((statement) => statement.account.name).sort(),
    addSub: summary.addSub,
    change: summary.change,
    income: summary.income,
    inFlows: summary.inFlows,
    outFlows: summary.outFlows,
    percentChange:
      summary.percentChange && Math.round((summary.percentChange + Number.EPSILON) * 100) / 100,
    annualizedPercentChange:
      summary.annualizedPercentChange &&
      Math.round((summary.annualizedPercentChange + Number.EPSILON) * 100) / 100,
    totalPayments: summary.totalPayments,
    totalTransfers: summary.totalTransfers,
    unaccounted: summary.unaccounted,
    ...(summary.startBalance && {
      startBalance: {
        amount: summary.startBalance.amount,
        date: summary.startBalance.date,
        type: BalanceType[summary.startBalance.type as BalanceType]
      }
    }),
    ...(summary.endBalance && {
      endBalance: {
        amount: summary.endBalance.amount,
        date: summary.endBalance.date,
        type: BalanceType[summary.endBalance.type as BalanceType]
      }
    })
  };
}
