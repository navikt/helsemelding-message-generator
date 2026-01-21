package no.nav.helsemelding.messagegenerator.processor

import arrow.autoCloseScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nav.helsemelding.messagegenerator.model.PayloadDialogMessage
import no.nav.helsemelding.messagegenerator.publisher.DialogMessagePublisher
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.uuid.Uuid

const val HELSEOPPLYSNINGER_TEST1 = "8142519"

class DialogMessageProcessor(
    private val dialogMessagePublisher: DialogMessagePublisher,
    private val dialogMessageTemplate: String,
    private val names: List<String>,
    private val messages: List<String>
) {
    fun processMessages(scope: CoroutineScope) =
        readMessages()
            .onEach(::publishDialogMessage)
            .flowOn(Dispatchers.IO)
            .launchIn(scope)

    private fun readMessages(): Flow<PayloadDialogMessage> = autoCloseScope {
        val randomName = names.random()
        val randomMessage = messages.random()

        flow {
            dialogMessageTemplate
                .replace("{genDate}", "2019-01-16T22:51:35.5317672+01:00") // TODO: UTC
                .replace("{messageId}", Uuid.random().toString())
                .replace("{herId}", HELSEOPPLYSNINGER_TEST1)
                .replace("{patientName}", randomName)
                .replace("{message}", randomMessage)
        }
    }

    private suspend fun publishDialogMessage(dialogMessage: PayloadDialogMessage): Result<RecordMetadata> =
        dialogMessagePublisher.publish(dialogMessage)
}
