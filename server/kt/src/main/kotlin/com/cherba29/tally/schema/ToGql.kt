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
  amount = amount.toInt(),
  date = date,
  type = type.toGql(),
  desc = description,
)

fun Transaction.toGql(): GqlTransaction = GqlTransaction(
  toAccountName = treeNode.name,
  isIncome = type == Transaction.Type.INCOME,
  isExpense = type == Transaction.Type.EXPENSE,
  balance = balance.toGql(),
  balanceFromStart = balanceFromStart?.toInt() ?: 0,
  description = description ?: ""
)

private fun Double?.round2Float(): Float {
  if (this == null) return 0.0f
  return ((this * 100.0).roundToInt() / 100.0).toFloat()
}

fun TransactionStatement.toGql(): GqlStatement = GqlStatement(
  name = treeNode.name,
  month = monthRange.first,
  isClosed = isClosed,
  isCovered = isCovered,
  isProjectedCovered = isProjectedCovered,
  hasProjectedTransfer = hasProjectedTransfer,
  startBalance = startBalance?.toGql(),
  endBalance = endBalance?.toGql(),
  inFlows = inFlows.toInt(),
  outFlows = outFlows.toInt(),
  income = income.toInt(),
  totalPayments = totalPayments.toInt(),
  totalTransfers = totalTransfers.toInt(),
  change = change?.toInt() ?: 0,
  addSub = addSub.toInt(),
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted?.toInt() ?: 0,
  transactions = transactions.map { it.toGql() }
)

fun TransactionStatement.toGqlTableCell(): GqlTableCell = GqlTableCell(
  month = monthRange.first,
  isClosed = isClosed,
  addSub = addSub.toInt(),
  balance = endBalance?.amount?.toInt(),
  isProjected = (endBalance != null && endBalance?.type != Balance.Type.CONFIRMED) || hasProjectedTransfer,
  isCovered = isCovered,
  isProjectedCovered = isProjectedCovered,
  hasProjectedTransfer = hasProjectedTransfer,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted?.toInt(),
  balanced = unaccounted == null || unaccounted == 0L
)

fun Statement.toGqlTableCell(): GqlTableCell = GqlTableCell(
  month = monthRange.first,
  isClosed = isClosed,
  addSub = addSub.toInt(),
  balance = endBalance?.amount?.toInt(),
  isProjected = endBalance?.type != Balance.Type.CONFIRMED,
  isCovered = false,
  isProjectedCovered = false,
  hasProjectedTransfer = false,
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted?.toInt(),
  balanced = unaccounted == null || unaccounted == 0L
)

fun SummaryStatement.toGql(): GqlSummaryStatement = GqlSummaryStatement(
  name = treeNode.name,
  month = monthRange.first,
  accounts = statements.map { it.treeNode.name }.sorted(),
  addSub = addSub.toInt(),
  income = income.toInt(),
  change = change?.toInt() ?: 0,
  inFlows = inFlows.toInt(),
  outFlows = outFlows.toInt(),
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  totalPayments = totalPayments.toInt(),
  totalTransfers = totalTransfers.toInt(),
  unaccounted = unaccounted?.toInt() ?: 0,
  endBalance = endBalance?.toGql(),
  startBalance = startBalance?.toGql()
)

/**
 * Converts summary statement as a summary data with substatements and a total.
 **/
fun SummaryStatement.toGqlSummaryData(): GqlSummaryData =  GqlSummaryData(
  statements = statements.sortedWith { a, b ->
    if (a.treeNode.name < b.treeNode.name) -1 else 1
  }.map { stmt ->
    when (stmt) {
      is SummaryStatement -> (stmt as Statement).toGql()  // Treat it as regular statement.
      else -> stmt.toGql()
    }
  },
  total = toGql()
)


fun Statement.toGql(): GqlStatement = GqlStatement(
  name = treeNode.name,
  month = monthRange.first,
  isClosed = isClosed,
  isCovered = true,
  isProjectedCovered = true,
  hasProjectedTransfer = false,
  startBalance = startBalance?.toGql(),
  endBalance = endBalance?.toGql(),
  inFlows = inFlows.toInt(),
  outFlows = outFlows.toInt(),
  income = income.toInt(),
  totalPayments = totalPayments.toInt(),
  totalTransfers = totalTransfers.toInt(),
  change = change?.toInt() ?: 0,
  addSub = addSub.toInt(),
  percentChange = percentChange.round2Float(),
  annualizedPercentChange = annualizedPercentChange.round2Float(),
  unaccounted = unaccounted?.toInt() ?: 0,
  transactions = listOf()
)

