package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.toGql
import com.cherba29.tally.statement.TransactionStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking

class StatementService(val loader: Loader) : Query {
  @GraphQLDescription("Returns a monthly statement for given account.")
  // TODO: remove owner.
  fun statement(owner:String, account: String, month: Month): GqlStatement {
    logger.info { "statement account=$account, month=$month" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val payload = runBlocking { loader.budget() }
        val accountPath = account.split("/").filter { it.isNotEmpty() }
        val accountNode = payload.tree[accountPath]
          ?: throw NotFoundException("Did not find account '$account'")
        if (accountNode !is TreeNode.Leaf) {
          throw NotFoundException("'$account' is not an account path")
        }
        val statement: TransactionStatement = payload.nodeToStatement[accountNode]?.get(month) as? TransactionStatement
          ?: throw NotFoundException("Did not find statement for month '$month' for account '$account'")
        statement.toGql()
      } catch (e: Exception) {
        logger.error(e) { "Error while processing table query account=$account month=$month" }
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
