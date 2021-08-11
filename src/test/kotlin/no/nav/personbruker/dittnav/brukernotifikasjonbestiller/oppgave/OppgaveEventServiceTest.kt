package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DuplicateCheckResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Test

internal class OppgaveEventServiceTest {
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val topic = "topic-oppgave-test"
    private val eventDispatcher = mockk<EventDispatcher<OppgaveIntern>>(relaxed = true)
    private val internalEvents = AvroOppgaveInternObjectMother.giveMeANumberOfInternalOppgaveEvents(2, "eventid", "systembruker", "fodselsnummer")

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgaveWithTooLongGrupperingsid = AvroOppgaveObjectMother.createOppgaveWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgaveWithTooLongGrupperingsid, topic)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedOppgave = mockk<Oppgave>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedOppgave, topic)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdatering, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<Nokkel, Oppgave>
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }
}
