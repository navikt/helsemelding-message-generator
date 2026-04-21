package no.nav.helsemelding.messagegenerator.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class SchedulerStatus(
    val enabled: Boolean,
    val interval: Duration,
    val lastRunAt: Instant? = null
)
