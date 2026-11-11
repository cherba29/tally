package com.cherba29.tally

import com.cherba29.tally.data.BudgetLoadingException
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.CustomSchemaGeneratorHooks
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.federation.directives.ContactDirective
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.Schema
import com.expediagroup.graphql.server.ktor.graphQLGetRoute
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphQLSubscriptionsRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import com.expediagroup.graphql.server.operations.Query
import graphql.ErrorType
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonWebsocketContentConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.util.AttributeKey
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

/**
 * GraphQL exception handler that logs exceptions with full contextual details (path, field, arguments)
 * and preserves specific GraphQLErrors for standard GraphQL error reporting.
 */
class LoggingDataFetcherExceptionHandler : DataFetcherExceptionHandler {
  override fun handleException(
    handlerParameters: DataFetcherExceptionHandlerParameters
  ): CompletableFuture<DataFetcherExceptionHandlerResult> {
    val exception = unwrap(handlerParameters.exception)
    val sourceLocation = handlerParameters.sourceLocation
    val path = handlerParameters.path
    val fieldName = handlerParameters.fieldDefinition?.name ?: "unknown"
    val argumentValues = handlerParameters.argumentValues

    logger.error(exception) {
      "GraphQL fetch error on field '$fieldName' at path '$path' with arguments $argumentValues ($sourceLocation): ${exception.message}"
    }

    val error: GraphQLError = when (exception) {
      is BudgetLoadingException -> exception.toGraphQLError(sourceLocation, path)
      is GraphQLError -> GraphqlErrorBuilder.newError()
        .message(exception.message ?: "GraphQL Error")
        .location(sourceLocation)
        .path(path)
        .errorType(exception.errorType ?: ErrorType.DataFetchingException)
        .extensions(exception.extensions)
        .build()
      else -> ExceptionWhileDataFetching(path, exception, sourceLocation)
    }

    val result = DataFetcherExceptionHandlerResult.newResult().error(error).build()
    return CompletableFuture.completedFuture(result)
  }

  private fun unwrap(exception: Throwable): Throwable {
    var current = exception
    while ((current is CompletionException || current is java.lang.reflect.InvocationTargetException) && current.cause != null) {
      current = current.cause!!
    }
    return current
  }
}

/**
 * Instrumentation that logs GraphQL query execution latency, operation details, and error summaries.
 */
class QueryLoggingInstrumentation : SimplePerformantInstrumentation() {
  override fun beginExecution(
    parameters: InstrumentationExecutionParameters,
    state: InstrumentationState?
  ): InstrumentationContext<ExecutionResult> {
    val startNano = System.nanoTime()
    val operation = parameters.operation ?: parameters.executionInput.operationName ?: "query"
    val firstLine = parameters.query?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: "unknown"
    logger.info { "--> GraphQL execution started: op='$operation', query='$firstLine'" }

    return object : InstrumentationContext<ExecutionResult> {
      override fun onDispatched() {}
      override fun onCompleted(result: ExecutionResult?, t: Throwable?) {
        val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
        val formattedDuration = String.format("%.2f", durationMs)
        if (t != null) {
          logger.error(t) { "<-- GraphQL execution failed in ${formattedDuration}ms: ${t.message}" }
        } else if (result != null && result.errors.isNotEmpty()) {
          val errorMessages = result.errors.joinToString(", ") { it.message ?: "unknown" }
          logger.warn { "<-- GraphQL execution completed with ${result.errors.size} error(s) in ${formattedDuration}ms: $errorMessages" }
        } else {
          logger.info { "<-- GraphQL execution completed successfully in ${formattedDuration}ms" }
        }
      }
    }
  }
}

/**
 * Ktor plugin that monitors HTTP requests and responses with latency measurement and error tracking.
 */
val RequestLoggingPlugin = createApplicationPlugin("RequestLogging") {
  val callStartTime = AttributeKey<Long>("CallStartTime")

  on(CallSetup) { call ->
    call.attributes.put(callStartTime, System.currentTimeMillis())
    call.request.origin.apply {
      logger.info { "--> ${call.request.httpMethod.value} $uri ($scheme://$localHost:$localPort)" }
    }
  }

  on(ResponseSent) { call ->
    val start = call.attributes.getOrNull(callStartTime)
    val durationMs = if (start != null) System.currentTimeMillis() - start else -1
    val status = call.response.status()?.value ?: 200
    logger.info { "<-- ${call.request.httpMethod.value} ${call.request.uri} -> $status in ${durationMs}ms" }
  }

  on(CallFailed) { call, cause ->
    val start = call.attributes.getOrNull(callStartTime)
    val durationMs = if (start != null) System.currentTimeMillis() - start else -1
    logger.error(cause) { "<-- ${call.request.httpMethod.value} ${call.request.uri} -> FAILED in ${durationMs}ms: ${cause.message}" }
  }
}

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
      when (cause) {
        is UnsupportedOperationException -> {
          logger.warn(cause) { "Method not allowed for ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.MethodNotAllowed)
        }
        is IllegalArgumentException -> {
          logger.warn(cause) { "Bad request on ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.BadRequest)
        }
        else -> {
          logger.error(cause) { "Unhandled server error on ${call.request.uri}: ${cause.message}" }
          call.respond(HttpStatusCode.BadRequest)
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
        HelloWorldQuery(),
        TableService(loader),
        SummaryService(loader),
        StatementService(loader),
      )
      subscriptions = listOf(
        TableSubscription(loader),
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
          plugin(GraphQL).engine.execute("query WarmUp { hello }")
        }
        logger.info { "Pre-warming complete in ${warmUpDuration.inWholeMilliseconds}ms" }
      } catch (e: Exception) {
        logger.warn(e) { "Pre-warming encountered error: ${e.message}" }
      }
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
}

private val logger = KotlinLogging.logger {}
