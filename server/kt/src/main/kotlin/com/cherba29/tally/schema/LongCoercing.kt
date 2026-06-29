package com.cherba29.tally.schema

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.util.Locale

object LongCoercing : Coercing<Long, String> {
  override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Long = runCatching {
    serialize(input, graphQLContext, locale).toLong()
  }.getOrElse {
    throw CoercingParseValueException("Expected valid Long but was '$input'")
  }

  override fun parseLiteral(
    input: Value<*>,
    variables: CoercedVariables,
    graphQLContext: GraphQLContext,
    locale: Locale
  ): Long? {
    val longString = (input as? StringValue)?.value ?: throw CoercingParseLiteralException("Expected valid Long literal but was '$input'")
    return runCatching {
      longString.toLong()
    }.getOrElse {
      throw CoercingParseLiteralException("Expected valid Month literal but was '$longString'")
    }
  }

  override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String = runCatching {
    dataFetcherResult.toString()
  }.getOrElse {
    throw CoercingSerializeException("Data fetcher result '$dataFetcherResult' cannot be serialized to a String")
  }
}
