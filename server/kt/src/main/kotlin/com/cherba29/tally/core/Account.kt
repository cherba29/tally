package com.cherba29.tally.core

// TODO: Introduce some notion of order/priority, so that those can be displayed higher up the list.
data class Account(
  val nodeId: NodeId,
  val description: String? = null,
  // Real account number associated with this account.
  val number: String? = null,
  // Month when account was opened and possibly closed.
  val openedOn: Month,
  val closedOn: Month? = null,
  // Url to the account.
  val url: String? = null,
  // Physical address for the account.
  val address: String? = null,
  // Phone number for customer support.
  val phone: String? = null,
  // Username/password to use to login to the account.
  val userName: String? = null,
  val password: String? = null,
) {
  override fun toString(): String = "Account ${nodeId.name} /${nodeId.path.joinToString("/")}${if (closedOn == null) "" else " Closed $closedOn"}"

  fun isClosed(month: Month): Boolean {
    return (closedOn != null) && (closedOn < month) || // After closed.
           (month < openedOn) // Before or on open.
  }
}

