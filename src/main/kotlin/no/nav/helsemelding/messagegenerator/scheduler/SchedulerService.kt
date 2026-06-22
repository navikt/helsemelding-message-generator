package no.nav.helsemelding.messagegenerator.scheduler

import kotlinx.coroutines.CoroutineScope
import no.nav.helsemelding.messagegenerator.config.Config
import no.nav.helsemelding.messagegenerator.generator.DialogMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.IncomingMessageProducer

class SchedulerService(
    scope: CoroutineScope,
    config: Config,
    dialogMessageGenerator: DialogMessageGenerator,
    incomingMessageProducer: IncomingMessageProducer
) {
    val dialogMessages = ManagedScheduler(
        name = "dialog-messages",
        initialEnabled = config.kafka.topics.dialogMessage.enabled,
        initialInterval = config.kafka.topics.dialogMessage.interval,
        scope = scope,
        action = {
            dialogMessageGenerator.generateMessages(scope)
        }
    )

    val incomingMessages = ManagedScheduler(
        name = "incoming-messages",
        initialEnabled = config.incomingMessages.enabled,
        initialInterval = config.incomingMessages.interval,
        scope = scope,
        action = {
            incomingMessageProducer.produceIncomingMessage()
        }
    )

    suspend fun init() {
        dialogMessages.init()
        incomingMessages.init()
    }
}
