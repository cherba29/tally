package com.cherba29.tally.data

import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import java.nio.file.Path

/**
 * Exception representing budget loading and validation errors.
 * Provides file and line metadata and can be converted directly to a GraphQLError.
 */
class BudgetLoadingException(
  message: String,
  cause: Throwable? = null,
  val fileName: String? = null,
  val lineNumber: Int? = null,
) : IllegalArgumentException(message, cause) {

  fun toGraphQLError(location: SourceLocation? = null, path: ResultPath? = null): GraphQLError =
    GraphqlErrorBuilder.newError()
      .message(message ?: "Budget loading error")
      .location(location)
      .path(path)
      .errorType(ErrorType.DataFetchingException)
      .extensions(
        buildMap {
          fileName?.let { put("file", it) }
          lineNumber?.let { put("line", it) }
        }
      )
      .build()
}

private val WHILE_PROCESSING_REGEX = Regex("""while processing ([^,\s]+?)(?::(\d+))?(?:,\s*|$)""")

fun Throwable.toBudgetLoadingException(defaultFile: Path? = null): BudgetLoadingException {
  if (this is BudgetLoadingException) return this
  val msg = this.message ?: "Budget loading error"
  val match = WHILE_PROCESSING_REGEX.find(msg)
  val file = match?.groups?.get(1)?.value ?: defaultFile?.toString()
  val line = match?.groups?.get(2)?.value?.toIntOrNull()
  return BudgetLoadingException(
    message = msg,
    cause = this,
    fileName = file,
    lineNumber = line
  )
}
