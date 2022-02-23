package no.nav.personbruker.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.StatusoppdateringInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.collections.ArrayList

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatusoppdateringInputIT {
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(
            KafkaTestTopics.statusoppdateringInputTopicName,
            KafkaTestTopics.statusoppdateringInternTopicName,
            KafkaTestTopics.feilresponsTopicName
    ))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()

    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents)
    )

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
            KafkaTestUtil.produceEventsInput(testEnvironment, KafkaTestTopics.statusoppdateringInputTopicName, statusoppdateringEvents)
        } shouldBeEqualTo true

        `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }

    fun `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERING)
        val kafkaConsumer = KafkaConsumer<NokkelInput, StatusoppdateringInput>(consumerProps)

        val statusoppdateringInternProducerProps = Kafka.producerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN, TopicSource.AIVEN)
        val internalKafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(statusoppdateringInternProducerProps)
        val internalEventProducer = Producer(KafkaTestTopics.statusoppdateringInternTopicName, internalKafkaProducer)

        val feilresponsProducerProps = Kafka.producerFeilresponsProps(testEnvironment, Eventtype.STATUSOPPDATERING, TopicSource.AIVEN)
        val feilresponsKafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps)
        val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

        val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val eventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

        val eventService = StatusoppdateringInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)
        val consumer = Consumer(KafkaTestTopics.statusoppdateringInputTopicName, kafkaConsumer, eventService)

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
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.STATUSOPPDATERINGINTERN)
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
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.FEILRESPONS)
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
        val eventId = UUID.randomUUID().toString()

        createNokkelInputWithEventIdAndGroupId(eventId, it.toString()) to createStatusoppdateringInput()
    }

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, StatusoppdateringInput> {
        val eventId = UUID.randomUUID().toString()
        val groupId = "groupId".repeat(100)

        return createNokkelInputWithEventIdAndGroupId(eventId, groupId) to createStatusoppdateringInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, StatusoppdateringInput> {
        val eventId = "notUuidOrUlid"

        return createNokkelInputWithEventId(eventId) to createStatusoppdateringInput()
    }

    private fun createEventWithDuplicateId(goodEvents: List<Pair<NokkelInput, StatusoppdateringInput>>): Pair<NokkelInput, StatusoppdateringInput> {
        val existingEventId = goodEvents.first().let { (nokkel, _) -> nokkel.getEventId() }

        return return createNokkelInputWithEventId(existingEventId) to createStatusoppdateringInput()
    }
}
