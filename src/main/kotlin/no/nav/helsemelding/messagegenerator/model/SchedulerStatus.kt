package no.nav.helsemelding.messagegenerator.model

import kotlinx.datetime.Instant
import kotlin.time.Duration

data class SchedulerStatus(
    val enabled: Boolean,
    val interval: Duration,
    val lastRunAt: Instant? = null
)
