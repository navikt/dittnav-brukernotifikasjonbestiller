package no.nav.personbruker.brukernotifikasjonbestiller.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother.createNokkelWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneEventService
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
class DoneIT {
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(
            KafkaTestTopics.doneInputTopicName,
            KafkaTestTopics.doneInternTopicName,
            KafkaTestTopics.feilresponsTopicName
    ))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(createEventWithTooLongGroupId("bad"))
    private val doneEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, DoneIntern>>()
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
    fun `Should read Done-events and send to hoved-topic or error response topic as appropriate`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, KafkaTestTopics.doneInputTopicName, doneEvents)
        } shouldBeEqualTo true

        `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONE, enableSecurity = false)
        val kafkaConsumer = KafkaConsumer<Nokkel, Done>(consumerProps)

        val doneInternProducerProps = Kafka.producerProps(testEnvironment, Eventtype.DONEINTERN)
        val internalKafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(doneInternProducerProps)
        val internalEventProducer = Producer(KafkaTestTopics.doneInternTopicName, internalKafkaProducer)

        val feilresponsProducerProps = Kafka.producerProps(testEnvironment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps)
        val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

        val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
        val handleDuplicateEvents = HandleDuplicateDoneEvents(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val eventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

        val eventService = DoneEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)
        val consumer = Consumer(KafkaTestTopics.doneInputTopicName, kafkaConsumer, eventService)

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            `Wait until all done events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all done events have been received by target topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONEINTERN, enableSecurity = false)
        val targetKafkaConsumer = KafkaConsumer<NokkelIntern, DoneIntern>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<NokkelIntern, DoneIntern>()

        val targetConsumer = Consumer(KafkaTestTopics.doneInternTopicName, targetKafkaConsumer, capturingProcessor)

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
        createNokkelWithEventId(it.toString()) to AvroDoneObjectMother.createDoneWithGrupperingsId(it.toString())
    }

    private fun createEventWithTooLongGroupId(eventId: String): Pair<Nokkel, Done> {
        val groupId = "groupId".repeat(100)

        return createNokkelWithEventId(eventId) to AvroDoneObjectMother.createDoneWithGrupperingsId(groupId)
    }
}
