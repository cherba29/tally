package com.cherba29.tally

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.diffplug.selfie.coroutines.expectSelfie
import com.expediagroup.graphql.server.execution.subscription.GRAPHQL_WS_PROTOCOL
import com.expediagroup.graphql.server.execution.subscription.GraphQLWebSocketServer
import com.expediagroup.graphql.server.types.GraphQLRequest
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.plugins.di.dependencies
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import com.cherba29.tally.data.Loader
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

fun ApplicationTestBuilder.configureTestClient(tallyPath: Path?) {
  application {
    graphQLModule()
  }
  environment {
    if (tallyPath != null) {
      config = config.mergeWith(
        MapApplicationConfig(
          "tally.data.path" to tallyPath.resolve("data").pathString,
          "tally.client.path" to tallyPath.resolve("client").pathString
        )
      )
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

    it("serves embedded frontend resources when tally.client.path is not set") {
      testApplication {
        application {
          graphQLModule()
        }
        environment {
          config = config.mergeWith(
            MapApplicationConfig(
              "tally.data.path" to tallyPath.resolve("data").pathString,
              "tally.client.path" to "",
            )
          )
        }
        val response = client.get("/")
        response shouldHaveStatus HttpStatusCode.OK
        response.bodyAsText() shouldContain "<tally-app></tally-app>"
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

    it("loader is managed by DI resource lifecycle") {
      mockkObject(Loader.Companion)
      var spyLoader: Loader? = null
      every { Loader.watchPath(any()) } answers {
        val real = callOriginal()
        spyk(real).also { spyLoader = it }
      }

      try {
        testApplication {
          configureTestClient(tallyPath)
          startApplication()
          val loader = application.dependencies.resolve<Loader>()
          loader.shouldNotBeNull()
          loader shouldBe spyLoader
        }
        verify(atLeast = 1) { spyLoader!!.close() }
      } finally {
        unmockkObject(Loader.Companion)
      }
    }

    it("returns GraphQL errors when YAML has syntax or validation errors and recovers once fixed") {
      val testPath = tempdir("tally-err-", keepOnFailure = false).toPath()
      ((testPath / "client").createDirectory() / "index.html").createFile().writeText("Hello World!")
      val dataDir = (testPath / "data").createDirectory()
      val accountFile = (dataDir / "account.yaml").createFile()
      accountFile.writeText(
        """
        name: test-account
        owner: [someone]
        path: [ external ]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      testApplication {
        configureTestClient(testPath)
        val client = createClient {
          install(ContentNegotiation) {
            jackson()
          }
        }

        val tableQuery = """query { table(owner: "someone", startMonth: "Mar2019", endMonth: "Mar2019") { currentOwner } }"""

        // 1. Initial valid query succeeds
        val initialResponse = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = tableQuery))
        }
        initialResponse.status shouldBe HttpStatusCode.OK
        val initialBody = initialResponse.bodyAsText()
        initialBody shouldContain """"currentOwner":"someone""""
        initialBody shouldNotContain """"errors":"""

        // 2. Introduce YAML parsing syntax error
        accountFile.writeText(
          """
          name: test-account
          owner: [someone
          """.trimIndent()
        )
        Thread.sleep(700)

        val parseErrResponse = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = tableQuery))
        }
        parseErrResponse.status shouldBe HttpStatusCode.OK
        val parseErrBody = parseErrResponse.bodyAsText()
        parseErrBody shouldContain """"errors":"""
        parseErrBody shouldContain "account.yaml"
        parseErrBody shouldContain """"file":"account.yaml""""

        // 3. Introduce validation error (balance date too far from grp)
        accountFile.writeText(
          """
          name: test-account
          owner: [someone]
          path: [ external ]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2020-05-01, camt: 100.00 }
          """.trimIndent()
        )
        Thread.sleep(700)

        val valErrResponse = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = tableQuery))
        }
        valErrResponse.status shouldBe HttpStatusCode.OK
        val valErrBody = valErrResponse.bodyAsText()
        valErrBody shouldContain """"errors":"""
        valErrBody shouldContain "months apart (2 max)"
        valErrBody shouldContain "account.yaml"
        valErrBody shouldContain """"file":"account.yaml""""

        // 4. Recover by writing valid YAML
        accountFile.writeText(
          """
          name: test-account
          owner: [someone]
          path: [ external ]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          """.trimIndent()
        )
        Thread.sleep(700)

        val recoveredResponse = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = tableQuery))
        }
        recoveredResponse.status shouldBe HttpStatusCode.OK
        val recoveredBody = recoveredResponse.bodyAsText()
        recoveredBody shouldContain """"currentOwner":"someone""""
        recoveredBody shouldNotContain """"errors":"""
      }
    }

    it("cleans up subscription without warnings when client disconnects before reload") {
      val testPath = tempdir("tally-ws-", keepOnFailure = false).toPath()
      ((testPath / "client").createDirectory() / "index.html").createFile().writeText("Hello World!")
      val dataDir = (testPath / "data").createDirectory()
      val accountFile = (dataDir / "account.yaml").createFile()
      accountFile.writeText(
        """
        name: test-account
        owner: [someone]
        path: [ external ]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val wsLogger = LoggerFactory.getLogger(GraphQLWebSocketServer::class.java) as Logger
      val listAppender = ListAppender<ILoggingEvent>().apply { start() }
      wsLogger.addAppender(listAppender)

      try {
        testApplication {
          configureTestClient(testPath)
          val wsClient = createClient {
            install(WebSockets)
          }

          // 1. Connect first client and subscribe, then close abruptly without sending 'complete'
          //    (simulates browser page reload or closed tab).
          val ws1 = wsClient.webSocketSession("/subscriptions") {
            header(HttpHeaders.SecWebSocketProtocol, GRAPHQL_WS_PROTOCOL)
          }
          ws1.send(Frame.Text("""{"type":"connection_init"}"""))
          val ack1 = withTimeout(5.seconds) { (ws1.incoming.receive() as Frame.Text).readText() }
          ack1 shouldContain """"type":"connection_ack""""
          ws1.send(Frame.Text("""{"id":"1","type":"subscribe","payload":{"query":"subscription { tableUpdates }"}}"""))
          delay(100)
          ws1.close()

          // 2. Connect second client (simulates reloaded page) and subscribe.
          val ws2 = wsClient.webSocketSession("/subscriptions") {
            header(HttpHeaders.SecWebSocketProtocol, GRAPHQL_WS_PROTOCOL)
          }
          ws2.send(Frame.Text("""{"type":"connection_init"}"""))
          val ack2 = withTimeout(5.seconds) { (ws2.incoming.receive() as Frame.Text).readText() }
          ack2 shouldContain """"type":"connection_ack""""
          ws2.send(Frame.Text("""{"id":"1","type":"subscribe","payload":{"query":"subscription { tableUpdates }"}}"""))
          delay(100)

          // 3. Trigger a budget reload and verify ws2 receives the update.
          accountFile.writeText(
            """
            name: test-account
            owner: [someone]
            path: [ external ]
            opened_on: Mar2019
            balances:
              - { grp: Mar2019, date: 2019-03-01, camt: 200.00 }
            """.trimIndent()
          )
          val nextMsg = withTimeout(5.seconds) { (ws2.incoming.receive() as Frame.Text).readText() }
          nextMsg shouldContain """"type":"next""""
          nextMsg shouldContain """"tableUpdates":true"""

          // 4. Gracefully complete and close ws2.
          ws2.send(Frame.Text("""{"id":"1","type":"complete"}"""))
          ws2.close()
        }

        val warningsOrErrors = listAppender.list.filter { it.level.isGreaterOrEqual(Level.WARN) }
        warningsOrErrors.shouldBeEmpty()
      } finally {
        wsLogger.detachAppender(listAppender)
        listAppender.stop()
      }
    }
  }
})
