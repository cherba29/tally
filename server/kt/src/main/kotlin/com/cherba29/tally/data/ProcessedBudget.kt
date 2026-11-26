package com.cherba29.tally.data

import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
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
  private val parsedAccountData = mutableMapOf<String, YamlData>()
  private val fileErrors = mutableMapOf<String, Throwable>()
  private var lastTransfersFingerprint: TransfersFingerprint? = null
  private var lastRecurringDeferred: Deferred<RecurringTransactionIndex>? = null
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
      val built = budget(recurringContext) {
        val unwantedFiles = mutableListOf<String>()
        for ((filePath, accountData) in parsedAccountData) {
          if (!loadYamlFile(this, accountData, Paths.get(filePath))) {
            logger.warn { "warning: $filePath is not an account file." }
            unwantedFiles.add(filePath)
          }
        }
        // Since reprocess is called multiple times, no need reprocess them.
        for (filePath in unwantedFiles) {
          parsedAccountData.remove(filePath)
        }
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
      parsedAccountData[key] = accountData
      fileErrors.remove(key)
    } catch (e: Throwable) {
      parsedAccountData.remove(key)
      val wrapped = e.toBudgetLoadingException(relativeFilePath)
      fileErrors[key] = wrapped
      throw wrapped
    }
  }

  fun removeFile(relativeFilePath: Path) {
    val key = relativeFilePath.toString()
    fileErrors.remove(key)
    parsedAccountData.remove(key)
  }

  fun removeAll() {
    fileErrors.clear()
    parsedAccountData.clear()
    lastTransfersFingerprint = null
    lastRecurringDeferred?.cancel()
    lastRecurringDeferred = null
    budget = null
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
