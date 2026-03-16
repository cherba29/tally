package com.cherba29.tally.core

enum class AccountType(val id: String) {
  UNSPECIFIED("_unspecified_"),
  BILL("bill"),
  CHECKING("checking"),
  CREDIT("credit"),
  CREDIT_CARD("credit-card"),
  DEFERRED_INCOME("deferred income"),
  EXTERNAL("external"),
  INCOME("income"),
  INVESTMENT("investment"),
  RETIREMENT("retirement"),
  SUMMARY("_summary_"),
  TAX("tax_"),
}

data class Account(
  val name: String,
  val description: String? = null,
  val path: List<String> = listOf(),
  // Account type, for example 'CREDIT_CARD'.
  val type: AccountType,
  // Real account number associated with this account.
  val number: String? = null,
  // Month when account was opened and possibly closed.
  val openedOn: Month? = null,
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
  override fun toString(): String = "Account $name ${type.id}${if (closedOn == null) "" else " Closed $closedOn"}"

  fun isClosed(month: Month): Boolean {
    return (closedOn != null) && (closedOn < month) || // After closed.
           (openedOn != null) && (month < openedOn) // Before or on open.
  }

  val isExternal: Boolean = type === AccountType.EXTERNAL ||
      type === AccountType.TAX || type === AccountType.DEFERRED_INCOME

  val isSummary: Boolean = type == AccountType.SUMMARY

  fun hasCommonOwner(other: Account): Boolean = owners.intersect(other.owners).isNotEmpty()

  val typeIdName = type.name
}
