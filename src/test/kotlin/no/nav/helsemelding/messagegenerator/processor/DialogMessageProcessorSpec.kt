package no.nav.helsemelding.messagegenerator.processor

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helsemelding.messagegenerator.model.InvalidDialogMessage
import no.nav.helsemelding.messagegenerator.model.ValidDialogMessage
import no.nav.helsemelding.messagegenerator.util.nowWithOffset
import no.nav.helsemelding.messagegenerator.util.readFileToString
import kotlin.uuid.Uuid

class DialogMessageProcessorSpec : StringSpec(
    {
        "Replaces values in xml template" {
            val xml = readFileToString("templates/dialogMessage.xml")!!

            xml shouldContain "{genDate}"
            xml shouldContain "{messageId}"
            xml shouldContain "{herId}"
            xml shouldContain "{patientName}"
            xml shouldContain "{message}"

            val uuid = Uuid.random().toString()
            val name = "Name"
            val message = "Message"

            val genDate = nowWithOffset()
            val myMap = mapOf(
                "{genDate}" to genDate,
                "{messageId}" to uuid,
                "{herId}" to ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID,
                "{patientName}" to name,
                "{message}" to message
            )

            val replacedXml = replaceInTemplate(xml, myMap)

            replacedXml shouldNotContain "{genDate}"
            replacedXml shouldNotContain "{messageId}"
            replacedXml shouldNotContain "{herId}"
            replacedXml shouldNotContain "{patientName}"
            replacedXml shouldNotContain "{message}"

            replacedXml shouldContain "<GenDate>$genDate</GenDate>"
            replacedXml shouldContain "<MsgId>$uuid</MsgId>"
            replacedXml shouldContain "<Id>$ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID</Id>"
            replacedXml shouldContain "<GivenName>$name</GivenName>"
            replacedXml shouldContain "<Sporsmal>$message</Sporsmal>"
        }

        val xml = "<MsgHead></MsgHead>"

        "Number 1 through 9 should create a valid dialog message" {
            (1..9).toList().forAll { number ->
                nextDialogMessage(xml, number).shouldBeInstanceOf<ValidDialogMessage>()
            }
        }

        "Number 10 should create an invalid dialog message" {
            val invalidDialogMessage = nextDialogMessage(xml, 10).shouldBeInstanceOf<InvalidDialogMessage>()
            invalidDialogMessage.id.shouldBeOneOf(null, "", "1234-abcd")
        }
    }
)
