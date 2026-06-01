package com.cherba29.tally.data.builder

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
