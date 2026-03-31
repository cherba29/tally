package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.utils.Map3
import kotlin.collections.iterator

// TODO: add tests.
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

// TODO: add tests.
fun combineSummaryStatements(summaryStatements: List<SummaryStatement>): SummaryStatement {
  if (summaryStatements.isEmpty()) {
    throw IllegalArgumentException("Cant combine empty list of summary statements")
  }
  var stmtName: String? = null
  var owners: Set<String> = setOf()
  var minMonth: Month? = null
  var maxMonth: Month? = null
  // Map of 'account name' -> month -> 'summary statement'.
  val accountStatements = mutableMapOf<NodeId, MutableMap<Month, Statement>>()

  for (summaryStmt in summaryStatements) {
    if (stmtName == null) {
      stmtName = summaryStmt.nodeId.name
      owners = summaryStmt.nodeId.owners
    } else if (stmtName !== summaryStmt.nodeId.name) {
      throw IllegalArgumentException(
        "Cant combine different summary statements $stmtName and ${summaryStmt.nodeId.name}"
      )
    }
    if (minMonth == null || summaryStmt.month < minMonth) {
      minMonth = summaryStmt.month
    }
    if (maxMonth == null || maxMonth < summaryStmt.month) {
      maxMonth = summaryStmt.month
    }
    for (stmt in summaryStmt.statements) {
      var accountMonthlyStatements = accountStatements[stmt.nodeId]
      if (accountMonthlyStatements == null) {
        accountMonthlyStatements = mutableMapOf()
        accountStatements[stmt.nodeId] = accountMonthlyStatements
      }
      val monthStatement = accountMonthlyStatements[stmt.month]
      if (monthStatement != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.nodeId.name} for ${stmt.month}")
      }
      accountMonthlyStatements[stmt.month] = stmt
    }
  }
  if (stmtName == null) {
    throw IllegalArgumentException("Statements do not have name set.")
  }
  if (minMonth == null) {
    throw IllegalArgumentException("Could not determine start month")
  }
  if (maxMonth == null) {
    throw IllegalArgumentException("Could not determine end month")
  }
  val summaryAccount = Account(NodeId(stmtName, owners), openedOn = minMonth)
  val combined = SummaryStatement(summaryAccount.nodeId, maxMonth, minMonth)
  for ((acctName, acctStatements) in accountStatements) {
    // Combine all statements for a given account over all months in the range.
    val stmt = CombinedStatement.fromStatements(
      acctName,
      minMonth,
      maxMonth,
      acctStatements
    )
    combined.addStatement(stmt)
  }
  return combined
}
