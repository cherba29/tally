package com.cherba29.tally.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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
  val watchedFolders = mutableSetOf<Path>()

  logger.info { "Registering all paths under $watchedPath" }
  watchedPath.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
    it.isDirectory() && !ignorePathRegex.containsMatchIn(it.pathString)
  }.associateTo(watchKeyToFolderMap) {
    logger.info { "Registering $it" }
    val relative = watchedPath.relativize(it)
    watchedFolders.add(relative)
    it.register(watcher,*eventsToWatch) to relative
  }
  val rootKey = watchKeyToFolderMap.entries.firstOrNull { it.value == Path.of("") }?.key

  // Stores references to active background checks to avoid spawning duplicate coroutines for the same file
  val activeChecks = ConcurrentHashMap<Path, Job>()
  // Create a scope specifically for handling background file-settling tasks
  val watcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  val rootPath = this
  // Keep track of watched files so that when directory is deleted,
  // delete events can be sent for all files within directory.
  val watchedFiles = mutableSetOf<Path>()
  return channelFlow {
    // Emit existing files.
    scan(filePathFilter).forEach {
      if (it.relativePath != null) {
        watchedFiles.add(it.relativePath)
      }
      send(it)
    }

    while (currentCoroutineContext().isActive) {
      val key: WatchKey = runInterruptible(Dispatchers.IO) {
        logger.info { "Waiting for changes to $watchedPath" }
        watcher.take()
      }
      val updatedFolderPath = watchKeyToFolderMap[key]
      // Optional brief delay to let OS complete multipart flush operations.
      // This almost eliminates multiple modify events for same file,
      // the rest is taken care of by stability check.
      delay(50.milliseconds)
      if (updatedFolderPath != null) {
        for (event in key.pollEvents()) {
          val filePath = updatedFolderPath / (event.context() as Path)  // Relative to watched root path.
          if (ignorePathRegex.containsMatchIn(filePath.pathString)) {
            continue
          }
          val isWatchedFolder = filePath in watchedFolders || watchedFolders.any { it.startsWith(filePath) }
          logger.info { "$ANSI_YELLOW$filePath$ANSI_RESET for event ${event.kind()} count=${event.count()}" }
          when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
              val fullPath = watchedPath / filePath
              if (fullPath.isDirectory()) {
                logger.info { "Registering new directory $fullPath" }
                fullPath.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter {
                  it.isDirectory() && !ignorePathRegex.containsMatchIn(it.pathString)
                }.forEach {
                  val relative = watchedPath.relativize(it)
                  val dirKey = withContext(Dispatchers.IO) {
                    it.register(watcher, *eventsToWatch)
                  }
                  watchKeyToFolderMap[dirKey] = relative
                  watchedFolders.add(relative)
                }
                // We added new directory, it can already contain files in it, so scan and emit them.
                fullPath.scan(filePathFilter).forEach {
                  if (it.relativePath != null) {
                    val relativeFile = filePath / it.relativePath
                    watchedFiles.add(relativeFile)
                    send(WatchResult(rootPath, relativeFile, WatchResult.Action.ADD))
                  } else { // Emit end of scan event.
                    send(WatchResult(rootPath, null, WatchResult.Action.REPROCESS))
                  }
                }
              } else if (filePathFilter(filePath)) {
                watchedFiles.add(filePath)
                send(WatchResult(rootPath, filePath, WatchResult.Action.REPROCESS))
              }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
              val fullPath = watchedPath / filePath
              if (isWatchedFolder) {
                logger.info { "Handling directory delete for $ANSI_YELLOW$fullPath$ANSI_RESET" }
                val keysToCancel = watchKeyToFolderMap.filter { it.value == filePath || it.value.startsWith(filePath) }
                for ((k, _) in keysToCancel) {
                  k.cancel()
                  watchKeyToFolderMap.remove(k)
                }
                watchedFolders.removeIf { it == filePath || it.startsWith(filePath) }

                val checksToCancel = activeChecks.filter { it.key.startsWith(fullPath) }
                for ((p, job) in checksToCancel) {
                  job.cancel()
                  activeChecks.remove(p)
                }

                val filesToRemove = watchedFiles.filter { it.startsWith(filePath) }
                for (file in filesToRemove) {
                  watchedFiles.remove(file)
                  send(WatchResult(rootPath, file, WatchResult.Action.REMOVE))
                }
              } else if (filePathFilter(filePath)) {
                watchedFiles.remove(filePath)
                send(WatchResult(rootPath, filePath, WatchResult.Action.REMOVE))
              }
            }
            StandardWatchEventKinds.ENTRY_MODIFY -> {
              if (filePathFilter(filePath)) {
                val fullPath = watchedPath / filePath
                val file = fullPath.toFile()
                if (file.exists() && file.isFile) {
                  // If we are already waiting for this file to settle, cancel the old check
                  // and restart the timer (Debounce/Settle mechanism)
                  activeChecks[fullPath]?.cancel()

                  // Launch a non-blocking job to monitor stability
                  activeChecks[fullPath] = watcherScope.launch {
                    if (waitForFileToSettle(file)) {
                      watchedFiles.add(filePath)
                      send(WatchResult(rootPath, filePath, WatchResult.Action.REPROCESS))
                    }
                    activeChecks.remove(fullPath) // Clean up tracking map
                  }
                }
              }
            }
            StandardWatchEventKinds.OVERFLOW -> {
              // This can happen if watcher event queue gets overflows. In that case just rescan everything.
              logger.warn { "Filesystem event overflow at $rootPath, rescanning..." }
              send(WatchResult(rootPath, null, WatchResult.Action.REMOVE_ALL))
              watchedFiles.clear()
              scan(filePathFilter).forEach {
                if (it.relativePath != null) {
                  watchedFiles.add(it.relativePath)
                }
                send(it)
              }
            }
            else -> {
              throw IllegalStateException("Unknown event ${event.kind()} for $filePath")
            }
          }
        }
      } else {
        logger.warn { "Could not find registered key for $key" }
      }
      if (!key.reset()) {
        watchKeyToFolderMap.remove(key)
        if (key == rootKey || updatedFolderPath == Path.of("")) {
          break
        }
      }
    }
  }.onCompletion {
    logger.info { "Closing watcher for path '$rootPath'"}
    withContext(Dispatchers.IO) {
      watcher.close()
    }
    activeChecks.forEach { it.value.cancel() }
  }
}

