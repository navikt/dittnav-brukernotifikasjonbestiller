package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
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
        innboksEvents.entries.forEachIndexed { index, innboks ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.innboksInputTopicName,
                0,
                index.toLong(),
                innboks.key,
                innboks.value
            ))
        }

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.innboksInputTopicName, innboksEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }

        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
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
