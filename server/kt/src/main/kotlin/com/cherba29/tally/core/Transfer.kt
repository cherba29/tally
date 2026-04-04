package com.cherba29.tally.core

data class Transfer(
  val fromAccount: Account,
  val toAccount: Account,
  val fromMonth: Month,
  val toMonth: Month,
  val description: String?,
  val balance: Balance,
) : Comparable<Transfer> {
  override fun compareTo(other: Transfer): Int {
    var eq: Int = balance.compareTo(other.balance)
    return if (eq != 0) eq
    else {
      eq = fromMonth.compareTo(other.fromMonth)
      if (eq != 0) eq
      else {
        eq = toMonth.compareTo(other.toMonth)
        if (eq != 0) eq
        else if (fromAccount.nodeId.name != other.fromAccount.nodeId.name) {
          if (fromAccount.nodeId.name < other.fromAccount.nodeId.name) -1 else 1
        } else if (toAccount.nodeId.name != other.toAccount.nodeId.name) {
          if (toAccount.nodeId.name < other.toAccount.nodeId.name) -1 else 1
        } else if (description != other.description) {
          if (description.orEmpty() < other.description.orEmpty()) -1 else 1
        } else {
          0
        }
      }
    }
  }
}