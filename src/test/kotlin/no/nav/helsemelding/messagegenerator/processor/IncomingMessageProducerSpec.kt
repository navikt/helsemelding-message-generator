package no.nav.helsemelding.messagegenerator.processor

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.http.ContentType
import no.nav.helsemelding.ediadapter.client.EdiAdapterClient
import no.nav.helsemelding.ediadapter.model.ApprecInfo
import no.nav.helsemelding.ediadapter.model.ErrorMessage
import no.nav.helsemelding.ediadapter.model.GetBusinessDocumentResponse
import no.nav.helsemelding.ediadapter.model.GetMessagesRequest
import no.nav.helsemelding.ediadapter.model.Message
import no.nav.helsemelding.ediadapter.model.Metadata
import no.nav.helsemelding.ediadapter.model.PostAppRecRequest
import no.nav.helsemelding.ediadapter.model.PostMessageRequest
import no.nav.helsemelding.ediadapter.model.StatusInfo
import no.nav.helsemelding.messagegenerator.util.readFileToString
import java.util.Base64
import kotlin.uuid.Uuid

class IncomingMessageProducerSpec : StringSpec(
    {
        val template = readFileToString("templates/dialogMessage.xml")!!

        val names = listOf("John")
        val messages = listOf("Dette er en testmelding")

        val ediAdapterClient = FakeEdiAdapterClient()
        val producer = IncomingMessageProducer(
            ediAdapterClient = ediAdapterClient,
            template = template,
            names = names,
            messages = messages
        )

        "processMessage should send correct XML to Edi Adapter" {
            val metadata = Metadata(
                id = Uuid.random(),
                location = "https://example.com/messages/${Uuid.random()}"
            )
            ediAdapterClient.setPostMessageResponse(Right(metadata))

            producer.produceIncomingMessage()

            val request = ediAdapterClient.getPostMessageRequest()
            request shouldNotBe null
            request!!.contentType shouldBeEqual ContentType.Application.Xml.toString()
            request.contentTransferEncoding shouldBeEqual "base64"

            val businessDocument = String(Base64.getDecoder().decode(request.businessDocument))
            businessDocument shouldContain "<Id>$EPJ_HERID</Id>"
            businessDocument shouldContain "<Id>$FAGSYSTEM_HERID</Id>"
            businessDocument shouldContain "<GivenName>${names.single()}</GivenName>"
            businessDocument shouldContain "<Sporsmal>${messages.single()}</Sporsmal>"

            businessDocument shouldNotContain "{genDate}"
            businessDocument shouldNotContain "{messageId}"
            businessDocument shouldNotContain "{senderHerId}"
            businessDocument shouldNotContain "{receiverHerId}"
            businessDocument shouldNotContain "{patientName}"
            businessDocument shouldNotContain "{message}"
        }

        "processMessage should handle an error response from Edi Adapter" {
            ediAdapterClient.setPostMessageResponse(
                Left(
                    ErrorMessage(
                        error = "Internal Server Error",
                        errorCode = 500,
                        requestId = Uuid.random().toString()
                    )
                )
            )

            shouldNotThrowAny {
                producer.produceIncomingMessage()
            }
        }
    }
)

class FakeEdiAdapterClient : EdiAdapterClient {
    private var postMessageResponse: Either<ErrorMessage, Metadata>? = null
    private var postMessageRequest: PostMessageRequest? = null
    private var postMessageThrowsException: Boolean = false

    val errorMessage404 = ErrorMessage(
        error = "Not Implemented",
        errorCode = 404,
        requestId = Uuid.random().toString()
    )

    fun setPostMessageResponse(message: Either<ErrorMessage, Metadata>) {
        postMessageResponse = message
    }

    fun getPostMessageRequest(): PostMessageRequest? = postMessageRequest

    fun setPostMessageThrowsException(throws: Boolean) {
        postMessageThrowsException = throws
    }

    override suspend fun postMessage(postMessagesRequest: PostMessageRequest): Either<ErrorMessage, Metadata> {
        this.postMessageRequest = postMessagesRequest

        if (postMessageThrowsException) {
            throw IllegalStateException("Arbitrary exception")
        }

        return postMessageResponse ?: error("Post message response not set")
    }

    override suspend fun getMessageStatus(id: Uuid): Either<ErrorMessage, List<StatusInfo>> = Left(errorMessage404)

    override suspend fun getMessage(id: Uuid): Either<ErrorMessage, Message> = Left(errorMessage404)

    override suspend fun getBusinessDocument(id: Uuid): Either<ErrorMessage, GetBusinessDocumentResponse> = Left(errorMessage404)

    override suspend fun postApprec(
        id: Uuid,
        apprecSenderHerId: Int,
        postAppRecRequest: PostAppRecRequest
    ): Either<ErrorMessage, Metadata> = Left(errorMessage404)

    override suspend fun markMessageAsRead(id: Uuid, herId: Int): Either<ErrorMessage, Boolean> = Left(errorMessage404)

    override suspend fun getApprecInfo(id: Uuid): Either<ErrorMessage, List<ApprecInfo>> = Left(errorMessage404)

    override suspend fun getMessages(getMessagesRequest: GetMessagesRequest): Either<ErrorMessage, List<Message>> = Left(errorMessage404)

    override fun close() {}
}
