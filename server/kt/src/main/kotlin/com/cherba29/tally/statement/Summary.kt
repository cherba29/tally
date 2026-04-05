package com.cherba29.tally.statement

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.utils.Map3
import kotlin.collections.iterator

fun buildSummaryStatementTable(
  statements: List<TransactionStatement>,
  selectedOwner: String?
): Map3<SummaryStatement> {
  val statementsAggregator = SummaryStatementAggregator()
  for (statement in statements) {
    for (owner in statement.nodeId.owners) {
      if (selectedOwner != null && owner != selectedOwner || statement.isEmpty()) {
        continue
      }
      if (statement.nodeId.path.isNotEmpty()) {
        val summaryName = "/" + statement.nodeId.path.joinToString("/")
        statementsAggregator.addStatement(summaryName, owner, statement)
      }
    }
  }
  statementsAggregator.propagateUpThePath2()
  return statementsAggregator.summaryStatements
}

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
  val combined = SummaryStatement(NodeId(stmtName!!, owners), monthRange!!)
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
