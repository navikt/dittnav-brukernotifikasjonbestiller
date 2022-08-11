package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInputObjectMother.createBeskjedInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeskjedInputIT {

    private val database = LocalPostgresDatabase.cleanDb()
    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents)
    )
    private val beskjedEvents = goodEvents + badEvents

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, BeskjedIntern>().apply {
      initTransactions()
    }
    private val internalEventProducer = Producer(KafkaTestTopics.beskjedInternTopicName, internalKafkaProducer)

    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>().apply {
        initTransactions()
    }
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = BeskjedInputEventService(
        metricsCollector,
        handleDuplicateEvents,
        eventDispatcher,
        BeskjedRapidProducer(rapidKafkaProducer, "rapid"),
        produceToRapid = true
    )

    @BeforeEach
    fun reset() {
        feilresponsKafkaProducer.clear()
        internalKafkaProducer.clear()
        rapidKafkaProducer.clear()
    }

    @Test
    fun `Should read Beskjed-events and send to hoved-topic or error response topic as appropriate`() {
        val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, BeskjedInput>(KafkaTestTopics.beskjedInputTopicName)
        val inputEventConsumer = Consumer(KafkaTestTopics.beskjedInputTopicName, inputKafkaConsumer, eventService)

        beskjedEvents.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.beskjedInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.beskjedInputTopicName, beskjedEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }

        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
    }

    @Test
    fun `Sender beskjeder på rapid-format`() {
        val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, BeskjedInput>(KafkaTestTopics.beskjedInputTopicName)
        val inputEventConsumer = Consumer(KafkaTestTopics.beskjedInputTopicName, inputKafkaConsumer, eventService)

        val beskjeder = createEvents(5)
        beskjeder.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.beskjedInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.beskjedInputTopicName, beskjeder.size.toLong())
            inputEventConsumer.stopPolling()
        }

        rapidKafkaProducer.history().size shouldBe beskjeder.size
        val beskjedJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        beskjedJson.has("@event_name") shouldBe true
        beskjedJson["@event_name"].asText() shouldBe "beskjed"
    }

    @Test
    fun `Sender beskjeder til rapid i tillegg til gammelt topic`() {
        val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, BeskjedInput>(KafkaTestTopics.beskjedInputTopicName)
        val inputEventConsumer = Consumer(KafkaTestTopics.beskjedInputTopicName, inputKafkaConsumer, eventService)

        val beskjeder = createEvents(5)
        beskjeder.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.beskjedInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.beskjedInputTopicName, beskjeder.size.toLong())
            inputEventConsumer.stopPolling()
        }

        internalKafkaProducer.history().size shouldBe beskjeder.size
        rapidKafkaProducer.history().size shouldBe beskjeder.size
    }

    private fun createEvents(number: Int) = (1..number).map {
        val eventId = UUID.randomUUID().toString()

        createNokkelInputWithEventIdAndGroupId(eventId, it.toString()) to createBeskjedInput()
    }

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, BeskjedInput> {
        val eventId = UUID.randomUUID().toString()
        val groupId = "groupId".repeat(100)

        return createNokkelInputWithEventIdAndGroupId(eventId, groupId) to createBeskjedInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, BeskjedInput> {
        val eventId = "notUuidOrUlid"

        return createNokkelInputWithEventId(eventId) to createBeskjedInput()
    }

    private fun createEventWithDuplicateId(goodEvents: List<Pair<NokkelInput, BeskjedInput>>): Pair<NokkelInput, BeskjedInput> {
        val existingEventId = goodEvents.first().let { (nokkel, _) -> nokkel.getEventId() }

        return createNokkelInputWithEventId(existingEventId) to createBeskjedInput()
    }
}
