package no.nav.helsemelding.messagegenerator.publisher

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition

private val log = KotlinLogging.logger {}

class FakeDialogMessagePublisher : MessagePublisher {
    override suspend fun publish(
        referenceId: String?,
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

        log.info { "Published message with reference id: $referenceId" }

        return Result.success(metadata)
    }
}
