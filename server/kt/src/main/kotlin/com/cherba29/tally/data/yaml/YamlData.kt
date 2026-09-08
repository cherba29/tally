package com.cherba29.tally.data.yaml

import com.cherba29.tally.core.Month
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDate

@JsonIgnoreProperties(value = ["xamt"])
data class BalanceYamlData(
  val grp: Month? = null,
  val date: LocalDate? = null,
  val camt: Double? = null,
  val pamt: Double? = null,
  val desc: String? = null,
)

data class TransferYamlData(
  val grp: Month? = null,
  val date: LocalDate? = null,
  val camt: Double? = null,
  val pamt: Double? = null,
  val desc: String? = null,
  val tags: List<String>? = null,
)

@JsonIgnoreProperties(value = [])
data class YamlData(
  val name: String? = null,
  val desc: String? = null,
  val rank: Int? = null,
  val number: String? = null,
  val path: List<String>? = null,
  @param:JsonProperty("opened_on")
  val openedOn: Month? = null,
  @param:JsonProperty("closed_on")
  val closedOn: Month? = null,
  val owner: List<String>? = null,
  @param:JsonProperty("statement_close_day")
  val statementCloseDay: String? = null,
  val url: String? = null,
  val phone: String? = null,
  val address: String? = null,
  val username: String? = null,
  val pswd: String? = null,
  val balances: List<BalanceYamlData>? = null,
  @param:JsonProperty("transfers_to")
  val transfersTo: Map<String, TransferDataWrapper?>? = null,
)
