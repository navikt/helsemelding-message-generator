package no.nav.helsemelding.messagegenerator.publisher

import io.github.nomisRev.kafka.publisher.KafkaPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helsemelding.messagegenerator.config
import no.nav.helsemelding.messagegenerator.model.PayloadDialogMessage
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

class DialogMessagePublisher(
    private val kafkaPublisher: KafkaPublisher<String, ByteArray>
) {
    private val kafka = config().kafkaTopics

    suspend fun publish(message: PayloadDialogMessage): Result<RecordMetadata> =
        publishMessage(message.id, message.payload)

    private suspend fun publishMessage(
        referenceId: Uuid,
        ebxml: ByteArray
    ): Result<RecordMetadata> = kafkaPublisher
        .publishScope {
            publishCatching(toProducerRecord(referenceId, ebxml))
        }
        .onSuccess { log.info { "Published message with reference id $referenceId to topic: ${kafka.dialogMessage.topic}" } }
        .onFailure { log.error { "Failed to publish message with reference id: $referenceId" } }

    private fun toProducerRecord(referenceId: Uuid, ebxml: ByteArray) =
        ProducerRecord(
            kafka.dialogMessage.topic,
            referenceId.toString(),
            ebxml
        )
}
