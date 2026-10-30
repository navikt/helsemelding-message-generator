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
import no.nav.helsemelding.messagegenerator.util.readFileToString
import kotlin.random.Random
import kotlin.uuid.Uuid

class JsonDialogMessageGenerator(
    private val messagePublisher: MessagePublisher,
    private val messages: List<String> = readFileToList("messages.txt").orEmpty(),
    private val patientIdents: List<String> = readFileToList("patient-idents.txt").orEmpty(),
    private val providerIds: List<String> = readFileToList("provider-ids.txt").orEmpty(),
    private val attachment: String? = readFileToString("attachment.txt"),
    private val random: Random = Random.Default
) {
    private val json = Json { encodeDefaults = true }

    fun generateMessages(scope: CoroutineScope) =
        flowOf(Unit)
            .onEach { publishNext() }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)

    internal fun buildMessage(id: Uuid) =
        OutgoingDialogMessage(
            version = 1,
            id = id,
            patientIdent = patientIdents.random(),
            providerId = providerIds.random(),
            conversationReference = null,
            type = OutgoingDialogMessageType.entries.random(),
            message = messages.random(),
            attachment = if (random.nextBoolean()) attachment else null
        )

    internal suspend fun publishNext() {
        val uuid = Uuid.random()
        val validJson = json.encodeToString(OutgoingDialogMessage.serializer(), buildMessage(uuid))
        when (random.nextInt(10)) {
            0 -> messagePublisher.publish(uuid.toString(), "{ invalid json {{{")
            1 -> messagePublisher.publish(uuid.toString(), """{"foo": "bar"}""")
            2 -> messagePublisher.publishWithoutHeader(uuid.toString(), validJson)
            else -> messagePublisher.publish(uuid.toString(), validJson)
        }
    }
}
