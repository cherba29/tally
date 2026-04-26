package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.watchedEventFlow
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlin.io.path.extension
import kotlin.io.path.pathString

class Report : CliktCommand() {
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
    val loader = Loader(tallyPath.watchedEventFlow {
      it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
    })
    val payload = runBlocking { loader.budget() }
    val accounts = payload.budget.accounts
    val statementTable = payload.statements
    echo(HEADER_ROW.joinToString(","))
    for (monthStatements in statementTable.values) {
      for ((month, transactionStatement) in monthStatements) {
        val stmtAccount = accounts[transactionStatement.nodeId]
          ?: throw IllegalStateException("Account not found ${transactionStatement.nodeId}")
        if (stmtAccount.nodeId.name != account || month < startMonth || endMonth < month) {
          continue
        }
        val row = listOf(
          stmtAccount.nodeId.name,
          stmtAccount.nodeId.path.joinToString("/"),
          stmtAccount.openedOn.toString(),
          stmtAccount.closedOn?.toString() ?: "",
          if (stmtAccount.nodeId.isExternal) "T" else "F",
          if (!stmtAccount.isClosed(month)) "T" else "F",
          month.year.toString(),
          (month.month + 1).toString(),
          transactionStatement.startBalance?.amount?.asAmount() ?: "",
          if (transactionStatement.startBalance != null) {
            if (transactionStatement.startBalance?.type == Balance.Type.PROJECTED) "P" else "C"
          } else "",
          transactionStatement.endBalance?.amount?.asAmount() ?: "",
          if (transactionStatement.endBalance != null) {
            if (transactionStatement.endBalance?.type == Balance.Type.PROJECTED) "P" else "C"
          } else "",
          transactionStatement.inFlows.asAmount(),
          transactionStatement.outFlows.asAmount(),
          transactionStatement.income.asAmount(),
          transactionStatement.totalPayments.asAmount(),
          transactionStatement.totalTransfers.asAmount(),
          transactionStatement.unaccounted?.asAmount(),
        )
        echo(row.joinToString(","))
      }
    }
  }

  companion object {
    private val ignorePathRegex = Regex("(^_)|(/_)")
    val HEADER_ROW = listOf(
      "Account",
      "Path",
      "OpenedOn",
      "ClosedOn",
      "External",
      "Closed",
      "Year",
      "Month",
      "Start Amount",
      "Start Projected",
      "End Amount",
      "End Projected",
      "Inflows",
      "OutFlows",
      "Income",
      "Expense",
      "Transfers",
      "Unaccounted",
    )
    private fun Int.asAmount(): String = "%.2f".format(this / 100.0)
  }
}
