package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month

// Extension of Statement for transactions over an account.
class TransactionStatement(account: Account, month: Month, startBalance: Balance?) :
  Statement(account, month, startBalance) {
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

  override val isClosed: Boolean = account.isClosed(this.month)
}
