package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.yaml.BalanceYamlData
import com.cherba29.tally.data.yaml.YamlData
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt

private fun YamlData.toAccount(): Account? {
  if (name == null) {
    // Ignore data which dont represent account.
    return null
  }
  if (path == null) {
    logger.warn { "Account '$name' has no path set" }
  }
  if (owner == null || owner.isEmpty()) {
    throw IllegalArgumentException("Account '$name' has no owners")
  }
  if (desc == null) {
    logger.warn { "$name is missing description field." }
  }
  if (path == null || path.isEmpty()) {
    throw IllegalArgumentException("$name is missing path field.")
  }
  if (openedOn == null) {
    throw IllegalArgumentException("$name is missing opened_on field.")
  }

  return Account(
    name = name,
    path = path,
    owners = owner.toSet(),
    description = desc,
    number = number,
    openedOn = openedOn,
    closedOn = closedOn,
    url = url,
    phone = phone,
    address = address,
    userName = username,
    password = pswd,
  )
}

private fun BalanceYamlData.toBalance(name: String): Balance {
  if (grp == null) {
    throw IllegalArgumentException("Balance entry $this has no grp setting.")
  }
  var amount: Int
  var balanceType: Balance.Type
  if (camt != null) {
    amount = (100.0 * camt).roundToInt()
    balanceType = Balance.Type.CONFIRMED
  } else if (pamt != null) {
    amount = (100.0 * pamt).roundToInt()
    balanceType = Balance.Type.PROJECTED
  } else {
    throw IllegalArgumentException("Balance $this does not have amount type set, expected camt or pamt entry.")
  }
  if (date == null) {
    throw IllegalArgumentException("Balance $this does not have date set.")
  }
  val balance =  Balance(amount, date, balanceType, desc ?: "")
  val balanceMonthDiff = abs(balance.date.year * 12 + balance.date.month.ordinal - grp.year * 12 - grp.month)
  if (balanceMonthDiff > 2) {
    throw IllegalArgumentException(
      "For $name account $balance and month $grp are $balanceMonthDiff months apart (2 max)."
    )
  }
  return balance
}

// TODO: Preprocess but do not put it into budget builder yet, so warnings are only produced files that change.
private fun processYamlData(budgetBuilder: BudgetBuilder, data: YamlData): Boolean {
  // Ignore data which dont represent account.
  val account = data.toAccount() ?: return false
  for (owner in account.owners) {
    val fullPath = listOf(owner) + account.path + listOf(account.name)
    budgetBuilder.setAccount(fullPath, account)
  }
  if (data.balances != null) {
    for (balanceData in data.balances) {
      val balance = balanceData.toBalance(account.name)
      for (owner in account.owners) {
        val fullPath = listOf(owner) + account.path + listOf(account.name)
        budgetBuilder.setBalance(fullPath, balanceData.grp!!, balance)
      }
    }
  }
  if (data.transfersTo != null) {
    for ((accountName, transfers) in data.transfersTo.entries) {
      if (transfers == null) continue
      for (transferData in transfers) {
        if (transferData.grp == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '$accountName' does not have 'grp' field."
          )
        }
        if (transferData.date == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '${accountName}' does not have a valid 'date' field."
          )
        }
        var balance: Balance? = null
        if (transferData.pamt != null) {
          balance = Balance(
            (100 * transferData.pamt).roundToInt(),
            transferData.date,
            Balance.Type.PROJECTED
          )
        } else if (transferData.camt != null) {
          balance = Balance(
            (100 * transferData.camt).roundToInt(),
            transferData.date,
            Balance.Type.CONFIRMED
          )
        }
        if (balance == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '${accountName}' " +
                "does not have 'pamt' or 'camt' field: ${transferData}."
          )
        }

        val transferMonth = transferData.grp
        val balanceMonth = Month.fromDate(balance.date)
        if (abs(balanceMonth - transferMonth) > 2) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '${accountName}' " +
                "for $transferMonth date ${balance.date} (${balanceMonth}) are too far apart."
          )
        }

        for (owner in account.owners) {
          val fullPath = listOf(owner) + account.path + listOf(account.name)

          budgetBuilder.addTransfer(
            fromAccountPath = fullPath,
            fromMonth = transferMonth,
            toAccountName = accountName,
            toMonth = transferMonth,
            balance = balance,
            description = transferData.desc,
          )
        }
      }
    }
  }
  return true
}

fun loadYamlFile(budgetBuilder: BudgetBuilder, accountData: YamlData, relativeFilePath: Path): Boolean {
  return try {
    processYamlData(budgetBuilder, accountData)
  } catch (e: IllegalArgumentException) {
    logger.error { e.javaClass.simpleName + ": " + e.message }
    throw IllegalArgumentException(e.message + " while processing $relativeFilePath", e)
  } catch (e: Exception) {
    val message = " while processing $relativeFilePath"
    logger.error { e.javaClass.simpleName + ": " + e.message + message }
    logger.info { "Account Data$accountData" }
    throw e
  }
}

private val logger = KotlinLogging.logger {}
