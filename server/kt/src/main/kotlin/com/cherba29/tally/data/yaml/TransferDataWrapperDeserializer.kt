package com.cherba29.tally.data.yaml

import com.cherba29.tally.core.Month
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class TransferDataWrapperDeserializer : JsonDeserializer<TransferDataWrapper>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): TransferDataWrapper =
    if (p.isExpectedStartArrayToken) {
      TransferDataWrapper(
        p.codec.readValue(p, transfersAsList)
      )
    } else if (p.currentToken == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
      val singleData: Map<String, List<TransferYamlData>?> = p.codec.readValue(p, transfersAsMap)
      TransferDataWrapper(
        singleData.flatMap { (grpStr, transfers) ->
          transfers?.map { it.copy(grp = Month.fromString(grpStr)) } ?: listOf()
        }
      )
    } else {
      val node: JsonNode = p.codec.readTree(p)
      throw IllegalArgumentException("Expecting an object or array but got $node")
    }

  companion object {
    private val transfersAsMap = object : TypeReference<Map<String, List<TransferYamlData>?>>() {}
    private val transfersAsList = object : TypeReference<List<TransferYamlData>>() {}
  }
}
