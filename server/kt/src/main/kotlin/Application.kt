package com.cherba29.tally

import com.expediagroup.graphql.server.ktor.GraphQL
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
import io.ktor.serialization.jackson.JacksonWebsocketContentConverter
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
  routing {
    get("/") {
      call.respondText("Hello World!")
    }
  }
}

class HelloWorldQuery : Query {
  fun hello(): String = "Hello GraphQL!"
}

fun Application.graphQLModule() {
  install(WebSockets) {
    pingPeriod = 1.seconds
    contentConverter = JacksonWebsocketContentConverter()
  }
  install(StatusPages) {
    defaultGraphQLStatusPages()
  }
  install(CORS) {
    anyHost()
  }

  install(GraphQL) {
    schema {
      packages = listOf("com.cherba29.tally")
      queries = listOf(
        HelloWorldQuery()
      )
    }
  }
  routing {
    get("/") {
      call.respondText("Hello gql World!")
    }
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
}

private val logger = KotlinLogging.logger {}

