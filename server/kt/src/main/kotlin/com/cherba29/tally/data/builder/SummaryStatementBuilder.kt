package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement

class SummaryStatementBuilder {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  private val summaryStatements = mutableMapOf<TreeNode, MutableMap<Month, Builder>>()

  // Adds statement to its immediate parent summary statement.
  fun addStatement(statement: Statement) {
    val parent = statement.treeNode.parent!!
    summaryStatements.getOrPut(parent) {
      mutableMapOf()
    }.getOrPut(statement.monthRange.first) {
      val builder = Builder()
      builder.treeNode = parent
      builder.monthRange = statement.monthRange
      builder
    }.addStatement(statement)
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

        for (monthlyStatement in monthlyStatements.values) {
          addStatement(monthlyStatement.build())
        }
      }
    }
    return summaryStatements.mapValues { (_,monthToSummaryBuilder) ->
      monthToSummaryBuilder.mapValues { (_, builder) -> builder.build() }
    }
  }

  class Builder {
    var treeNode: TreeNode? = null
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
      require(treeNode != null) { "build failed: treeNode is not set"}
      require(monthRange != null)
      return SummaryStatement(
        treeNode!!,
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
