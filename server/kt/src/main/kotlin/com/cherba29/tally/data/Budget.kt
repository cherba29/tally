package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.Transfer

/**
 * Data representing all accounts, their transactions and totals per month.
 */
data class Budget(
  /** Period over which the budget is defined. */
  val months: MonthRange,
  /** Hierarchical structure of accounts and summaries. */
  val tree: Group,
  /** Maps leaf tree node to corresponding account. */
  val leafToAccount: Map<Group.Leaf, Account>,
  // Account id to account map.
  val accounts: Map<NodeId, Account>,
  // Account id -> month -> balance map.
  val balances: Map<NodeId, Map<Month, Balance>>,
  // Account id -> month -> transfers map.
  val transfers: Map<NodeId, Map<Month, List<Transfer>>>
)