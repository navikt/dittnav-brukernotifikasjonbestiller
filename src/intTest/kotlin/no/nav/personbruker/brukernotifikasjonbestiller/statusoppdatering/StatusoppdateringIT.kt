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
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.H2Database
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother.createNokkelWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringEventService
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
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(Kafka.statusoppdateringInputTopicName, Kafka.statusoppdateringHovedTopicName, Kafka.feilresponsTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = H2Database()

    private val goodEvents = createEvents(10)
    private val badEvent = createEventWithTooLongGroupId("bad")
    private val statusoppdateringEvents = goodEvents.toMutableList().apply {
        add(badEvent)
    }.toMap()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val producerNameAlias = "dittnav"
    private val client = getClient(producerNameAlias)
    private val metricsReporter = StubMetricsReporter()
    private val nameResolver = ProducerNameResolver(client, testEnvironment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

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
            KafkaTestUtil.produceEvents(testEnvironment, Kafka.statusoppdateringInputTopicName, statusoppdateringEvents)
        } shouldBeEqualTo true

        `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` 1
    }


    fun `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERING, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Statusoppdatering>(consumerProps)

        val statusoppdateringInternProducerProps = Kafka.producerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN, enableSecurity = true)
        val internalKafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(statusoppdateringInternProducerProps)
        val internalEventProducer = Producer(Kafka.statusoppdateringHovedTopicName, internalKafkaProducer)

        val feilresponsProducerProps = Kafka.producerProps(testEnvironment, Eventtype.FEILRESPONS, enableSecurity = true)
        val feilresponsKafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps)
        val feilresponsEventProducer = Producer(Kafka.feilresponsTopicName, feilresponsKafkaProducer)

        val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val eventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

        val eventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)
        val consumer = Consumer(Kafka.statusoppdateringInputTopicName, kafkaConsumer, eventService)

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
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN, true)
        val targetKafkaConsumer = KafkaConsumer<NokkelIntern, StatusoppdateringIntern>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<NokkelIntern, StatusoppdateringIntern>()

        val targetConsumer = Consumer(Kafka.statusoppdateringHovedTopicName, targetKafkaConsumer, capturingProcessor)

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
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.FEILRESPONS, true)
        val targetKafkaConsumer = KafkaConsumer<NokkelFeilrespons, Feilrespons>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<NokkelFeilrespons, Feilrespons>()

        val targetConsumer = Consumer(Kafka.feilresponsTopicName, targetKafkaConsumer, capturingProcessor)

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
        createNokkelWithEventId(it.toString()) to AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId(it.toString())
    }

    private fun createEventWithTooLongGroupId(eventId: String): Pair<Nokkel, Statusoppdatering> {
        val groupId = "groupId".repeat(100)

        return createNokkelWithEventId(eventId) to AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId(groupId)
    }
}
