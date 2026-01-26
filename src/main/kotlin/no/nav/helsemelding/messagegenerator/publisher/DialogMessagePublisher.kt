package no.nav.helsemelding.messagegenerator.publisher

import io.github.nomisRev.kafka.publisher.KafkaPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.messagegenerator.config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

interface MessagePublisher {
    suspend fun publish(referenceId: Uuid, message: String): Result<RecordMetadata>
}

class DialogMessagePublisher(
    private val kafkaPublisher: KafkaPublisher<String, ByteArray>
) : MessagePublisher {
    private val kafka = config().kafkaTopics

    override suspend fun publish(
        referenceId: Uuid,
        message: String
    ): Result<RecordMetadata> = kafkaPublisher
        .publishScope {
            publishCatching(toProducerRecord(referenceId, message))
        }
        .onSuccess { log.info { "Published message with reference id $referenceId to topic: ${kafka.dialogMessage.topic}" } }
        .onFailure { log.error { "Failed to publish message with reference id: $referenceId" } }

    private fun toProducerRecord(referenceId: Uuid, message: String) =
        ProducerRecord(
            kafka.dialogMessage.topic,
            referenceId.toString(),
            message.toByteArray()
        )
}

class FakeDialogMessagePublisher : MessagePublisher {
    override suspend fun publish(
        referenceId: Uuid,
        message: String
    ): Result<RecordMetadata> {
        val metadata = RecordMetadata(
            TopicPartition("TOPIC", 0),
            0L,
            0,
            System.currentTimeMillis(),
            referenceId.toString().length,
            message.toByteArray().size
        )

        return Result.success(metadata)
    }
}
