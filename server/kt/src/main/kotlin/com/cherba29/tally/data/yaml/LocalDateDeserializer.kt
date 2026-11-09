package com.cherba29.tally.data.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import kotlinx.datetime.LocalDate

class LocalDateDeserializer : JsonDeserializer<LocalDate>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
    return LocalDate.parse(p.text)
  }
}
