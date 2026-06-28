package com.cherba29.tally.data

import com.cherba29.tally.utils.LastSetFlowState
import com.cherba29.tally.utils.WatchResult
import com.cherba29.tally.utils.scan
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.AutoCloseable
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.measureTime

class Loader(
  pathUpdates: Flow<WatchResult>,
  watchScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
  private val timeSource: TimeSource = TimeSource.Monotonic,
  private val processedBudget: ProcessedBudget = ProcessedBudget(timeSource)
): AutoCloseable {
  val loadedOn: Duration? get() = processedBudget.loadedOn
  private val state = LastSetFlowState(
    pathUpdates.map { result ->
      process(processedBudget, result)
    },
    watchScope)

  suspend fun budget(): Budget = state.last()
  override fun close() = state.close()

  companion object {
    private val logger = KotlinLogging.logger {}
    private val ignorePathRegex = Regex("(^_)|(/_)")

    fun loadFrom(path: Path, timeSource: TimeSource = TimeSource.Monotonic): Budget {
      val fileResults = path.scan {
        it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
      }
      val processedBudget = ProcessedBudget(timeSource)
      for (fileResult in fileResults) {
        process(processedBudget, fileResult)
      }
      return processedBudget.budget!!
    }

    private fun process(
      processedBudget: ProcessedBudget,
      watchResult: WatchResult
    ): Budget? {
      if (watchResult.relativePath != null) {
        try {
          processedBudget.addFile(watchResult.rootPath, watchResult.relativePath)
        } catch (e: Exception) {
          logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
          return null
        }
      }
      if (watchResult.reprocess) {
        val reprocessTime = processedBudget.timeSource.measureTime {
          try {
            processedBudget.reProcess()
          } catch (e: Exception) {
            logger.error(e) { "Failed to reprocess $watchResult." }
            return null
          }
        }
        logger.info { "Rebuilt budget in ${reprocessTime.inWholeMilliseconds}ms" }
        return processedBudget.dataPayload
      }
      return null
    }
  }
}
