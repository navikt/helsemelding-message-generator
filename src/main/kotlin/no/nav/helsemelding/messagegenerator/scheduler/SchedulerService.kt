package no.nav.helsemelding.messagegenerator.scheduler

import kotlinx.coroutines.CoroutineScope
import no.nav.helsemelding.messagegenerator.config.Config
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor
import no.nav.helsemelding.messagegenerator.processor.IncomingMessageProducer

class SchedulerService(
    scope: CoroutineScope,
    config: Config,
    dialogMessageProcessor: DialogMessageProcessor,
    incomingMessageProducer: IncomingMessageProducer
) {
    val dialogMessages = ManagedScheduler(
        name = "dialog-messages",
        initialEnabled = config.kafka.topics.dialogMessage.enabled,
        initialInterval = config.kafka.topics.dialogMessage.interval,
        scope = scope,
        action = {
            dialogMessageProcessor.processMessages(scope)
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
