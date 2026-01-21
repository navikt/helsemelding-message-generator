package no.nav.helsemelding.messagegenerator

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import arrow.resilience.Schedule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import no.nav.helsemelding.messagegenerator.plugin.configureMetrics
import no.nav.helsemelding.messagegenerator.plugin.configureRoutes
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor
import no.nav.helsemelding.messagegenerator.publisher.DialogMessagePublisher
import no.nav.helsemelding.messagegenerator.util.coroutineScope
import no.nav.helsemelding.messagegenerator.util.readFileToList
import no.nav.helsemelding.messagegenerator.util.readFileToString

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = messageGeneratorModule(deps.meterRegistry)
            )

            val dialogMessagePublisher = DialogMessagePublisher(deps.kafkaPublisher)
            // TODO: What should happen if config is missing?
            val names = readFileToList("names.txt")
            val messages = readFileToList("messages.txt")
            val dialogMessageTemplate = readFileToString("templates/dialogMessage.xml") ?: ""
            val dialogMessageProcessor = DialogMessageProcessor(
                dialogMessagePublisher,
                dialogMessageTemplate,
                names,
                messages
            )

            scheduleProcessDialogMessages(dialogMessageProcessor)

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun messageGeneratorModule(
    meterRegistry: PrometheusMeterRegistry
): Application.() -> Unit {
    return {
        configureMetrics(meterRegistry)
        configureRoutes(meterRegistry)
    }
}

private suspend fun ResourceScope.scheduleProcessDialogMessages(processor: DialogMessageProcessor): Long {
    val scope = coroutineScope(currentCoroutineContext())
    return Schedule
        .spaced<Unit>(config().kafkaTopics.dialogMessage.fixedInterval)
        .repeat { processor.processMessages(scope) }
}

private fun logError(t: Throwable) = log.error { "Shutdown message-generator due to: ${t.stackTraceToString()}" }
