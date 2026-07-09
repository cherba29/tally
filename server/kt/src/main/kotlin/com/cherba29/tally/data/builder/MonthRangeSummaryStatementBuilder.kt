package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.iterator

/**
 * Creates parent summary statement containing all provided summary statements
 */
class MonthRangeSummaryStatementBuilder {
  // Map of 'treeNode' -> month -> 'summary statement'.
  val nodeMonthStatementMap = mutableMapOf<TreeNode, MutableMap<Month, Statement>>()

  fun addStatement(stmt: Statement) {
    val prevEntry = nodeMonthStatementMap.getOrPut(stmt.treeNode) {
      mutableMapOf()
    }.putIfAbsent(stmt.monthRange.first, stmt)
    if (prevEntry != null) {
      throw IllegalArgumentException("Duplicate month statement for ${stmt.treeNode.name} for ${stmt.monthRange}")
    }
  }

  fun build(summaryTreeNode: TreeNode): SummaryStatement {
    val accumulatedMonthRange: MonthRange? = nodeMonthStatementMap.values.map { it.keys }.flatten().fold(null as MonthRange?) {
      acc, elem -> acc + elem
    }
    return MonthSummaryStatementBuilder.builder {
      treeNode = summaryTreeNode
      for ((stmtTreeNode, monthStatementMap) in nodeMonthStatementMap) {
        // Combine all statements for a given account over all months in the range.
        val stmt = makeSummaryStatementFromSubstatements(stmtTreeNode, accumulatedMonthRange!!,monthStatementMap)
        addStatement(stmt)
      }
    }
  }

  companion object {
    internal fun makeSummaryStatementFromSubstatements(
      treeNode: TreeNode,
      monthRange: MonthRange,
      statements: Map<Month, Statement>
    ): Statement {
      val firstStmt = statements[monthRange.first.previous()]
      val lastStmt = statements[monthRange.last.next()]

      val startBalance: Balance? = firstStmt?.endBalance
        ?: (statements[monthRange.first]?.startBalance
          ?: Balance(0, monthRange.first.toDate(), Balance.Type.PROJECTED))
      val endBalance: Balance? = lastStmt?.startBalance
        ?: (statements[monthRange.last]?.endBalance
          ?: Balance(0, monthRange.last.next().toDate(), Balance.Type.PROJECTED))

      var inFlows = 0L
      var outFlows = 0L
      var totalTransfers = 0L
      var totalPayments = 0L
      var income = 0L

      for (currentMonth in monthRange) {
        val stmt = statements[currentMonth]
          ?: Statement(treeNode, currentMonth..currentMonth)
        inFlows += stmt.inFlows
        outFlows += stmt.outFlows
        totalTransfers += stmt.totalTransfers
        totalPayments += stmt.totalPayments
        income += stmt.income
      }
      return Statement(
        treeNode,
        monthRange,
        isClosed = false,
        startBalance,
        endBalance,
        inFlows,
        outFlows,
        totalTransfers,
        totalPayments,
        income
      )
    }
  }
}
