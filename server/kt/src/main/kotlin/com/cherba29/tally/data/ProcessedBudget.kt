package com.cherba29.tally.data

import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.core.Month
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.buildSummaryStatementTable
import com.cherba29.tally.statement.buildTransactionStatementTable
import com.cherba29.tally.utils.Map3
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.contains
import kotlin.io.path.readText
import kotlin.time.TimeSource
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class ProcessedBudget(val timeSource: TimeSource = TimeSource.Monotonic) {
  private val parsedAccountData = mutableMapOf<String, YamlData>()
  var budget: Budget? = null
  val accountToMonthToTransactionStatement: MutableMap<String, MutableMap<Month, TransactionStatement>> =
    mutableMapOf()
  var summaryNameMonthMap = Map3<SummaryStatement>()

  fun reProcess() {
    val elapsedBudgetTime = timeSource.measureTime {
      val budgetBuilder = BudgetBuilder()
      val unwantedFiles = mutableListOf<String>()
      for ((filePath, accountData) in parsedAccountData) {
        try {
          loadYamlFile(budgetBuilder, accountData, Paths.get(filePath))
          if (!budgetBuilder.accounts.contains(accountData.name ?: "")) {
            logger.warn { "warning: $filePath is not an account file." }
            unwantedFiles.add(filePath)
          }
        } catch (e: Exception) {
          logger.error { "error: Failed to add $filePath, $e" }
          return
        }
      }
      // Since reprocess is called multiple times, no need reprocess them.
      for (filePath in unwantedFiles) {
        parsedAccountData.remove(filePath)
      }
      budget = budgetBuilder.build()
    }
    logger.info {
      "Done building budget for ${budget?.accounts?.size} accounts  in $elapsedBudgetTime"
    }

    accountToMonthToTransactionStatement.clear()
    summaryNameMonthMap.clear()

    val (transactionStatementTable, elapsedTransactionTime) = timeSource.measureTimedValue {
      val transactionStatementTable = buildTransactionStatementTable(budget!!, owner = null)
      for (stmt in transactionStatementTable) {
        var monthToStatement = accountToMonthToTransactionStatement[stmt.account.name]
        if (monthToStatement == null) {
          monthToStatement = mutableMapOf()
          accountToMonthToTransactionStatement[stmt.account.name] = monthToStatement
        }
        monthToStatement[stmt.month] = stmt
      }
      transactionStatementTable
    }
    logger.info {
      "Done building ${transactionStatementTable.size} transaction statements in ${elapsedTransactionTime}ms"
    }

    val elapsedBuildSummaryStatements = timeSource.measureTime {
      summaryNameMonthMap = buildSummaryStatementTable(transactionStatementTable, selectedOwner = null)
    }
    val numSummaryStatements = summaryNameMonthMap.size
    logger.info {
      "Done building $numSummaryStatements summary statements in $elapsedBuildSummaryStatements"
    }

    logger.info {
      "Done reprocessing ${parsedAccountData.size} file(s) ${transactionStatementTable.size} tran statements and $numSummaryStatements summaries in ${
        elapsedBudgetTime + elapsedTransactionTime + elapsedBuildSummaryStatements
      }"
    }
  }

  fun addFile(rootPath: Path, relativeFilePath: Path) {
    val content = rootPath.resolve(relativeFilePath).readText(Charsets.UTF_8)
    val accountData = parseYamlContent(content, relativeFilePath)
    parsedAccountData[relativeFilePath.toString()] = accountData
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
