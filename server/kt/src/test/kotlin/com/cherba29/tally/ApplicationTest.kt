package com.cherba29.tally

import com.diffplug.selfie.coroutines.expectSelfie
import com.expediagroup.graphql.server.types.GraphQLRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun ApplicationTestBuilder.configureTestClient(tallyPath: Path?) {
  application {
    graphQLModule()
  }
  environment {
    if (tallyPath != null) {
      config = config.mergeWith(MapApplicationConfig(
        "tally.data.path" to tallyPath.resolve("data").pathString,
        "tally.client.path" to tallyPath.resolve("client").pathString,
      ))
    }
  }
}

class ApplicationTest : DescribeSpec({
  timeout = 15.seconds.toLong(DurationUnit.MILLISECONDS)
  val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
  ((tallyPath / "client").createDirectory() / "index.html").createFile().writeText("Hello World!")
  ((tallyPath / "data").createDirectory() / "file2.yaml").createFile().writeText(
    """
        name: test-account
        owner: [someone]
        path: [ external ]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
  )

  describe("routing") {
    it("root") {
      testApplication {
        configureTestClient(tallyPath)
        val response = client.get("/")
        response shouldHaveStatus HttpStatusCode.OK
        response.bodyAsText() shouldBe "Hello World!"
      }
    }
    it("server should return Bad Request for invalid GET requests") {
      testApplication {
        configureTestClient(tallyPath)
        val response = client.get("/graphql")
        response.status shouldBe HttpStatusCode.BadRequest
      }
    }

    it("server should return Method Not Allowed for Mutation GET requests") {
      testApplication {
        configureTestClient(tallyPath)
        val response = client.get("/graphql") {
          parameter("query", "mutation { foo }")
        }
        response.status shouldBe HttpStatusCode.MethodNotAllowed
      }
    }

    it("server should handle valid POST requests") {
      testApplication {
        configureTestClient(tallyPath)
        val client = createClient {
          install(ContentNegotiation) {
            jackson()
          }
        }
        val response = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = "query HelloWorldQuery { hello }"))
        }
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldContain """"hello":"Hello GraphQL!""""
      }
    }

    it("SDL route test") {
      var result = ""
      testApplication {
        configureTestClient(tallyPath)

        val response = client.get("/sdl")
        response.status shouldBe HttpStatusCode.OK
        result = response.bodyAsText().trim()
      }
      expectSelfie(result).toMatchDisk()
    }

    it("server should provide GraphiQL endpoint") {
      testApplication {
        configureTestClient(tallyPath)

        val response = client.get("/graphiql")

        response.status shouldBe HttpStatusCode.OK

        val html = response.bodyAsText()
        html shouldContain "var serverUrl = '/graphql';"
        html shouldContain """var subscriptionUrl = new URL("/subscriptions", location.href);"""
      }
    }
  }
})