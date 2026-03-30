package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.statement.CombinedStatement
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import kotlin.math.roundToInt

fun Account.toGql(): GqlAccount = GqlAccount(
  name = nodeId.name,
  description = description ?: "",
  path = nodeId.path,
  external = nodeId.isExternal,
  summary = nodeId.isSummary,
  number = number,
  openedOn = openedOn,
  closedOn = closedOn,
  owners = nodeId.owners,
  url = url ?: "",
  address = address ?: "",
  userName = userName ?: "",
  password = password ?: "",
  phone = phone ?: "",
)

fun Balance.Type.toGql() = id

fun Balance.toGql(): GqlBalance = GqlBalance(
  amount = amount,
  date = date,
  type = type.toGql()
)

fun Transaction.toGql(): GqlTransaction = GqlTransaction(
  toAccountName = nodeId.name,
  isIncome = type == Transaction.Type.INCOME,
  isExpense = type == Transaction.Type.EXPENSE,
  balance = balance.toGql(),
  balanceFromStart = balanceFromStart ?: 0,
  description = description ?: ""
)

private fun Double?.round2Float(): Float {
  if (this == null) return 0.0f
  return ((this * 100.0).roundToInt() / 100.0).toFloat()
}

fun TransactionStatement.toGql(): GqlStatement = GqlStatement(
  name = nodeId.name,
  month = month,
  isClosed = isClosed,
  isCovered = isCovered,
  isProjectedCovered = isProjectedCovered,
  hasProjectedTransfer = hasProjectedTransfer,
  startBalance = startBalance?.toGql(),
  endBalance = endBalance?.toGql(),
  inFlows = inFlows,
  outFlows = outFlows,
  income = income,
  totalPayments = totalPayments,
  totalTransfers = totalTransfers,
  change = change ?: 0,
  addSub = addSub,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted ?: 0,
  transactions = transactions.map { it.toGql() }
)

fun TransactionStatement.toGqlTableCell(): GqlTableCell = GqlTableCell(
  month = month,
  isClosed = isClosed,
  addSub = addSub,
  balance = endBalance?.amount,
  isProjected = (endBalance != null && endBalance?.type != Balance.Type.CONFIRMED) || hasProjectedTransfer,
  isCovered = isCovered,
  isProjectedCovered = isProjectedCovered,
  hasProjectedTransfer = hasProjectedTransfer,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted,
  balanced = unaccounted == null || unaccounted == 0
)

fun CombinedStatement.toGqlStatement(): GqlStatement = GqlStatement(
  name = nodeId.name,
  month = month,
  isClosed = isClosed,
  isCovered = true,
  isProjectedCovered = false,
  hasProjectedTransfer = false,
  startBalance = startBalance?.toGql(),
  endBalance = endBalance?.toGql(),
  inFlows = inFlows,
  outFlows = outFlows,
  income = income,
  totalPayments = totalPayments,
  totalTransfers = totalTransfers,
  change = change ?: 0,
  addSub = addSub,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted ?: 0,
  transactions = listOf()
)

fun Statement.toGqlTableCell(): GqlTableCell = GqlTableCell(
  month = month,
  isClosed = isClosed,
  addSub = addSub,
  balance = endBalance?.amount,
  isProjected = endBalance?.type != Balance.Type.CONFIRMED,
  isCovered = false,
  isProjectedCovered = false,
  hasProjectedTransfer = false,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted,
  balanced = unaccounted == null || unaccounted == 0
)

fun SummaryStatement.toGql(): GqlSummaryStatement = GqlSummaryStatement(
  name = nodeId.name,
  month = month,
  accounts = statements.map { it.nodeId.name }.sorted(),
  addSub = addSub,
  income = income,
  change = change ?: 0,
  inFlows = inFlows,
  outFlows = outFlows,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  totalPayments = totalPayments,
  totalTransfers = totalTransfers,
  unaccounted = unaccounted ?: 0,
  endBalance = endBalance?.toGql(),
  startBalance = startBalance?.toGql()
)

fun SummaryStatement.toGqlStatement(): GqlStatement = GqlStatement(
  name = nodeId.name,
  month = month,
  isClosed = isClosed,
  isCovered = true,
  isProjectedCovered = true,
  hasProjectedTransfer = false,
  startBalance = startBalance?.toGql(),
  endBalance = endBalance?.toGql(),
  inFlows = inFlows,
  outFlows = outFlows,
  income = income,
  totalPayments = totalPayments,
  totalTransfers = totalTransfers,
  change = change ?: 0,
  addSub = addSub,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted ?: 0,
  transactions = listOf()
)

