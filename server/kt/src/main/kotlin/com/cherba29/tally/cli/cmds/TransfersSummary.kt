package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.transfersSummary
import com.cherba29.tally.utils.asAmount
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table

class TransfersSummary : CliktCommand() {
  override fun help(context: Context) = "List of transactions within given period."

  val accountPath by argument(help = "Account path")
  val startMonth by option(
    "--start-month",
    help = "Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()
  val endMonth by option(
    "--end-month",
    help = "Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }.required()

  val width: Int by option(
    "-w",
    "--width",
    help = "Output width in characters"
  ).int().default(120)

  val tallyPath by tallyPathOption()

  override fun run() {
    val budget = Loader.loadFrom(tallyPath)
    val payload = budget.transfersSummary(accountPath, startMonth, endMonth)
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
            index + 1,
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
    echo(t.render(terminal, width = width))
  }
}
