package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DuplicateCheckResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneLegacyObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class OppgaveLegacyEventServiceTest {
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-oppgave-test"
    private val transformer = mockk<OppgaveLegacyTransformer>()
    private val feilresponsTransformer = mockk<FeilresponsLegacyTransformer>()
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<OppgaveIntern>>(relaxed = true)
    private val internalEvents = AvroOppgaveInternObjectMother.giveMeANumberOfInternalOppgaveEvents(2, "systembruker", "eventId", "fodselsnummer")

    @AfterEach
    fun cleanUp() {
        clearMocks(transformer)
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalOppgave = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalOppgave)
        val internalOppgave = createOppgaveIntern(externalOppgave)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toOppgaveInternal(externalOppgave) } returns internalOppgave
        every { transformer.toNokkelInternal(externalNokkel, externalOppgave) } returns internalNokkel

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
        verify(exactly = 1) { transformer.toOppgaveInternal(externalOppgave) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalOppgave) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalOppgave = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val internalOppgave = createOppgaveIntern(externalOppgave)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNullNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toOppgaveInternal(externalOppgave) } returns internalOppgave

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toOppgaveInternal(externalOppgave) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), externalOppgave) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalOppgaveWithTooLongGrupperingsid = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithGrupperingsId("G".repeat(101))

        val internalOppgave = createOppgaveIntern(externalOppgaveWithTooLongGrupperingsid)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalOppgaveWithTooLongGrupperingsid, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toOppgaveInternal(externalOppgaveWithTooLongGrupperingsid) } returns internalOppgave
        every { transformer.toNokkelInternal(externalNokkel, externalOppgaveWithTooLongGrupperingsid) } throws FieldValidationException("")
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toOppgaveInternal(externalOppgaveWithTooLongGrupperingsid) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalOppgaveWithTooLongGrupperingsid) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalUnexpectedOppgave = mockk<Oppgave>()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalUnexpectedOppgave, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toNokkelInternal(externalNokkel, externalUnexpectedOppgave) } throws Exception()
        every { transformer.toOppgaveInternal(externalUnexpectedOppgave) } throws Exception()
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toOppgaveInternal(externalUnexpectedOppgave) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalUnexpectedOppgave) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalOppgave = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalOppgave)
        val internalOppgave = createOppgaveIntern(externalOppgave)

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toOppgaveInternal(externalOppgave) } returns internalOppgave
        every { transformer.toNokkelInternal(externalNokkel, externalOppgave) } returns internalNokkel

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toOppgaveInternal(externalOppgave) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalOppgave) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<Nokkel, Oppgave>
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toOppgaveInternal(any()) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), any()) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal la ServiceUserMappingException boble oppover, og skal ikke skrive til noen topics eller basen`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalOppgave = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toNokkelInternal(externalNokkel, externalOppgave) } throws ServiceUserMappingException("")

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                oppgaveEventService.processEvents(externalEvents)
            }
        } `should throw` ServiceUserMappingException::class

        coVerify(exactly = 0) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, OppgaveIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toOppgaveInternal(externalOppgave) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalOppgave) }
    }

    private fun createOppgaveIntern(oppgaveLegacy: Oppgave) =
            OppgaveIntern(
                    oppgaveLegacy.getTidspunkt(),
                    oppgaveLegacy.getTekst(),
                    oppgaveLegacy.getLink(),
                    oppgaveLegacy.getSikkerhetsnivaa(),
                    oppgaveLegacy.getEksternVarsling(),
                    oppgaveLegacy.getPrefererteKanaler()
            )

    private fun createNokkelIntern(nokkelLegacy: Nokkel, oppgaveLegacy: Oppgave) =
            NokkelIntern(
                    "1234",
                    nokkelLegacy.getEventId(),
                    oppgaveLegacy.getGrupperingsId(),
                    oppgaveLegacy.getFodselsnummer(),
                    "${nokkelLegacy.getSystembruker()}-namespace",
                    "${nokkelLegacy.getSystembruker()}-app",
                    nokkelLegacy.getSystembruker()
            )
}
