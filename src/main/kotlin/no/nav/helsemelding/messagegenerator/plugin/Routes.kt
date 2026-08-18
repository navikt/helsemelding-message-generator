package no.nav.helsemelding.messagegenerator.plugin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
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
import no.nav.helsemelding.messagegenerator.generator.IncomingMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.JsonDialogMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.XmlDialogMessageGenerator
import no.nav.helsemelding.messagegenerator.scheduler.SchedulerService
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    xmlDialogMessageGenerator: XmlDialogMessageGenerator,
    jsonDialogMessageGenerator: JsonDialogMessageGenerator,
    incomingMessageGenerator: IncomingMessageGenerator,
    schedulerService: SchedulerService
) {
    routing {
        internalRoutes(registry)
        externalRoutes(
            xmlDialogMessageGenerator,
            jsonDialogMessageGenerator,
            incomingMessageGenerator,
            schedulerService
        )
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
    xmlDialogMessageGenerator: XmlDialogMessageGenerator,
    jsonDialogMessageGenerator: JsonDialogMessageGenerator,
    incomingMessageGenerator: IncomingMessageGenerator,
    schedulerService: SchedulerService
) {
    route("/generate") {
        get("/xml-dialog-messages") {
            var count = call.request.queryParameters["count"]?.toIntOrNull() ?: 1
            if (count > 100) count = 100

            var published = 0
            coroutineScope {
                repeat(count) {
                    xmlDialogMessageGenerator.generateMessages(this)
                    published++
                    if (it < count - 1) delay(1000)
                }
            }

            call.respondText("Published $published dialog messages.")
        }

        get("/json-dialog-messages") {
            var count = call.request.queryParameters["count"]?.toIntOrNull() ?: 1
            if (count > 100) count = 100

            var published = 0
            coroutineScope {
                repeat(count) {
                    jsonDialogMessageGenerator.generateMessages(this)
                    published++
                    if (it < count - 1) delay(1000)
                }
            }

            call.respondText("Published $published json dialog messages.")
        }

        get("/incoming-messages") {
            var count = call.request.queryParameters["count"]?.toIntOrNull() ?: 1
            if (count > 100) count = 100

            var published = 0
            repeat(count) {
                incomingMessageGenerator.generateIncomingMessage()
                published++
                if (it < count - 1) delay(1000)
            }

            call.respondText("Published $published incoming messages.")
        }
    }

    route("/scheduler") {
        get("/status") {
            try {
                val xmlDialogStatus = schedulerService.xmlDialogMessages.status()
                val jsonDialogStatus = schedulerService.jsonDialogMessages.status()
                val incomingStatus = schedulerService.incomingMessages.status()
                call.respond(
                    mapOf(
                        "xmlDialogMessages" to xmlDialogStatus,
                        "jsonDialogMessages" to jsonDialogStatus,
                        "incomingMessages" to incomingStatus
                    )
                )
            } catch (e: Exception) {
                log.error(e) { "Failed to get scheduler status" }
                throw e
            }
        }

        route("/xml-dialog-messages") {
            post("/start") {
                schedulerService.xmlDialogMessages.start()
                call.respondText("Dialog messages scheduler started.")
            }

            post("/stop") {
                schedulerService.xmlDialogMessages.stop()
                call.respondText("Dialog messages scheduler stopped.")
            }

            post("/interval/{intervalSeconds}") {
                val intervalSeconds = call.parameters["intervalSeconds"]?.toLongOrNull()
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    call.respondText("Invalid interval. Please provide a positive number of seconds.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                schedulerService.xmlDialogMessages.updateInterval(intervalSeconds.seconds)

                call.respondText("Dialog messages scheduler interval updated to $intervalSeconds seconds.")
            }
        }

        route("/json-dialog-messages") {
            post("/start") {
                schedulerService.jsonDialogMessages.start()
                call.respondText("Json dialog messages scheduler started.")
            }

            post("/stop") {
                schedulerService.jsonDialogMessages.stop()
                call.respondText("Json dialog messages scheduler stopped.")
            }

            post("/interval/{intervalSeconds}") {
                val intervalSeconds = call.parameters["intervalSeconds"]?.toLongOrNull()
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    call.respondText("Invalid interval. Please provide a positive number of seconds.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                schedulerService.jsonDialogMessages.updateInterval(intervalSeconds.seconds)

                call.respondText("Json dialog messages scheduler interval updated to $intervalSeconds seconds.")
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
                    call.respondText("Invalid interval. Please provide a positive number of seconds.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                schedulerService.incomingMessages.updateInterval(intervalSeconds.seconds)

                call.respondText("Incoming messages scheduler interval updated to $intervalSeconds seconds.")
            }
        }
    }
}
