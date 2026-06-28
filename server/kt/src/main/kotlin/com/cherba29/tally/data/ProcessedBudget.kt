package com.cherba29.tally.data

import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.data.yaml.YamlData
import com.cherba29.tally.data.yaml.YamlDataParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

class ProcessedBudget(val timeSource: TimeSource = TimeSource.Monotonic) {
  private val yamlDataParser = YamlDataParser()
  private val parsedAccountData = mutableMapOf<String, YamlData>()
  var budget: Budget? = null

  val dataPayload: Budget get() = budget!!
  private val startTime = timeSource.markNow()
  private var processedOn: Duration? = null  // Not yet processed
  val loadedOn: Duration? get() = processedOn

  fun reProcess() {
    val elapsedBudgetTime = timeSource.measureTime {
      budget = budget {
        val unwantedFiles = mutableListOf<String>()
        for ((filePath, accountData) in parsedAccountData) {
          try {
            if (!loadYamlFile(this, accountData, Paths.get(filePath))) {
              logger.warn { "warning: $filePath is not an account file." }
              unwantedFiles.add(filePath)
            }
          } catch (e: Exception) {
            logger.error(e) { "error: Failed to add $filePath" }
            continue
          }
        }
        // Since reprocess is called multiple times, no need reprocess them.
        for (filePath in unwantedFiles) {
          parsedAccountData.remove(filePath)
        }
      }
      processedOn = startTime.elapsedNow()
    }
    logger.info {
      "Done building budget for ${budget?.leafToAccount?.size} accounts in $elapsedBudgetTime"
    }
  }

  fun addFile(rootPath: Path, relativeFilePath: Path) {
    val content = rootPath.resolve(relativeFilePath).readText(Charsets.UTF_8)
    val accountData = yamlDataParser.parseContent(content, relativeFilePath)
    parsedAccountData[relativeFilePath.toString()] = accountData
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
