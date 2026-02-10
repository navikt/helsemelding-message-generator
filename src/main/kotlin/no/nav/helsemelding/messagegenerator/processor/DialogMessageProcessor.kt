package no.nav.helsemelding.messagegenerator.processor

import arrow.core.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nav.helsemelding.messagegenerator.model.DialogMessage
import no.nav.helsemelding.messagegenerator.model.InvalidDialogMessage
import no.nav.helsemelding.messagegenerator.model.ValidDialogMessage
import no.nav.helsemelding.messagegenerator.publisher.MessagePublisher
import no.nav.helsemelding.messagegenerator.util.nowWithOffset
import no.nav.helsemelding.messagegenerator.util.readFileToList
import no.nav.helsemelding.messagegenerator.util.readFileToString
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.uuid.Uuid

const val ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID = "8142519"
val invalidRecordKeys = listOf(null, "", "1234-abcd")

class DialogMessageProcessor(
    private val messagePublisher: MessagePublisher,
    private val template: String = readFileToString("templates/dialogMessage.xml") ?: "",
    private val names: List<String> = readFileToList("names.txt").orEmpty(),
    private val messages: List<String> = readFileToList("messages.txt").orEmpty()
) {
    fun processMessages(scope: CoroutineScope) =
        messageFlow()
            .onEach(::publishDialogMessage)
            .flowOn(Dispatchers.IO)
            .launchIn(scope)

    private fun messageFlow(): Flow<DialogMessage> {
        val uuid = Uuid.random()
        val params = mapOf(
            "{genDate}" to nowWithOffset(),
            "{messageId}" to uuid.toString(),
            "{herId}" to ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID,
            "{patientName}" to names.random(),
            "{message}" to messages.random()
        )
        val xml = replaceInTemplate(template, params)
        return flowOf(nextDialogMessage(xml, (1..10).random()))
    }

    private suspend fun publishDialogMessage(dialogMessage: DialogMessage): Result<RecordMetadata> =
        messagePublisher.publish(dialogMessage.id, dialogMessage.xml)
}

internal fun nextDialogMessage(xml: String, number: Int): DialogMessage {
    return when (number) {
        in 1..9 -> ValidDialogMessage(Uuid.random(), xml)
        else -> InvalidDialogMessage(invalidRecordKeys.random(), xml)
    }
}

fun replaceInTemplate(template: String, params: Map<String, String>): String =
    params.fold(template) { acc, (key, value) ->
        acc.replace(key, value)
    }
