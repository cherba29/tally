package com.cherba29.tally.data

import com.cherba29.tally.Map3
import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.buildSummaryStatementTable
import com.cherba29.tally.statement.buildTransactionStatementTable
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

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
    for ((filePath, accountData) in parsedAccountData) {
      try {
        loadYamlFile(budgetBuilder, accountData, Paths.get(filePath))
        if (!budgetBuilder.accounts.contains(accountData.name ?: "")) {
          logger.warn { "warning: $filePath is not an account file." }
        }
      } catch (e: Exception) {
        logger.error { "error: Failed to add $filePath, $e" }
        return
      }
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

  fun addFolder(rootPath: Path) {
    val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
    for (filePath in rootPath.walk()) {
      val relativeFilePath = filePath.relativeTo(rootPath)
      if (relativeFilePath.extension == "yaml") {
        addFile(rootPath, relativeFilePath)
      }
    }
    logger.info {
      "Done loading ${parsedAccountData.size} file(s) in ${
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

fun WatchService.eventFlow(): Flow<List<WatchEvent<out Any>>> = flow {
  while (currentCoroutineContext().isActive) {
    coroutineScope {
      var key: WatchKey? = null
      val job = launch {
        runInterruptible(Dispatchers.IO) {
          key = take()
        }
      }
      job.join()
      val currentKey = key
      if (currentKey != null) {
        emit(currentKey.pollEvents())
        currentKey.reset()
      }
    }
  }
}

class Loader(tallyFilesPath: Path): AutoCloseable {
  private val watchedPath: Path = tallyFilesPath.toRealPath()

  private var watcher: WatchService = FileSystems.getDefault().newWatchService()
  private var processedBudget: ProcessedBudget? = null
  private var processedOn: Long = 0
  val loadedOn: Long get() = processedOn

  private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var watcherJob: Job? = null

  init {
    watchedPath.register(
      watcher,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
      StandardWatchEventKinds.ENTRY_MODIFY
    )
  }

  fun loadBudget(): DataPayload {
    if (processedBudget != null) {
      return DataPayload(
        budget = processedBudget?.budget!!,
        statements = processedBudget?.accountToMonthToTransactionStatement!!,
        summaries = processedBudget?.summaryNameMonthMap!!,
      )
    }

    processedBudget = ProcessedBudget()

    processedBudget!!.addFolder(watchedPath)
    processedBudget!!.reProcess()
    processedOn = Clock.System.now().toEpochMilliseconds()

    watcherJob = watcher.eventFlow()
      .onCompletion { watcher.close() }
      .onEach { events: List<WatchEvent<out Any>> ->
        for (event in events) {
          // TODO: skip uninteresting events.
          val kind = event.kind()
          val context = event.context() as Path // Relative path to the file/directory changed
          // Handle the event
          logger.info { "Event type: $kind. File affected: $context" }
          val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
          if (processedBudget != null) {
            processedBudget!!.addFile(watchedPath, context)
            processedBudget!!.reProcess()
          } else {
            logger.error { "Unexpected error: processedBudget is unset" }
          }
          processedOn = Clock.System.now().toEpochMilliseconds()
          logger.info { "info: Rebuilt budget in ${processedOn - startTimeMs}ms" }
        }
      }.launchIn(ioScope)

    logger.info { "info: Watching $watchedPath" }

    return DataPayload(
      budget = processedBudget?.budget!!,
      statements = processedBudget?.accountToMonthToTransactionStatement!!,
      summaries = processedBudget?.summaryNameMonthMap!!,
    )
  }

  override fun close() {
    watcher.close()
    processedBudget = null
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}