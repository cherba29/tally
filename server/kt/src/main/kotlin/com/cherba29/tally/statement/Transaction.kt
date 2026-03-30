package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId

data class Transaction(
  val nodeId: NodeId,
  val balance: Balance,
  val description: String?,
  val type: Type,
  // TODO: make immutable.
  var balanceFromStart: Int?
) {
  enum class Type {
    UNKNOWN,
    TRANSFER,
    INCOME,
    EXPENSE,
  }
}

fun buildTransactionStatementTable(budget: Budget, owner: String?): List<TransactionStatement> {
  val statementTable = mutableListOf<TransactionStatement>()

  val makeStatement: (NodeId, Month, Boolean) -> TransactionStatement = { nodeId, month, isClosed ->
    TransactionStatement.fromTransfers(
      nodeId,
      month,
      isClosed,
      budget.transfers[nodeId]?.get(month),
      budget.balances[nodeId]?.get(month)
    )
  }

  // Working backwards.
  val months = budget.months.sortedDescending()
  if (months.isEmpty()) {
    throw IllegalArgumentException("Budget must have at least one month.")
  }

  for (account in budget.accounts.values) {
    if (owner != null && !account.nodeId.owners.contains(owner)) {
      continue
    }
    val accountStatements = mutableListOf<TransactionStatement>()
    // Make statement outside range so that its attributes relating to previous can be used.
    var nextMonthStatement = makeStatement(account.nodeId, months[0].next(), account.isClosed(months[0].next()))
    for (month in months) {
      val statement = makeStatement(account.nodeId, month, account.isClosed(month))
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
