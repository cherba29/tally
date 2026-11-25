package com.cherba29.tally.data.yaml

import com.cherba29.tally.core.Month
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class MonthDeserializer : JsonDeserializer<Month>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): Month = Month.fromString(p.text)
}
