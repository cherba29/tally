package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.statement.Statement

/**
 * Data representing all accounts, their transactions and totals per month.
 */
data class Budget(
  /** Period over which the budget is defined. */
  val months: MonthRange,
  /** Hierarchical structure of accounts and summaries. */
  val tree: TreeNode,
  /** Maps leaf tree node to corresponding account. */
  val leafToAccount: Map<TreeNode.Leaf, Account>,
  // Tree node to corresponding statement.
  // Parent nodes map to SummaryStatement and leaf nodes to TransactionStatement.
  val nodeToStatement: Map<TreeNode, Map<Month, Statement>>,
) {
  fun getAccountNode(accountName: String) = leafToAccount.entries.find { it.value.name == accountName }?.key
}