private suspend fun waitForFileToSettle(file: File): Boolean {
  var lastSize = -1L
  var currentSize = file.length()
  val checkInterval = 200.milliseconds // Time to wait between checks
  val maxWaitAttempts = 15   // Prevents infinite loops if a file is permanently locked

  var attempts = 0
  while (currentSize != lastSize && attempts < maxWaitAttempts) {
    lastSize = currentSize
    delay(checkInterval) // Suspend thread safely, letting other tasks run

    if (!file.exists()) return false // File was deleted or moved mid-write
    currentSize = file.length()
    attempts++
  }
  if (currentSize != lastSize) {
    throw IllegalStateException("File $file never settled after ${checkInterval*maxWaitAttempts}")
  }

  // Final sanity check: try to open it briefly to verify exclusive write lock is released
  return isFileAccessible(file)
}

private fun isFileAccessible(file: File): Boolean {
  return try {
    // Attempt to open in read/write mode. If OS flush is incomplete, this will throw an IOException
    java.io.RandomAccessFile(file, "rw").use { true }
  } catch (_: Exception) {
    throw IllegalStateException("File $file is not accessible")
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
// private const val ANSI_RED = "\u001B[31m"
// private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_YELLOW = "\u001B[33m"
// private const val ANSI_BLUE = "\u001B[34m"
private val logger = KotlinLogging.logger {}
