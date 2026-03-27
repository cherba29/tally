package com.cherba29.tally.core

data class Account(
  val name: String,
  val description: String? = null,
  val path: List<String> = listOf(),
  // Real account number associated with this account.
  val number: String? = null,
  // Month when account was opened and possibly closed.
  val openedOn: Month,
  val closedOn: Month? = null,
  // List of account owner ids.
  val owners: List<String> = listOf(),
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
  override fun toString(): String = "Account $name /${path.joinToString("/")}${if (closedOn == null) "" else " Closed $closedOn"}"

  fun isClosed(month: Month): Boolean {
    return (closedOn != null) && (closedOn < month) || // After closed.
           (month < openedOn) // Before or on open.
  }

  val isExternal: Boolean = path.firstOrNull() == "external"
  val isSummary: Boolean = name.startsWith("/")

  fun hasCommonOwner(other: Account): Boolean = owners.intersect(other.owners).isNotEmpty()
}

