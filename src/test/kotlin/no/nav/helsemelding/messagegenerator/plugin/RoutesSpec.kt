package no.nav.helsemelding.messagegenerator.plugin

import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

            statusResponse["dialogMessages"] shouldNotBe null
            statusResponse["dialogMessages"]?.enabled shouldBe true
            statusResponse["dialogMessages"]?.interval shouldBe 3.minutes

            statusResponse["incomingMessages"] shouldNotBe null
            statusResponse["incomingMessages"]?.enabled shouldBe true
            statusResponse["incomingMessages"]?.interval shouldBe 4.minutes
        }
    }

    // /scheduler/dialog-messages/stop endpoint disables scheduler
    // /scheduler/incoming-messages/stop endpoint disables scheduler
    withData(
        nameFn = { (stopEndpoint) -> "$stopEndpoint endpoint disables scheduler" },
        listOf(
            listOf("/scheduler/dialog-messages/stop", "dialogMessages"),
            listOf("/scheduler/incoming-messages/stop", "incomingMessages")
        )
    ) { (stopEndpoint, schedulerKey) ->
        routesTestApplication { client ->
            client.post(stopEndpoint)

            val response = client.get("/scheduler/status") {
                accept(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            val statusResponse = response.body<Map<String, SchedulerStatus>>()
            statusResponse[schedulerKey] shouldNotBe null
            statusResponse[schedulerKey]?.enabled shouldBe false
        }
    }

    // /scheduler/dialog-messages/start endpoint enables scheduler
    // /scheduler/incoming-messages/start endpoint enables scheduler
    withData(
        nameFn = { (startEndpoint) -> "$startEndpoint endpoint enables scheduler" },
        listOf(
            listOf("/scheduler/dialog-messages/start", "dialogMessages"),
            listOf("/scheduler/incoming-messages/start", "incomingMessages")
        )
    ) { (startEndpoint, schedulerKey) ->
        routesTestApplication(enableSchedulers = false) { client ->
            client.post(startEndpoint)

            val response = client.get("/scheduler/status") {
                accept(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            val statusResponse = response.body<Map<String, SchedulerStatus>>()
            statusResponse[schedulerKey] shouldNotBe null
            statusResponse[schedulerKey]?.enabled shouldBe true
        }
    }

    // /scheduler/dialog-messages/interval endpoint changes interval for scheduler
    // /scheduler/incoming-messages/interval endpoint changes interval for scheduler
    withData(
        nameFn = { (intervalEndpoint) -> "$intervalEndpoint endpoint changes interval for scheduler" },
        listOf(
            listOf("/scheduler/dialog-messages/interval", "dialogMessages"),
            listOf("/scheduler/incoming-messages/interval", "incomingMessages")
        )
    ) { (intervalEndpoint, schedulerKey) ->
        routesTestApplication(enableSchedulers = false) { client ->
            client.post("$intervalEndpoint/600")

            val response = client.get("/scheduler/status") {
                accept(ContentType.Application.Json)
            }

            val statusResponse = response.body<Map<String, SchedulerStatus>>()
            statusResponse[schedulerKey] shouldNotBe null
            statusResponse[schedulerKey]?.enabled shouldBe true
            statusResponse[schedulerKey]?.interval shouldBe 10.minutes
        }
    }

    // /scheduler/dialog-messages/interval endpoint rejects non-positive values
    // /scheduler/incoming-messages/interval endpoint rejects non-positive values
    withData(
        nameFn = { (intervalEndpoint) -> "$intervalEndpoint endpoint rejects non-positive values" },
        listOf(
            listOf("/scheduler/dialog-messages/interval"),
            listOf("/scheduler/incoming-messages/interval")
        )
    ) { (intervalEndpoint) ->
        routesTestApplication(enableSchedulers = false) { client ->
            val response = client.post("$intervalEndpoint/0")

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldBe "Invalid interval. Please provide a positive number of seconds."
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
