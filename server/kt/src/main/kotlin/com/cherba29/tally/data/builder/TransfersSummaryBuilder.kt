package com.cherba29.tally.data.builder

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.rangeTo
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.Profile
import com.cherba29.tally.schema.GqlMonthTransferSummary
import com.cherba29.tally.schema.GqlTransfersSummary
import com.cherba29.tally.utils.IrregularCashFlow
import com.cherba29.tally.utils.TreeNode
import com.cherba29.tally.utils.asRounded
import com.cherba29.tally.utils.asRoundedPercent

class TransfersSummaryBuilder(
  private val budget: Budget,
  private val accountPath: String,
  private val startMonth: Month? = null,
  private val endMonth: Month
) {
  fun build(): GqlTransfersSummary {
    val treePath = accountPath.split("/").filter { it.isNotEmpty() }
    val treeNode = budget.tree[treePath]
      ?: throw NotFoundException("'$accountPath' not found.")
    return build(treeNode)
  }

  fun build(treeNode: TreeNode<Profile>): GqlTransfersSummary {
    val monthlyStatements = budget.nodeToStatement[treeNode]
      ?: throw IllegalStateException("Could not find statements for $accountPath")
    val ascMonthList = monthlyStatements
      .filter {
        !budget.isClosed(treeNode, it.key)
      }.keys
      .sorted()
    if (ascMonthList.isEmpty()) {
      throw NotFoundException("Could not any non-closed statements for $accountPath between $startMonth..$endMonth")
    }
    val summaries = mutableMapOf<Month, GqlMonthTransferSummary>()

    val cashFlow = IrregularCashFlow()
    for (month in ascMonthList) {
      val statement = monthlyStatements[month] ?: continue
      val internalTransfers = statement.totalTransfers
      val externalTransfers = statement.income + statement.totalPayments
      cashFlow.add(internalTransfers, externalTransfers)

      summaries[month] = GqlMonthTransferSummary(
        internalTransfers,
        externalTransfers,
        totalMonthTransfers = internalTransfers + externalTransfers,
        totalInternalTransfers = cashFlow.totalContributions,
        totalInternalTransfersPrct = cashFlow.contributionFraction.asRoundedPercent(1),
        totalInternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnContributions().asRoundedPercent(2),
        weightedInternalAge = cashFlow.weightedContributionsAge().asRounded(2),
        totalExternalTransfers = cashFlow.totalGains,
        totalExternalTransfersPrct = cashFlow.gainsFraction.asRoundedPercent(1),
        totalExternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnGains().asRoundedPercent(2),
        weightedExternalAge = cashFlow.weightedGainsAge().asRounded(2),
        totalTransfers = cashFlow.total,
        totalAnnualPrct = cashFlow.effectiveRateOfReturn().asRoundedPercent(2),
        weightedAge = cashFlow.weightedAverageAmountAge().asRounded(2),
        unaccounted = (
          statement.startBalance?.amount
            ?: 0
        ) - cashFlow.total + internalTransfers + externalTransfers
      )
    }

    val limitMonths = (ascMonthList.first()..ascMonthList.last()).reduceTo(startMonth..endMonth)
      ?: throw NotFoundException("Not statements for $accountPath for month range $startMonth..$endMonth")
    return GqlTransfersSummary(
      months = limitMonths.reversed(),
      data = summaries
        .filterKeys { it in limitMonths }
        .toSortedMap()
        .values
        .reversed()
    )
  }
}

fun Budget.transfersSummary(
  accountPath: String,
  startMonth: Month? = null,
  endMonth: Month
): GqlTransfersSummary = TransfersSummaryBuilder(this, accountPath, startMonth, endMonth).build()
