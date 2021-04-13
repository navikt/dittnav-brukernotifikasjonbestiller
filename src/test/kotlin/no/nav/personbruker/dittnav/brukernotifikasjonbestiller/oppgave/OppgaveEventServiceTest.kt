package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class OppgaveEventServiceTest {
    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, OppgaveIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val handleEvents = mockk<HandleEvents>(relaxed = true)
    private val topic = "topic-oppgave-test"
    private val eventDispatcher = mockk<EventDispatcher>(relaxed = true)

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) } returns emptyList()
        coEvery { handleEvents.createFeilresponsEvents(any(), any()) } returns mutableListOf()
        coEvery { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) } returns emptyMap()
        coEvery { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) } returns Unit
        coEvery { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 1) { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) }
        coVerify(exactly = 1) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) }

        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgaveWithTooLongGrupperingsid = AvroOppgaveObjectMother.createOppgaveWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgaveWithTooLongGrupperingsid, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) }

        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedOppgave = mockk<Oppgave>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) }

        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()
        val duplicateEvents = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED))
        val problematicEvents = FeilresponsObjectMother.giveMeANumberOfFeilresponsEvents(1, "eventId", "systembruker", Eventtype.BESKJED)

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) } returns duplicateEvents
        coEvery { handleEvents.createFeilresponsEvents(any(), any()) } returns problematicEvents
        coEvery { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) } returns emptyMap()
        coEvery { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) } returns Unit
        coEvery { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 1) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 1) { handleEvents.getRemainingValidatedEvents(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any(), any()) }
        coVerify(exactly = 1) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, OppgaveIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, OppgaveIntern>>()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }
}