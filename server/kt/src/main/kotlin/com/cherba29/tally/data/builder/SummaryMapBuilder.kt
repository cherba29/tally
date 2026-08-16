package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement

class SummaryMapBuilder {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  private val summaryStatements = mutableMapOf<TreeNode, MutableMap<Month, MonthSummaryStatementBuilder>>()

  // Adds statement to its immediate parent summary statement.
  fun addStatement(treeNode: TreeNode, month: Month, statement: Statement) {
    summaryStatements.getOrPut(treeNode.parent!!) {
      mutableMapOf()
    }.getOrPut(month) {
      MonthSummaryStatementBuilder()
    }.addStatement(treeNode, statement)
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun build(tree: TreeNode): Map<TreeNode, Map<Month, SummaryStatement>> {
    // For each owner bottom up, build up summaries.
    for (ownerRoot in tree.children) {
      for (node in ownerRoot.traverseBottomUp()) {
        if (node.children.isEmpty()) continue  // These were already processed with addStatement.
        val fullPath = node.path
        // skip this is root node it does not need to be added to anything.
        if (fullPath.size < 2) continue

        val monthlyStatements = summaryStatements[node]
          ?: throw IllegalStateException(
            "$fullPath has no monthly statements. Available ${summaryStatements.keys}"
          )  // Should never happen.

        for ((month, monthlyStatement) in monthlyStatements) {
          addStatement(
            node,
            month,
            try {
              monthlyStatement.build()
            } catch (e: Exception) {
              throw IllegalStateException(
                "Failed to build summary for ${node.path.joinToString("/")} for month $month",
                e
              )
            }
          )
        }
      }
    }
    return summaryStatements.mapValues { (_,monthToSummaryBuilder) ->
      monthToSummaryBuilder.mapValues { (_, builder) -> builder.build() }
    }
  }
}
