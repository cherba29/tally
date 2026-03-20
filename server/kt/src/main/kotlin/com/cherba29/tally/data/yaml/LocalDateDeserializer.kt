package com.cherba29.tally.data.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.datetime.LocalDate

class LocalDateDeserializer : JsonDeserializer<LocalDate>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
    val node: JsonNode = p.codec.readTree(p)
    return LocalDate.parse(node.asText())
  }
}
