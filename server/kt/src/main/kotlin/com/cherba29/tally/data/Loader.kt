package com.cherba29.tally.data

import com.cherba29.tally.utils.LastSetFlowState
import com.cherba29.tally.utils.WatchResult
import com.cherba29.tally.utils.scan
import com.cherba29.tally.utils.watchedEventFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.AutoCloseable
import java.nio.file.Path
import kotlin.io.path.extension
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
    private val filePathFilter: (Path)-> Boolean = { it.extension == "yaml" }

    fun loadFrom(path: Path, timeSource: TimeSource = TimeSource.Monotonic): Budget {
      val fileResults = path.scan(filePathFilter)
      val processedBudget = ProcessedBudget(timeSource)
      for (fileResult in fileResults) {
        process(processedBudget, fileResult)
      }
      return processedBudget.budget!!
    }

    fun watchPath(path: Path) = Loader(path.watchedEventFlow(filePathFilter))

    private fun process(
      processedBudget: ProcessedBudget,
      watchResult: WatchResult
    ): Budget? {
      when (watchResult.action) {
        WatchResult.Action.REPROCESS -> {
          if (watchResult.relativePath != null) {
            try {
              processedBudget.addFile(watchResult.rootPath,watchResult.relativePath)
            } catch (e: Exception) {
              logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
              return null
            }
          }
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

        WatchResult.Action.ADD -> {
          try {
            processedBudget.addFile(
              watchResult.rootPath,
              watchResult.relativePath
                ?: throw IllegalStateException("Adding null file to ${watchResult.rootPath}")
            )
          } catch (e: Exception) {
            logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
            return null
          }
        }
        WatchResult.Action.REMOVE -> {
          val reprocessTime = processedBudget.timeSource.measureTime {
            try {
              processedBudget.removeFile(
                watchResult.relativePath
                  ?: throw IllegalStateException("Adding null file to ${watchResult.rootPath}")
              )
              processedBudget.reProcess()
            } catch (e: Exception) {
              logger.error { "Failed to remove file ${watchResult.relativePath}. $e" }
              return null
            }
          }
          logger.info { "Rebuilt budget in ${reprocessTime.inWholeMilliseconds}ms" }
          return processedBudget.dataPayload
        }
        WatchResult.Action.REMOVE_ALL -> {
          try {
            processedBudget.removeAll()
          } catch (e: Exception) {
            logger.error { "Failed to remove all for ${watchResult.rootPath}. $e" }
            return null
          }

        }
      }
      return null
    }
  }
}
