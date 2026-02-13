package no.nav.helsemelding.messagegenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    dialogMessageProcessor: DialogMessageProcessor
) {
    routing {
        internalRoutes(registry)
        externalRoutes(registry, dialogMessageProcessor)
    }
}

fun Route.internalRoutes(registry: PrometheusMeterRegistry) {
    get("/prometheus") {
        call.respond(registry.scrape())
    }
    route("/internal") {
        get("/health/liveness") {
            call.respondText("I'm alive! :)")
        }
        get("/health/readiness") {
            call.respondText("I'm ready! :)")
        }
    }
}

@Suppress("UNUSED_PARAMETER")
fun Route.externalRoutes(registry: PrometheusMeterRegistry, dialogMessageProcessor: DialogMessageProcessor) {
    get("/generate-messages") {
        var count = call.request.queryParameters["count"]?.toIntOrNull() ?: 1
        if (count > 100) count = 100

        var published = 0
        coroutineScope {
            repeat(count) {
                dialogMessageProcessor.processMessages(this)
                published++
                if (it < count - 1) delay(1000)
            }
        }

        call.respondText("Published $published dialog messages.")
    }
}
