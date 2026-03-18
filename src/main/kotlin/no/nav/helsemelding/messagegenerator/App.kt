package no.nav.helsemelding.messagegenerator

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import arrow.resilience.Schedule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.logError
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import no.nav.helsemelding.messagegenerator.plugin.configureMetrics
import no.nav.helsemelding.messagegenerator.plugin.configureRoutes
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor
import no.nav.helsemelding.messagegenerator.processor.IncomingMessageProducer
import no.nav.helsemelding.messagegenerator.publisher.DialogMessagePublisher
import no.nav.helsemelding.messagegenerator.util.coroutineScope

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()
            val scope = coroutineScope(coroutineContext)

            val dialogMessagePublisher = DialogMessagePublisher(deps.kafkaPublisher)
            val dialogMessageProcessor = DialogMessageProcessor(dialogMessagePublisher)

            val incomingMessageProducer = IncomingMessageProducer(deps.ediAdapterClient)

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = messageGeneratorModule(deps.meterRegistry, dialogMessageProcessor)
            )

            scope.launch {
                scheduleProcessDialogMessages(dialogMessageProcessor)
            }

            scope.launch {
                scheduleGeneratingIncomingMessages(incomingMessageProducer)
            }

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun messageGeneratorModule(
    meterRegistry: PrometheusMeterRegistry,
    dialogMessageProcessor: DialogMessageProcessor
): Application.() -> Unit {
    return {
        configureMetrics(meterRegistry)
        configureRoutes(meterRegistry, dialogMessageProcessor)
    }
}

private suspend fun ResourceScope.scheduleProcessDialogMessages(processor: DialogMessageProcessor) {
    val scheduleConfig = config().kafka.topics.dialogMessage
    if (!scheduleConfig.enabled) {
        return
    }
    val scope = coroutineScope(currentCoroutineContext())
    Schedule
        .spaced<Unit>(scheduleConfig.interval)
        .repeat { processor.processMessages(scope) }
}

private suspend fun ResourceScope.scheduleGeneratingIncomingMessages(producer: IncomingMessageProducer) {
    if (!config().incomingMessages.enabled) {
        return
    }

    Schedule
        .spaced<Unit>(config().incomingMessages.interval)
        .repeat { producer.produceIncomingMessage() }
}

private fun logError(t: Throwable) = log.error { "Shutdown message-generator due to: ${t.stackTraceToString()}" }
