package com.cherba29.tally.data.yaml

import com.cherba29.tally.core.Month
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class TransferDataWrapperDeserializer : JsonDeserializer<TransferDataWrapper>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TransferDataWrapper {
    val node: JsonNode = p.codec.readTree(p)
    return if (node.isObject) {
      val singleData = p.codec.readValue(node.traverse(p.codec), transfersAsMap)
      TransferDataWrapper(
        singleData.flatMap { (grpStr, transfers) ->
          transfers?.map { it.copy(grp = Month.fromString(grpStr)) } ?: listOf()
        }
      )
    } else if (node.isArray) {
      TransferDataWrapper(
        p.codec.readValue(node.traverse(p.codec), transfersAsList)
      )
    } else {
      throw IllegalArgumentException("Expecting an object or array but got $node")
    }
  }

  companion object {
    private val transfersAsMap = object : TypeReference<Map<String, List<TransferYamlData>?>>() {}
    private val transfersAsList = object : TypeReference<List<TransferYamlData>>() {}
  }
}
