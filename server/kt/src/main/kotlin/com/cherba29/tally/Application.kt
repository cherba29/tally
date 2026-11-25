package com.cherba29.tally

import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.CustomSchemaGeneratorHooks
import com.cherba29.tally.server.LoggingDataFetcherExceptionHandler
import com.cherba29.tally.server.QueryLoggingInstrumentation
import com.cherba29.tally.server.RequestLoggingPlugin
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.execution.subscription.GRAPHQL_WS_PROTOCOL
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.graphQLGetRoute
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonWebsocketContentConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

fun main(args: Array<String>) {
  EngineMain.main(args)
}

@ContactDirective(
  name = "Tally",
  url = "https://github.com/cherba29/tally",
  description = "Report issues on github."
)
@GraphQLDescription("Schema for Tally data")
class TallySchema : Schema

private const val TALLY_PATH_ENV_SETTING = "tally.data.path"
private const val TALLY_LEGACY_PATH_ENV_SETTING = "tally.data.legacyPath"
private const val TALLY_CLIENT_BUNDLE_PATH = "tally.client.path"
private const val DEFAULT_SAMPLE_DATA_PATH = "sample-data"

internal fun resolveTallyDataPath(
  config: ApplicationConfig,
  defaultPath: Path = Paths.get(DEFAULT_SAMPLE_DATA_PATH)
): Path {
  val primary = config.propertyOrNull(TALLY_PATH_ENV_SETTING)?.getString()?.takeIf { it.isNotBlank() }
  val legacy = if (primary == null) {
    config.propertyOrNull(TALLY_LEGACY_PATH_ENV_SETTING)?.getString()?.takeIf { it.isNotBlank() }?.also {
      logger.warn { "Environment variable TALLY_PATH is deprecated; use TALLY_FILES instead." }
    }
  } else {
    null
  }
  val configuredPath = primary ?: legacy
  val tallyFiles = if (configuredPath != null) {
    Paths.get(configuredPath)
  } else if (defaultPath.exists()) {
    logger.info { "No data path configured; falling back to bundled '$defaultPath'." }
    defaultPath
  } else {
    throw IllegalArgumentException(
      "Tally data directory is not configured. Set TALLY_FILES=/path/to/data " +
        "(or application config '$TALLY_PATH_ENV_SETTING')."
    )
  }
  require(tallyFiles.exists()) {
    "'$tallyFiles' path does not exist as set in application config '$TALLY_PATH_ENV_SETTING'."
  }
  logger.info { "Using tally files path: ${tallyFiles.toRealPath()}" }
  return tallyFiles
}

fun Application.graphQLModule() {
  val tallyFiles = resolveTallyDataPath(environment.config)

  dependencies {
    provide<Loader> { Loader.watchPath(tallyFiles) }
  }
  val loader: Loader by dependencies

  install(RequestLoggingPlugin)

  install(WebSockets) {
    // Pings the client every 10 seconds to keep alive
    pingPeriod = 10.seconds
    // Closes the connection if 60s pass without a pong
    timeout = 60.seconds
    contentConverter = JacksonWebsocketContentConverter()
  }

  install(StatusPages) {
    exception<Throwable> { call, cause ->
      when {
        cause is UnsupportedOperationException -> {
          logger.warn(cause) { "Method not allowed for ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.MethodNotAllowed)
        }
        cause is IllegalArgumentException ||
          (cause is IllegalStateException && cause.message?.startsWith("Invalid HTTP request") == true) -> {
          logger.warn(cause) { "Bad request on ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.BadRequest)
        }
        else -> {
          logger.error(cause) { "Unhandled server error on ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
    }
  }

  install(CORS) {
    anyHost()

    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Get)

    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
  }

  install(GraphQL) {
    schema {
      packages = listOf("com.cherba29.tally", "kotlinx.datetime")
      queries = listOf(
        TableService(loader),
        SummaryService(loader),
        StatementService(loader)
      )
      subscriptions = listOf(
        TableSubscription(loader)
      )
      hooks = CustomSchemaGeneratorHooks()
      schemaObject = TallySchema()
    }
    engine {
      exceptionHandler = LoggingDataFetcherExceptionHandler()
      instrumentations = listOf(QueryLoggingInstrumentation())
    }
  }

  // Pre-warm the budget loader and GraphQL engine on application start
  // to eliminate the cold-start latency spike on the first request.
  monitor.subscribe(ApplicationStarted) {
    launch(Dispatchers.IO) {
      try {
        logger.info { "Pre-warming budget loader and GraphQL engine..." }
        val warmUpDuration = measureTime {
          loader.budget()
          plugin(GraphQL).engine.execute("query WarmUp { __typename }")
        }
        logger.info { "Pre-warming complete in ${warmUpDuration.inWholeMilliseconds}ms" }
      } catch (e: Exception) {
        logger.warn(e) { "Pre-warming encountered error: ${e.message}" }
      }
    }
  }

  val tallyBundlePath = environment.config
    .propertyOrNull(TALLY_CLIENT_BUNDLE_PATH)
    ?.getString()
    ?.takeIf { it.isNotBlank() }

  routing {
    if (tallyBundlePath != null) {
      logger.info { "Serving client bundle from filesystem: $tallyBundlePath" }
      staticFiles("/", File(tallyBundlePath))
    } else {
      logger.info { "Serving client bundle from embedded classpath resources (/static)" }
      staticResources("/", "static")
    }
    graphQLGetRoute()
    graphQLPostRoute()
    // Cancel active subscription collection as soon as the WebSocket closes.
    // graphql-kotlin's default graphQLSubscriptionsRoute() uses flatMapMerge over a request flow
    // that completes normally on client disconnect, leaving infinite SharedFlow subscriptions alive
    // until the next emission fails with CancellationException on the closed outgoing channel.
    webSocket(path = "subscriptions", protocol = GRAPHQL_WS_PROTOCOL) {
      val subscriptionJob = launch {
        call.application
          .plugin(GraphQL)
          .subscriptionServer
          .handleSubscription(this@webSocket)
          .collect()
      }
      closeReason.invokeOnCompletion { subscriptionJob.cancel() }
      subscriptionJob.join()
    }
    graphiQLRoute()
    graphQLSDLRoute()
  }
}

private val logger = KotlinLogging.logger {}
