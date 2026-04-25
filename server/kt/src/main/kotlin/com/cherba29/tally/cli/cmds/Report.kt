package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class Report : CliktCommand() {
  override fun help(context: Context) = "Full report"

  val account by argument(help = "Account name")
  val startMonth by option("--start-month", help="Inclusive start month, eg Apr2026").required()
  val endMonth by option("--end-month", help="Inclusive end month, eg May2026").required()

  override fun run() {
    echo("Executing report for $account from $startMonth to $endMonth")
  }
}