package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.utils.Map3

// TODO: add tests for this class.
class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  val summaryStatements = Map3<String, String, Month, SummaryStatement>()
  // Map of owner+name -> summary node
  private val summaryNodes: MutableMap<String, NodeId> = mutableMapOf()

  // Adds statement to its immediate parent summary statement.
  // Once all statements are added one has to call propagateUpThePath2
  // to create summary statements of these summaries up the tree.
  fun addStatement(summaryName: String, owner: String, statement: Statement) {
    val parentSummaryNodeId = summaryNodes.getOrPut("$owner - $summaryName") {
      NodeId(summaryName, setOf(owner), statement.nodeId.parentPath)
    }
    summaryStatements.getDefault(
      owner, parentSummaryNodeId.name, statement.monthRange.first
    ) { SummaryStatement(parentSummaryNodeId, statement.monthRange) }
      .addStatement(statement)
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun propagateUpThePath2() {
    // Build a multi-root tree based on account paths for each owner.
    val tree: MutableMap<String, MutableSet<String>> = mutableMapOf()  // node -> set of children.
    val owners: MutableSet<String> = mutableSetOf()
    for (nodeId in summaryNodes.values) {
      for (owner in nodeId.owners) {
        if (!nodeId.isSummary) throw IllegalStateException("Unexpected non-summary node $nodeId")
        owners.add(owner)
        val path = nodeId.path
        var entry = "/" + owner + nodeId.name
        for (sub in path.size downTo 0) {
          val subPath = path.slice(0..sub - 1)
          val subPathId = "/" + owner + "/" + subPath.joinToString("/")
          if (subPathId != entry) {  // Make sure root does not reference itself.
            tree.getOrPut(subPathId) { mutableSetOf()}.add(entry)
          }
          entry = subPathId
        }
      }
    }
    // For each owner bottom up, build up summaries.
    for (owner in owners) {
      val ownerRoot = "/$owner/"
      for (node in traverseBottomUp(ownerRoot, tree)) {
        val fullPath = node.split('/')
        val summaryId = "/" + fullPath.subList(2, fullPath.size).joinToString("/")
        // skip this is root node it does not need to be added to anything.
        if (summaryId == "/") continue
        val monthlyStatements = summaryStatements[owner, summaryId]
          ?: throw IllegalStateException(
            "$node has no monthly statements, [$owner, $summaryId] key not found."
          )  // Should never happen.

        val parentSummaryId = '/' + fullPath.subList(2, fullPath.lastIndex).joinToString("/")
        for (monthlyStatement in monthlyStatements.values) {
          addStatement(parentSummaryId, owner, monthlyStatement)
        }
      }
    }
  }

  companion object {
    private fun traverseBottomUp(root: String, tree: Map<String, Set<String>>): Sequence<String> = sequence {
      for (child in tree[root] ?: setOf()) {
        yieldAll(traverseBottomUp(child, tree))
      }
      yield(root)
    }
  }
}
