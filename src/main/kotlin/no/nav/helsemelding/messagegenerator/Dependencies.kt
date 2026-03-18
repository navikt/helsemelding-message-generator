package no.nav.helsemelding.messagegenerator

import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.await.awaitAll
import io.github.nomisRev.kafka.publisher.KafkaPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.prometheus.PrometheusConfig.DEFAULT
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helsemelding.ediadapter.client.EdiAdapterClient
import no.nav.helsemelding.ediadapter.client.HttpEdiAdapterClient
import no.nav.helsemelding.ediadapter.client.scopedAuthHttpClient
import no.nav.helsemelding.messagegenerator.config.EdiAdapter
import no.nav.helsemelding.messagegenerator.config.Kafka

private val log = KotlinLogging.logger {}

data class Dependencies(
    val meterRegistry: PrometheusMeterRegistry,
    val kafkaPublisher: KafkaPublisher<String?, ByteArray>,
    val ediAdapterClient: EdiAdapterClient
)

internal suspend fun ResourceScope.metricsRegistry(): PrometheusMeterRegistry =
    install({ PrometheusMeterRegistry(DEFAULT) }) { p, _: ExitCase ->
        p.close().also { log.info { "Closed prometheus registry" } }
    }

internal suspend fun ResourceScope.kafkaPublisher(kafka: Kafka): KafkaPublisher<String?, ByteArray> =
    install({ KafkaPublisher(kafka.toPublisherSettings()) }) { p, _: ExitCase ->
        p.close().also { log.info { "Closed kafka publisher" } }
    }

internal suspend fun ResourceScope.ediAdapterClient(ediAdapter: EdiAdapter): EdiAdapterClient =
    install({ HttpEdiAdapterClient(scopedAuthHttpClient(ediAdapter.scope.value)) }) { p, _: ExitCase ->
        p.close().also { log.info { "Closed Edi Adapter Client" } }
    }

suspend fun ResourceScope.dependencies(): Dependencies = awaitAll {
    val config = config()

    val metricsRegistry = async { metricsRegistry() }
    val kafkaPublisher = async { kafkaPublisher(config.kafka) }
    val ediAdapterClient = async { ediAdapterClient(config.ediAdapter) }

    Dependencies(
        metricsRegistry.await(),
        kafkaPublisher.await(),
        ediAdapterClient.await()
    )
}
