package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.utils.Map3
import kotlin.collections.plus

class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  val summaryStatements = Map3<String, String, Month, SummaryStatement>()
  // Map of owner+name -> summary node
  private val summaryNodes: MutableMap<List<String>, NodeId> = mutableMapOf()
  private val groupTreeBuilder = Group.Companion.Builder()

  // Adds statement to its immediate parent summary statement.
  // Once all statements are added one has to call propagateUpThePath2
  // to create summary statements of these summaries up the tree.
  fun addStatement(summaryPath: List<String>, owner: String, statement: Statement) {
    groupTreeBuilder.addPath(listOf(owner) + statement.nodeId.path)
    val parentSummaryNodeId = summaryNodes.getOrPut(listOf(owner) + summaryPath) {
      NodeId(summaryPath.joinToString("/"), setOf(owner), statement.nodeId.parentPath)
    }
    summaryStatements.getDefault(
      owner, parentSummaryNodeId.name, statement.monthRange.first
    ) { SummaryStatement(parentSummaryNodeId, statement.monthRange) }
      .addStatement(statement)
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

        val summaryId = fullPath.subList(1, fullPath.size).joinToString("/")
        val monthlyStatements = summaryStatements[ownerRoot.name, summaryId]
          ?: throw IllegalStateException(
            "$node has no monthly statements, [${ownerRoot.name}, $summaryId] key not found."
          )  // Should never happen.

        val parentSummaryPath = fullPath.subList(1, fullPath.lastIndex)
        for (monthlyStatement in monthlyStatements.values) {
          addStatement(parentSummaryPath, ownerRoot.name, monthlyStatement)
        }
      }
    }
  }
}
