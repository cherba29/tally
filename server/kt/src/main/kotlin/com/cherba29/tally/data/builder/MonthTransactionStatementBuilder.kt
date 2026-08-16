package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.TransactionStatement

class MonthTransactionStatementBuilder {
  companion object {
    fun make(
      leafTreeNode: TreeNode.Leaf,
      months: MonthRange,
      monthToClosed: Map<Month, Boolean>,
      monthlyBalances: Map<Month, Balance>,
      monthlyTransfers: Map<Month, List<Transfer>>,
    ): Map<Month, TransactionStatement> {
      val accountStatements = mutableMapOf<Month, TransactionStatement>()

      // Make statement outside range so that its attributes relating to previous can be used.
      val nextMonth = months.last().next()

      // Dummy future statement to help with endBalance and whether last month is covered.
      // It's not included int the result.
      var nextMonthStatement = transactionStatement {
        treeNode = leafTreeNode
        month = nextMonth
        isClosed = false
        startBalance = monthlyBalances[nextMonth]
        monthlyTransfers[nextMonth]?.forEach { addTransfer(it) }
      }
      // TODO: maybe do not generate statement for closed account.
      // Working backwards.
      var areClosed = true
      for (month in months.reversed()) {
        nextMonthStatement = transactionStatement {
          treeNode = leafTreeNode
          this.month = month
          isClosed = monthToClosed[month] ?: throw IllegalArgumentException("No isClosed value for $month")
          startBalance = monthlyBalances[month]
          monthlyTransfers[month]?.forEach { addTransfer(it) }
          endBalance = nextMonthStatement.startBalance
          isCovered =
            endBalance == null || endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
          isProjectedCovered = isCovered || nextMonthStatement.coversProjectedPrevious
        }
        areClosed = areClosed and nextMonthStatement.isClosed
        accountStatements[month] = nextMonthStatement
      }
      // Do not include internal account if for all months it was closed.
      // Internal accounts are closed with zero balance, so are not interesting anymore.
      return if (!leafTreeNode.isExternal and areClosed) mutableMapOf() else accountStatements
    }
  }
}
