package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.statement.Statement
import kotlin.collections.get

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
  fun getAccount(treeNode: TreeNode) = if (treeNode.children.isEmpty()) {
      leafToAccount[treeNode]
    } else {
      // Summaries don't have associated account, create a dummy.
      // TODO: instead of dummy return null.
      val path = if (treeNode.path.size < 2) listOf() else treeNode.path.subList(1, treeNode.path.size)
      Account(
        path.lastOrNull() ?: "",
        if (path.size > 1) path.subList(0, path.size-1) else listOf(""),
        setOf(treeNode.path.first()),
        isSummary = treeNode.children.isNotEmpty(),
        openedOn = Month(2010, 0)
      )
    }
}