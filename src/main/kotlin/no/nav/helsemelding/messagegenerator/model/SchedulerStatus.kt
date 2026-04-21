package no.nav.helsemelding.messagegenerator.model

import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class SchedulerStatus(
    val enabled: Boolean,
    val interval: Duration,
    val lastRunAt: Instant? = null
)
