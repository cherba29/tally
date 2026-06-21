package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Group

data class Transaction(
  val nodeId: Group,
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
