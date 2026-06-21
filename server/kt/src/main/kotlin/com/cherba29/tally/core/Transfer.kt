package com.cherba29.tally.core

data class Transfer(
  val fromAccount: List<String>,
  val toAccount: List<String>,
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
        else if (fromAccount.last() != other.fromAccount.last()) {
          if (fromAccount.last() < other.fromAccount.last()) -1 else 1
        } else if (toAccount.last() != other.toAccount.last()) {
          if (toAccount.last() < other.toAccount.last()) -1 else 1
        } else if (description != other.description) {
          if (description.orEmpty() < other.description.orEmpty()) -1 else 1
        } else {
          0
        }
      }
    }
  }
}