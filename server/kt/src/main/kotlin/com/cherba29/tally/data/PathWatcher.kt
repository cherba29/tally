package com.cherba29.tally.data

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.PathWalkOption
import kotlin.io.path.div
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

data class WatchResult(
  val rootPath: Path,
  val relativePath: Path?,
  val reprocess: Boolean
)

fun Path.watchedEventFlow(predicate: (Path)->Boolean): Flow<WatchResult> {
  val watcher: WatchService = FileSystems.getDefault().newWatchService()
  val watchedPath = try {
    this.toRealPath()  // walk below does not work for relative paths.
  } catch (_: IOException) {
    return flowOf()  // Return empty flow if path does not exist.
  }
  val watchKeyToFolderMap = mutableMapOf<WatchKey, Path>()

  logger.info { "Registering all paths under $watchedPath" }
  watchedPath.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
    // TODO: reconcile with predicate.
    it.isDirectory() && !it.name.startsWith("_")
  }.associateTo(watchKeyToFolderMap) {
    logger.info { "Registering $it" }
    it.register(watcher,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
      StandardWatchEventKinds.ENTRY_MODIFY) to watchedPath.relativize(it)
  }

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
            // TODO: Perhaps use poll so no need for runInterruptable.
            key = watcher.take()
          }
          // TODO: remove this delay. Without it same modify is triggered multiple times. See discussion.
          // https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event
          // On linux (wsl2) this can be as low as 200ms, but on macos needed to be at least 500ms.
          delay(500.milliseconds)
        }.join()

        val currentKey = key
        if (currentKey != null) {
          val updatedFolderPath = watchKeyToFolderMap[currentKey]
          if (updatedFolderPath != null) {
            // TODO: detect creation of new directories.
            // TODO: support deletions.
            // TODO: remove eventIndex.
            for ((eventIndex, event) in currentKey.pollEvents().withIndex()) {
              val filePath = updatedFolderPath / (event.context() as Path)  // Relative to watched root path.
              if (predicate(filePath)) {
                logger.info { "Emitting $eventIndex kind=${event.kind()} $ANSI_YELLOW$filePath$ANSI_RESET for event ${event.kind()}" }
                emit(WatchResult(this@watchedEventFlow, filePath, true))
              }
            }
          } else {
            logger.warn { "Could not find registered key for $key" }
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
