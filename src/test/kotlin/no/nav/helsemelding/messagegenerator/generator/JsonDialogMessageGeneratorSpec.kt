package no.nav.helsemelding.messagegenerator.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.messagegenerator.publisher.CapturingPublisher
import no.nav.helsemelding.messagegenerator.publisher.FakeDialogMessagePublisher
import no.nav.helsemelding.messagegenerator.publisher.MessagePublisher
import kotlin.random.Random
import kotlin.uuid.Uuid

private class FixedRandom(private val bits: Int) : Random() {
    override fun nextBits(bitCount: Int) = bits
}

class JsonDialogMessageGeneratorSpec : StringSpec(
    {
        "Generates a valid OutgoingDialogMessage" {
            val message = generator(Random.Default).buildMessage()

            Uuid.parseOrNull(message.id) shouldNotBe null
            message.version shouldBe 1
            message.patientIdent shouldBe "01449105539"
            message.providerId shouldBe "d5741bae-3fc2-420d-ac82-055ffd7c4cb4"
            message.message shouldBe "Test message"
        }

        "publishNext sends message with invalid key" {
            val publisher = CapturingPublisher()
            generator(FixedRandom(0), publisher).publishNext()

            val call = publisher.calls.first()
            Uuid.parseOrNull(call.key!!) shouldBe null
            call.withoutHeader shouldBe false
        }

        "publishNext sends invalid JSON" {
            val publisher = CapturingPublisher()
            generator(FixedRandom(2), publisher).publishNext()

            val call = publisher.calls.first()
            Uuid.parseOrNull(call.key!!) shouldNotBe null
            call.value shouldBe "{ invalid json {{{"
        }

        "publishNext sends valid JSON with wrong structure" {
            val publisher = CapturingPublisher()
            generator(FixedRandom(4), publisher).publishNext()

            val call = publisher.calls.first()
            Uuid.parseOrNull(call.key!!) shouldNotBe null
            call.value shouldBe """{"foo": "bar"}"""
        }

        "publishNext sends message without sourceSystem header" {
            val publisher = CapturingPublisher()
            generator(FixedRandom(6), publisher).publishNext()

            val call = publisher.calls.first()
            Uuid.parseOrNull(call.key!!) shouldNotBe null
            call.withoutHeader shouldBe true
        }

        "publishNext sends valid message" {
            val publisher = CapturingPublisher()
            generator(FixedRandom(8), publisher).publishNext()

            val call = publisher.calls.first()
            Uuid.parseOrNull(call.key!!) shouldNotBe null
            call.withoutHeader shouldBe false

            val decoded = Json.decodeFromString(OutgoingDialogMessage.serializer(), call.value)
            Uuid.parseOrNull(decoded.id) shouldNotBe null
            decoded.version shouldBe 1
            decoded.patientIdent shouldBe "01449105539"
            decoded.providerId shouldBe "d5741bae-3fc2-420d-ac82-055ffd7c4cb4"
            decoded.message shouldBe "Test message"
        }
    }
)

private fun generator(random: Random, publisher: MessagePublisher = FakeDialogMessagePublisher()) =
    JsonDialogMessageGenerator(
        messagePublisher = publisher,
        messages = listOf("Test message"),
        patientIdents = listOf("01449105539"),
        providerIds = listOf("d5741bae-3fc2-420d-ac82-055ffd7c4cb4"),
        random = random
    )
