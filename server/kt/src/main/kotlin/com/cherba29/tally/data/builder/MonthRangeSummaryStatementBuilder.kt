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
  var accumulatedMonthRange: MonthRange? = null
  // Map of 'treeNode' -> month -> 'summary statement'.
  val nodeMonthStatementMap = mutableMapOf<TreeNode, MutableMap<Month, Statement>>()

  fun addStatement(month: Month, stmt: SummaryStatement) {
    accumulatedMonthRange += month
    for (stmt in stmt.statements) {
      val accountMonthlyStatements = nodeMonthStatementMap.getOrPut(stmt.treeNode) {
        mutableMapOf()
      }
      val prevEntry = accountMonthlyStatements.putIfAbsent(stmt.monthRange.first, stmt)
      if (prevEntry != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.treeNode.name} for ${stmt.monthRange}")
      }
    }
  }

  fun build(summaryTreeNode: TreeNode): SummaryStatement {
    require(accumulatedMonthRange != null ) { "Cant combine empty list of summary statements" }
    return MonthSummaryStatementBuilder.builder {
      treeNode = summaryTreeNode
      monthRange = accumulatedMonthRange
      for ((stmtTreeNode, monthStatementMap) in nodeMonthStatementMap) {
        // Combine all statements for a given account over all months in the range.
        val stmt = makeSummaryStatementFromSubstatements(stmtTreeNode, monthRange!!,monthStatementMap)
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
      val combined = Statement(treeNode, monthRange)
      for (currentMonth in monthRange) {
        val stmt = statements[currentMonth]
          ?: Statement(treeNode, currentMonth..currentMonth)
        setStatementBalance(
          currentMonth,
          stmt,
          statements[currentMonth.previous()],
          statements[currentMonth.next()]
        )
        combined.startBalance = Balance.pickMinDate(combined.startBalance, stmt.startBalance)
        combined.endBalance = Balance.pickMaxDate(combined.endBalance, stmt.endBalance)
        combined.inFlows += stmt.inFlows
        combined.outFlows += stmt.outFlows
        combined.totalTransfers += stmt.totalTransfers
        combined.totalPayments += stmt.totalPayments
        combined.income += stmt.income
      }
      return combined
    }

    private fun setStatementBalance(
      month: Month,
      currStmt: Statement,
      prevStmt: Statement?,
      nextStmt: Statement?
    ): Statement {
      if (currStmt.startBalance == null) {
        currStmt.startBalance = prevStmt?.endBalance
          ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
      }
      if (currStmt.endBalance == null) {
        currStmt.endBalance = nextStmt?.startBalance
          ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
      }
      return currStmt
    }
  }
}
