package com.cherba29.tally.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException

private val logger = KotlinLogging.logger {}

/**
 * Ktor plugin that monitors HTTP requests and responses with latency measurement and error tracking.
 */
val RequestLoggingPlugin = createApplicationPlugin("RequestLogging") {
  val callStartTime = AttributeKey<Long>("CallStartTime")

  on(CallSetup) { call ->
    call.attributes.put(callStartTime, System.currentTimeMillis())
    call.request.origin.apply {
      logger.info { "--> ${call.request.httpMethod.value} $uri ($scheme://$localHost:$localPort)" }
    }
  }

  on(ResponseSent) { call ->
    val start = call.attributes.getOrNull(callStartTime)
    val durationMs = if (start != null) System.currentTimeMillis() - start else -1
    val status = call.response.status()?.value ?: 200
    logger.info { "<-- ${call.request.httpMethod.value} ${call.request.uri} -> $status in ${durationMs}ms" }
  }

  on(CallFailed) { call, cause ->
    val start = call.attributes.getOrNull(callStartTime)
    val durationMs = if (start != null) System.currentTimeMillis() - start else -1
    if (cause is CancellationException) {
      logger.debug { "<-- ${call.request.httpMethod.value} ${call.request.uri} -> cancelled in ${durationMs}ms" }
    } else {
      logger.error(cause) {
        "<-- ${call.request.httpMethod.value} ${call.request.uri} -> FAILED in ${durationMs}ms: ${cause.message}"
      }
    }
  }
}
