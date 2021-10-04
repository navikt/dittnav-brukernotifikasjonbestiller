package no.nav.personbruker.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventIdAndSystembruker
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithGrupperingsId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyTransformer
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatusoppdateringIT {
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(
            KafkaTestTopics.statusoppdateringLegacyTopicName,
            KafkaTestTopics.statusoppdateringInternTopicName,
            KafkaTestTopics.feilresponsTopicName
    ))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val producerNameAlias = "dittnav"
    private val client = getClient(producerNameAlias)
    private val metricsReporter = StubMetricsReporter()
    private val nameResolver = ProducerNameResolver(client, testEnvironment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)
    private val producerServiceUser = "dummySystembruker"
    private val producerNamespace = "namespace"
    private val producerAppName = "appName"
    private val mapper = ServiceUserMapper(mapOf(producerServiceUser to NamespaceAppName(producerNamespace, producerAppName)))
    private val statusoppdateringTransformer = StatusoppdateringLegacyTransformer(mapper)
    private val feilresponsTransformer = FeilresponsLegacyTransformer(mapper)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(createEventWithTooLongGroupId("bad"))
    private val statusoppdateringEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    @BeforeAll
    fun setup() {
        embeddedEnv.start()
    }

    @AfterAll
    fun tearDown() {
        embeddedEnv.tearDown()
    }

    @Test
    fun `Started Kafka instance in memory`() {
        embeddedEnv.serverPark.status `should be equal to` KafkaEnvironment.ServerParkStatus.Started
    }

    @Test
    fun `Should read Statusoppdatering-events and send to hoved-topic or error response topic as appropriate`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, KafkaTestTopics.statusoppdateringLegacyTopicName, statusoppdateringEvents)
        } shouldBeEqualTo true

        `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERING, enableSecurity = false)
        val kafkaConsumer = KafkaConsumer<Nokkel, Statusoppdatering>(consumerProps)

        val statusoppdateringInternProducerProps = Kafka.producerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN)
        val internalKafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(statusoppdateringInternProducerProps)
        val internalEventProducer = Producer(KafkaTestTopics.statusoppdateringInternTopicName, internalKafkaProducer)

        val feilresponsProducerProps = Kafka.producerProps(testEnvironment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps)
        val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

        val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val eventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

        val eventService = StatusoppdateringLegacyEventService(statusoppdateringTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)
        val consumer = Consumer(KafkaTestTopics.statusoppdateringLegacyTopicName, kafkaConsumer, eventService)

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            `Wait until all statusoppdatering events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all statusoppdatering events have been received by target topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN, enableSecurity = false)
        val targetKafkaConsumer = KafkaConsumer<NokkelIntern, StatusoppdateringIntern>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<NokkelIntern, StatusoppdateringIntern>()

        val targetConsumer = Consumer(KafkaTestTopics.statusoppdateringInternTopicName, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0

        targetConsumer.startPolling()

        while (currentNumberOfRecords < goodEvents.size) {
            runBlocking {
                currentNumberOfRecords = capturingProcessor.getEvents().size
                delay(100)
            }
        }

        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedInternalRecords.addAll(capturingProcessor.getEvents())
    }


    private fun `Wait until bad event has been received by error topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.FEILRESPONS, enableSecurity = false)
        val targetKafkaConsumer = KafkaConsumer<NokkelFeilrespons, Feilrespons>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<NokkelFeilrespons, Feilrespons>()

        val targetConsumer = Consumer(KafkaTestTopics.feilresponsTopicName, targetKafkaConsumer, capturingProcessor)

        var receivedEvent = false

        targetConsumer.startPolling()

        while (!receivedEvent) {
            runBlocking {
                receivedEvent = capturingProcessor.getEvents().isNotEmpty()
                delay(100)
            }
        }

        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedErrorResponseRecords.addAll(capturingProcessor.getEvents())
    }

    private fun createEvents(number: Int) = (1..number).map {
        createNokkelLegacyWithEventIdAndSystembruker(it.toString(), producerServiceUser) to createStatusoppdateringLegacyWithGrupperingsId(it.toString())
    }

    private fun createEventWithTooLongGroupId(eventId: String): Pair<Nokkel, Statusoppdatering> {
        val groupId = "groupId".repeat(100)

        return createNokkelLegacyWithEventIdAndSystembruker(eventId, producerServiceUser) to createStatusoppdateringLegacyWithGrupperingsId(groupId)
    }
}
