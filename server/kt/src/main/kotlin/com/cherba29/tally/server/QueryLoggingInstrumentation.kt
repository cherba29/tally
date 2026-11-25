package com.cherba29.tally.server

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Instrumentation that logs GraphQL query execution latency, operation details, and error summaries.
 */
class QueryLoggingInstrumentation : SimplePerformantInstrumentation() {
  override fun beginExecution(
    parameters: InstrumentationExecutionParameters,
    state: InstrumentationState?
  ): InstrumentationContext<ExecutionResult> {
    val startNano = System.nanoTime()
    val operation = parameters.operation ?: parameters.executionInput.operationName ?: "query"
    val firstLine = parameters.query
      ?.lineSequence()
      ?.firstOrNull { it.isNotBlank() }
      ?.trim() ?: "unknown"
    logger.info { "--> GraphQL execution started: op='$operation', query='$firstLine'" }

    return object : InstrumentationContext<ExecutionResult> {
      override fun onDispatched() {}

      override fun onCompleted(
        result: ExecutionResult?,
        t: Throwable?
      ) {
        val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
        val formattedDuration = String.format("%.2f", durationMs)
        if (t != null) {
          logger.error(t) { "<-- GraphQL execution failed in ${formattedDuration}ms: ${t.message}" }
        } else if (result != null && result.errors.isNotEmpty()) {
          val errorMessages = result.errors.joinToString(", ") { it.message ?: "unknown" }
          logger.warn {
            "<-- GraphQL execution completed with ${result.errors.size} error(s) in ${formattedDuration}ms: " +
              errorMessages
          }
        } else {
          logger.info { "<-- GraphQL execution completed successfully in ${formattedDuration}ms" }
        }
      }
    }
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
