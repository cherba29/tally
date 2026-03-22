package com.cherba29.tally.core

import io.github.oshai.kotlinlogging.KotlinLogging

data class Budget(
  // Period over which the budget is defined.
  val months: List<Month>,
  // Account name to account map.
  val accounts: Map<String, Account>,
  // Account name -> month -> balance map.
  val balances: Map<String, Map<Month, Balance>>,
  // Account name -> month -> transfers map.
  val transfers: Map<String, Map<Month, List<Transfer>>>
) {
  // TODO: is this needed, since this will always return all accounts.
  fun findActiveAccounts(): List<Account> = accounts.values.filter {
    account -> months.any { !account.isClosed(it) }
  }
}
