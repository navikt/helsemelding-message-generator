package no.nav.helsemelding.messagegenerator.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

class DateTimeUtilSpec : StringSpec(
    {
        "UTC time should be formatted with offset" {
            val instant = Instant.parse("2026-01-20T18:43:00.123456789Z")

            nowWithOffset(instant) shouldBe "2026-01-20T19:43:00.123456789+01:00"
        }
    }
)
