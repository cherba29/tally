package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement

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
  /** Lookup map from account to its statement for given month */
  val statements: Map<NodeId, Map<Month, TransactionStatement>>,
  // owner -> account name -> month -> summary.
  val summaries: Map<List<String>, Map<Month, SummaryStatement>>,
)