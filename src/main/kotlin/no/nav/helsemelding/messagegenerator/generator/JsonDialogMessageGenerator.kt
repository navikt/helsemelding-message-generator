package no.nav.helsemelding.messagegenerator.generator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.messagegenerator.publisher.MessagePublisher
import no.nav.helsemelding.messagegenerator.util.readFileToList
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.uuid.Uuid

class JsonDialogMessageGenerator(
    private val messagePublisher: MessagePublisher,
    private val messages: List<String> = readFileToList("messages.txt").orEmpty(),
    private val patientIdents: List<String> = readFileToList("patient-idents.txt").orEmpty(),
    private val providerIds: List<String> = readFileToList("provider-ids.txt").orEmpty()
) {
    private val json = Json { encodeDefaults = true }

    fun generateMessages(scope: CoroutineScope) =
        flowOf(buildMessage())
            .onEach(::publishMessage)
            .flowOn(Dispatchers.IO)
            .launchIn(scope)

    internal fun buildMessage() =
        OutgoingDialogMessage(
            version = 1,
            id = Uuid.random().toString(),
            patientIdent = patientIdents.random(),
            providerId = providerIds.random(),
            conversationReference = null,
            type = OutgoingDialogMessageType.entries.random(),
            message = messages.random(),
            attachment = null
        )

    private suspend fun publishMessage(message: OutgoingDialogMessage): Result<RecordMetadata> =
        messagePublisher.publish(message.id, json.encodeToString(OutgoingDialogMessage.serializer(), message))
}
