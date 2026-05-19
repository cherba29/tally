package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.Transfer

fun buildTransactionStatementTable(
  months: MonthRange,
  accounts: Map<NodeId, Account>,
  balances: Map<NodeId, Map<Month, Balance>>,
  transfers: Map<NodeId, Map<Month, List<Transfer>>>,
  owner: String?
): List<TransactionStatement> {
  val statementTable = mutableListOf<TransactionStatement>()

  // Working backwards.
  val months = months.sortedDescending()
  if (months.isEmpty()) {
    throw IllegalArgumentException("Budget must have at least one month.")
  }

  for ((nodeId, account) in accounts) {
    if (owner != null && owner !in nodeId.owners) {
      continue
    }
    val accountStatements = mutableListOf<TransactionStatement>()
    val monthlyTransfers = transfers[nodeId] ?: mapOf()
    val monthlyBalances = balances[nodeId] ?: mapOf()

    // Make statement outside range so that its attributes relating to previous can be used.
    val nextMonth = months.first().next()
    var nextMonthStatement = TransactionStatement.fromTransfers(
      nodeId,
      nextMonth..nextMonth,
      account.isClosed(nextMonth),
      monthlyTransfers[nextMonth],
      monthlyBalances[nextMonth]
    )
    for (month in months) {
      val statement = TransactionStatement.fromTransfers(
        nodeId,
        month..month,
        account.isClosed(month),
        monthlyTransfers[month],
        monthlyBalances[month]
      )
      statement.endBalance = nextMonthStatement.startBalance
      statement.isCovered =
        statement.endBalance == null || statement.endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
      statement.isProjectedCovered = statement.isCovered || nextMonthStatement.coversProjectedPrevious
      nextMonthStatement = statement
      if (statement.startBalance != null) {
        var prevBalance = statement.startBalance!!.amount
        for (t in statement.transactions.asReversed()) {
          t.balanceFromStart = prevBalance + t.balance.amount
          prevBalance = t.balanceFromStart!!
        }
      }
      accountStatements.add(statement)
    }
    // Do not include account if for all months it was closed.
    if (accountStatements.any { !it.isClosed }) {
      statementTable.addAll(accountStatements)
    }
  }
  return statementTable
}
