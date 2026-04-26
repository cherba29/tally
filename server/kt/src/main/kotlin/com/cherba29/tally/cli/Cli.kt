package com.cherba29.tally.cli

import com.cherba29.tally.cli.cmds.Generate
import com.cherba29.tally.cli.cmds.Report
import com.cherba29.tally.cli.cmds.Transactions
import com.cherba29.tally.cli.cmds.Unaccounted
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Cli : CliktCommand() {
  val verbose by option().flag("--no-verbose")
  override fun run() {
    echo("Verbose mode is ${if (verbose) "on" else "off"}")
  }
}

fun main(args: Array<String>) = Cli().subcommands(
  Generate(),
  Report(),
  Transactions(),
  Unaccounted()
).main(args)
