package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.DayInMonth
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.yaml.BalanceYamlData
import com.cherba29.tally.data.yaml.YamlData
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.math.roundToLong

private fun YamlData.toAccount(): Account? {
  if (name == null) {
    // Ignore data which dont represent account.
    return null
  }
  if (path == null) {
    logger.warn { "Account '$name' has no path set" }
  }
  if (owner.isNullOrEmpty()) {
    throw IllegalArgumentException("Account '$name' has no owners")
  }
  if (desc == null) {
    logger.warn { "$name is missing description field." }
  }
  if (path.isNullOrEmpty()) {
    throw IllegalArgumentException("$name is missing path field")
  }
  if (openedOn == null) {
    throw IllegalArgumentException("$name is missing opened_on field")
  }

  return Account(
    name = name,
    path = path,
    owners = owner.toSet(),
    rank = rank,
    description = desc,
    number = number,
    openedOn = openedOn,
    closedOn = closedOn,
    statementCloseDate = DayInMonth.fromString(statementCloseDay),
    url = url,
    phone = phone,
    address = address,
    userName = username,
    password = pswd,
  )
}

private fun BalanceYamlData.toBalance(name: String): Balance {
  requireNotNull(grp)
  var amount: Long
  var balanceType: Balance.Type
  if (camt != null) {
    amount = (100.0 * camt).roundToLong()
    balanceType = Balance.Type.CONFIRMED
  } else if (pamt != null) {
    amount = (100.0 * pamt).roundToLong()
    balanceType = Balance.Type.PROJECTED
  } else {
    throw IllegalArgumentException("For $name account balance for $grp does not have amount type set, expected camt or pamt entry")
  }
  if (date == null) {
    throw IllegalArgumentException("For $name account balance for $grp does not have date set")
  }
  return Balance(amount, date, balanceType, desc ?: "")
}

/**
 * Validates YamlData and atomically adds it to the budget.
 */
private fun processYamlData(budgetBuilder: BudgetBuilder, data: YamlData, relativeFilePath: Path? = null): Boolean {
  // Ignore data which dont represent account.
  val account = data.toAccount() ?: return false
  val convertedBalances = data.balances?.associate {
    (it.grp ?: throw IllegalArgumentException(
      "For ${account.name} account balance entry with date '${it.date}' and desc '${it.desc}' has no grp setting"
    )) to it.toBalance(account.name) } ?: mapOf()

  budgetBuilder.addAccount(account, convertedBalances, relativeFilePath)

  if (data.transfersTo != null) {
    for ((toAccountName, transfersWrapper) in data.transfersTo.entries) {
      if (transfersWrapper?.transfers == null) continue
      for ((transferMonth, date, camt, pamt, desc, tags) in transfersWrapper.transfers) {
        if (transferMonth == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '$toAccountName' does not have 'grp' field"
          )
        }
        if (date == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '$toAccountName' does not have a valid 'date' field"
          )
        }
        var balance: Balance? = null
        if (pamt != null) {
          balance = Balance(
            (100 * pamt).roundToLong(),
            date,
            Balance.Type.PROJECTED
          )
        } else if (camt != null) {
          balance = Balance(
            (100 * camt).roundToLong(),
            date,
            Balance.Type.CONFIRMED
          )
        }
        if (balance == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '$toAccountName' " +
                "for $transferMonth $date " +
                "does not have 'pamt' or 'camt' field"
          )
        }

        budgetBuilder.addAccountTransfer(
          account,
          toAccountName,
          transferMonth,
          balance,
          desc,
          tags ?: listOf(),
          relativeFilePath
        )
      }
    }
  }
  return true
}

fun loadYamlFile(budgetBuilder: BudgetBuilder, accountData: YamlData, relativeFilePath: Path): Boolean {
  return try {
    processYamlData(budgetBuilder, accountData, relativeFilePath)
  } catch (e: IllegalArgumentException) {
    logger.error { e.javaClass.simpleName + ": " + e.message }
    throw BudgetLoadingException(
      message = e.message + ", while processing $relativeFilePath",
      cause = e,
      fileName = relativeFilePath.toString()
    )
  } catch (e: Exception) {
    val message = ", while processing $relativeFilePath"
    logger.error { e.javaClass.simpleName + ": " + e.message + message }
    logger.info { "Account Data$accountData" }
    throw e.toBudgetLoadingException(relativeFilePath)
  }
}

private val logger = KotlinLogging.logger {}
