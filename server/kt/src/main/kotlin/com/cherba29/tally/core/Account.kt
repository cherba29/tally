package com.cherba29.tally.core

data class Account(
  val name: String,
  val description: String? = null,
  val path: List<String> = listOf(),
  // Account type, for example 'CREDIT_CARD'.
  val type: Type,
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
  enum class Type(val id: String) {
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
    TAX("tax_");

    companion object {
      fun fromString(value: String?): Type? {
        if (value == null) { return UNSPECIFIED }
        return entries.find { it.id == value }
      }
    }
  }

  override fun toString(): String = "Account $name ${type.id}${if (closedOn == null) "" else " Closed $closedOn"}"

  fun toSnapshot(): String {
     return """Account {
       name = $name
       description = $description
       path = [${path.joinToString()}]
       type = $type
       number = $number
       openedOn = $openedOn
       closedOn = $closedOn
       owners = [${owners.joinToString()}]
       url = $url
       address = $address
       phone = $phone
       userName = $userName
       password = $password
     }""".trimIndent()
  }

  fun isClosed(month: Month): Boolean {
    return (closedOn != null) && (closedOn < month) || // After closed.
           (openedOn != null) && (month < openedOn) // Before or on open.
  }

  val isExternal: Boolean = type === Type.EXTERNAL ||
      type === Type.TAX || type === Type.DEFERRED_INCOME

  val isSummary: Boolean = type == Type.SUMMARY

  fun hasCommonOwner(other: Account): Boolean = owners.intersect(other.owners).isNotEmpty()

  val typeIdName = type.name
}
