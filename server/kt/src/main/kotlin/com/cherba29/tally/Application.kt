package com.cherba29.tally

import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.watchedEventFlow
import com.cherba29.tally.schema.CustomSchemaGeneratorHooks
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLGetRoute
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphQLSubscriptionsRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import com.expediagroup.graphql.server.operations.Query
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonWebsocketContentConverter
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds

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

class HelloWorldQuery : Query {
  fun hello(): String = "Hello GraphQL!"
}

private const val TALLY_PATH_ENV_SETTING = "tally.data.path"
private const val TALLY_CLIENT_BUNDLE_PATH = "tally.client.path"

fun Application.graphQLModule() {
  val tallyFiles = Paths.get(requireNotNull(
    environment.config.propertyOrNull(TALLY_PATH_ENV_SETTING)?.getString()
  ) {
    "Application config does not set $TALLY_PATH_ENV_SETTING property."
  })
  logger.info { "Using tally files path: ${tallyFiles.toRealPath()}" }
  require(tallyFiles.exists()) {
    "'$tallyFiles' path does not exist as set in application config '$TALLY_PATH_ENV_SETTING'."
  }

  // TODO: Maybe use resource lifecycle https://ktor.io/docs/server-di-resource-lifecycle-management.html
  val loader = Loader(tallyFiles.watchedEventFlow {
    it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
  })

  install(WebSockets) {
    pingPeriod = 1.seconds
    contentConverter = JacksonWebsocketContentConverter()
  }
  install(StatusPages) {
    defaultGraphQLStatusPages()
  }
  // TODO: re-enable after testing is done.
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
        HelloWorldQuery(),
        TableService(loader),
        SummaryService(loader),
        StatementService(loader),
      )
      hooks = CustomSchemaGeneratorHooks()
      schemaObject = TallySchema()
    }
  }

  val tallyBundlePath = requireNotNull(
    environment.config.propertyOrNull(TALLY_CLIENT_BUNDLE_PATH)?.getString()
  ) {
    "Application config does not set $TALLY_CLIENT_BUNDLE_PATH property."
  }

  routing {
    staticFiles("/", File(tallyBundlePath))
    graphQLGetRoute()
    graphQLPostRoute()
    graphQLSubscriptionsRoute()
    graphiQLRoute()
    graphQLSDLRoute()
  }

  intercept(ApplicationCallPipeline.Monitoring) {
    call.request.origin.apply {
      logger.info { "Request URL: $scheme://$localHost:$localPort$uri" }
    }
  }

  monitor.subscribe(ApplicationStopped) {
    logger.info { "Closing loader..." }
    loader.close()
  }
}
private val ignorePathRegex = Regex("(^_)|(/_)")
private val logger = KotlinLogging.logger {}

