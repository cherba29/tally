package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.buildStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.github.oshai.kotlinlogging.KotlinLogging

class StatementService(val loader: Loader) : Query {
  @GraphQLDescription("Returns a monthly statement for given account.")
  @Suppress("unused")
  fun statement(owner:String, account: String, month: Month, dfe: DataFetchingEnvironment): GqlStatement {
    logger.info { "statement owner=$owner, account=$account, month=$month" }
    return try {
      buildStatement(loader.budget, owner, account, month)
    } catch (e: Exception) {
      logger.error(e) { "Error while processing table query owner=$owner, account=$account month=$month" }
      throw e
    }
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}