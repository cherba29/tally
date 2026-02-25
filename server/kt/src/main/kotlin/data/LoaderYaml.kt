package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.AccountType
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.TransferData
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

@JsonIgnoreProperties(value = ["xamt"])
data class BalanceData(
  val grp: String?,
  val date: LocalDate?,
  val camt: Double?,
  val pamt: Double?,
)

data class TransferYamlData(
  val grp: String?,
  val date: LocalDate?,
  val camt: Double?,
  val pamt: Double?,
  val desc: String?,
)

@JsonIgnoreProperties(value = [])
data class YamlData(
  val name: String?,
  val desc: String?,
  val number: String?,
  val path: List<String>?,
  val type: String?,
  @param:JsonProperty("opened_on")
  val openedOn: String?,
  @param:JsonProperty("closed_on")
  val closedOn: String?,
  val owner: List<String>?,
  val url: String?,
  val phone: String?,
  val address: String?,
  val username: String?,
  val pswd: String?,
  val balances: List<BalanceData>?,
  @param:JsonProperty("transfers_to")
  val transfersTo: Map<String, List<TransferYamlData>?>?,
)

fun lookupAccountType(type: String): AccountType? = AccountType.entries.find { it.id == type }

fun makeBalance(data: BalanceData): Balance {
  var amount: Int
  var balanceType: BalanceType
  if (data.camt != null) {
    amount = (100.0 * data.camt).roundToInt()
    balanceType = BalanceType.CONFIRMED
  } else if (data.pamt != null) {
    amount = (100.0 * data.pamt).roundToInt()
    balanceType = BalanceType.PROJECTED
  } else {
    throw IllegalArgumentException("Balance $data does not have amount type set, expected camt or pamt entry.")
  }
  if (data.date == null) {
    throw IllegalArgumentException("Balance $data does not have date set.")
  }
  return Balance(amount, data.date, balanceType)
}

fun processYamlData(budgetBuilder: BudgetBuilder, data: YamlData) {
  if (data.name == null) {
    // Ignore data which dont represent account.
    return
  }
  val accountType = if (data.type == null) AccountType.UNSPECIFIED else lookupAccountType(data.type)
  if (accountType == null) {
    throw IllegalArgumentException("Unknown type '${data.type}' for account '${data.name}'")
  }
  if (data.path == null) {
    logger.warn { "Account '${data.name}' has no path set" }
  }
  if (data.owner == null || data.owner.isEmpty()) {
    throw IllegalArgumentException("Account '${data.name}' has no owners")
  }
  val account = Account(
    name = data.name,
    description = data.desc,
    path = data.path ?: listOf(),
    type = accountType,
    number = data.number,
    openedOn = if (data.openedOn != null) Month.fromString(data.openedOn) else null,
    closedOn = if (data.closedOn != null) Month.fromString(data.closedOn) else null,
    owners = data.owner,
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
        month = Month.fromString(balanceData.grp)
      } catch (e: Exception) {
        throw IllegalArgumentException("Balance $balanceData has bad grp setting: ${e.message}")
      }
      val balance = makeBalance(balanceData)
      val balanceMonthDiff = abs(balance.date.year * 12 + balance.date.month.ordinal - month.year * 12 - month.month)
      if (balanceMonthDiff > 2) {
        throw IllegalArgumentException(
          "For ${account.name} account $balance and month $month are $balanceMonthDiff months apart (2 max)."
        )
      }
      budgetBuilder.setBalance(account.name, month.toString(), balance)
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
            BalanceType.PROJECTED
          )
        } else if (transferData.camt != null) {
          balance = Balance(
            (100 * transferData.camt).roundToInt(),
            transferData.date,
            BalanceType.CONFIRMED
          )
        }
        if (balance == null) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '${accountName}' " +
                "does not have 'pamt' or 'camt' field: ${transferData}."
          )
        }

        val transferMonth = Month.fromString(transferData.grp)
        val balanceMonth = Month.fromDate(balance.date)
        if (abs(balanceMonth - transferMonth) > 2) {
          throw IllegalArgumentException(
            "For account '${account.name}' transfer to '${accountName}' " +
                "for $transferMonth date ${balance.date} (${balanceMonth}) are too far apart."
          )
        }
        val transfer = TransferData(
          fromAccount = account.name,
          fromMonth = transferMonth,
          toAccount = accountName,
          toMonth = transferMonth,
          balance = balance,
          description = transferData.desc,
        )

        budgetBuilder.addTransfer(transfer)
      }
    }
  }
}

class LocalDateDeserializer : JsonDeserializer<LocalDate>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
    val node: JsonNode = p.codec.readTree(p)
    return LocalDate.parse(node.asText())
  }
}

class CustomProblemHandler : DeserializationProblemHandler() {
  private val yamlIgnoredFields = mutableListOf<String>()

  val ignoredFields: List<String> get() = yamlIgnoredFields

  override fun handleUnknownProperty(
    ctxt: DeserializationContext,
    p: JsonParser,
    deserializer: JsonDeserializer<*>,
    beanOrClass: Any,
    propertyName: String?
  ): Boolean {
    if (propertyName != null) {
      yamlIgnoredFields.add(propertyName)
    }
    // Return true to tell Jackson to ignore the field and continue deserialization
    return true
  }
}

// TODO: make it a class so mapper does not have to be instantiated every time.
fun parseYamlContent(content: String, relativeFilePath: Path): YamlData? {
  val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  val module = SimpleModule()
  module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())
  mapper.registerModule(module)
  val problemHandler = CustomProblemHandler()
  mapper.addHandler(problemHandler)
  val result = try {
    mapper.readValue(content, YamlData::class.java)
  } catch (e: JsonMappingException) {
    throw IllegalArgumentException(e.message + " while processing $relativeFilePath", e)
  }
  if (problemHandler.ignoredFields.isNotEmpty()) {
    logger.warn { "Unknown ignored fields: ${problemHandler.ignoredFields}" }
  }
  return result
}

fun loadYamlFile(budgetBuilder: BudgetBuilder, accountData: YamlData, relativeFilePath: Path) {
  try {
    processYamlData(budgetBuilder, accountData)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException(e.message + " while processing $relativeFilePath", e)
  } catch (e: Exception) {
    val message = " while processing $relativeFilePath"
    logger.error { e.javaClass.simpleName + ": " + e.message + message }
    logger.info { "Account Data$accountData" }
    throw e
  }
}

private val logger = KotlinLogging.logger {}
