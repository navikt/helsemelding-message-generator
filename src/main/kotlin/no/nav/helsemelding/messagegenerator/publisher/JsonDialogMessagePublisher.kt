package no.nav.helsemelding.messagegenerator.publisher

import io.github.nomisRev.kafka.publisher.KafkaPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.messagegenerator.config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader

private val log = KotlinLogging.logger {}

const val SOURCE_SYSTEM_HEADER = "sourceSystem"
const val SOURCE_SYSTEM = "helsemelding-message-generator"

class JsonDialogMessagePublisher(
    private val kafkaPublisher: KafkaPublisher<String?, String>
) : MessagePublisher {
    private val kafka = config().kafka.topics

    override suspend fun publish(
        referenceId: String?,
        message: String
    ): Result<RecordMetadata> = kafkaPublisher
        .publishScope {
            publishCatching(toProducerRecord(referenceId, message))
        }
        .onSuccess { log.info { "Published JSON message with reference id: $referenceId to topic: ${kafka.dialogMessageJson.topic}" } }
        .onFailure { t -> log.error { "Failed to publish JSON message with reference id: $referenceId: ${t.stackTraceToString()}" } }

    private fun toProducerRecord(referenceId: String?, message: String) =
        ProducerRecord<String?, String>(
            kafka.dialogMessageJson.topic,
            null,
            null,
            referenceId,
            message
        ).also { record ->
            record.headers().add(RecordHeader(SOURCE_SYSTEM_HEADER, SOURCE_SYSTEM.toByteArray()))
        }
}
