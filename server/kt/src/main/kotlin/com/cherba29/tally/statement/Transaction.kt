package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode

data class Transaction(
  val treeNode: TreeNode,
  val balance: Balance,
  val description: String?,
  val type: Type,
  val balanceFromStart: Long?
)  : Comparable<Transaction> {
  override fun compareTo(other: Transaction): Int {
    val eq: Int = balance.compareTo(other.balance)
    return if (eq != 0) eq
    else {
      if (treeNode.name != other.treeNode.name) {
        if (treeNode.name < other.treeNode.name) -1 else 1
      } else if (description != other.description) {
        if (description.orEmpty() < other.description.orEmpty()) -1 else 1
      } else if (type != other.type) {
        type.compareTo(other.type)
      } else {
        0
      }
    }
  }

  enum class Type {
    UNKNOWN,
    TRANSFER,
    INCOME,
    EXPENSE,
  }
}
