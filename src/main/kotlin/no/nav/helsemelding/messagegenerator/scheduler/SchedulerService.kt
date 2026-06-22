package no.nav.helsemelding.messagegenerator.scheduler

import kotlinx.coroutines.CoroutineScope
import no.nav.helsemelding.messagegenerator.config.Config
import no.nav.helsemelding.messagegenerator.generator.DialogMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.Edi1MessageGenerator
import no.nav.helsemelding.messagegenerator.generator.IncomingMessageGenerator

class SchedulerService(
    scope: CoroutineScope,
    config: Config,
    dialogMessageGenerator: DialogMessageGenerator,
    incomingMessageGenerator: IncomingMessageGenerator,
    edi1MessageGenerator: Edi1MessageGenerator
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
            incomingMessageGenerator.generateIncomingMessage()
        }
    )

    val edi1Messages = ManagedScheduler(
        name = "edi1-messages",
        initialEnabled = config.edi1Messages.enabled,
        initialInterval = config.edi1Messages.interval,
        scope = scope,
        action = {
            edi1MessageGenerator.generateEdi1Message()
        }
    )

    suspend fun init() {
        dialogMessages.init()
        incomingMessages.init()
        edi1Messages.init()
    }
}
