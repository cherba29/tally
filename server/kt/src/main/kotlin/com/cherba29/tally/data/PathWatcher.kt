package com.cherba29.tally.data

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.PathWalkOption
import kotlin.io.path.isDirectory
import kotlin.io.path.walk
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

data class WatchResult(
  val rootPath: Path,
  val relativePath: Path?,
  val reprocess: Boolean
)

fun Path.watchedEventFlow(predicate: (Path)->Boolean): Flow<WatchResult> {
  val watcher: WatchService = FileSystems.getDefault().newWatchService()
  val watchKeys = mutableMapOf<WatchKey, Path>()
    logger.info { "Registering all paths under $this" }
    this.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
      // TODO: reconcile with predicate.
      it.isDirectory() && !it.name.startsWith("_")
    }.associateTo(watchKeys) {
      logger.info { "Registering $it" }
      it.register(watcher,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY) to this.relativize(it)
    }

  val watchedPath = this
  return flow {
    // Emit existing files.
    for (filePath in watchedPath.walk()) {
      val relativeFilePath = filePath.relativeTo(watchedPath)
      if (predicate(relativeFilePath)) {
        emit(WatchResult(this@watchedEventFlow, relativeFilePath, false))
      }
    }
    emit(WatchResult(this@watchedEventFlow,null, true))

    while (currentCoroutineContext().isActive) {
      coroutineScope {
        var key: WatchKey? = null
        launch {
          runInterruptible(Dispatchers.IO) {
            logger.info { "Waiting for changes to $watchedPath" }
            key = watcher.take()
          }
          // TODO: remove this delay. Without it same modify is triggered multiple times.
          delay(500)
        }.join()

        val currentKey = key
        if (currentKey != null) {
          val updatedPath = watchKeys[currentKey]
          // TODO: detect creation of new directories.
          // TODO: remove eventIndex.
          for ((eventIndex, event) in currentKey.pollEvents().withIndex()) {
            val context = event.context() as Path // Relative path to the file/directory changed

            if (updatedPath != null) {
              val filePath = updatedPath.resolve(context)
              if (predicate(filePath)) {
                logger.info { "Emitting $eventIndex $ANSI_YELLOW$filePath$ANSI_RESET for event ${event.kind()}" }
                emit(WatchResult(this@watchedEventFlow, filePath, true))
              }
            } else {
              logger.warn { "Could not find registered key for $key" }
            }
            }
          currentKey.reset()
        }
      }
    }
  }.onCompletion {
    watcher.close()
  }
}

private const val ANSI_RESET = "\u001B[0m"
private const val ANSI_RED = "\u001B[31m"
private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_YELLOW = "\u001B[33m"
private const val ANSI_BLUE = "\u001B[34m"
private val logger = KotlinLogging.logger {}
