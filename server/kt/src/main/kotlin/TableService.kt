package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.buildGqlTable
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query

class TableService(val loader: Loader) : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  @Suppress("unused")
  fun table(owner: String?, startMonth: Month, endMonth: Month) = buildGqlTable(loader.loadBudget(), owner, startMonth, endMonth)
}
