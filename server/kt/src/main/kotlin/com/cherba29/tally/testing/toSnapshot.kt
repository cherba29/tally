package com.cherba29.tally.testing

import com.cherba29.tally.data.yaml.toObjectNode
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlTable
import com.cherba29.tally.statement.TransactionStatement
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import kotlin.collections.forEach

fun List<TransactionStatement>.toSnapshot() = toSnapshot { root ->
  forEach { it.toObjectNode(root.addObject()) }
}
fun GqlSummaryData.toSnapshot() = toSnapshot { root -> toObjectNode(root.addObject()) }
fun GqlTable.toSnapshot() = toSnapshot { root -> toObjectNode(root.addObject()) }

fun toSnapshot(block: (root: ArrayNode)->Unit): String {
  val mapper = YAMLMapper.builder()
    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    .build()
  val arrayNode = mapper.createArrayNode()
  block(arrayNode)
  return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode)
}
