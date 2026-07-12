package com.cherba29.tally.core

data class Transfer(
  val fromAccount: TreeNode.Leaf,
  val toAccount: TreeNode.Leaf,
  val month: Month,
  val balance: Balance,
  val description: String?,
  val tags: List<String>,
) : Comparable<Transfer> {
  override fun compareTo(other: Transfer): Int {
    var eq: Int = balance.compareTo(other.balance)
    return if (eq != 0) eq
    else {
      eq = month.compareTo(other.month)
      if (eq != 0) eq
      else {
        if (fromAccount.name != other.fromAccount.name) {
          if (fromAccount.name < other.fromAccount.name) -1 else 1
        } else if (toAccount.name != other.toAccount.name) {
          if (toAccount.name < other.toAccount.name) -1 else 1
        } else if (description != other.description) {
          if (description.orEmpty() < other.description.orEmpty()) -1 else 1
        } else {
          0
        }
      }
    }
  }
}
