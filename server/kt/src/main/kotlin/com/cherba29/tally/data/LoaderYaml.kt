package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.yaml.CustomProblemHandler
import com.cherba29.tally.data.yaml.LocalDateDeserializer
import com.cherba29.tally.data.yaml.MonthDeserializer
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

@JsonIgnoreProperties(value = ["xamt"])
data class BalanceYamlData(
  val grp: Month?,
  val date: LocalDate?,
  val camt: Double?,
  val pamt: Double?,
  // TODO: wire it in.
  val desc: String?,
)

// TODO: add checks for duplicate keys, eg: two desc fields are provided.
data class TransferYamlData(
  val grp: Month?,
  val date: LocalDate?,
  val camt: Double?,
  val pamt: Double?,
  val desc: String?,
  // TODO: choose cat or tags and wire it in.
  val cat: String?,
  val tags: List<String>?,
  // TODO: add option for running annual total.
  // TODO: add support for confirmation number to display it.
)

@JsonIgnoreProperties(value = [])
data class YamlData(
  val name: String?,
  val desc: String?,
  val number: String?,
  val path: List<String>?,
  val type: String?,
  @param:JsonProperty("opened_on")
  val openedOn: Month?,
  @param:JsonProperty("closed_on")
  val closedOn: Month?,
  val owner: List<String>?,
  val url: String?,
  val phone: String?,
  val address: String?,
  val username: String?,
  val pswd: String?,
  val balances: List<BalanceYamlData>?,
  @param:JsonProperty("transfers_to")
  val transfersTo: Map<String, List<TransferYamlData>?>?,
)

private fun BalanceYamlData.toBalance(): Balance {
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
  return Balance(amount, date, balanceType)
}

// TODO: Preprocess but do not put it into budget builder yet, so warnings are only produced files that change.
fun processYamlData(budgetBuilder: BudgetBuilder, data: YamlData): Boolean {
  if (data.name == null) {
    // Ignore data which dont represent account.
    return false
  }
  if (data.path == null) {
    logger.warn { "Account '${data.name}' has no path set" }
  }
  if (data.owner == null || data.owner.isEmpty()) {
    throw IllegalArgumentException("Account '${data.name}' has no owners")
  }
  if (data.desc == null) {
    logger.warn { "${data.name} is missing description field." }
  }
  if (data.path == null || data.path.isEmpty()) {
    throw IllegalArgumentException("${data.name} is missing path field.")
  }
  if (data.openedOn == null) {
    throw IllegalArgumentException("${data.name} is missing opened_on field.")
  }

  val account = Account(
    nodeId = NodeId(data.name, data.owner.toSet(), data.path),
    description = data.desc,
    number = data.number,
    openedOn = data.openedOn,
    closedOn = data.closedOn,
    url = data.url,
    phone = data.phone,
    address = data.address,
    userName = data.username,
    password = data.pswd,
  )
  budgetBuilder.setAccount(account)
  if (data.balances != null) {
    for (balanceData in data.balances) {
      if (balanceData.grp == null) {
        throw IllegalArgumentException("Balance entry $balanceData has no grp setting.")
      }
      var month: Month
      try {
        month = balanceData.grp
      } catch (e: Exception) {
        throw IllegalArgumentException("Balance $balanceData has bad grp setting: ${e.message}")
      }
      val balance = balanceData.toBalance()
      val balanceMonthDiff = abs(balance.date.year * 12 + balance.date.month.ordinal - month.year * 12 - month.month)
      if (balanceMonthDiff > 2) {
        throw IllegalArgumentException(
          "For ${account.nodeId} account $balance and month $month are $balanceMonthDiff months apart (2 max)."
        )
      }
      budgetBuilder.setBalance(account.nodeId, month, balance)
    }
  }
  if (data.transfersTo != null) {
    for ((accountName, transfers) in data.transfersTo.entries) {
      if (transfers == null) continue
      for (transferData in transfers) {
        if (transferData.grp == null) {
          throw IllegalArgumentException(
            "For account '${account.nodeId}' transfer to '$accountName' does not have 'grp' field."
          )
        }
        if (transferData.date == null) {
          throw IllegalArgumentException(
            "For account '${account.nodeId}' transfer to '${accountName}' does not have a valid 'date' field."
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
            "For account '${account.nodeId}' transfer to '${accountName}' " +
                "does not have 'pamt' or 'camt' field: ${transferData}."
          )
        }

        val transferMonth = transferData.grp
        val balanceMonth = Month.fromDate(balance.date)
        if (abs(balanceMonth - transferMonth) > 2) {
          throw IllegalArgumentException(
            "For account '${account.nodeId}' transfer to '${accountName}' " +
                "for $transferMonth date ${balance.date} (${balanceMonth}) are too far apart."
          )
        }

        budgetBuilder.addTransfer(
          fromAccount = account.nodeId,
          fromMonth = transferMonth,
          toAccount = accountName,
          toMonth = transferMonth,
          balance = balance,
          description = transferData.desc,
        )
      }
    }
  }
  return true
}

// TODO: make it a class so mapper does not have to be instantiated every time.
fun parseYamlContent(content: String, relativeFilePath: Path): YamlData {
  val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  val module = SimpleModule()
  module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())
  module.addDeserializer(Month::class.java, MonthDeserializer())
  mapper.registerModule(module)
  val problemHandler = CustomProblemHandler()
  mapper.addHandler(problemHandler)
  val result = try {
    mapper.readValue(content, YamlData::class.java)
  } catch (e: JsonMappingException) {
    logger.error { "Failed to parse $relativeFilePath: ${e.message}" }
    throw IllegalArgumentException(e.message + " while processing $relativeFilePath", e)
  }
  if (problemHandler.ignoredFields.isNotEmpty()) {
    logger.warn { "Unknown ignored fields: ${problemHandler.ignoredFields} in $relativeFilePath" }
  }
  return result
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
