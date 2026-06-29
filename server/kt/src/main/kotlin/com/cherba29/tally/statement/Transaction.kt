package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode

data class Transaction(
  val treeNode: TreeNode,
  val balance: Balance,
  val description: String?,
  val type: Type,
  // TODO: make immutable.
  var balanceFromStart: Long?
) {
  enum class Type {
    UNKNOWN,
    TRANSFER,
    INCOME,
    EXPENSE,
  }
}
