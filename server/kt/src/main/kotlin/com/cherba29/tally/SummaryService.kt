package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.buildSummaryData
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking

class SummaryService(val loader: Loader) : Query {
  @GraphQLDescription("Generates delta summary table between two months.")
  @Suppress("unused")
  fun summary(owner: String, accountType: String, startMonth: Month? = null, endMonth: Month): GqlSummaryData {
    logger.info { "summary owner=$owner, accountType=$accountType, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        buildSummaryData(runBlocking { loader.budget() }, owner, accountType, startMonth, endMonth)
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing summary query owner=$owner, accountType=$accountType " +
              "startMont=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Summary in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}