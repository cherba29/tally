package com.cherba29.tally.core

/**
 * Represents an account to serve as source and destination of transfers.
 */
data class Account(
  // Account name.
  val name: String,
  // Path to categorize this account by.
  val path: List<String>,
  // All owners of this account.
  val owners: Set<String>,
  // Human-readable description for the account for display.
  val description: String? = null,
  /**
   * Compared to other accounts in this path how important it is.
   * Lower rank will sort this account above others.
   */
  val rank: Int? = null,
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
  // Day when statement closes for this account.
  val statementCloseDate: DayInMonth = DayInMonth.Unknown()
) {
  override fun toString(): String = "Account $name /${path.joinToString("/")}${if (closedOn == null) "" else " Closed $closedOn"}"

  val isExternal = path.contains(EXTERNAL_NAME)
  val isInactive = path.contains(INACTIVE_NAME)

  fun isClosed(month: Month): Boolean {
    return (closedOn != null) && (closedOn < month) || // After closed.
           (month < openedOn) // Before or on open.
  }

  fun cloneInactive(): Account =
    copy(
      name = "_$name",
      path = path + INACTIVE_NAME,
      openedOn = closedOn!!,
      closedOn = null
    )
}
