package no.nav.helsemelding.messagegenerator.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.offsetAt

/**
 * Returns the current date and time with offset using ISO 8601 format.
 *
 * Example: `2026-08-30T19:43:00.123456789+01:00`
 */
fun nowWithOffset(now: Instant = Clock.System.now()): String {
    val offsetNow = TimeZone.currentSystemDefault().offsetAt(now)
    return now.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET, offsetNow)
}
