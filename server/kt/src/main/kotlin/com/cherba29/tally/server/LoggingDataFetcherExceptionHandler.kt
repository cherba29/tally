package com.cherba29.tally.server

import com.cherba29.tally.data.BudgetLoadingException
import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * GraphQL exception handler that logs exceptions with full contextual details (path, field, arguments)
 * and preserves specific GraphQLErrors for standard GraphQL error reporting.
 */
class LoggingDataFetcherExceptionHandler : DataFetcherExceptionHandler {
  override fun handleException(
    handlerParameters: DataFetcherExceptionHandlerParameters
  ): CompletableFuture<DataFetcherExceptionHandlerResult> {
    val exception = unwrap(handlerParameters.exception)
    val sourceLocation = handlerParameters.sourceLocation
    val path = handlerParameters.path
    val fieldName = handlerParameters.fieldDefinition?.name ?: "unknown"
    val argumentValues = handlerParameters.argumentValues

    logger.error(exception) {
      "GraphQL fetch error on field '$fieldName' at path '$path' with arguments $argumentValues " +
        "($sourceLocation): ${exception.message}"
    }

    val error: GraphQLError = when (exception) {
      is BudgetLoadingException -> exception.toGraphQLError(sourceLocation, path)
      is GraphQLError ->
        GraphqlErrorBuilder
          .newError()
          .message(exception.message ?: "GraphQL Error")
          .location(sourceLocation)
          .path(path)
          .errorType(exception.errorType ?: ErrorType.DataFetchingException)
          .extensions(exception.extensions)
          .build()
      else -> ExceptionWhileDataFetching(path, exception, sourceLocation)
    }

    val result = DataFetcherExceptionHandlerResult.newResult().error(error).build()
    return CompletableFuture.completedFuture(result)
  }

  private fun unwrap(exception: Throwable): Throwable {
    var current = exception
    while ((current is CompletionException || current is InvocationTargetException) &&
      current.cause != null
    ) {
      current = current.cause!!
    }
    return current
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
