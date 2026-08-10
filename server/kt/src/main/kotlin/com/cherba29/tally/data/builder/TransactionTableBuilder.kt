package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.TransactionStatement

class TransactionTableBuilder {
  // TODO: turn this into more idiomatic builder pattern.
  fun buildTransactionStatementTable(
    months: MonthRange,
    leafToAccountMap: Map<TreeNode.Leaf, Account>,
    leafToMonthlyBalancesMap: Map<TreeNode.Leaf, Map<Month, Balance>>,
    leafToMonthlyTransfersMap: Map<TreeNode.Leaf, Map<Month, List<Transfer>>>
  ): Map<TreeNode, Map<Month, TransactionStatement>> {
    if (months.isEmpty()) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }
    return leafToAccountMap.map { (leafTreeNode, account) ->
      leafTreeNode to buildAccountTransactionStatements(
        leafTreeNode,
        account,
        months,
        leafToMonthlyBalancesMap[leafTreeNode] ?: mapOf(),
        leafToMonthlyTransfersMap[leafTreeNode] ?: mapOf()
      )
    }.toMap()
  }

  private fun buildAccountTransactionStatements(
    leafTreeNode: TreeNode.Leaf,
    account: Account,
    months: MonthRange,
    monthlyBalances: Map<Month, Balance>,
    monthlyTransfers: Map<Month, List<Transfer>>,
  ): Map<Month, TransactionStatement> {
    val accountStatements = mutableMapOf<Month, TransactionStatement>()

    // Make statement outside range so that its attributes relating to previous can be used.
    val nextMonth = months.last().next()

    var nextMonthStatement = transactionStatement {
      treeNode = leafTreeNode
      month = nextMonth
      isClosed = account.isClosed(nextMonth)
      startBalance = monthlyBalances[nextMonth]

      for (transfer in monthlyTransfers[nextMonth] ?: listOf()) {
        addTransfer(transfer)
      }
    }
    // TODO: maybe do not generate statement for closed account.
    // Working backwards.
    var areClosed = true
    for (month in months.reversed()) {
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
      areClosed = areClosed and nextMonthStatement.isClosed
      accountStatements[month] = nextMonthStatement
    }
    // Do not include account if for all months it was closed.
    return if (areClosed) mutableMapOf() else accountStatements
  }
}
