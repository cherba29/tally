package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.data.Profile
import com.cherba29.tally.statement.TransactionStatement

/**
 * Builds transaction statements for particular account over a sequence of months.
 */
class MonthTransactionStatementBuilder {
  var months: MonthRange = MonthRange.EMPTY
  var monthlyBalances: Map<Month, Balance> = mutableMapOf()
  private val monthlyTransfers = mutableMapOf<Month, MutableList<Transaction>>()
  fun addTransfer(
    fromAccount: TreeNode.Leaf<Profile>,
    toAccount: TreeNode.Leaf<Profile>,
    month: Month,
    balance: Balance,
    description: String?
  ) {
    monthlyTransfers.getOrPut(month) {
      mutableListOf()
    }.add(
      Transaction(
        targetTreeNode = toAccount,
        balance = balance,
        description = description,
        type = Transaction.typeOf(fromAccount, toAccount, balance.amount)
      )
    )
  }

  fun build(): Map<Month, TransactionStatement> {
    val accountStatements = mutableMapOf<Month, TransactionStatement>()
    // Make statement outside range so that its attributes relating to previous can be used.
    val nextMonth = months.last().next()

    // Dummy future statement to help with endBalance and whether last month is covered.
    // It's not included int the result.
    var nextMonthStatement = transactionStatement {
      month = nextMonth
      startBalance = monthlyBalances[nextMonth]
      monthlyTransfers[nextMonth]?.forEach { addTransfer(it) }
    }
    // Working backwards.
    for (month in months.reversed()) {
      nextMonthStatement = transactionStatement {
        this.month = month
        startBalance = monthlyBalances[month]
        monthlyTransfers[month]?.forEach { addTransfer(it) }
        endBalance = nextMonthStatement.startBalance
        isCovered =
          endBalance == null || endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
        isProjectedCovered = isCovered || nextMonthStatement.coversProjectedPrevious
      }
      accountStatements[month] = nextMonthStatement
    }
    return accountStatements
  }
}
