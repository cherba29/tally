package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path

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
    val payload = Loader.loadFrom(tallyPath)
    val accountPath = account.split("/")
    val accountNode = if (accountPath.size == 1) {
      payload.getAccountNode(account)
    } else {
      payload.tree[accountPath]
    } ?: throw IllegalStateException("Account not found $account known accounts\n${payload.tree.toPrettyString()}")
    val stmtAccount = payload.getAccount(accountNode)!!

    val monthStatements = payload.nodeToStatement[accountNode] ?: mapOf()
    echo(HEADER_ROW.joinToString(","))
    val monthRange = startMonth..endMonth
    for ((month, transactionStatement) in monthStatements) {
      if (month !in monthRange) {
        continue
      }
      val row = listOf(
        stmtAccount.name,
        stmtAccount.path.joinToString("/"),
        stmtAccount.openedOn.toString(),
        stmtAccount.closedOn?.toString() ?: "",
        if (accountNode.isExternal) "T" else "F",
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

  companion object {
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
    private fun Long.asAmount(): String = "%.2f".format(this / 100.0)
  }
}
