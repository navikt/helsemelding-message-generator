package no.nav.helsemelding.messagegenerator.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.helsemelding.messagegenerator.publisher.FakeDialogMessagePublisher
import kotlin.uuid.Uuid

class JsonDialogMessageGeneratorSpec : StringSpec(
    {
        "Generates a valid OutgoingDialogMessage" {
            val generator = JsonDialogMessageGenerator(
                messagePublisher = FakeDialogMessagePublisher(),
                messages = listOf("Test message"),
                patientIdents = listOf("01449105539"),
                providerIds = listOf("d5741bae-3fc2-420d-ac82-055ffd7c4cb4")
            )

            val message = generator.buildMessage()

            Uuid.parseOrNull(message.id) shouldNotBe null
            message.version shouldBe 1
            message.patientIdent shouldBe "01449105539"
            message.providerId shouldBe "d5741bae-3fc2-420d-ac82-055ffd7c4cb4"
            message.message shouldBe "Test message"
        }
    }
)
