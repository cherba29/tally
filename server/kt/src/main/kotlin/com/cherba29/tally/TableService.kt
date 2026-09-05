package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlTable
import com.cherba29.tally.schema.GqlTableRow
import com.cherba29.tally.schema.toGql
import com.cherba29.tally.schema.toGqlTableCell
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking

class TableService(val loader: Loader) : Query {
  @GraphQLDescription("Generates full tally table in given month range.")
  fun table(owner: String?, startMonth: Month, endMonth: Month): GqlTable {
    logger.info { "table owner=$owner startMonth=$startMonth endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        buildGqlTable(runBlocking { loader.budget() }, owner, startMonth, endMonth)
      } catch (e: Exception) {
        logger.error(e) { "Error while processing table query owner=$owner, startMont=$startMonth, endMonth=$endMonth" }
        throw e
      }
    }
    logger.info { "Computed table in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    private fun buildGqlTable(payload: Budget, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
      val requestedMonths = payload.months.reduceTo(startMonth..endMonth)?.sortedDescending()
      if (requestedMonths.isNullOrEmpty()) {
        throw IllegalArgumentException(
          "Bad month range, budget has ${payload.months} yet ${startMonth..endMonth} was requested"
        )
      }
      val owners = payload.tree.children.map { it.name }.sorted()
      val forOwner = if (owner.isNullOrEmpty()) {
        owners.firstOrNull { !it.isEmpty() }
          ?: throw IllegalArgumentException("No owner is specified and one cannot be derived from accounts")
      } else {
        owner
      }
      val ownerTree = payload.tree[forOwner]
        ?: throw java.lang.IllegalArgumentException(
          "Owner $forOwner is not in account tree, known owners ${payload.tree.children.joinToString { it.name }}"
        )

      val rows = mutableListOf<GqlTableRow>()
      for (treeNode in ownerTree.traverseDepthDown()) {
        val account = payload.leafToAccount[treeNode]
        val monthMap = payload.nodeToStatement[treeNode]
          ?: throw java.lang.IllegalArgumentException(
            "Did not find monthly statements at '${treeNode.path.joinToString("/")}'"
          )

        val cells = requestedMonths.map { month ->
          monthMap[month]?.toGqlTableCell(payload.isClosed(treeNode, month))
            ?: throw IllegalStateException(
              "Missing statement for ${treeNode.path.joinToString("/")} for month $month, " +
                  "only available ${monthMap.keys}")
        }

        if (cells.any { c -> !c.isClosed }) {
          rows.add(
            GqlTableRow(
              id = treeNode.path.joinToString("/"),
              title = treeNode.name,
              indent = treeNode.path.size - 1,
              account = account?.toGql(treeNode.data.isExternal, treeNode.children.isNotEmpty()),
              isTotal = treeNode.children.isNotEmpty(),
              cells = cells,
              isSpace = false,
              isInactive = treeNode.data.isInactive,
              isNormal = treeNode.children.isEmpty(),
            )
          )
        }
      }
      return GqlTable(currentOwner = forOwner, owners, requestedMonths, rows)
    }
  }
}
