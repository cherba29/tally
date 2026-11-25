package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.summary
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class Summary : CliktCommand() {
  override fun help(context: Context) = "Full report"

  val tallyPath by tallyPathOption()

  val account by argument(help = "Account name")
  val startMonth by option(
    "--start-month",
    help = "Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()
  val endMonth by option(
    "--end-month",
    help = "Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }.required()

  override fun run() {
    echo("Executing report for $account from $startMonth to $endMonth for $tallyPath")
    val payload = Loader.loadFrom(tallyPath)
    val (summaryNode, summary) = payload.summary(account, startMonth, endMonth)
    echo("name: ${summaryNode.path.joinToString("/")}")
    echo("month: ${summary.monthRange}")
    echo("addSub: ${summary.addSub.asAmount()}")
    echo("income: ${summary.income.asAmount()}")
    echo("change: ${summary.change?.asAmount()}")
    echo("inFlows: ${summary.inFlows.asAmount()}")
    echo("outFlows: ${summary.outFlows.asAmount()}")
    echo("percentChange: ${summary.percentChange}")
    echo("annualizedPercentChange: ${summary.annualizedPercentChange}")
    echo("totalPayments: ${summary.totalPayments.asAmount()}")
    echo("totalTransfers: ${summary.totalTransfers.asAmount()}")
    echo("unaccounted: ${summary.unaccounted?.asAmount()}")
    echo("startBalance: ${summary.startBalance}")
    echo("endBalance: ${summary.endBalance}")
  }

  companion object {
    private fun Long.asAmount(): String = "%.2f".format(this / 100.0)
  }
}
