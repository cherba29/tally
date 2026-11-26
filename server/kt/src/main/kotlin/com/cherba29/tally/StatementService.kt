package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.statement
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.toGql
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

class StatementService(val loader: Loader) : Query {
  @GraphQLDescription("Returns a monthly statement for given account.")
  suspend fun statement(
    accountPath: String,
    month: Month
  ): GqlStatement {
    logger.info { "statement account=$accountPath, month=$month" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = loader.budget()
        val (accountNode, statement, isClosed) = budget.statement(accountPath, month)
        val recurringIndex = budget.awaitRecurringIndex()
        statement.toGql(accountNode.name, isClosed) { tx ->
          recurringIndex.findGroup(
            accountPath = accountNode.pathString,
            targetAccountPath = tx.targetTreeNode.pathString,
            month = month,
            balance = tx.balance,
            description = tx.description
          )
        }
      } catch (e: Exception) {
        logger.error(e) { "Error while processing statement query account=$accountPath month=$month" }
        throw e
      }
    }
    logger.info { "statement in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
