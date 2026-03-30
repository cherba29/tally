package com.cherba29.tally.core

data class Budget(
  // Period over which the budget is defined.
  val months: List<Month>,
  // Account id to account map.
  val accounts: Map<NodeId, Account>,
  // Account id -> month -> balance map.
  val balances: Map<NodeId, Map<Month, Balance>>,
  // Account id -> month -> transfers map.
  val transfers: Map<NodeId, Map<Month, List<Transfer>>>
)