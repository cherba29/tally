package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlTable
import com.cherba29.tally.schema.buildGqlTable
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class TableService(val loader: Loader) : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  @Suppress("unused")
  fun table(owner: String?, startMonth: Month, endMonth: Month): GqlTable {
    logger.info { "table owner=$owner startMonth=$startMonth endMonth=$endMonth" }
    return try {
      buildGqlTable(runBlocking { loader.budget() }, owner, startMonth, endMonth)
    } catch (e: Exception) {
      logger.error(e) { "Error while processing table query owner=$owner, startMont=$startMonth, endMonth=$endMonth" }
      throw e
    }
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}