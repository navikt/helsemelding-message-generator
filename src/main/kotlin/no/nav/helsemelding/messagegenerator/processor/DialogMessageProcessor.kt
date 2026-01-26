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
import no.nav.helsemelding.messagegenerator.publisher.DialogMessagePublisher
import no.nav.helsemelding.messagegenerator.util.nowWithOffset
import no.nav.helsemelding.messagegenerator.util.readFileToList
import no.nav.helsemelding.messagegenerator.util.readFileToString
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.uuid.Uuid

const val ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1 = "8142519"

class DialogMessageProcessor(
    private val dialogMessagePublisher: DialogMessagePublisher,
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
            "{herId}" to ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1,
            "{patientName}" to names.random(),
            "{message}" to messages.random()
        )
        val xml = replaceInTemplate(template, params)
        return flowOf(DialogMessage(uuid, xml))
    }

    private suspend fun publishDialogMessage(dialogMessage: DialogMessage): Result<RecordMetadata> =
        dialogMessagePublisher.publish(dialogMessage.id, dialogMessage.xml)
}

fun replaceInTemplate(template: String, params: Map<String, String>): String =
    params.fold(template) { acc, (key, value) ->
        acc.replace(key, value)
    }
