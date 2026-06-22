package no.nav.helsemelding.messagegenerator

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.resourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import no.nav.helsemelding.messagegenerator.generator.DialogMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.IncomingMessageGenerator
import no.nav.helsemelding.messagegenerator.plugin.configureContentNegotiation
import no.nav.helsemelding.messagegenerator.plugin.configureMetrics
import no.nav.helsemelding.messagegenerator.plugin.configureRoutes
import no.nav.helsemelding.messagegenerator.processor.Edi1MessageProducer
import no.nav.helsemelding.messagegenerator.publisher.DialogMessagePublisher
import no.nav.helsemelding.messagegenerator.scheduler.SchedulerService
import no.nav.helsemelding.messagegenerator.util.coroutineScope

private val log = KotlinLogging.logger {}

fun main() = SuspendApp {
    result {
        resourceScope {
            val deps = dependencies()
            val scope = coroutineScope(coroutineContext)

            val dialogMessagePublisher = DialogMessagePublisher(deps.kafkaPublisher)
            val dialogMessageGenerator = DialogMessageGenerator(dialogMessagePublisher)

            val incomingMessageGenerator = IncomingMessageGenerator(deps.ediAdapterClient)

            val edi1MessageProducer = Edi1MessageProducer(deps.ediAdapterClient)

            val schedulerService = SchedulerService(
                scope = scope,
                config = config(),
                dialogMessageGenerator = dialogMessageGenerator,
                incomingMessageGenerator = incomingMessageGenerator,
                edi1MessageProducer = edi1MessageProducer
            )

            server(
                Netty,
                port = config().server.port.value,
                preWait = config().server.preWait,
                module = messageGeneratorModule(
                    deps.meterRegistry,
                    dialogMessageGenerator,
                    incomingMessageGenerator,
                    schedulerService
                )
            )

            schedulerService.init()

            awaitCancellation()
        }
    }
        .onFailure { error -> if (error !is CancellationException) logError(error) }
}

internal fun messageGeneratorModule(
    meterRegistry: PrometheusMeterRegistry,
    dialogMessageGenerator: DialogMessageGenerator,
    incomingMessageGenerator: IncomingMessageGenerator,
    schedulerService: SchedulerService
): Application.() -> Unit {
    return {
        configureMetrics(meterRegistry)
        configureContentNegotiation()
        configureRoutes(
            meterRegistry,
            dialogMessageGenerator,
            incomingMessageGenerator,
            schedulerService
        )
    }
}

private fun logError(t: Throwable) = log.error { "Shutdown message-generator due to: ${t.stackTraceToString()}" }
