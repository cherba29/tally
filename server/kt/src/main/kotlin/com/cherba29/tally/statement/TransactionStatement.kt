package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transaction

/**
 * Extension of Statement for transactions for an account ie treeNode.
 */
class TransactionStatement(
  monthRange: MonthRange,
  startBalance: Balance?,
  endBalance: Balance? = null,
  inFlows: Long = 0L,
  outFlows: Long = 0L,
  totalTransfers: Long = 0L,
  totalPayments: Long = 0L,
  income: Long = 0L,
  // True if any transactions in this statement "cover" previous statement.
  val coversPrevious: Boolean = false,

  // True if any projected transactions in this statement "cover"
  // previous statement.
  val coversProjectedPrevious: Boolean = false,

  // True if any of the transactions are projects.
  val hasProjectedTransfer: Boolean = false,

  // True if this statement is covered by next.
  val isCovered: Boolean = false,

  // True if this statement is covered by any projected transactions in next statement.
  val isProjectedCovered: Boolean = false,

  // List of transaction in this statement.
  val transactions: List<Transaction> = listOf(),

  /** Balance after each transaction. */
  val balanceFromStart: List<Long?> = listOf()
) :
  Statement(
    monthRange,
    startBalance,
    endBalance,
    inFlows,
    outFlows,
    totalTransfers,
    totalPayments,
    income
  ) {

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
