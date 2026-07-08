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

      var nextMonthStatement = transactionStatement {
        treeNode = leafTreeNode
        month = nextMonth
        isClosed = account.isClosed(nextMonth)
        startBalance = monthlyBalances[nextMonth]

        for (transfer in monthlyTransfers[nextMonth] ?: listOf()) {
          addTransfer(transfer)
        }
      }
      for (month in months) {
        nextMonthStatement = transactionStatement {
          treeNode = leafTreeNode
          this.month = month
          isClosed = account.isClosed(month)
          startBalance = monthlyBalances[month]
          for (transfer in monthlyTransfers[month] ?: listOf()) {
            addTransfer(transfer)
          }
          endBalance = nextMonthStatement.startBalance
          isCovered =
            endBalance == null || endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
          isProjectedCovered = isCovered || nextMonthStatement.coversProjectedPrevious
        }
        accountStatements.add(nextMonthStatement)
      }
      // Do not include account if for all months it was closed.
      if (accountStatements.any { !it.isClosed }) {
        statementTable.addAll(accountStatements)
      }
    }
    return statementTable
  }
}
