package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import kotlin.math.roundToInt

fun Account.toGql(isExternal: Boolean, isSummary: Boolean): GqlAccount = GqlAccount(
  name = if (isSummary) (path + listOf(name)).filter { it.isNotEmpty() }.joinToString("/") else name,
  description = description ?: "",
  path = path,
  external = isExternal,
  summary = isSummary,
  number = number,
  openedOn = openedOn,
  closedOn = closedOn,
  owners = owners.sorted(),
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
  type = type.toGql(),
  desc = description,
)

fun Transaction.toGql(): GqlTransaction = GqlTransaction(
  toAccountName = targetTreeNode.name,
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

fun TransactionStatement.toGql(accountName: String): GqlStatement = GqlStatement(
  name = accountName,
  month = monthRange.first,
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
  month = monthRange.first,
  isClosed = isClosed,
  addSub = addSub,
  balance = endBalance?.amount,
  isProjected = (endBalance != null && endBalance.type != Balance.Type.CONFIRMED) || hasProjectedTransfer,
  isCovered = isCovered,
  isProjectedCovered = isProjectedCovered,
  hasProjectedTransfer = hasProjectedTransfer,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted,
  balanced = unaccounted == null || unaccounted == 0L
)

fun SummaryStatement.toGqlTableCell(): GqlTableCell = GqlTableCell(
  month = monthRange.first,
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
  balanced = unaccounted == null || unaccounted == 0L
)

fun Statement.toGqlTableCell() = when (this) {
  is TransactionStatement -> toGqlTableCell()
  is SummaryStatement -> toGqlTableCell()
}

fun SummaryStatement.toGql(summaryName: String): GqlSummaryStatement = GqlSummaryStatement(
  name = summaryName,
  month = monthRange.first,
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

/**
 * Converts summary statement as a summary data with substatements and a total.
 **/
fun SummaryStatement.toGqlSummaryData(summaryName: String): GqlSummaryData =  GqlSummaryData(
  statements = statements.toSortedMap().map { (treeNode, stmt) ->
    when (stmt) {
      is SummaryStatement -> (stmt as Statement).toGql(treeNode.name)  // Treat it as regular statement.
      else -> stmt.toGql(treeNode.name)
    }
  },
  total = toGql(summaryName)
)


fun Statement.toGql(statementName: String): GqlStatement = GqlStatement(
  name = statementName,
  month = monthRange.first,
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

