package com.cherba29.tally

import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.buildStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment

class StatementService(val loader: Loader) : Query {
  @GraphQLDescription("Returns a monthly statement for given account.")
  @Suppress("unused")
  fun statement(owner:String, account: String, month: String, dfe: DataFetchingEnvironment): GqlStatement {
    return buildStatement(loader.loadBudget(), owner,account, month)
  }
}