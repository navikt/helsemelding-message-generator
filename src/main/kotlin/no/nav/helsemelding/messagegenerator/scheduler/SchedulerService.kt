package no.nav.helsemelding.messagegenerator.scheduler

import kotlinx.coroutines.CoroutineScope
import no.nav.helsemelding.messagegenerator.config.Config
import no.nav.helsemelding.messagegenerator.generator.DialogMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.IncomingMessageGenerator
import no.nav.helsemelding.messagegenerator.generator.JsonDialogMessageGenerator

class SchedulerService(
    scope: CoroutineScope,
    config: Config,
    dialogMessageGenerator: DialogMessageGenerator,
    jsonDialogMessageGenerator: JsonDialogMessageGenerator,
    incomingMessageGenerator: IncomingMessageGenerator
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

    val jsonDialogMessages = ManagedScheduler(
        name = "json-dialog-messages",
        initialEnabled = config.kafka.topics.dialogMessageJson.enabled,
        initialInterval = config.kafka.topics.dialogMessageJson.interval,
        scope = scope,
        action = {
            jsonDialogMessageGenerator.generateMessages(scope)
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

    suspend fun init() {
        dialogMessages.init()
        jsonDialogMessages.init()
        incomingMessages.init()
    }
}
