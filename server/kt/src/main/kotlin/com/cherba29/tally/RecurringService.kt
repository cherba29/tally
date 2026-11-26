package com.cherba29.tally

import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.awaitRecurringGroup
import com.cherba29.tally.schema.GqlRecurringGroup
import com.cherba29.tally.schema.toGql
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

class RecurringService(val loader: Loader) : Query {
  @GraphQLDescription("Returns details and all occurrences for a recurring transaction group.")
  suspend fun recurringTransactions(
    groupId: String,
    accountPath: String? = null
  ): GqlRecurringGroup {
    logger.info { "recurringTransactions groupId=$groupId, accountPath=$accountPath" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = loader.budget()
        budget.awaitRecurringGroup(groupId).toGql(accountPath)
      } catch (e: Exception) {
        logger.error(e) { "Error while processing recurringTransactions query groupId=$groupId" }
        throw e
      }
    }
    logger.info { "recurringTransactions in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
