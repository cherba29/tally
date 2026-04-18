package com.cherba29.tally.data

import com.cherba29.tally.utils.LastSetFlowState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.AutoCloseable
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.measureTime

class Loader(
  pathUpdates: Flow<WatchResult>,
  watchScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
  private val timeSource: TimeSource = TimeSource.Monotonic,
  private val processedBudget: ProcessedBudget = ProcessedBudget(timeSource)
): AutoCloseable {
  private val startTime = timeSource.markNow()
  private var processedOn: Duration? = null  // Not yet processed
  val loadedOn: Duration? get() = processedOn

  private val state = LastSetFlowState(pathUpdates.map(this::process), watchScope)

  suspend fun budget(): DataPayload = state.last()
  override fun close() = state.close()

  private fun process(watchResult: WatchResult): DataPayload? {
    if (watchResult.relativePath != null) {
      try {
        processedBudget.addFile(watchResult.rootPath, watchResult.relativePath)
      } catch (e: Exception) {
        logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
        return null
      }
    }
    if (watchResult.reprocess) {
      val reprocessTime = timeSource.measureTime {
        processedBudget.reProcess()
        processedOn = startTime.elapsedNow()
      }
      logger.info { "Rebuilt budget in ${reprocessTime.inWholeMilliseconds}ms" }
      return processedBudget.dataPayload
    }
    return null
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
