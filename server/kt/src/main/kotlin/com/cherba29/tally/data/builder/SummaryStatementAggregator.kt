package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.plus

class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  val summaryStatements = mutableMapOf<List<String>, MutableMap<Month, SummaryStatement>>()
  // Map of owner+name -> summary node
  private val summaryNodes: MutableMap<List<String>, NodeId> = mutableMapOf()
  private val groupTreeBuilder = Group.Companion.Builder()

  // Adds statement to its immediate parent summary statement.
  // Once all statements are added one has to call propagateUpThePath2
  // to create summary statements of these summaries up the tree.
  fun addStatement(owner: String, statement: Statement) {
    val fullPath = listOf(owner) + statement.nodeId.path
    groupTreeBuilder.addPath(fullPath)
    val parentSummaryNodeId = summaryNodes.getOrPut(fullPath) {
      NodeId(
        statement.nodeId.path.joinToString("/"),
        isSummary=true,
        setOf(owner),
        statement.nodeId.parentPath
      )
    }
    summaryStatements.getOrPut(fullPath) {
      mutableMapOf()
    }.getOrPut(statement.monthRange.first) {
      SummaryStatement(parentSummaryNodeId, statement.monthRange)
    }.addStatement(statement)
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun propagateUpThePath2() {
    // Build a multi-root tree based on account paths for each owner.
    val tree = groupTreeBuilder.build()
    // For each owner bottom up, build up summaries.
    for (ownerRoot in tree.children) {
      for (node in ownerRoot.traverseBottomUp()) {
        val fullPath = node.path
        // skip this is root node it does not need to be added to anything.
        if (fullPath.size < 2) continue

        val monthlyStatements = summaryStatements[fullPath]
          ?: throw IllegalStateException(
            "$node has no monthly statements, [$fullPath] key not found."
          )  // Should never happen.

        for (monthlyStatement in monthlyStatements.values) {
          addStatement(ownerRoot.name, monthlyStatement)
        }
      }
    }
  }
}
