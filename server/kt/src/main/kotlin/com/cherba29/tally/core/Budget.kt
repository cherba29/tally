package com.cherba29.tally.core

data class Budget(
  // Period over which the budget is defined.
  val months: List<Month>,
  // Account name to account map.
  val accounts: Map<String, Account>,
  // Account name -> month -> balance map.
  val balances: Map<String, Map<Month, Balance>>,
  // Account name -> month -> transfers map.
  val transfers: Map<String, Map<Month, List<Transfer>>>
)