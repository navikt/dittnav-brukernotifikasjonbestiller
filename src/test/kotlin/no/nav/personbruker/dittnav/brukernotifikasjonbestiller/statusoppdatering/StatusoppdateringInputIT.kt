package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.StatusoppdateringInput
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatusoppdateringInputIT {
    private val database = LocalPostgresDatabase.cleanDb()

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

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, StatusoppdateringIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.statusoppdateringInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = StatusoppdateringInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, StatusoppdateringInput>(KafkaTestTopics.statusoppdateringInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.statusoppdateringInputTopicName, inputKafkaConsumer, eventService)

    @Test
    fun `Should read Statusoppdatering-events and send to hoved-topic or error response topic as appropriate`() {
        var i = 0
        statusoppdateringEvents.forEach {
            inputKafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.statusoppdateringInputTopicName,
                    0,
                    (i++).toLong(),
                    it.key,
                    it.value
                )
            )
        }

        `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }

    fun `Read all Statusoppdatering-events from our input-topic and verify that they have been sent to the main-topic`() {

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()

            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.statusoppdateringInputTopicName, statusoppdateringEvents.size.toLong())
            `Wait until all statusoppdatering events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            inputEventConsumer.stopPolling()
        }
    }


    private fun `Wait until all statusoppdatering events have been received by target topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelIntern, StatusoppdateringIntern>(KafkaTestTopics.statusoppdateringInternTopicName)
        KafkaTestUtil.loopbackRecords(internalKafkaProducer, targetKafkaConsumer)

        val capturingProcessor = CapturingEventProcessor<NokkelIntern, StatusoppdateringIntern>()
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

        return createNokkelInputWithEventId(existingEventId) to createStatusoppdateringInput()
    }
}
