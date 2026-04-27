package no.nav.helsemelding.messagegenerator.plugin

import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import no.nav.helsemelding.messagegenerator.config.Config
import no.nav.helsemelding.messagegenerator.config.DialogMessage
import no.nav.helsemelding.messagegenerator.config.EdiAdapter
import no.nav.helsemelding.messagegenerator.config.IncomingMessages
import no.nav.helsemelding.messagegenerator.config.Kafka
import no.nav.helsemelding.messagegenerator.config.Port
import no.nav.helsemelding.messagegenerator.config.Scope
import no.nav.helsemelding.messagegenerator.config.Server
import no.nav.helsemelding.messagegenerator.config.Topics
import no.nav.helsemelding.messagegenerator.model.SchedulerStatus
import no.nav.helsemelding.messagegenerator.processor.DialogMessageProcessor
import no.nav.helsemelding.messagegenerator.processor.FakeEdiAdapterClient
import no.nav.helsemelding.messagegenerator.processor.IncomingMessageProducer
import no.nav.helsemelding.messagegenerator.publisher.FakeDialogMessagePublisher
import no.nav.helsemelding.messagegenerator.scheduler.SchedulerService
import kotlin.time.Duration.Companion.minutes

class RoutesSpec : StringSpec({
    "/scheduler/status endpoint returns json payload" {
        routesTestApplication { client ->
            val response = client.get("/scheduler/status") {
                accept(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            val statusResponse = response.body<Map<String, SchedulerStatus>>()

            val dialogMessageScheduler = statusResponse["dialogMessages"]
            dialogMessageScheduler.shouldNotBeNull()
            dialogMessageScheduler.enabled shouldBe true
            dialogMessageScheduler.interval shouldBe 3.minutes

            val incomingMessagesScheduler = statusResponse["incomingMessages"]
            incomingMessagesScheduler.shouldNotBeNull()
            incomingMessagesScheduler.enabled shouldBe true
            incomingMessagesScheduler.interval shouldBe 4.minutes
        }
    }

    "/stop endpoints disable schedulers" {
        routesTestApplication { client ->
            val testCases = listOf(
                listOf("/scheduler/dialog-messages/stop", "dialogMessages"),
                listOf("/scheduler/incoming-messages/stop", "incomingMessages")
            )

            testCases.forEach { (stopEndpoint, schedulerKey) ->
                client.post(stopEndpoint)

                val response = client.get("/scheduler/status") {
                    accept(ContentType.Application.Json)
                }

                response.status shouldBe HttpStatusCode.OK

                val statusResponse = response.body<Map<String, SchedulerStatus>>()

                val scheduler = statusResponse[schedulerKey]
                scheduler.shouldNotBeNull()
                scheduler.enabled shouldBe false
            }
        }
    }

    "/start endpoints enable schedulers" {
        routesTestApplication(enableSchedulers = false) { client ->
            val testCases = listOf(
                listOf("/scheduler/dialog-messages/start", "dialogMessages"),
                listOf("/scheduler/incoming-messages/start", "incomingMessages")
            )

            testCases.forEach { (startEndpoint, schedulerKey) ->
                client.post(startEndpoint)

                val response = client.get("/scheduler/status") {
                    accept(ContentType.Application.Json)
                }

                response.status shouldBe HttpStatusCode.OK

                val statusResponse = response.body<Map<String, SchedulerStatus>>()

                val scheduler = statusResponse[schedulerKey]
                scheduler.shouldNotBeNull()
                scheduler.enabled shouldBe true
            }
        }
    }

    "/interval/{intervalSeconds} endpoint changes interval for scheduler" {
        routesTestApplication { client ->
            val testCases = listOf(
                listOf("/scheduler/dialog-messages/interval", "dialogMessages"),
                listOf("/scheduler/incoming-messages/interval", "incomingMessages")
            )

            testCases.forEach { (intervalEndpoint, schedulerKey) ->
                client.post("$intervalEndpoint/600")

                val response = client.get("/scheduler/status") {
                    accept(ContentType.Application.Json)
                }

                val statusResponse = response.body<Map<String, SchedulerStatus>>()

                val scheduler = statusResponse[schedulerKey]
                scheduler.shouldNotBeNull()
                scheduler.enabled shouldBe true
                scheduler.interval shouldBe 10.minutes
            }
        }
    }

    "/interval/{intervalSeconds} endpoint rejects non-positive values" {
        routesTestApplication { client ->
            val testCases = listOf(
                listOf("/scheduler/dialog-messages/interval", 0),
                listOf("/scheduler/dialog-messages/interval", -60),
                listOf("/scheduler/incoming-messages/interval", 0),
                listOf("/scheduler/incoming-messages/interval", -60)
            )

            testCases.forEach { (intervalEndpoint, intervalSeconds) ->
                val response = client.post("$intervalEndpoint/$intervalSeconds")

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe "Invalid interval. Please provide a positive number of seconds."
            }
        }
    }
})

private fun routesTestApplication(
    enableSchedulers: Boolean = true,
    testBlock: suspend ApplicationTestBuilder.(client: HttpClient) -> Unit
) = testApplication {
    val schedulerService: SchedulerService = testSchedulerService(enableSchedulers)
    application {
        configureContentNegotiation()
        configureRoutes(
            registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            dialogMessageProcessor = DialogMessageProcessor(FakeDialogMessagePublisher()),
            schedulerService = schedulerService
        )
    }

    val client = createClient {
        install(ContentNegotiation) {
            json()
        }
    }

    testBlock(client)
}

private fun testSchedulerService(enableSchedulers: Boolean): SchedulerService {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val config = Config(
        server = Server(Port(8080), 1.minutes),
        kafka = Kafka(
            groupId = "test-group",
            bootstrapServers = "localhost:9092",
            securityProtocol = Kafka.SecurityProtocol("SSL"),
            keystoreType = Kafka.KeystoreType("JKS"),
            keystoreLocation = Kafka.KeystoreLocation("/tmp/keystore.jks"),
            keystorePassword = Masked("secret"),
            truststoreType = Kafka.TruststoreType("JKS"),
            truststoreLocation = Kafka.TruststoreLocation("/tmp/truststore.jks"),
            truststorePassword = Masked("secret"),
            topics = Topics(
                dialogMessage = DialogMessage(
                    topic = "dialog-topic",
                    enabled = enableSchedulers,
                    interval = 3.minutes
                )
            )
        ),
        ediAdapter = EdiAdapter(Scope("api://test/.default")),
        incomingMessages = IncomingMessages(
            enabled = enableSchedulers,
            interval = 4.minutes
        )
    )

    return SchedulerService(
        scope = scope,
        config = config,
        dialogMessageProcessor = DialogMessageProcessor(FakeDialogMessagePublisher()),
        incomingMessageProducer = IncomingMessageProducer(
            ediAdapterClient = FakeEdiAdapterClient(),
            template = "<MsgHead></MsgHead>",
            names = listOf("Test Person"),
            messages = listOf("Test message")
        )
    )
}
