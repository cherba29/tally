package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.util.Locale

object MonthCoercing : Coercing<Month, String> {
  override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Month = runCatching {
    Month.fromString(serialize(input, graphQLContext, locale))
  }.getOrElse {
    throw CoercingParseValueException("Expected valid Month but was '$input'")
  }

  override fun parseLiteral(
    input: Value<*>,
    variables: CoercedVariables,
    graphQLContext: GraphQLContext,
    locale: Locale
  ): Month? {
    val monthString = (input as? StringValue)?.value ?: throw CoercingParseLiteralException("Expected valid Month literal but was '$input'")
    return runCatching {
      Month.fromString(monthString)
    }.getOrElse {
      throw CoercingParseLiteralException("Expected valid Month literal but was '$monthString'")
    }
  }

  override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String = runCatching {
    dataFetcherResult.toString()
  }.getOrElse {
    throw CoercingSerializeException("Data fetcher result '$dataFetcherResult' cannot be serialized to a String")
  }
}
