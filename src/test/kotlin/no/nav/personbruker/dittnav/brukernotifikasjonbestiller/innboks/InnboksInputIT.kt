package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksInputObjectMother.createInnboksInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnboksInputIT {
    private val database = LocalPostgresDatabase.cleanDb()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, InnboksIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents)
    )
    private val innboksEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, InnboksIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.innboksInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = InnboksInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, InnboksInput>(KafkaTestTopics.innboksInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.innboksInputTopicName, inputKafkaConsumer, eventService)

    @Test
    fun `Should read Innboks-events and send to hoved-topic or error response topic as appropriate`() {
        var i = 0
        innboksEvents.forEach {
            inputKafkaConsumer.addRecord(
                ConsumerRecord(
                KafkaTestTopics.innboksInputTopicName,
                0,
                (i++).toLong(),
                it.key,
                it.value
            )
            )
        }

        `Read all Innboks-events from our input-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size shouldBe goodEvents.size
        capturedErrorResponseRecords.size shouldBe badEvents.size
    }

    fun `Read all Innboks-events from our input-topic and verify that they have been sent to the main-topic`() {

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()

            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.innboksInputTopicName, innboksEvents.size.toLong())
            `Wait until all innboks events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            inputEventConsumer.stopPolling()
        }
    }


    private fun `Wait until all innboks events have been received by target topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelIntern, InnboksIntern>(KafkaTestTopics.innboksInternTopicName)
        KafkaTestUtil.loopbackRecords(internalKafkaProducer, targetKafkaConsumer)

        val capturingProcessor = CapturingEventProcessor<NokkelIntern, InnboksIntern>()
        val targetConsumer = Consumer(KafkaTestTopics.innboksInternTopicName, targetKafkaConsumer, capturingProcessor)

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

        createNokkelInputWithEventIdAndGroupId(eventId, it.toString()) to createInnboksInput()
    }

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, InnboksInput> {
        val eventId = UUID.randomUUID().toString()
        val groupId = "groupId".repeat(100)

        return createNokkelInputWithEventIdAndGroupId(eventId, groupId) to createInnboksInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, InnboksInput> {
        val eventId = "notUuidOrUlid"

        return createNokkelInputWithEventId(eventId) to createInnboksInput()
    }

    private fun createEventWithDuplicateId(goodEvents: List<Pair<NokkelInput, InnboksInput>>): Pair<NokkelInput, InnboksInput> {
        val existingEventId = goodEvents.first().let { (nokkel, _) -> nokkel.getEventId() }

        return createNokkelInputWithEventId(existingEventId) to createInnboksInput()
    }
}
