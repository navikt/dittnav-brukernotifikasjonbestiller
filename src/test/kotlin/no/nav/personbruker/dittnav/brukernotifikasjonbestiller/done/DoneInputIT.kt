package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype.BESKJED
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneInputObjectMother.createDoneInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime.now
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoneInputIT {
    private val database = LocalPostgresDatabase.cleanDb()
    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, DoneIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val goodEvents = createEvents(10) + createEventWithInvalidEventId()
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithDuplicateId(goodEvents)
    )
    private val doneEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, DoneIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.doneInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = DoneInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, DoneInput>(KafkaTestTopics.doneInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.doneInputTopicName, inputKafkaConsumer, eventService)

    @Test
    fun `Should read Done-events and send to hoved-topic or error response topic, and allow invalid eventIds for backwards-compatibility`() {
        var i = 0
        doneEvents.forEach {
            inputKafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.doneInputTopicName,
                    0,
                    (i++).toLong(),
                    it.key,
                    it.value
                )
            )
        }
        runBlocking {
            createMatchingBeskjedEventsInDatabase(goodEvents)
        }

        `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Done-events from our input-topic and verify that they have been sent to the main-topic`() {

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()

            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.doneInputTopicName, doneEvents.size.toLong())
            `Wait until all done events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            inputEventConsumer.stopPolling()
        }
    }

    private fun `Wait until all done events have been received by target topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelIntern, DoneIntern>(KafkaTestTopics.doneInternTopicName)
        KafkaTestUtil.loopbackRecords(internalKafkaProducer, targetKafkaConsumer)

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
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelFeilrespons, Feilrespons>(KafkaTestTopics.feilresponsTopicName)
        KafkaTestUtil.loopbackRecords(feilresponsKafkaProducer, targetKafkaConsumer)

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

        return AvroNokkelInputObjectMother.createNokkelInputWithEventId(existingEventId) to createDoneInput()
    }
}
