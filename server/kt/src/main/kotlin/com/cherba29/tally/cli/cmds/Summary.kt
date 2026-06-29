package com.cherba29.tally.cli.cmds

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.combineSummaryStatements
import com.cherba29.tally.statement.SummaryStatement
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlin.collections.filter

class Summary : CliktCommand() {
  override fun help(context: Context) = "Full report"

  val tallyPath by option(envvar = "TALLY_PATH").path(mustExist = true).required()

  val account by argument(help = "Account name")
  val startMonth by option(
    "--start-month",
    help="Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()
  val endMonth by option(
    "--end-month",
    help="Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }.required()

  override fun run() {
    echo("Executing report for $account from $startMonth to $endMonth for $tallyPath")
    val payload = Loader.loadFrom(tallyPath)
    val accountPath = account.split("/")
    val summaryNode = payload.tree[accountPath]
      ?: throw IllegalStateException("Account not found $account known accounts\n${payload.tree.toPrettyString()}")

    val monthRange = startMonth..endMonth

    val summaryStatements = payload.nodeToStatement[summaryNode]!!.filter { it.key in monthRange }.values.map { it as SummaryStatement }
    if (summaryStatements.isEmpty()) {
      throw NotFoundException(
        "Summary '$account' for months [$startMonth, $endMonth] not found."
      )
    }
    // Multi-month queries will produce multiple summary statements which need to be combined,
    // but for single month we can simply return found single summary.
    val summary =
      if (summaryStatements.size == 1)
        summaryStatements.first()
      else
        combineSummaryStatements(payload.tree, summaryNode.path, summaryStatements)

    echo("name: ${summary.treeNode.path.joinToString("/")}")
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
