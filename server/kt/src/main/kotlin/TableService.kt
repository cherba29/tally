package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.schema.GqlTable
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query

class TableService : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  @Suppress("unused")
  fun table(owner: String, startMonth: Month, endMonth: Month): GqlTable {
    return GqlTable("abc", listOf(), listOf(), listOf())
  }
}