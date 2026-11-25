package com.cherba29.tally.data

import com.cherba29.tally.utils.LastSetFlowState
import com.cherba29.tally.utils.WatchResult
import com.cherba29.tally.utils.scan
import com.cherba29.tally.utils.watchedEventFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.lang.AutoCloseable
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

sealed interface LoadResult {
  data class Success(val budget: Budget) : LoadResult

  data class Failure(val error: Throwable) : LoadResult
}

class Loader(
  pathUpdates: Flow<WatchResult>,
  watchScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
  private val timeSource: TimeSource = TimeSource.Monotonic,
  private val processedBudget: ProcessedBudget = ProcessedBudget(timeSource)
) : AutoCloseable {
  val loadedOn: Duration? get() = processedBudget.loadedOn
  private val _updates =
    MutableSharedFlow<LoadResult>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val updates: Flow<LoadResult> = _updates.asSharedFlow()

  private val state = LastSetFlowState<LoadResult>(
    pathUpdates.map { result ->
      process(processedBudget, result)?.also {
        _updates.tryEmit(it)
      }
    },
    watchScope
  )

  suspend fun budget(): Budget =
    when (val result = state.last()) {
      is LoadResult.Success -> result.budget
      is LoadResult.Failure -> throw result.error
    }

  override fun close() = state.close()

  companion object {
    private val logger = KotlinLogging.logger {}
    private val filePathFilter: (Path) -> Boolean = { it.extension == "yaml" }

    fun loadFrom(
      path: Path,
      timeSource: TimeSource = TimeSource.Monotonic
    ): Budget {
      val fileResults = path.scan(filePathFilter)
      val processedBudget = ProcessedBudget(timeSource)
      var lastResult: LoadResult? = null
      for (fileResult in fileResults) {
        val result = process(processedBudget, fileResult)
        if (result != null) {
          lastResult = result
        }
      }
      return when (lastResult) {
        is LoadResult.Success -> lastResult.budget
        is LoadResult.Failure -> throw lastResult.error
        null -> processedBudget.dataPayload
      }
    }

    fun watchPath(path: Path) = Loader(path.watchedEventFlow(filePathFilter))

    private fun process(
      processedBudget: ProcessedBudget,
      watchResult: WatchResult
    ): LoadResult? {
      when (watchResult.action) {
        WatchResult.Action.REPROCESS -> {
          if (watchResult.relativePath != null) {
            try {
              processedBudget.addFile(watchResult.rootPath, watchResult.relativePath)
            } catch (e: Throwable) {
              logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
              return LoadResult.Failure(e.toBudgetLoadingException(watchResult.relativePath))
            }
          }
          val reprocessTime = processedBudget.timeSource.measureTime {
            try {
              processedBudget.reProcess()
            } catch (e: Throwable) {
              logger.error(e) { "Failed to reprocess $watchResult." }
              return LoadResult.Failure(e.toBudgetLoadingException(watchResult.relativePath))
            }
          }
          logger.info { "Rebuilt budget in ${reprocessTime.inWholeMilliseconds}ms" }
          return LoadResult.Success(processedBudget.dataPayload)
        }

        WatchResult.Action.ADD -> {
          try {
            processedBudget.addFile(
              watchResult.rootPath,
              watchResult.relativePath
                ?: throw IllegalStateException("Adding null file to ${watchResult.rootPath}")
            )
          } catch (e: Throwable) {
            logger.error { "Failed to reload ${watchResult.relativePath}. $e" }
            return LoadResult.Failure(e.toBudgetLoadingException(watchResult.relativePath))
          }
          return null
        }

        WatchResult.Action.REMOVE -> {
          val reprocessTime = processedBudget.timeSource.measureTime {
            try {
              processedBudget.removeFile(
                watchResult.relativePath
                  ?: throw IllegalStateException("Removing null file from ${watchResult.rootPath}")
              )
              processedBudget.reProcess()
            } catch (e: Throwable) {
              logger.error { "Failed to remove file ${watchResult.relativePath}. $e" }
              return LoadResult.Failure(e.toBudgetLoadingException(watchResult.relativePath))
            }
          }
          logger.info { "Rebuilt budget in ${reprocessTime.inWholeMilliseconds}ms" }
          return LoadResult.Success(processedBudget.dataPayload)
        }

        WatchResult.Action.REMOVE_ALL -> {
          try {
            processedBudget.removeAll()
          } catch (e: Throwable) {
            logger.error { "Failed to remove all for ${watchResult.rootPath}. $e" }
            return LoadResult.Failure(e.toBudgetLoadingException())
          }
          return null
        }
      }
    }
  }
}
