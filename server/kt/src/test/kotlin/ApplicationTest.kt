package com.cherba29.tally

import com.expediagroup.graphql.server.types.GraphQLRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals


class ApplicationTest {
  @Test
  fun testRoot() = testApplication {
    application {
      graphQLModule()
    }
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello gql World!", response.bodyAsText())
  }

  @Test
  fun `server should return Bad Request for invalid GET requests`() {
    testApplication {
      application {
        graphQLModule()
      }
      val response = client.get("/graphql")
      assertEquals(HttpStatusCode.BadRequest, response.status)
    }
  }

  @Test
  fun `server should return Method Not Allowed for Mutation GET requests`() {
    testApplication {
      application {
        graphQLModule()
      }
      val response = client.get("/graphql") {
        parameter("query", "mutation { foo }")
      }
      assertEquals(HttpStatusCode.MethodNotAllowed, response.status)
    }
  }

  @Test
  fun `server should handle valid POST requests`() = testApplication {
    application {
      graphQLModule()

    }
    val client = createClient {
      install(ContentNegotiation) {
        jackson()
      }
    }
    val response = client.post("/graphql") {
      contentType(ContentType.Application.Json)
      setBody(GraphQLRequest(query = "query HelloWorldQuery { hello }"))
    }
    assertEquals(HttpStatusCode.OK, response.status)
    assertContains(response.bodyAsText(), """"hello":"Hello GraphQL!"""")
  }

  @Test
  fun `SDL route test`() {
    val expectedSchema = """
      "Schema for Tally data"
      schema @contact(description : "Report issues on github.", name : "Tally", url : "https://github.com/cherba29/tally"){
        query: Query
      }
  
      "Provides contact information of the owner responsible for this subgraph schema."
      directive @contact(description: String!, name: String!, url: String!) on SCHEMA
  
      "Marks the field, argument, input field or enum value as deprecated"
      directive @deprecated(
          "The reason for the deprecation"
          reason: String = "No longer supported"
        ) on FIELD_DEFINITION | ARGUMENT_DEFINITION | ENUM_VALUE | INPUT_FIELD_DEFINITION
  
      "Directs the executor to include this field or fragment only when the `if` argument is true"
      directive @include(
          "Included when true."
          if: Boolean!
        ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
  
      "Indicates an Input Object is a OneOf Input Object."
      directive @oneOf on INPUT_OBJECT
  
      "Directs the executor to skip this field or fragment when the `if` argument is true."
      directive @skip(
          "Skipped when true."
          if: Boolean!
        ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
  
      "Exposes a URL that specifies the behaviour of this scalar."
      directive @specifiedBy(
          "The URL that specifies the behaviour of this scalar."
          url: String!
        ) on SCALAR
  
      type Query {
        hello: String!
      }
      """.trimIndent()
    testApplication {
      application {
        graphQLModule()
      }

      val response = client.get("/sdl")
      assertEquals(HttpStatusCode.OK, response.status)
      assertEquals(expectedSchema, response.bodyAsText().trim())
    }
  }

  @Test
  fun `server should provide GraphiQL endpoint`() {
    testApplication {
      application {
        graphQLModule()
      }

      val response = client.get("/graphiql")
      assertEquals(HttpStatusCode.OK, response.status)

      val html = response.bodyAsText()
      assertContains(html, "var serverUrl = '/graphql';")
      assertContains(html, """var subscriptionUrl = new URL("/subscriptions", location.href);""")
    }
  }
}