package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query

data class GqlAccount(
  @param:GraphQLDescription("Account id/name.")
  val name: String,
  @param:GraphQLDescription("Long description for the account.")
  val description: String,
  @param:GraphQLDescription("Replacement for type, so that accounts are grouped.")
  val path: List<String>,
  //  TODO: This should change to enum, but maybe remove now that we have path.
  @param:GraphQLDescription("Account type. Determines how account is grouped.")
  val type: String,
  val external: Boolean,
  val summary: Boolean,
  @param:GraphQLDescription("Account number. Can be null or unknown for external or proxy accounts.")
  val number: String?,
  @param:GraphQLDescription("Month when account was open. Can be unknown.")
  val openedOn: Month?,
  @param:GraphQLDescription("Month when account was closed. If not set means account is still open.")
  val closedOn: Month?,
  @param:GraphQLDescription("List of owner ids for this account.")
  val owners: List<String>,
  val url: String,
  val address: String,
  val userName: String,
  val password: String,
  val phone: String,
)

data class GqlTableCell(
  val month: Month,
  val isClosed: Boolean,
  val addSub: Int,
  val balance: Int,
  val isProjected: Boolean,
  val isCovered: Boolean,
  val isProjectedCovered: Boolean,
  val hasProjectedTransfer: Boolean,
  val percentChange: Float,
  val annualizedPercentChange: Float,
  val unaccounted: Int,
  val balanced: Boolean,
)

data class GqlTableRow(
  val title: String,
  val account: GqlAccount,
  val indent: Int,
  val isSpace: Boolean,
  val isTotal: Boolean,
  val isNormal: Boolean,
  val cells: List<GqlTableCell>
)

data class GqlTable(
  val currentOwner: String,
  val owners: List<String>,
  val months: List<Month>,
  val rows: List<GqlTableRow>
)

class TableService : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  @Suppress("unused")
  fun table(owner: String, startMonth: Month, endMonth: Month): GqlTable {
    return GqlTable("abc", listOf(), listOf(), listOf())
  }
}