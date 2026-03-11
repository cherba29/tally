package com.cherba29.tally.data

import com.cherba29.tally.Map3
import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.buildSummaryStatementTable
import com.cherba29.tally.statement.buildTransactionStatementTable
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.AutoCloseable
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// TODO: probably this is not needed, remove?
fun listFiles(tallyPath: Path): List<String> {
  val filePaths: MutableList<String> = mutableListOf()
  for (filePath in tallyPath.walk()) {
    val relativeFilePath = filePath.relativeTo(tallyPath)
    if (relativeFilePath.extension == "yaml") {
      filePaths.add(relativeFilePath.toString())
    }
  }
  return filePaths.sorted()
}

// TODO: separate this logic into separate module.
class ProcessedBudget {
  private val parsedAccountData = mutableMapOf<String, YamlData>()
  var budget: Budget? = null
  val accountToMonthToTransactionStatement: MutableMap<String, MutableMap<String, TransactionStatement>> =
    mutableMapOf()
  var summaryNameMonthMap = Map3<SummaryStatement>()

  fun reProcess() {
    // TODO: use measure time.
    val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
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
    logger.info {
      "Done building budget for ${budget?.accounts?.size} accounts  in ${
        Clock.System.now().toEpochMilliseconds() - startTimeMs
      }ms"
    }

    accountToMonthToTransactionStatement.clear()
    summaryNameMonthMap.clear()

    // TODO: use measureTime.
    val startBuildTransactionStatementsTimeMs: Long = Clock.System.now().toEpochMilliseconds()
    val transactionStatementTable = buildTransactionStatementTable(budget!!, owner = null)
    for (stmt in transactionStatementTable) {
      var monthToStatement = accountToMonthToTransactionStatement[stmt.account.name]
      if (monthToStatement == null) {
        monthToStatement = mutableMapOf()
        accountToMonthToTransactionStatement[stmt.account.name] = monthToStatement
      }
      monthToStatement[stmt.month.toString()] = stmt
    }
    logger.info {
      "Done building ${transactionStatementTable.size} transaction statements in ${
        Clock.System.now().toEpochMilliseconds() - startBuildTransactionStatementsTimeMs
      }ms"
    }

    val startBuildSummaryStatementsTimeMs: Long = Clock.System.now().toEpochMilliseconds()
    summaryNameMonthMap = buildSummaryStatementTable(transactionStatementTable, selectedOwner = null)
    val numSummaryStatements = summaryNameMonthMap.size
    logger.info {
      "Done building $numSummaryStatements summary statements in ${
        Clock.System.now().toEpochMilliseconds() - startBuildSummaryStatementsTimeMs
      }ms"
    }

    logger.info {
      "Done reprocessing ${parsedAccountData.size} file(s) ${transactionStatementTable.size} tran statements and $numSummaryStatements summaries in ${
        Clock.System.now().toEpochMilliseconds() - startTimeMs
      }ms"
    }
  }

  fun addFile(rootPath: Path, relativeFilePath: Path) {
    val content = rootPath.resolve(relativeFilePath).readText(Charsets.UTF_8)
    val accountData = parseYamlContent(content, relativeFilePath)
    if (accountData == null) {
      throw IllegalStateException(
        "Failed to parse $relativeFilePath content of size ${content.length} fileStat: $relativeFilePath"
      )
    }
    parsedAccountData[relativeFilePath.toString()] = accountData
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

data class DataPayload(
  val budget: Budget,
  val statements: Map<String, Map<String, TransactionStatement>>,
  // owner -> account name -> month -> summary.
  val summaries: Map3<SummaryStatement>,
)

class Loader(tallyFilesPath: Path): AutoCloseable {
  private val watchedPath: Path = tallyFilesPath.toRealPath()

  private val processedBudget = ProcessedBudget()

  private val dataLock = ReentrantLock()
  private val signalLock = ReentrantLock()
  private val condition = signalLock.newCondition()
  private var processedOn: Long = 0
  val loadedOn: Long get() = processedOn


  private val watcherJob: Job = watchedPath.watchedEventFlow {
    it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
  }.onEach { (relativePath: Path?, reprocess: Boolean) ->
    val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
    if (relativePath != null) {
      processedBudget.addFile(watchedPath, relativePath)
    }
    if (reprocess) {
      dataLock.withLock {
        processedBudget.reProcess()
        if (processedOn == 0L) {
          signalLock.withLock {
            condition.signalAll()
          }
        }
        // TODO: use provided clock instead.
        processedOn = Clock.System.now().toEpochMilliseconds()
      }
      logger.info { "info: Rebuilt budget in ${processedOn - startTimeMs}ms" }
    }
  }.launchIn(CoroutineScope(Dispatchers.IO + SupervisorJob()))

  init {
    logger.info { "Watching $watchedPath" }
  }

  val budget: DataPayload get() {
    if (processedOn == 0L) {
      // If first time, make sure the data has been loaded before proceeding.
      signalLock.withLock {
        while (processedOn == 0L) {
          condition.await()
        }
      }
    }
    dataLock.withLock {
      return DataPayload(
        budget = processedBudget.budget!!,
        statements = processedBudget.accountToMonthToTransactionStatement,
        summaries = processedBudget.summaryNameMonthMap,
      )
    }
  }

  override fun close() {
    watcherJob.cancel()
  }

  companion object {
    val ignorePathRegex = Regex("(^_)|(/_)")
    private val logger = KotlinLogging.logger {}
  }
}
