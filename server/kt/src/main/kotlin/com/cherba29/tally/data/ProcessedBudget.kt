package com.cherba29.tally.data

import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.IncrementalBudgetState
import com.cherba29.tally.data.yaml.YamlData
import com.cherba29.tally.data.yaml.YamlDataParser
import com.cherba29.tally.recurring.RecurringTransactionIndex
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

class ProcessedBudget(
  val timeSource: TimeSource = TimeSource.Monotonic,
  private val asyncScope: CoroutineScope? = null
) {
  private val yamlDataParser = YamlDataParser()
  private val parsedAccountData = LinkedHashMap<String, YamlData>()
  private val fileErrors = mutableMapOf<String, Throwable>()
  private val dirtyFiles = mutableSetOf<String>()
  private var structureDirty = true
  private var incrementalState: IncrementalBudgetState? = null
  private var lastTransfersFingerprint: TransfersFingerprint? = null
  private var lastRecurringDeferred: Deferred<RecurringTransactionIndex>? = null
  internal var lastRebuildWasIncremental: Boolean = false
    private set
  var budget: Budget? = null

  val dataPayload: Budget get() = budget!!
  private val startTime = timeSource.markNow()
  private var processedOn: Duration? = null // Not yet processed
  val loadedOn: Duration? get() = processedOn

  fun reProcess() {
    if (fileErrors.isNotEmpty()) {
      throw fileErrors.values.first()
    }
    val elapsedBudgetTime = timeSource.measureTime {
      val recurringContext = BudgetBuilder.RecurringBuildContext(
        previousFingerprint = lastTransfersFingerprint,
        previousDeferred = lastRecurringDeferred,
        asyncScope = asyncScope
      )

      val state = incrementalState
      val incrementalBudget = if (!structureDirty && state != null) {
        val dirtyYamlFiles = LinkedHashMap<String, YamlData>(dirtyFiles.size)
        for (fileKey in dirtyFiles) {
          val yaml = parsedAccountData[fileKey]
          if (yaml != null) {
            dirtyYamlFiles[fileKey] = yaml
          }
        }
        state.tryApplyDirtyFiles(dirtyYamlFiles, recurringContext)
      } else {
        null
      }

      val built = if (incrementalBudget != null) {
        lastRebuildWasIncremental = true
        dirtyFiles.clear()
        incrementalBudget
      } else {
        lastRebuildWasIncremental = false
        val parsedFiles = LinkedHashMap<String, Pair<Path, ParsedYamlAccount>>(parsedAccountData.size)
        val builder = BudgetBuilder()
        val unwantedFiles = mutableListOf<String>()
        for ((filePath, accountData) in parsedAccountData) {
          val relativePath = Paths.get(filePath)
          val parsed = extractYamlAccountData(accountData, relativePath)
          if (parsed == null) {
            logger.warn { "warning: $filePath is not an account file." }
            unwantedFiles.add(filePath)
          } else {
            try {
              addParsedYamlAccount(builder, parsed, relativePath)
            } catch (e: IllegalArgumentException) {
              logger.error { e.javaClass.simpleName + ": " + e.message }
              throw BudgetLoadingException(
                message = e.message + ", while processing $relativePath",
                cause = e,
                fileName = relativePath.toString()
              )
            } catch (e: Exception) {
              val message = ", while processing $relativePath"
              logger.error { e.javaClass.simpleName + ": " + e.message + message }
              logger.info { "Account Data$accountData" }
              throw e.toBudgetLoadingException(relativePath)
            }
            parsedFiles[filePath] = relativePath to parsed
          }
        }
        // Since reprocess is called multiple times, no need reprocess them.
        for (filePath in unwantedFiles) {
          parsedAccountData.remove(filePath)
        }
        val snapshot = builder.buildWithSnapshot(recurringContext)
        incrementalState = IncrementalBudgetState.fromFullBuild(snapshot, parsedFiles, timeSource)
        dirtyFiles.clear()
        structureDirty = false
        snapshot.budget
      }

      budget = built
      lastTransfersFingerprint = built.transfersFingerprint
      lastRecurringDeferred = built.recurringIndexDeferred
      processedOn = startTime.elapsedNow()
    }
    logger.info {
      "Done building budget for ${budget?.tree?.nLeaves} accounts in $elapsedBudgetTime"
    }
  }

  fun addFile(
    rootPath: Path,
    relativeFilePath: Path
  ) {
    val key = relativeFilePath.toString()
    try {
      val content = rootPath.resolve(relativeFilePath).readText(Charsets.UTF_8)
      val accountData = yamlDataParser.parseContent(content, relativeFilePath)
      val state = incrementalState
      if (state == null || !state.hasFile(key)) {
        structureDirty = true
      }
      parsedAccountData[key] = accountData
      dirtyFiles.add(key)
      fileErrors.remove(key)
    } catch (e: Throwable) {
      parsedAccountData.remove(key)
      structureDirty = true
      val wrapped = e.toBudgetLoadingException(relativeFilePath)
      fileErrors[key] = wrapped
      throw wrapped
    }
  }

  fun removeFile(relativeFilePath: Path) {
    val key = relativeFilePath.toString()
    fileErrors.remove(key)
    parsedAccountData.remove(key)
    dirtyFiles.remove(key)
    structureDirty = true
  }

  fun removeAll() {
    fileErrors.clear()
    parsedAccountData.clear()
    dirtyFiles.clear()
    structureDirty = true
    incrementalState = null
    lastTransfersFingerprint = null
    lastRecurringDeferred?.cancel()
    lastRecurringDeferred = null
    lastRebuildWasIncremental = false
    budget = null
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
