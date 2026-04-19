package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Month
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class Generate : CliktCommand() {
  override fun help(context: Context) = "Full report"

  val account by argument(help = "Account name")
  val startMonth by option(
    "--start-month",
    help="Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()

  val useTransfers by option(
    "--use-transfers",
    help="Ignore balance entries and use existing transfer to compute them."
  ).flag(default = false)

  val showTransfers by option(
    "--show-transfers",
    help="Show existing transfers for debugging."
  ).flag(default = false)

  val withAnnualFlush by option(
    "--with-annual-flush",
    help="Generate annual flush transfers. (Make sure to remove existing transfers)."
  ).flag(default = false)

  override fun run() {
    echo("Generating balances for $account starting from $startMonth")
  }
}