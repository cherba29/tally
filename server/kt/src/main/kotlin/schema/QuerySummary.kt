package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.DataPayload
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.combineSummaryStatements
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

fun buildSummaryData(payload: DataPayload, owner: String, accountType:String, startMonth: Month, endMonth: Month): GqlSummaryData {
  // TODO: replace with measureTime.
  val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
  val summaryName = if (accountType.startsWith("/")) accountType else owner + " " + (if (accountType === owner) "SUMMARY" else accountType)
  // TODO: replace with indexing operator.
  val monthSummaries = payload.summaries.get2(owner, summaryName)
  if (monthSummaries == null) {
    throw NotFoundException("Summary $accountType for $owner not found.")
  }
  val summaryStatements = monthSummaries.values.filter { stmt -> stmt.month in startMonth..endMonth }
  if (summaryStatements.isEmpty()) {
    throw NotFoundException(
        "Summary $accountType for $owner for months [$startMonth, $endMonth] not found."
        )
  }
  val summary = if (summaryStatements.size == 1) summaryStatements.first() else combineSummaryStatements(summaryStatements)
  val result = GqlSummaryData(
    statements = summary.statements.sortedWith { a, b -> if (a.account.name < b.account.name) -1 else 1 }.map { stmt -> (stmt as TransactionStatement).toGql()},
    total = summary.toGql()
  )
  logger.info {
    "gql '${summaryName}' summary data in ${Clock.System.now().toEpochMilliseconds() - startTimeMs}ms for [$startMonth, $endMonth]"
  }
  return result
}

private val logger = KotlinLogging.logger {}