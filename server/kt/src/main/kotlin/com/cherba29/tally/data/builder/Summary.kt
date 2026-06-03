package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.iterator

/** Creates parent summary statement containing all provided summary statements */
fun combineSummaryStatements(summaryStatements: List<SummaryStatement>): SummaryStatement {
  require(summaryStatements.isNotEmpty()) { "Cant combine empty list of summary statements" }
  val stmtName: String = summaryStatements.first().nodeId.name
  val owners: Set<String> = summaryStatements.first().nodeId.owners
  var monthRange: MonthRange = summaryStatements.first().monthRange
  // Map of 'nodeId' -> month -> 'summary statement'.
  val nodeMonthStatementMap = mutableMapOf<NodeId, MutableMap<Month, Statement>>()

  for (summaryStmt in summaryStatements) {
    if (stmtName != summaryStmt.nodeId.name) {
      throw IllegalArgumentException(
        "Cant combine different summary statements $stmtName and ${summaryStmt.nodeId.name}"
      )
    }
    monthRange = monthRange.enlargeTo(summaryStmt.monthRange)!!
    for (stmt in summaryStmt.statements) {
      val accountMonthlyStatements = nodeMonthStatementMap.getOrPut(stmt.nodeId) {
        mutableMapOf()
      }
      val prevEntry = accountMonthlyStatements.putIfAbsent(stmt.monthRange.first, stmt)
      if (prevEntry != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.nodeId.name} for ${stmt.monthRange}")
      }
    }
  }
  val combined = SummaryStatement(NodeId(stmtName, isSummary=true, owners), monthRange)
  for ((nodeId, monthStatementMap) in nodeMonthStatementMap) {
    // Combine all statements for a given account over all months in the range.
    val stmt = fromStatements(
      nodeId,
      monthRange,
      monthStatementMap
    )
    combined.addStatement(stmt)
  }
  return combined
}

fun fromStatements(
  nodeId: NodeId,
  monthRange: MonthRange,
  statements: Map<Month, Statement>
): Statement {
  val combined = Statement(nodeId, monthRange)
  for (currentMonth in monthRange) {
    val stmt: Statement = makeProxyStatement(
      nodeId,
      currentMonth,
      statements[currentMonth],
      statements[currentMonth.previous()],
      statements[currentMonth.next()]
    )
    combined.startBalance = Balance.pickMinDate(combined.startBalance, stmt.startBalance)
    combined.endBalance = Balance.pickMaxDate(combined.endBalance, stmt.endBalance)
    combined.addInFlow(stmt.inFlows)
    combined.addOutFlow(stmt.outFlows)
    combined.totalTransfers += stmt.totalTransfers
    combined.totalPayments += stmt.totalPayments
    combined.income += stmt.income
  }
  return combined
}

private fun makeProxyStatement(
  nodeId: NodeId,
  month: Month,
  currStmt: Statement?,
  prevStmt: Statement?,
  nextStmt: Statement?
): Statement {
  val stmt = currStmt ?: Statement(nodeId, month..month)
  if (stmt.startBalance == null) {
    stmt.startBalance = prevStmt?.endBalance ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
  }
  if (stmt.endBalance == null) {
    stmt.endBalance = nextStmt?.startBalance ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
  }
  return stmt
}
