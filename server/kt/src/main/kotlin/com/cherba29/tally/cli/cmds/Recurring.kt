package com.cherba29.tally.cli.cmds

import com.cherba29.tally.data.Loader
import com.cherba29.tally.recurring.RecurringGroup
import com.cherba29.tally.utils.asAmount
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Recurring : CliktCommand() {
  override fun help(context: Context) = "List detected recurring transaction groups."

  val tallyPath by tallyPathOption()

  val owner: String? by option(
    "--owner",
    help = "Filter recurring groups by owner name"
  )

  val account: String? by option(
    "--account",
    help = "Filter recurring groups by source or destination account name or path"
  )

  val verbose: Boolean by option(
    "-v",
    "--verbose",
    help = "Include individual occurrences for each recurring group"
  ).flag(default = false)

  override fun run() {
    val budget = Loader.loadFrom(tallyPath)
    val matchingGroups = budget.recurringIndex.groups
      .filter { matchesFilters(it) }
      .sortedWith(
        compareByDescending<RecurringGroup> { it.occurrenceCount }
          .thenByDescending { it.averageAmount }
          .thenBy { it.label }
      )

    if (matchingGroups.isEmpty()) {
      echo("No recurring transaction groups found.")
      return
    }

    for (group in matchingGroups) {
      val dayPart = group.typicalDayOfMonth?.let { " (~day $it)" } ?: ""
      val amountPart = if (group.isFixedAmount) {
        "Fixed ${group.averageAmount.asAmount()}"
      } else {
        "Avg ${group.averageAmount.asAmount()} (${group.minAmount.asAmount()}..${group.maxAmount.asAmount()})"
      }
      val routes = group.occurrences
        .map { "${it.fromAccountName} -> ${it.toAccountName}" }
        .distinct()
        .joinToString(", ")

      echo(
        "[${group.id}] ${group.label} | ${group.cadence.name}$dayPart | " +
          "${group.occurrenceCount}x | $amountPart | $routes"
      )

      if (verbose) {
        for (occ in group.occurrences) {
          echo(
            "  ${occ.balance.date}  ${occ.month.toString().padEnd(8)}  " +
              "${occ.balance.amount.asAmount().padStart(10)}  " +
              "${occ.balance.type.id.padEnd(9)}  " +
              "${occ.fromAccountName} -> ${occ.toAccountName}  ${occ.description}"
          )
        }
      }
    }
  }

  private fun matchesFilters(group: RecurringGroup): Boolean {
    if (owner != null) {
      val matchesOwner = group.occurrences.any { occ ->
        occ.fromAccountPath.substringBefore('/') == owner ||
          occ.toAccountPath.substringBefore('/') == owner
      }
      if (!matchesOwner) return false
    }
    if (account != null) {
      val normalizedAccount = account!!.trim('/')
      val matchesAccount = group.occurrences.any { occ ->
        occ.fromAccountName == normalizedAccount ||
          occ.toAccountName == normalizedAccount ||
          occ.fromAccountPath == normalizedAccount ||
          occ.toAccountPath == normalizedAccount
      }
      if (!matchesAccount) return false
    }
    return true
  }
}
