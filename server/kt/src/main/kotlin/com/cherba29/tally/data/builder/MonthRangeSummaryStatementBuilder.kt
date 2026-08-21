package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.component1

/**
 * Creates parent summary statement containing all provided summary statements
 */
class MonthRangeSummaryStatementBuilder {
  // Map of 'treeNode' -> month -> 'summary statement'.
  val nodeMonthStatementMap = mutableMapOf<TreeNode, MutableMap<Month, Statement>>()

  fun addStatement(treeNode: TreeNode, month: Month, stmt: Statement) {
    val prevEntry = nodeMonthStatementMap.getOrPut(treeNode) {
      mutableMapOf()
    }.putIfAbsent(month, stmt)
    if (prevEntry != null) {
      throw IllegalArgumentException("Duplicate month statement for ${treeNode.name} for ${stmt.monthRange}")
    }
  }

  fun build(): SummaryStatement {
    val accumulatedMonthRange: MonthRange? = nodeMonthStatementMap.values
      .map { it.keys }
      .flatten()
      .fold(null as MonthRange?) { acc, elem -> acc + elem }
    return MonthSummaryStatementBuilder.builder {
      for ((stmtTreeNode, monthStatementMap) in nodeMonthStatementMap) {
        // Combine all statements for a given account over all months in the range.
        val stmt = makeSummaryStatementFromSubstatements(
          monthRange = accumulatedMonthRange!!,
          statements = monthStatementMap
        )
        addStatement(stmtTreeNode, stmt)
      }
    }
  }

  companion object {
    internal fun makeSummaryStatementFromSubstatements(
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
        val stmt = statements[currentMonth] ?: continue
        inFlows += stmt.inFlows
        outFlows += stmt.outFlows
        totalTransfers += stmt.totalTransfers
        totalPayments += stmt.totalPayments
        income += stmt.income
      }
      return SummaryStatement(
        monthRange,
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
