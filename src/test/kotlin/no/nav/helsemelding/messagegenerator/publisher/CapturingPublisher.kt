package no.nav.helsemelding.messagegenerator.publisher

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition

class CapturingPublisher : MessagePublisher {
    data class Call(val key: String?, val value: String, val withoutHeader: Boolean = false)

    val calls = mutableListOf<Call>()

    override suspend fun publish(referenceId: String?, message: String): Result<RecordMetadata> {
        calls.add(Call(referenceId, message))
        return Result.success(RecordMetadata(TopicPartition("TOPIC", 0), 0L, 0, 0L, 0, 0))
    }

    override suspend fun publishWithoutHeader(referenceId: String?, message: String): Result<RecordMetadata> {
        calls.add(Call(referenceId, message, withoutHeader = true))
        return Result.success(RecordMetadata(TopicPartition("TOPIC", 0), 0L, 0, 0L, 0, 0))
    }
}
