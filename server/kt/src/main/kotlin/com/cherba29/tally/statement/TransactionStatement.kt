package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.MonthRange

// Extension of Statement for transactions over an account.
class TransactionStatement(treeNode: TreeNode, monthRange: MonthRange, isClosed: Boolean, startBalance: Balance?) :
  Statement(treeNode, monthRange, isClosed, startBalance) {
  // List of transaction in this statement.
  val transactions: MutableList<Transaction> = mutableListOf()

  // True if any transactions in this statement "cover" previous statement.
  var coversPrevious = false

  // True if any projected transactions in this statement "cover"
  // previous statement.
  var coversProjectedPrevious = false

  // True if any of the transactions are projects.
  var hasProjectedTransfer = false

  // True if this statement is covered by next.
  var isCovered = false

  // True if this statement is covered by any projected transactions in next statement.
  var isProjectedCovered = false

  override fun toString(): String {
    return super.toString() + " coversPrevious=$coversPrevious coversProjectPrevious=$coversProjectedPrevious" +
        " hasProjectedTransfer=$hasProjectedTransfer isCovered=$isCovered isProjectedCovered=$isProjectedCovered" +
        " transactions=$transactions"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as TransactionStatement

    if (coversPrevious != other.coversPrevious) return false
    if (coversProjectedPrevious != other.coversProjectedPrevious) return false
    if (hasProjectedTransfer != other.hasProjectedTransfer) return false
    if (isCovered != other.isCovered) return false
    if (isProjectedCovered != other.isProjectedCovered) return false
    if (transactions != other.transactions) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + coversPrevious.hashCode()
    result = 31 * result + coversProjectedPrevious.hashCode()
    result = 31 * result + hasProjectedTransfer.hashCode()
    result = 31 * result + isCovered.hashCode()
    result = 31 * result + isProjectedCovered.hashCode()
    result = 31 * result + transactions.hashCode()
    return result
  }
}
