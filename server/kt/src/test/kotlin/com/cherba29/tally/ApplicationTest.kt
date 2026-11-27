package com.cherba29.tally

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.cherba29.tally.data.Loader
import com.diffplug.selfie.coroutines.expectSelfie
import com.expediagroup.graphql.server.execution.subscription.GRAPHQL_WS_PROTOCOL
import com.expediagroup.graphql.server.execution.subscription.GraphQLWebSocketServer
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val testObjectMapper = ObjectMapper()

// Serialize GraphQL POST bodies eagerly as ByteArrayContent/TextContent instead of streaming
// OutputStreamContent via ContentNegotiation(jackson()), avoiding Ktor TestHttpClientEngine's
// race where the server closes the request channel before the client flush coroutine completes.
private suspend fun HttpClient.postGraphQL(query: String): HttpResponse =
  post("/graphql") {
    contentType(ContentType.Application.Json)
    setBody(testObjectMapper.writeValueAsString(GraphQLRequest(query = query)))
  }

private suspend fun HttpClient.awaitGraphQLBody(
  query: String,
  predicate: (String) -> Boolean
): Pair<HttpStatusCode, String> =
  withTimeout(5.seconds) {
    while (true) {
      val response = postGraphQL(query)
      val body = response.bodyAsText()
      if (predicate(body)) {
        return@withTimeout response.status to body
      }
      delay(30)
    }
    @Suppress("UNREACHABLE_CODE")
    error("unreachable")
  }

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

  describe("dispatchMain") {
    it("routes 'cli' invocations to the CLI entry point without the leading 'cli' token") {
      var cliArgs: List<String>? = null
      var serverArgs: List<String>? = null

      dispatchMain(
        args = arrayOf("cli", "summary", "--start-month", "Jul2026"),
        runCli = { cliArgs = it.toList() },
        runServer = { serverArgs = it.toList() }
      )

      cliArgs shouldBe listOf("summary", "--start-month", "Jul2026")
      serverArgs shouldBe null
    }

    it("routes bare 'cli' invocation to the CLI entry point with empty args") {
      var cliArgs: List<String>? = null
      var serverArgs: List<String>? = null

      dispatchMain(
        args = arrayOf("cli"),
        runCli = { cliArgs = it.toList() },
        runServer = { serverArgs = it.toList() }
      )

      cliArgs shouldBe emptyList()
      serverArgs shouldBe null
    }

    it("routes empty or server args to the Ktor server entry point") {
      var cliArgs: List<String>? = null
      var serverArgs: List<String>? = null

      dispatchMain(
        args = arrayOf("-port=9090"),
        runCli = { cliArgs = it.toList() },
        runServer = { serverArgs = it.toList() }
      )

      cliArgs shouldBe null
      serverArgs shouldBe listOf("-port=9090")
    }
  }

  describe("resolveTallyDataPath") {
    it("prefers tally.data.path over legacyPath") {
      val primaryDir = tempdir("tally-primary-", keepOnFailure = false).toPath()
      val legacyDir = tempdir("tally-legacy-", keepOnFailure = false).toPath()
      val config = MapApplicationConfig(
        "tally.data.path" to primaryDir.pathString,
        "tally.data.legacyPath" to legacyDir.pathString
      )
      resolveTallyDataPath(config, defaultPath = tallyPath.resolve("missing")) shouldBe primaryDir
    }

    it("falls back to tally.data.legacyPath when tally.data.path is unset or blank") {
      val legacyDir = tempdir("tally-legacy-", keepOnFailure = false).toPath()
      val config = MapApplicationConfig(
        "tally.data.path" to "",
        "tally.data.legacyPath" to legacyDir.pathString
      )
      resolveTallyDataPath(config, defaultPath = tallyPath.resolve("missing")) shouldBe legacyDir
    }

    it("falls back to default sample-data path when unconfigured and default exists") {
      val defaultDir = tempdir("tally-default-", keepOnFailure = false).toPath()
      resolveTallyDataPath(MapApplicationConfig(), defaultPath = defaultDir) shouldBe defaultDir
    }

    it("throws clear IllegalArgumentException when unconfigured and no default exists") {
      val ex = shouldThrow<IllegalArgumentException> {
        resolveTallyDataPath(MapApplicationConfig(), defaultPath = tallyPath.resolve("missing-default"))
      }
      ex.message shouldContain "TALLY_FILES"
    }

    it("throws IllegalArgumentException when configured path does not exist") {
      val missing = tallyPath.resolve("does-not-exist")
      val ex = shouldThrow<IllegalArgumentException> {
        resolveTallyDataPath(MapApplicationConfig("tally.data.path" to missing.pathString))
      }
      ex.message shouldContain "does not exist"
    }
  }

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
              "tally.client.path" to ""
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

    it("server should return Internal Server Error for unhandled exceptions") {
      testApplication {
        configureTestClient(tallyPath)
        routing {
          get("/boom") {
            throw RuntimeException("unexpected failure")
          }
        }
        val response = client.get("/boom")
        response.status shouldBe HttpStatusCode.InternalServerError
      }
    }

    it("server should handle valid POST requests") {
      testApplication {
        configureTestClient(tallyPath)
        val response = client.postGraphQL("query { __typename }")
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldContain """"__typename":"Query""""
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

        val tableQuery =
          """query { table(owner: "someone", startMonth: "Mar2019", endMonth: "Mar2019") { currentOwner } }"""

        // 1. Initial valid query succeeds
        val initialResponse = client.postGraphQL(tableQuery)
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
        val (parseErrStatus, parseErrBody) = client.awaitGraphQLBody(tableQuery) { it.contains(""""errors":""") }
        parseErrStatus shouldBe HttpStatusCode.OK
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
        val (valErrStatus, valErrBody) = client.awaitGraphQLBody(tableQuery) { it.contains("months apart (2 max)") }
        valErrStatus shouldBe HttpStatusCode.OK
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
        val (recoveredStatus, recoveredBody) = client.awaitGraphQLBody(tableQuery) { !it.contains(""""errors":""") }
        recoveredStatus shouldBe HttpStatusCode.OK
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
