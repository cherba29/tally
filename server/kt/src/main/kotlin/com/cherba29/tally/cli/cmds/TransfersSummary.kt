package com.cherba29.tally.cli.cmds

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlMonthTransferSummary
import com.cherba29.tally.schema.GqlTransfersSummary
import com.cherba29.tally.utils.IrregularCashFlow
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlin.math.pow
import kotlin.math.round

class TransfersSummary : CliktCommand() {
  override fun help(context: Context) = "List of transactions within given period."

  val accountPath by argument(help = "Account path")
  val startMonth by option(
    "--start-month",
    help="Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()
  val endMonth by option(
    "--end-month",
    help="Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }.required()

  val tallyPath by option(envvar = "TALLY_PATH").path(mustExist = true).required()

  private val terminal = Terminal(width = 120)

  override fun run() {
    val treePath = accountPath.split("/").filter { it.isNotEmpty() }
    val budget = Loader.loadFrom(tallyPath)
    val treeNode = budget.tree[treePath]
      ?: throw NotFoundException("'$accountPath' not found.")

    val monthlyStatements = budget.nodeToStatement[treeNode]
      ?: throw IllegalStateException("Could not find statements for $accountPath")
    val ascMonthList = monthlyStatements.filterValues { !it.isClosed }.keys.sorted()
    val summaries = mutableMapOf<Month, GqlMonthTransferSummary>()

    // TODO: Unify this logic with SummaryService.
    val cashFlow = IrregularCashFlow()
    for (month in ascMonthList) {
      val statement = monthlyStatements[month] ?: continue
      val internalTransfers = statement.totalTransfers
      val externalTransfers = statement.income + statement.totalPayments
      cashFlow.add(internalTransfers, externalTransfers)

      // TODO: probably should not use GQL types here.
      summaries[month] = GqlMonthTransferSummary(
        internalTransfers,
        externalTransfers,
        totalMonthTransfers = internalTransfers + externalTransfers,
        totalInternalTransfers = cashFlow.totalContributions,
        totalInternalTransfersPrct = cashFlow.contributionFraction.asRoundedPercent(1),
        totalInternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnContributions().asRoundedPercent(2),
        totalExternalTransfers = cashFlow.totalGains,
        totalExternalTransfersPrct = cashFlow.gainsFraction.asRoundedPercent(1),
        totalExternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnGains().asRoundedPercent(2),
        totalTransfers = cashFlow.total,
        totalAnnualPrct = cashFlow.effectiveRateOfReturn().asRoundedPercent(2),
        unaccounted = (statement.startBalance?.amount ?: 0) - cashFlow.total + internalTransfers + externalTransfers,
      )
    }

    val limitMonths = (ascMonthList.first()..ascMonthList.last()).reduceTo(startMonth..endMonth)
      ?: throw NotFoundException("Not statements for $accountPath for month range $startMonth..$endMonth")
    val payload = GqlTransfersSummary(
      months = limitMonths.reversed(),
      data = summaries.filterKeys { it in limitMonths }.toSortedMap().values.reversed()
    )
    val t = table {
      align = TextAlign.RIGHT
      tableBorders = Borders.NONE
      header {
        row(
          "Idx",
          "Month",
          "Internal",
          "External",
          "Change",
          "Tot Int",
          "Int %",
          "Int A %",
          "Tot Ext",
          "Ext %",
          "Ext A %",
          "Total",
          "Tot A %",
          "Unaccounted"
        )
      }
      body {
        for ((index, month) in payload.months.withIndex()) {
          val summary = payload.data[index]
          row(
            index+1,
            month,
            summary.internalTransfers.asAmount(),
            summary.externalTransfers.asAmount(),
            summary.totalMonthTransfers.asAmount(),
            summary.totalInternalTransfers.asAmount(),
            summary.totalInternalTransfersPrct,
            summary.totalInternalTransfersAnnualPrct,
            summary.totalExternalTransfers.asAmount(),
            summary.totalExternalTransfersPrct,
            summary.totalExternalTransfersAnnualPrct,
            summary.totalTransfers.asAmount(),
            summary.totalAnnualPrct,
            summary.unaccounted.asAmount()
          )
        }
      }
    }
    terminal.println(t)
  }


  companion object {
    // TODO: collect these formatting utilities somewhere centrally so not to repeat.
    private fun Long.asAmount(): String = "%.2f".format(this / 100.0)

    fun Double.asRoundedPercent(decimalPlaces: Int): Float {
      val roundingFactor = 10.0.pow(decimalPlaces)
      return (round(100.0 * this * roundingFactor) / roundingFactor).toFloat()
    }
  }
}
