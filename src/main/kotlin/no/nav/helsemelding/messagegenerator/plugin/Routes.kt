package no.nav.helsemelding.messagegenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor
import no.nav.helsemelding.messagegenerator.scheduler.SchedulerService
import kotlin.time.Duration.Companion.seconds

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    dialogMessageProcessor: DialogMessageProcessor,
    schedulerService: SchedulerService
) {
    routing {
        internalRoutes(registry)
        externalRoutes(dialogMessageProcessor, schedulerService)
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

fun Route.externalRoutes(
    dialogMessageProcessor: DialogMessageProcessor,
    schedulerService: SchedulerService
) {
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

    route("/scheduler") {
        get("/status") {
            val dialogStatus = schedulerService.dialogMessages.status()
            val incomingStatus = schedulerService.incomingMessages.status()
            call.respond(
                mapOf(
                    "dialogMessages" to dialogStatus,
                    "incomingMessages" to incomingStatus
                )
            )
        }

        route("/dialog-messages") {
            post("/start") {
                schedulerService.dialogMessages.start()
                call.respondText("Dialog messages scheduler started.")
            }

            post("/stop") {
                schedulerService.dialogMessages.stop()
                call.respondText("Dialog messages scheduler stopped.")
            }

            post("/interval/{intervalSeconds}") {
                val intervalSeconds = call.parameters["intervalSeconds"]?.toLongOrNull()
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    call.respondText("Invalid interval. Please provide a positive number of seconds.")
                    return@post
                }

                schedulerService.dialogMessages.updateInterval(intervalSeconds.seconds)

                call.respondText("Dialog messages scheduler interval updated to $intervalSeconds seconds.")
            }
        }

        route("/incoming-messages") {
            post("/start") {
                schedulerService.incomingMessages.start()
                call.respondText("Incoming messages scheduler started.")
            }

            post("/stop") {
                schedulerService.incomingMessages.stop()
                call.respondText("Incoming messages scheduler stopped.")
            }

            post("/interval/{intervalSeconds}") {
                val intervalSeconds = call.parameters["intervalSeconds"]?.toLongOrNull()
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    call.respondText("Invalid interval. Please provide a positive number of seconds.")
                    return@post
                }

                schedulerService.incomingMessages.updateInterval(intervalSeconds.seconds)

                call.respondText("Incoming messages scheduler interval updated to $intervalSeconds seconds.")
            }
        }
    }
}
