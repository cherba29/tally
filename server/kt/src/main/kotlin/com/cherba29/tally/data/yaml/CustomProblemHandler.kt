package com.cherba29.tally.data.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler

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
      yamlIgnoredFields.add(p.parsingContext.pathAsPointer(false).toString())
    }
    p.skipChildren()  // Skip the unknown property's value
    return true  // Mark as handled
  }
}
