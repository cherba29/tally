package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import kotlin.collections.iterator

fun combineSummaryStatements(summaryStatements: List<SummaryStatement>): SummaryStatement {
  if (summaryStatements.isEmpty()) {
    throw IllegalArgumentException("Cant combine empty list of summary statements")
  }
  var stmtName: String? = null
  var owners: Set<String> = setOf()
  var monthRange: MonthRange? = null
  // Map of 'nodeId' -> month -> 'summary statement'.
  val nodeMonthStatementMap = mutableMapOf<NodeId, MutableMap<Month, Statement>>()

  for (summaryStmt in summaryStatements) {
    if (stmtName == null) {
      stmtName = summaryStmt.nodeId.name
      owners = summaryStmt.nodeId.owners
    } else if (stmtName !== summaryStmt.nodeId.name) {
      throw IllegalArgumentException(
        "Cant combine different summary statements $stmtName and ${summaryStmt.nodeId.name}"
      )
    }
    monthRange = monthRange.enlargeTo(summaryStmt.monthRange)
    for (stmt in summaryStmt.statements) {
      val accountMonthlyStatements = nodeMonthStatementMap.getOrPut(stmt.nodeId) {
        mutableMapOf()
      }
      val monthStatement = accountMonthlyStatements[stmt.monthRange.first]
      if (monthStatement != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.nodeId.name} for ${stmt.monthRange}")
      }
      accountMonthlyStatements[stmt.monthRange.first] = stmt
    }
  }
  val combined = SummaryStatement(NodeId(stmtName!!, isSummary=true, owners), monthRange!!)
  for ((nodeId, monthStatementMap) in nodeMonthStatementMap) {
    // Combine all statements for a given account over all months in the range.
    val stmt = CombinedStatement.fromStatements(
      nodeId,
      monthRange,
      monthStatementMap
    )
    combined.addStatement(stmt)
  }
  return combined
}
