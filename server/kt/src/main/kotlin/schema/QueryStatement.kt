package com.cherba29.tally.schema

import com.cherba29.tally.data.DataPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

fun buildStatement(payload: DataPayload, owner:String, account: String, month: String): GqlStatement {
  val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()

  val statement = payload.statements[account]?.get(month)
  if (statement == null) {
    throw NotFoundException("Did not find statement for $owner $account $month")
  }
  val result = statement.toGql()
  logger.info { "gql statement in ${Clock.System.now().toEpochMilliseconds() - startTimeMs}ms" }
  return result
}

private val logger = KotlinLogging.logger {}
