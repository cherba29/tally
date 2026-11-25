package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.LoadResult
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.table
import com.cherba29.tally.schema.GqlTable
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTimedValue

class TableService(val loader: Loader) : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  fun table(
    owner: String?,
    startMonth: Month,
    endMonth: Month
  ): GqlTable {
    logger.info { "table owner=$owner startMonth=$startMonth endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        runBlocking { loader.budget() }.table(owner, startMonth, endMonth)
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing table query owner=$owner, startMonth=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Computed table in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

class TableSubscription(val loader: Loader) : Subscription {
  @GraphQLDescription("Subscribes to notifications when budget data is reloaded.")
  fun tableUpdates(): Flow<Boolean> =
    loader.updates.map {
      when (it) {
        is LoadResult.Success -> true
        is LoadResult.Failure -> throw it.error
      }
    }
}
