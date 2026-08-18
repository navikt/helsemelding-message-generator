package no.nav.helsemelding.messagegenerator.publisher

import io.github.nomisRev.kafka.publisher.KafkaPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.messagegenerator.config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

private val log = KotlinLogging.logger {}

interface MessagePublisher {
    suspend fun publish(referenceId: String?, message: String): Result<RecordMetadata>
    suspend fun publishWithoutHeader(referenceId: String?, message: String): Result<RecordMetadata> = publish(referenceId, message)
}

class XmlDialogMessagePublisher(
    private val kafkaPublisher: KafkaPublisher<String?, ByteArray>
) : MessagePublisher {
    private val kafka = config().kafka.topics

    override suspend fun publish(
        referenceId: String?,
        message: String
    ): Result<RecordMetadata> = kafkaPublisher
        .publishScope {
            publishCatching(toProducerRecord(referenceId, message))
        }
        .onSuccess { log.info { "Published message with reference id: $referenceId to topic: ${kafka.dialogMessage.topic}" } }
        .onFailure { t -> log.error { "Failed to publish message with reference id: $referenceId: ${t.stackTraceToString()}" } }

    private fun toProducerRecord(referenceId: String?, message: String) =
        ProducerRecord(
            kafka.dialogMessage.topic,
            referenceId,
            message.toByteArray()
        )
}
