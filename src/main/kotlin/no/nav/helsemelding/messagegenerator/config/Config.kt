package no.nav.helsemelding.messagegenerator.config

import com.sksamuel.hoplite.Masked
import io.github.nomisRev.kafka.publisher.PublisherSettings
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import kotlin.time.Duration

data class Config(
    val server: Server,
    val kafka: Kafka,
    val kafkaTopics: KafkaTopics
)

data class Server(
    val port: Port,
    val preWait: Duration
)

data class Kafka(
    val groupId: String,
    val bootstrapServers: String,
    val securityProtocol: SecurityProtocol,
    val keystoreType: KeystoreType,
    val keystoreLocation: KeystoreLocation,
    val keystorePassword: Masked,
    val truststoreType: TruststoreType,
    val truststoreLocation: TruststoreLocation,
    val truststorePassword: Masked
) {
    private val securityProtocolConfig = "security.protocol"
    private val sslKeystoreTypeConfig = "ssl.keystore.type"
    private val sslKeystoreLocationConfig = "ssl.keystore.location"
    private val sslKeystorePasswordConfig = "ssl.keystore.password"
    private val sslTruststoreTypeConfig = "ssl.truststore.type"
    private val sslTruststoreLocationConfig = "ssl.truststore.location"
    private val sslTruststorePasswordConfig = "ssl.truststore.password"

    @JvmInline
    value class SecurityProtocol(val value: String)

    @JvmInline
    value class KeystoreType(val value: String)

    @JvmInline
    value class KeystoreLocation(val value: String)

    @JvmInline
    value class TruststoreType(val value: String)

    @JvmInline
    value class TruststoreLocation(val value: String)

    fun toPublisherSettings(): PublisherSettings<String, ByteArray> =
        PublisherSettings(
            bootstrapServers = bootstrapServers,
            keySerializer = StringSerializer(),
            valueSerializer = ByteArraySerializer(),
            properties = toProperties()
        )

    private fun toProperties() = Properties()
        .apply {
            put(securityProtocolConfig, securityProtocol.value)
            put(sslKeystoreTypeConfig, keystoreType.value)
            put(sslKeystoreLocationConfig, keystoreLocation.value)
            put(sslKeystorePasswordConfig, keystorePassword.value)
            put(sslTruststoreTypeConfig, truststoreType.value)
            put(sslTruststoreLocationConfig, truststoreLocation.value)
            put(sslTruststorePasswordConfig, truststorePassword.value)
        }
}

data class KafkaTopics(
    val dialogMessage: DialogMessage
)

data class DialogMessage(
    val topic: String,
    val enabled: Boolean,
    val fixedInterval: Duration
)

@JvmInline
value class Port(val value: Int)
