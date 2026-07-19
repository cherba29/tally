package com.cherba29.tally.utils

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
import kotlin.io.path.relativeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.milliseconds


data class WatchResult(
  val rootPath: Path,
  val relativePath: Path?,
  val action: Action
) {
  enum class Action {
    /**
     * Add this item to the list to be processed.
     */
    ADD,

    /**
     * Remove this item from the list and reprocess.
     */
    REMOVE,

    /**
     * Remove all items from the list, and wait for new ones to be added.
     */
    REMOVE_ALL,

    /**
     * Reprocess all added items in the list.
     */
    REPROCESS,
  }

}

fun Path.scan(filePathFilter: (Path)->Boolean) = sequence {
  val watchedPath = try {
    this@scan.toRealPath()  // walk below does not work for relative paths.
  } catch (_: IOException) {
    return@sequence  // Return empty sequence if path does not exist.
  }
  for (filePath in watchedPath.walk()) {
    val relativeFilePath = filePath.relativeTo(watchedPath)
    if (ignorePathRegex.containsMatchIn(relativeFilePath.pathString)) {
      continue
    }
    if (filePathFilter(relativeFilePath)) {
      yield(WatchResult(this@scan, relativeFilePath, WatchResult.Action.ADD))
    }
  }
  yield(WatchResult(this@scan,null, WatchResult.Action.REPROCESS))
}

/**
 * Watches for changes in this path and emits WatchResults.
 * Paths starting with '_' are skipped and ignored.
 */
fun Path.watchedEventFlow(filePathFilter: (Path)->Boolean): Flow<WatchResult> {
  val watcher: WatchService = FileSystems.getDefault().newWatchService()
  val watchedPath = try {
    this.toRealPath()  // walk below does not work for relative paths.
  } catch (_: IOException) {
    return flowOf()  // Return empty flow if path does not exist.
  }
  val watchKeyToFolderMap = mutableMapOf<WatchKey, Path>()

  logger.info { "Registering all paths under $watchedPath" }
  watchedPath.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
    it.isDirectory() && !ignorePathRegex.containsMatchIn(it.pathString)
  }.associateTo(watchKeyToFolderMap) {
    logger.info { "Registering $it" }
    it.register(watcher,*eventsToWatch) to watchedPath.relativize(it)
  }

  val rootPath = this
  return flow {
    // Emit existing files.
    scan(filePathFilter).forEach { emit(it) }

    while (currentCoroutineContext().isActive) {
      val key: WatchKey = runInterruptible(Dispatchers.IO) {
        logger.info { "Waiting for changes to $watchedPath" }
        watcher.take()
      }
      // TODO: remove this delay. Without it same modify is triggered multiple times. See discussion.
      // https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event
      // On linux (wsl2) this can be as low as 200ms, but on macOS needed to be at least 500ms.
      delay(500.milliseconds)

      val updatedFolderPath = watchKeyToFolderMap[key]
      if (updatedFolderPath != null) {
        for (event in key.pollEvents()) {
          val filePath = updatedFolderPath / (event.context() as Path)  // Relative to watched root path.
          if (filePathFilter(filePath)) {
            logger.info { "$ANSI_YELLOW$filePath$ANSI_RESET for event ${event.kind()}" }
            when (event.kind()) {
              StandardWatchEventKinds.ENTRY_CREATE -> {
                val fullPath = watchedPath / filePath
                if (fullPath.isDirectory()) {
                  logger.info { "Registering new directory $fullPath" }
                  watchKeyToFolderMap[fullPath.register(watcher, *eventsToWatch)] = filePath
                  // We added new directory, it can already contain files in it, so scan and emit them.
                  fullPath.scan(filePathFilter).forEach {
                    if (it.relativePath != null) {
                      emit(WatchResult(rootPath, filePath / it.relativePath, WatchResult.Action.ADD))
                    } else { // Emit end of scan event.
                      emit(WatchResult(rootPath, null, WatchResult.Action.REPROCESS))
                    }
                  }
                } else {
                  emit(WatchResult(rootPath, filePath, WatchResult.Action.REPROCESS))
                }
              }
              StandardWatchEventKinds.ENTRY_DELETE -> {
                val fullPath = watchedPath / filePath
                if (fullPath.isDirectory()) {
                  TODO("Handle directory delete")
                } else {
                  emit(WatchResult(rootPath, filePath, WatchResult.Action.REMOVE))
                }
              }
              StandardWatchEventKinds.ENTRY_MODIFY -> {
                emit(WatchResult(rootPath, filePath, WatchResult.Action.REPROCESS))
              }
              StandardWatchEventKinds.OVERFLOW -> {
                // This can happen if watcher event queue gets overflows. In that case just rescan everything.
                logger.warn { "Filesystem event overflow at $rootPath, rescanning..." }
                emit(WatchResult(rootPath, null, WatchResult.Action.REMOVE_ALL))
                scan(filePathFilter).forEach { emit(it) }
              }
              else -> {
                throw IllegalStateException("Unknown event ${event.kind()} for $filePath")
              }
            }
          }
        }
      } else {
        logger.warn { "Could not find registered key for $key" }
      }
      if (!key.reset()) break
    }
  }.onCompletion {
    logger.info { "Closing watcher for path '$rootPath'"}
    watcher.close()
  }
}

private val eventsToWatch = arrayOf(
  StandardWatchEventKinds.ENTRY_CREATE,
  StandardWatchEventKinds.ENTRY_DELETE,
  StandardWatchEventKinds.ENTRY_MODIFY,
  StandardWatchEventKinds.OVERFLOW
)
private val ignorePathRegex = Regex("(^_)|(/_)")
private const val ANSI_RESET = "\u001B[0m"
private const val ANSI_RED = "\u001B[31m"
private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_YELLOW = "\u001B[33m"
private const val ANSI_BLUE = "\u001B[34m"
private val logger = KotlinLogging.logger {}
