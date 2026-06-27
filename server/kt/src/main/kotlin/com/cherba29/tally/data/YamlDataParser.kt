package com.cherba29.tally.data

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.yaml.CustomProblemHandler
import com.cherba29.tally.data.yaml.LocalDateDeserializer
import com.cherba29.tally.data.yaml.MonthDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlinx.datetime.LocalDate

class YamlDataParser {
  private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  private val module = SimpleModule()
  private val problemHandler = CustomProblemHandler()

  init {
    module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())
    module.addDeserializer(Month::class.java, MonthDeserializer())
    mapper.registerModule(module)
    mapper.addHandler(problemHandler)
  }

  fun parseContent(content: String, relativeFilePath: Path): YamlData {
    val result = try {
      mapper.readValue(content, YamlData::class.java)
    } catch (e: JsonMappingException) {
      logger.error { "Failed to parse $relativeFilePath: ${e.message}" }
      throw IllegalArgumentException(e.message + " while processing $relativeFilePath", e)
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
