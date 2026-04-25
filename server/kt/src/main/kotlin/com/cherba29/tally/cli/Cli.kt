package com.cherba29.tally.cli

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

class Execute : CliktCommand() {
  override fun run() {
    echo("executing")
  }
}

fun main(args: Array<String>) = Cli().subcommands(Execute()).main(args)
