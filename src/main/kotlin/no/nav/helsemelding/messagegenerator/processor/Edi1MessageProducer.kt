package no.nav.helsemelding.messagegenerator.processor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import no.nav.helsemelding.ediadapter.client.EdiAdapterClient
import no.nav.helsemelding.ediadapter.model.EbXmlInfo
import no.nav.helsemelding.ediadapter.model.PostMessageRequest
import no.nav.helsemelding.messagegenerator.util.nowWithOffset
import no.nav.helsemelding.messagegenerator.util.readFileToList
import no.nav.helsemelding.messagegenerator.util.readFileToString
import no.nav.helsemelding.messagegenerator.util.replaceInTemplate
import java.util.Base64
import kotlin.collections.orEmpty
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}
private const val EDI1_BASE64_ENCODING = "base64"

class Edi1MessageProducer(
    private val ediAdapterClient: EdiAdapterClient,
    private val template: String = readFileToString("templates/sykemelding.xml") ?: "",
    private val names: List<String> = readFileToList("names.txt").orEmpty(),
    private val messages: List<String> = readFileToList("messages.txt").orEmpty()
) {
    suspend fun produceEdi1Message() {
        val messageId = Uuid.random().toString()

        val params = mapOf(
            "{genDate}" to nowWithOffset(),
            "{messageId}" to messageId,
            "{senderHerId}" to EPJ_HERID,
            "{receiverHerId}" to FAGSYSTEM_HERID,
            "{patientName}" to names.random(),
            "{message}" to messages.random()
        )

        val xml = replaceInTemplate(template, params)

        val ebXmlOverrides = EbXmlInfo(
            cpaId = "nav:qass:39056",
            role = "Sykmelder",
            service = "Sykmelding",
            action = "Registrering",
            receiverRole = "Saksbehandler"
        )

        ediAdapterClient.postMessage(xml.toPostMessageRequest(ebXmlOverrides))
            .onRight { metadata ->
                log.info {
                    "eMottak test: messageId=$messageId Successfully sent EDI1 message to EDI Adapter with externalRefId=${metadata.id}"
                }
            }
            .onLeft { error ->
                log.error {
                    "eMottak test: messageId=$messageId Failed sending EDI1 message to EDI Adapter: $error"
                }
            }
    }

    private fun String.toPostMessageRequest(ebXmlOverrides: EbXmlInfo? = null): PostMessageRequest = PostMessageRequest(
        businessDocument = Base64.getEncoder().encodeToString(toByteArray()),
        contentType = ContentType.Application.Xml.toString(),
        contentTransferEncoding = EDI1_BASE64_ENCODING,
        ebXmlOverrides = ebXmlOverrides
    )
}
