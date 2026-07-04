package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.TransactionStatement

class TransactionTableBuilder {
  fun buildTransactionStatementTable(
    months: MonthRange,
    leafToAccountMap: Map<TreeNode.Leaf, Account>,
    leafToMonthlyBalancesMap: Map<TreeNode.Leaf, Map<Month, Balance>>,
    leafToMonthlyTransfersMap: Map<TreeNode.Leaf, Map<Month, List<Transfer>>>
  ): List<TransactionStatement> {
    val statementTable = mutableListOf<TransactionStatement>()

    // Working backwards.
    val months = months.sortedDescending()
    if (months.isEmpty()) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    for ((leafTreeNode, account) in leafToAccountMap) {
      val accountStatements = mutableListOf<TransactionStatement>()
      val monthlyTransfers = leafToMonthlyTransfersMap[leafTreeNode] ?: mapOf()
      val monthlyBalances = leafToMonthlyBalancesMap[leafTreeNode] ?: mapOf()

      // Make statement outside range so that its attributes relating to previous can be used.
      val nextMonth = months.first().next()
      var nextMonthStatement = TransactionStatementBuilder().fromTransfers(
        leafTreeNode,
        nextMonth..nextMonth,
        account.isClosed(nextMonth),
        monthlyTransfers[nextMonth],
        monthlyBalances[nextMonth]
      )
      for (month in months) {
        val statement = TransactionStatementBuilder().fromTransfers(
          leafTreeNode,
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
        accountStatements.add(statement)
      }
      // Do not include account if for all months it was closed.
      if (accountStatements.any { !it.isClosed }) {
        statementTable.addAll(accountStatements)
      }
    }
    return statementTable
  }
}