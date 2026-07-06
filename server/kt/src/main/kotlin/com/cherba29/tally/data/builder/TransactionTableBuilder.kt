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

      val nextMonthStatementBuilder = TransactionStatementBuilder()
      nextMonthStatementBuilder.treeNode = leafTreeNode
      nextMonthStatementBuilder.month = nextMonth
      nextMonthStatementBuilder.isClosed = account.isClosed(nextMonth)
      nextMonthStatementBuilder.startBalance = monthlyBalances[nextMonth]

      for (transfer in monthlyTransfers[nextMonth] ?: listOf()) {
        nextMonthStatementBuilder.addTransfer(transfer)
      }
      var nextMonthStatement = nextMonthStatementBuilder.build()
      for (month in months) {
        val statementBuilder = TransactionStatementBuilder()
        statementBuilder.treeNode = leafTreeNode
        statementBuilder.month = month
        statementBuilder.isClosed = account.isClosed(month)
        statementBuilder.startBalance = monthlyBalances[month]
        for (transfer in monthlyTransfers[month] ?: listOf()) {
          statementBuilder.addTransfer(transfer)
        }
        val statement = statementBuilder.build()
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
