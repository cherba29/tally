package com.cherba29.tally.data

import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.Map3
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.AutoCloseable
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class Loader(tallyFilesPath: Path, timeSource: TimeSource = TimeSource.Monotonic): AutoCloseable {
  private val watchedPath: Path = tallyFilesPath.toRealPath()

  private val processedBudget = ProcessedBudget(timeSource)

  private val dataLock = ReentrantLock()
  private val signalLock = ReentrantLock()
  private val condition = signalLock.newCondition()
  private var processedOn: Long = 0
  val loadedOn: Long get() = processedOn


  data class DataPayload(
    val budget: Budget,
    val statements: Map<NodeId, Map<Month, TransactionStatement>>,
    // owner -> account name -> month -> summary.
    val summaries: Map3<SummaryStatement>,
  )

  private val watcherJob: Job = watchedPath.watchedEventFlow {
    it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
  }.onEach { (relativePath: Path?, reprocess: Boolean) ->
    val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
    if (relativePath != null) {
      try {
        processedBudget.addFile(watchedPath, relativePath)
      } catch (e: Exception) {
        logger.error { "Failed to reload $relativePath. $e" }
        return@onEach
      }
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
