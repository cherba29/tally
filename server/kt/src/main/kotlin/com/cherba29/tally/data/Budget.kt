package com.cherba29.tally.data

import com.cherba29.tally.utils.TreeNode
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
  val tree: TreeNode<Profile>,
  // Tree node to corresponding statement.
  // Parent nodes map to SummaryStatement and leaf nodes to TransactionStatement.
  val nodeToStatement: Map<TreeNode<Profile>, Map<Month, Statement>>,
) {
  private val isClosedCache = mutableMapOf<TreeNode<Profile>, MutableMap<Month, Boolean>>()

  fun getAccountNode(accountName: String) =  tree.traverseLeaves().find { it.data.account?.name == accountName }

  /**
   * Checks if node is closed.
   * It is closed if its corresponding account is closed, or all its children are closed.
   */
  fun isClosed(treeNode: TreeNode<Profile>, month: Month): Boolean
    = isClosedCache.getOrPut(treeNode) { mutableMapOf() }.getOrPut(month) {
    when (treeNode) {
      is TreeNode.Leaf -> treeNode.data.account?.isClosed(month) ?: throw IllegalStateException("No matching account for $treeNode")
      is TreeNode.Root,
      is TreeNode.Branch -> {
        treeNode.children.all { isClosed(it, month) }
      }
    }
  }
}
