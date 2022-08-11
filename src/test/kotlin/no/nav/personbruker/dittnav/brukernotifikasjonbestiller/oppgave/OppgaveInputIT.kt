package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.AvroOppgaveInputObjectMother.createOppgaveInput
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveInputIT {
    private val database = LocalPostgresDatabase.cleanDb()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents)
    )
    private val oppgaveEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, OppgaveIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.oppgaveInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = OppgaveInputEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, OppgaveInput>(KafkaTestTopics.oppgaveInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.oppgaveInputTopicName, inputKafkaConsumer, eventService)

    @Test
    fun `Should read Oppgave-events and send to hoved-topic or error response topic as appropriate`() {
        oppgaveEvents.entries.forEachIndexed { index, oppgave ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.oppgaveInputTopicName,
                0,
                index.toLong(),
                oppgave.key,
                oppgave.value
            ))
        }

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.oppgaveInputTopicName, oppgaveEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }

        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
    }

    private fun createEvents(number: Int) = (1..number).map {
        val eventId = UUID.randomUUID().toString()

        createNokkelInputWithEventIdAndGroupId(eventId, it.toString()) to createOppgaveInput()
    }

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, OppgaveInput> {
        val eventId = UUID.randomUUID().toString()
        val groupId = "groupId".repeat(100)

        return createNokkelInputWithEventIdAndGroupId(eventId, groupId) to createOppgaveInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, OppgaveInput> {
        val eventId = "notUuidOrUlid"

        return createNokkelInputWithEventId(eventId) to createOppgaveInput()
    }

    private fun createEventWithDuplicateId(goodEvents: List<Pair<NokkelInput, OppgaveInput>>): Pair<NokkelInput, OppgaveInput> {
        val existingEventId = goodEvents.first().let { (nokkel, _) -> nokkel.getEventId() }

        return createNokkelInputWithEventId(existingEventId) to createOppgaveInput()
    }
}
