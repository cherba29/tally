package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.plus

class SummaryStatementBuilder {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  private val summaryStatements = mutableMapOf<List<String>, MutableMap<Month, Builder>>()
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
      val builder = Builder()
      builder.nodeId = parentSummaryNodeId
      builder.monthRange = statement.monthRange
      builder
    }.addStatement(statement)
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun build(): Map<List<String>, Map<Month, SummaryStatement>> {
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
          addStatement(ownerRoot.name, monthlyStatement.build())
        }
      }
    }
    return summaryStatements.mapValues { (_,monthToSummaryBuilder) ->
      monthToSummaryBuilder.mapValues { (_, builder) -> builder.build() }
    }
  }

  class Builder {
    var nodeId: NodeId? = null
    var monthRange: MonthRange? = null
    private var startBalance: Balance? = null
    private var endBalance: Balance? = null
    private var inFlows: Int = 0
    private var outFlows: Int = 0
    private var totalTransfers: Int = 0
    private var totalPayments: Int = 0
    private var income: Int = 0

    private val statements: MutableList<Statement> = mutableListOf()

    fun addStatement(statement: Statement) {
      if (statement.isClosed) return  // Does not contribute to the summary.

      monthRange = monthRange.enlargeTo(statement.monthRange)

      startBalance += statement.startBalance
      endBalance += statement.endBalance
      if (statement.inFlows > 0) {
        inFlows += statement.inFlows
      } else {
        outFlows += statement.inFlows
      }
      if (statement.outFlows > 0) {
        inFlows += statement.outFlows
      } else {
        outFlows += statement.outFlows
      }
      totalTransfers += statement.totalTransfers
      totalPayments += statement.totalPayments
      income += statement.income
      statements.add(statement)
    }
    fun build(): SummaryStatement {
      require(nodeId != null)
      require(monthRange != null)
      return SummaryStatement(
        nodeId!!,
        monthRange!!,
        statements.any { statement -> statement.isClosed },
        startBalance,
        endBalance,
        inFlows,
        outFlows,
        totalTransfers,
        totalPayments,
        income,
        statements
      )
    }
  }

  companion object {
    fun builder(block: Builder.() -> Unit): SummaryStatement {
      val builder = Builder()
      block(builder)
      return builder.build()
    }
  }
}
