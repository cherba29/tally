package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.DataPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

fun buildStatement(payload: DataPayload, owner:String, account: String, month: Month): GqlStatement {
    val accountNode = payload.budget.accounts.values.find { it.nodeId.name == account }
    val statement = accountNode?.let { payload.statements[it.nodeId]?.get(month) }
      ?: throw NotFoundException("Did not find statement for $owner $account $month")
    return statement.toGql()
}
