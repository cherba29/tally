package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.watchedEventFlow
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.io.path.extension
import kotlin.io.path.pathString

class Generate : CliktCommand() {
  override fun help(context: Context) = "Full report"

  val tallyPath by option(envvar = "TALLY_PATH").path(mustExist = true).required()

  val account by argument(help = "Account name")
  val startMonth by option(
    "--start-month",
    help = "Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }.required()

  val useTransfers by option(
    "--use-transfers",
    help = "Ignore balance entries and use existing transfer to compute them."
  ).flag(default = false)

  val showTransfers by option(
    "--show-transfers",
    help = "Show existing transfers for debugging."
  ).flag(default = false)

  val withAnnualFlush by option(
    "--with-annual-flush",
    help = "Generate annual flush transfers. (Make sure to remove existing transfers)."
  ).flag(default = false)

  override fun run() {
    echo("Generating balances for $account starting from $startMonth for $tallyPath")
    val loader = Loader(tallyPath.watchedEventFlow {
      it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
    })
    val budget = runBlocking { loader.budget() }.budget
    val acct = budget.accounts.keys.find { it.name == account } ?: throw UsageError("Account $account not found")
    val accountBalances: Map<Month, Balance> = budget.balances[acct] ?: mapOf()
    val accountTransfers: Map<Month, List<Transfer>> = budget.transfers[acct] ?: mapOf()

    // Find minimum/maximum month used by this account.
    val accountMonths = accountBalances.keys + accountTransfers.keys
    val minMonth = accountMonths.min()
    val maxMonth = accountMonths.max()
    val firstMonth = if (startMonth < minMonth) minMonth.previous() else startMonth
    if (firstMonth > maxMonth) {
      throw UsageError("The account $account has no balances or transactions after $maxMonth.")
    }
    // Compute max padding for amounts to be right aligned.
    val amountLengths: List<Int> = budget.months.toList().map {
      (budget.balances[acct]?.get(it)?.amount ?: 0).toString().length
    }
    val padAmtLength = amountLengths.max() + 2  // 1 extra leading space plus "."

    val lines = mutableListOf<String>()
    val flushLines = mutableListOf<String>()

    // For each month, compute running predicted balance and actual balance.
    var predictedBalance: Balance? = null
    for (currentMonth in firstMonth..maxMonth) {
      val recordedBalance = accountBalances[currentMonth]
      var currentBalance: Balance? =
        if (useTransfers) (predictedBalance ?: recordedBalance) else (recordedBalance ?: predictedBalance)
      if (currentBalance == null) {
        predictedBalance = null
        val prevTransfers = accountTransfers[currentMonth.previous()] ?: setOf()
        lines.add(
          "  - { grp: $currentMonth } # has no balance and had ${prevTransfers.size} transfers."
        )
        continue
      }
      lines.add(printBalanceLine(currentMonth, currentBalance, padAmtLength))
      // If there is disagreement print it as a comment.
      if (predictedBalance != null && predictedBalance.amount != currentBalance.amount) {
        val predictedAmtValue = predictedBalance.amount.asAmount().padStart(padAmtLength)
        val diffAmtValue = (currentBalance.amount - predictedBalance.amount).asAmount().padStart(padAmtLength)
        lines[lines.size - 1] += " # predicted $predictedAmtValue unaccounted $diffAmtValue"
      }
      val transfers: Set<Transfer> = accountTransfers[currentMonth]?.toSet() ?: setOf()
      if (showTransfers) {
        for (transfer in transfers) {
          lines.add(
            "    ${transfer.toMonth} ${transfer.fromAccount.nodeId.name} --> ${transfer.toAccount.nodeId.name} ${transfer.balance}"
          )
        }
      }
      // Find first date of next month transaction, our predicated balance cannot be older.
      val nextTransfers = accountTransfers[currentMonth.next()] ?: listOf()
      val minDateNextMonthTransfer: LocalDate? = if (nextTransfers.isNotEmpty()) {
        nextTransfers.maxBy { it.balance.date }.balance.date
      } else null
      if (withAnnualFlush && currentMonth.month == 0) {
        val amtValue = currentBalance.amount.asAmount().padStart(padAmtLength)
        flushLines.add(
          "    - { grp: $currentMonth, date: ${currentMonth.year}-01-02, camt: ${amtValue}, " +
              "desc: \"Total for ${currentMonth.year - 1}\" }"
        )
        currentBalance = Balance(0, currentBalance.date, currentBalance.type)
      }
      predictedBalance = nextBalance(
        account,
        currentBalance,
        transfers,
        minDateNextMonthTransfer,
        if (useTransfers) Balance.Type.CONFIRMED else Balance.Type.PROJECTED
      )
    }

    lines.reverse()
    for (line in lines) {
      echo(line)
    }
    flushLines.reverse()
    for (line in flushLines) {
      echo(line)
    }
  }

  companion object {
    private val ignorePathRegex = Regex("(^_)|(/_)")
    private fun Int.asAmount(): String = "%.2f".format(this / 100.0)

    private fun printBalanceLine(month: Month, balance: Balance, padAmtLength: Int): String {
      val amtPrefix = if (balance.type == Balance.Type.PROJECTED) "pamt" else "camt"
      val amtValue = balance.amount.asAmount().padStart(padAmtLength)
      val dateValue = balance.date.toString()
      return "  - { grp: ${month}, date: ${dateValue}, ${amtPrefix}: $amtValue }"
    }

    private fun nextBalance(
      accountName: String,
      startBalance: Balance,
      transfers: Set<Transfer>,
      minDateNextMonthTransfer: LocalDate?,
      balanceType: Balance.Type
    ): Balance? {
      //if (transfers.size === 0) { return undefined; }
      // The balance date is set to max transfer date,
      // but make sure it is not lower than next month start by default.
      val nextDate = startBalance.date + DatePeriod(months = 1)
      var balance: Balance? = Balance(startBalance.amount, nextDate, balanceType)
      for (transfer in transfers) {
        balance = if (transfer.toAccount.nodeId.name == accountName) {
          Balance.add(balance, transfer.balance)
        } else {
          Balance.subtract(balance, transfer.balance)
        }
      }
      // Make sure next balance start does not exceed its minimum transfer date.
      if (minDateNextMonthTransfer != null && balance!!.date > minDateNextMonthTransfer) {
        return Balance(balance.amount, minDateNextMonthTransfer, balance.type)
      }
      return balance
    }
  }
}