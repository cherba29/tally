package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.datetime.LocalDate

val graphqlLocalDateType: GraphQLScalarType? = GraphQLScalarType.newScalar()
  .name("Date")
  .description("Date representation in YYYY-MM-DD format.")
  .coercing(LocalDateCoercing)
  .build()

val graphqlMonthType: GraphQLScalarType? = GraphQLScalarType.newScalar()
  .name("GqlMonth")
  .description("Month representation in XxxYYYY format.")
  .coercing(MonthCoercing)
  .build()

class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
  override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
    LocalDate::class -> graphqlLocalDateType
    Month::class -> graphqlMonthType
    else -> null
  }
}
