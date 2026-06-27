package com.cherba29.tally.data

import com.cherba29.tally.core.Month
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.datetime.LocalDate

@JsonIgnoreProperties(value = ["xamt"])
data class BalanceYamlData(
  val grp: Month?,
  val date: LocalDate?,
  val camt: Double?,
  val pamt: Double?,
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
