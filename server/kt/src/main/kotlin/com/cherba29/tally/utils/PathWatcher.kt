package com.cherba29.tally.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.PathWalkOption
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.time.Duration
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
    REPROCESS
  }
}

fun Path.scan(filePathFilter: (Path) -> Boolean) =
  sequence {
    val watchedPath = try {
      this@scan.toRealPath() // walk below does not work for relative paths.
    } catch (_: IOException) {
      return@sequence // Return empty sequence if path does not exist.
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
    yield(WatchResult(this@scan, null, WatchResult.Action.REPROCESS))
  }

internal data class PathWatchDefaults(
  val coalesceDelay: Duration,
  val settleCheckInterval: Duration,
  val maxSettleWait: Duration = DEFAULT_MAX_SETTLE_WAIT
)

internal fun defaultWatchTimingForOs(osName: String = System.getProperty("os.name").orEmpty()): PathWatchDefaults {
  val normalized = osName.lowercase()
  return when {
    normalized.contains("mac") || normalized.contains("darwin") ->
      PathWatchDefaults(coalesceDelay = 50.milliseconds, settleCheckInterval = 50.milliseconds)
    normalized.contains("win") ->
      PathWatchDefaults(coalesceDelay = 25.milliseconds, settleCheckInterval = 50.milliseconds)
    else ->
      PathWatchDefaults(coalesceDelay = 10.milliseconds, settleCheckInterval = 25.milliseconds)
  }
}

/**
 * Watches for changes in this path and emits WatchResults.
 * Paths starting with '_' are skipped and ignored.
 *
 * @param coalesceDelay Brief delay after a filesystem event wakes the watcher so OS multi-part
 *   flush bursts are drained together from the same [WatchKey]. Defaults are tuned per OS.
 * @param settleCheckInterval Interval between size/mtime stability checks for created or modified
 *   files. Defaults are tuned per OS.
 * @param maxSettleWait Total maximum duration to wait for a file to finish writing.
 */
fun Path.watchedEventFlow(
  coalesceDelay: Duration = defaultWatchTimingForOs().coalesceDelay,
  settleCheckInterval: Duration = defaultWatchTimingForOs().settleCheckInterval,
  maxSettleWait: Duration = DEFAULT_MAX_SETTLE_WAIT,
  filePathFilter: (Path) -> Boolean
): Flow<WatchResult> {
  val watcher: WatchService = FileSystems.getDefault().newWatchService()
  val watchedPath = try {
    this.toRealPath() // walk below does not work for relative paths.
  } catch (_: IOException) {
    return flowOf() // Return empty flow if path does not exist.
  }
  val watchKeyToFolderMap = mutableMapOf<WatchKey, Path>()
  val watchedFolders = mutableSetOf<Path>()

  logger.info { "Registering all paths under $watchedPath" }
  watchedPath
    .walk(PathWalkOption.INCLUDE_DIRECTORIES)
    .filter {
      it.isDirectory() && !ignorePathRegex.containsMatchIn(it.pathString)
    }.associateTo(watchKeyToFolderMap) {
      logger.info { "Registering $it" }
      val relative = watchedPath.relativize(it)
      watchedFolders.add(relative)
      it.register(watcher, *eventsToWatch) to relative
    }
  val rootKey = watchKeyToFolderMap.entries.firstOrNull { it.value == Path.of("") }?.key

  // Stores references to active background checks to avoid spawning duplicate coroutines for the same file
  val activeChecks = ConcurrentHashMap<Path, Job>()
  // Create a scope specifically for handling background file-settling tasks
  val watcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  val rootPath = this
  // Keep track of watched files so that when directory is deleted,
  // delete events can be sent for all files within directory.
  val watchedFiles = ConcurrentHashMap.newKeySet<Path>()
  return channelFlow {
    fun scheduleFileSettle(
      fullPath: Path,
      filePath: Path
    ) {
      val file = fullPath.toFile()
      if (!file.exists() || !file.isFile) return
      // Cancel any in-flight settle check for this file and restart the timer (Debounce/Settle).
      activeChecks[fullPath]?.cancel()

      val checkJob = watcherScope.launch(start = CoroutineStart.LAZY) {
        val selfJob = coroutineContext[Job]
        try {
          if (waitForFileToSettle(file, settleCheckInterval, maxSettleWait)) {
            watchedFiles.add(filePath)
            send(WatchResult(rootPath, filePath, WatchResult.Action.REPROCESS))
          }
        } finally {
          if (selfJob != null) {
            activeChecks.remove(fullPath, selfJob)
          }
        }
      }
      activeChecks[fullPath] = checkJob
      checkJob.start()
    }

    // Emit existing files.
    scan(filePathFilter).forEach {
      if (it.relativePath != null) {
        watchedFiles.add(it.relativePath)
      }
      send(it)
    }

    while (currentCoroutineContext().isActive) {
      val key: WatchKey = try {
        runInterruptible(Dispatchers.IO) {
          logger.info { "Waiting for changes to $watchedPath" }
          watcher.take()
        }
      } catch (_: ClosedWatchServiceException) {
        break
      }
      val updatedFolderPath = watchKeyToFolderMap[key]
      // Brief delay to let OS queue multipart flush events into the same pollEvents() batch.
      if (coalesceDelay.isPositive()) {
        delay(coalesceDelay)
      }
      if (updatedFolderPath != null) {
        for (event in key.pollEvents()) {
          val filePath = updatedFolderPath / (event.context() as Path) // Relative to watched root path.
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
                fullPath
                  .walk(PathWalkOption.INCLUDE_DIRECTORIES)
                  .filter {
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
                // Settle newly created files too so Windows/macOS writers release locks and finish
                // flushing, and create + immediate modify events coalesce into one REPROCESS.
                scheduleFileSettle(fullPath, filePath)
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
                  activeChecks.remove(p, job)
                }

                val filesToRemove = watchedFiles.filter { it.startsWith(filePath) }
                for (file in filesToRemove) {
                  watchedFiles.remove(file)
                  send(WatchResult(rootPath, file, WatchResult.Action.REMOVE))
                }
              } else if (filePathFilter(filePath)) {
                activeChecks.remove(fullPath)?.cancel()
                watchedFiles.remove(filePath)
                send(WatchResult(rootPath, filePath, WatchResult.Action.REMOVE))
              }
            }
            StandardWatchEventKinds.ENTRY_MODIFY -> {
              if (filePathFilter(filePath)) {
                scheduleFileSettle(watchedPath / filePath, filePath)
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
    logger.info { "Closing watcher for path '$rootPath'" }
    activeChecks.values.forEach { it.cancel() }
    activeChecks.clear()
    watcherScope.cancel()
    withContext(NonCancellable + Dispatchers.IO) {
      watcher.close()
    }
  }
}

private suspend fun waitForFileToSettle(
  file: File,
  checkInterval: Duration,
  maxWait: Duration
): Boolean {
  val intervalMs = checkInterval.inWholeMilliseconds.coerceAtLeast(1L)
  val maxWaitAttempts = (maxWait.inWholeMilliseconds / intervalMs).coerceAtLeast(1L).toInt()
  var lastSize = -1L
  var lastModified = -1L
  var currentSize = file.length()
  var currentModified = file.lastModified()

  var attempts = 0
  while (attempts < maxWaitAttempts) {
    val stable = currentSize == lastSize && currentModified == lastModified
    if (stable && isFileAccessible(file)) {
      return true
    }
    lastSize = currentSize
    lastModified = currentModified
    delay(checkInterval) // Suspend thread safely, letting other tasks run

    if (!file.exists()) return false // File was deleted or moved mid-write
    currentSize = file.length()
    currentModified = file.lastModified()
    attempts++
  }

  if (currentSize == lastSize && currentModified == lastModified && isFileAccessible(file)) {
    return true
  }
  throw IllegalStateException("File $file never settled after ${checkInterval * maxWaitAttempts}")
}

private fun isFileAccessible(file: File): Boolean =
  try {
    val mode = if (file.canWrite()) "rw" else "r"
    java.io.RandomAccessFile(file, mode).use { true }
  } catch (_: Exception) {
    false
  }

private val DEFAULT_MAX_SETTLE_WAIT = 3_000.milliseconds

private val eventsToWatch = arrayOf(
  StandardWatchEventKinds.ENTRY_CREATE,
  StandardWatchEventKinds.ENTRY_DELETE,
  StandardWatchEventKinds.ENTRY_MODIFY,
  StandardWatchEventKinds.OVERFLOW
)
internal val ignorePathRegex = Regex("(^_)|([/\\\\]_)")
private const val ANSI_RESET = "\u001B[0m"

// private const val ANSI_RED = "\u001B[31m"
// private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_YELLOW = "\u001B[33m"

// private const val ANSI_BLUE = "\u001B[34m"
private val logger = KotlinLogging.logger {}
