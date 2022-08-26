package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype.BESKJED
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneInputObjectMother.createDoneInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
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

    private val goodEvents = createEvents(10) + createEventWithInvalidEventId()
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithDuplicateId(goodEvents)
    )
    private val doneEvents = goodEvents + badEvents

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
        doneEvents.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.doneInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        runBlocking {
            createMatchingBeskjedEventsInDatabase(goodEvents)
        }

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.doneInputTopicName, doneEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }

        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
    }

    private suspend fun createMatchingBeskjedEventsInDatabase(doneEvents: List<Pair<NokkelInput, DoneInput>>) {
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
