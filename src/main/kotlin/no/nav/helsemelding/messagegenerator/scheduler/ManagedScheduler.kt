package no.nav.helsemelding.messagegenerator.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.nav.helsemelding.messagegenerator.model.SchedulerStatus
import kotlin.time.Duration

private val log = KotlinLogging.logger {}

class ManagedScheduler(
    private val name: String,
    initialEnabled: Boolean,
    initialInterval: Duration,
    private val scope: CoroutineScope,
    private val action: suspend () -> Unit
) {
    private val mutex = Mutex()

    private var enabled: Boolean = initialEnabled
    private var interval: Duration = initialInterval
    private var lastRunAt: Instant? = null
    private var job: Job? = null

    suspend fun init() {
        if (enabled) start()
    }

    suspend fun start() = mutex.withLock {
        enabled = true
        if (job?.isActive == true) return

        job = scope.launch {
            log.info { "Starting scheduler '$name' with interval=$interval" }
            while (isActive) {
                try {
                    action()
                    mutex.withLock {
                        lastRunAt = Clock.System.now()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.error(e) { "Scheduled action failed for '$name'" }
                }

                val currentInterval = mutex.withLock { interval }
                delay(currentInterval)
            }
        }
    }

    suspend fun stop() = mutex.withLock {
        enabled = false
        job?.cancel()
        job = null
        log.info { "Stopped scheduler '$name'" }
    }

    suspend fun updateInterval(newInterval: Duration) {
        stop()
        mutex.withLock {
            interval = newInterval
            log.info { "Updated scheduler '$name' interval to $newInterval" }
        }
        start()
    }

    suspend fun status(): SchedulerStatus = mutex.withLock {
        SchedulerStatus(
            enabled = enabled,
            interval = interval,
            lastRunAt = lastRunAt
        )
    }
}
