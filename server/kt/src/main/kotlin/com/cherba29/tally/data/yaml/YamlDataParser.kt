package com.cherba29.tally.data.yaml

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.BudgetLoadingException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDate
import java.nio.file.Path

class YamlDataParser {
  private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  private val module = SimpleModule()
  private val problemHandler = CustomProblemHandler()

  init {
    module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())
    module.addDeserializer(Month::class.java, MonthDeserializer())
    module.addDeserializer(TransferDataWrapper::class.java, TransferDataWrapperDeserializer())
    mapper.registerModule(module)
    mapper.addHandler(problemHandler)
  }

  fun parseContent(
    content: String,
    relativeFilePath: Path
  ): YamlData {
    val result = try {
      mapper.readValue(content, YamlData::class.java)
    } catch (e: JsonProcessingException) {
      val line = e.location?.lineNr?.takeIf { it > 0 }
      val locationStr = if (line != null) ":$line" else ""

      // Clean core error message by stripping Jackson's verbose "at [Source: ...]" and "(through reference chain: ...)"
      val rawMessage = e.originalMessage ?: e.message ?: "YAML processing error"
      val coreMessage = rawMessage
        .replace(Regex("""\n?\s*at \[Source:.*?\]""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""\s*\(through reference chain:.*?\)$"""), "")
        .trim()

      val chain = if (e is JsonMappingException && e.path.isNotEmpty()) {
        val fieldNames = e.path.mapNotNull { it.fieldName?.let { name -> "\"$name\"" } }
        if (fieldNames.isNotEmpty()) {
          fieldNames.joinToString("->")
        } else {
          e.path.mapNotNull { if (it.index >= 0) "[${it.index}]" else null }.joinToString("->")
        }
      } else {
        null
      }

      val pathStr = if (!chain.isNullOrEmpty()) " at $chain" else ""
      val message = "$coreMessage$pathStr while processing $relativeFilePath$locationStr"
      logger.error { "Failed to parse $relativeFilePath$locationStr: $message" }
      throw BudgetLoadingException(
        message = message,
        cause = e,
        fileName = relativeFilePath.toString(),
        lineNumber = line,
        fieldPath = chain
      )
    } catch (e: Exception) {
      logger.error { "Failed to parse $relativeFilePath: ${e.message}" }
      val cleanMsg = (e.message ?: "YAML processing error")
        .replace(Regex("""\n?\s*at \[Source:.*?\]""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""\s*\(through reference chain:.*?\)$"""), "")
        .trim()
      throw BudgetLoadingException(
        message = "$cleanMsg while processing $relativeFilePath",
        cause = e,
        fileName = relativeFilePath.toString()
      )
    }
    if (problemHandler.ignoredFields.isNotEmpty()) {
      logger.warn { "Unknown ignored fields: ${problemHandler.ignoredFields} in $relativeFilePath" }
    }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
