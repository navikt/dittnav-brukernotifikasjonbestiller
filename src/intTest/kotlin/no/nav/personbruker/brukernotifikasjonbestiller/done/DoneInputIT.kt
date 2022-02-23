package no.nav.personbruker.brukernotifikasjonbestiller.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.deleteAllBrukernotifikasjonbestillinger
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEventsLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype.BESKJED
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneInputObjectMother.createDoneInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneLegacyObjectMother.createDoneLegacyWithGrupperingsId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventIdAndSystembruker
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime.now
import java.util.*
import kotlin.collections.ArrayList

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoneInputIT {
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(
            KafkaTestTopics.doneInputTopicName,
            KafkaTestTopics.doneInternTopicName,
            KafkaTestTopics.feilresponsTopicName
    ))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, DoneIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents(10) + createEventWithInvalidEventId()
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithDuplicateId(goodEvents)
    )

    private val doneEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    @BeforeAll
    fun setup() {
        embeddedEnv.start()
    }

    @AfterAll
    fun tearDown() {
        embeddedEnv.tearDown()
        runBlocking {
            database.deleteAllBrukernotifikasjonbestillinger()
        }
    }

    @Test
    fun `Started Kafka instance in memory`() {
        embeddedEnv.serverPark.status `should be equal to` KafkaEnvironment.ServerParkStatus.Started
    }

    @Test
    fun `Should read Done-events and send to hoved-topic or error response topic, and allow invalid eventIds for backwards-compatibility`() {
        runBlocking {
            createMatchingBeskjedEventsInDatabase(goodEvents)

            KafkaTestUtil.produceEventsInput(testEnvironment, KafkaTestTopics.doneInputTopicName, doneEvents)
        } shouldBeEqualTo true


        `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`()


        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONE)
        val kafkaConsumer = KafkaConsumer<NokkelInput, DoneInput>(consumerProps)

        val doneInternProducerProps = Kafka.producerProps(testEnvironment, Eventtype.DONEINTERN, TopicSource.ON_PREM)
        val internalKafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(doneInternProducerProps)
        val internalEventProducer = Producer(KafkaTestTopics.doneInternTopicName, internalKafkaProducer)

        val feilresponsProducerProps = Kafka.producerProps(testEnvironment, Eventtype.FEILRESPONS, TopicSource.ON_PREM)
        val feilresponsKafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps)
        val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

        val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)
        val eventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

        val eventService = DoneInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)
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
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONEINTERN)
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

    suspend fun createMatchingBeskjedEventsInDatabase(doneEvents: List<Pair<NokkelInput, DoneInput>>) {
        val beskjedEvents = doneEvents.map { (nokkel, _) ->
            Brukernotifikasjonbestilling(nokkel.getEventId(), "", BESKJED, now(), "123")
        }

        database.createBrukernotifikasjonbestillinger(beskjedEvents)
    }

    private fun createEvents(number: Int) = (1..number).map {
        val eventId = UUID.randomUUID().toString()

        AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, it.toString()) to createDoneInput()
    }

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, DoneInput> {
        val eventId = UUID.randomUUID().toString()
        val groupId = "groupId".repeat(100)

        return AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, groupId) to createDoneInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, DoneInput> {
        val eventId = "notUuidOrUlid"

        return AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId) to createDoneInput()
    }

    private fun createEventWithDuplicateId(goodEvents: List<Pair<NokkelInput, DoneInput>>): Pair<NokkelInput, DoneInput> {
        val existingEventId = goodEvents.first().let { (nokkel, _) -> nokkel.getEventId() }

        return return AvroNokkelInputObjectMother.createNokkelInputWithEventId(existingEventId) to createDoneInput()
    }
}
