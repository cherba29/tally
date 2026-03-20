package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.utils.Map3
import kotlin.collections.iterator

// TODO: add tests.
fun buildSummaryStatementTable(
  statements: List<TransactionStatement>,
  selectedOwner: String?
): Map3<SummaryStatement> {
  val statementsAggregator = SummaryStatementAggregator()
  for (statement in statements) {
    for (owner in statement.account.owners) {
      if (selectedOwner != null && owner != selectedOwner || statement.isEmpty()) {
        continue
      }
      val summariesToAddTo = mutableListOf("$owner ${statement.account.typeIdName}", "$owner SUMMARY")
      if (statement.account.path.isNotEmpty()) {
        summariesToAddTo.add("/" + statement.account.path.joinToString("/"))
      }
      for (summaryName in summariesToAddTo) {
        if (statement.account.isExternal && summaryName.contains("SUMMARY")) {
          continue
        }
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
  var owners: List<String> = listOf()
  var minMonth: Month? = null
  var maxMonth: Month? = null
  // Map of 'account name' -> month -> 'summary statement'.
  val accountStatements = mutableMapOf<String, MutableMap<Month, Statement>>()

  for (summaryStmt in summaryStatements) {
    if (stmtName == null) {
      stmtName = summaryStmt.account.name
      owners = summaryStmt.account.owners
    } else if (stmtName !== summaryStmt.account.name) {
      throw IllegalArgumentException(
        "Cant combine different summary statements $stmtName and ${summaryStmt.account.name}"
      )
    }
    if (minMonth == null || summaryStmt.month < minMonth) {
      minMonth = summaryStmt.month
    }
    if (maxMonth == null || maxMonth < summaryStmt.month) {
      maxMonth = summaryStmt.month
    }
    for (stmt in summaryStmt.statements) {
      var accountMonthlyStatements = accountStatements[stmt.account.name]
      if (accountMonthlyStatements == null) {
        accountMonthlyStatements = mutableMapOf()
        accountStatements[stmt.account.name] = accountMonthlyStatements
      }
      val monthStatement = accountMonthlyStatements[stmt.month]
      if (monthStatement != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.account.name} for ${stmt.month}")
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
  val summaryAccount = Account(name = stmtName, type = Account.Type.SUMMARY, owners = owners)
  val combined = SummaryStatement(summaryAccount, maxMonth, minMonth)
  for ((acctName, acctStatements) in accountStatements) {
    // Combine all statements for a given account over all months in the range.
    val stmt = CombinedStatement.fromStatements(
      Account(name = acctName, type = Account.Type.SUMMARY, owners = listOf()),
      minMonth,
      maxMonth,
      acctStatements
    )
    combined.addStatement(stmt)
  }
  return combined
}

// private val logger = KotlinLogging.logger {}