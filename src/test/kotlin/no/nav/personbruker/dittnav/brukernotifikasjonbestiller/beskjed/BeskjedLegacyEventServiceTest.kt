package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.legacy.BeskjedLegacy
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
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

internal class BeskjedLegacyEventServiceTest {
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-beskjed-test"
    private val transformer = mockk<BeskjedLegacyTransformer>()
    private val feilresponsTransformer = mockk<FeilresponsLegacyTransformer>()
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<BeskjedIntern>>(relaxed = true)
    private val internalEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(2, "systembruker", "eventId", "fodselsnummer")

    @AfterEach
    fun cleanUp() {
        clearMocks(transformer)
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalBeskjed = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalBeskjed)
        val internalBeskjed = createBeskjedIntern(externalBeskjed)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toBeskjedInternal(externalBeskjed) } returns internalBeskjed
        every { transformer.toNokkelInternal(externalNokkel, externalBeskjed) } returns internalNokkel

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toBeskjedInternal(externalBeskjed) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalBeskjed) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalBeskjed = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        val internalBeskjed = createBeskjedIntern(externalBeskjed)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNullNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toBeskjedInternal(externalBeskjed) } returns internalBeskjed

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toBeskjedInternal(externalBeskjed) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), externalBeskjed) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalBeskjedWithTooLongGrupperingsid = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithGrupperingsId("G".repeat(101))

        val internalBeskjed = createBeskjedIntern(externalBeskjedWithTooLongGrupperingsid)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalBeskjedWithTooLongGrupperingsid, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toBeskjedInternal(externalBeskjedWithTooLongGrupperingsid) } returns internalBeskjed
        every { transformer.toNokkelInternal(externalNokkel, externalBeskjedWithTooLongGrupperingsid) } throws FieldValidationException("")
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toBeskjedInternal(externalBeskjedWithTooLongGrupperingsid) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalBeskjedWithTooLongGrupperingsid) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalUnexpectedBeskjed = mockk<BeskjedLegacy>()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalUnexpectedBeskjed, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toNokkelInternal(externalNokkel, externalUnexpectedBeskjed) } throws Exception()
        every { transformer.toBeskjedInternal(externalUnexpectedBeskjed) } throws Exception()
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toBeskjedInternal(externalUnexpectedBeskjed) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalUnexpectedBeskjed) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalBeskjed = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalBeskjed)
        val internalBeskjed = createBeskjedIntern(externalBeskjed)

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toBeskjedInternal(externalBeskjed) } returns internalBeskjed
        every { transformer.toNokkelInternal(externalNokkel, externalBeskjed) } returns internalNokkel

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toBeskjedInternal(externalBeskjed) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalBeskjed) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<NokkelLegacy, BeskjedLegacy>
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toBeskjedInternal(any()) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), any()) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal la ServiceUserMappingException boble oppover, og skal ikke skrive til noen topics eller basen`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalBeskjed = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toNokkelInternal(externalNokkel, externalBeskjed) } throws ServiceUserMappingException("")

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                beskjedEventService.processEvents(externalEvents)
            }
        } `should throw` ServiceUserMappingException::class

        coVerify(exactly = 0) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, BeskjedIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toBeskjedInternal(externalBeskjed) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalBeskjed) }
    }

    private fun createBeskjedIntern(beskjedLegacy: BeskjedLegacy) =
            BeskjedIntern(
                    beskjedLegacy.getTidspunkt(),
                    beskjedLegacy.getSynligFremTil(),
                    beskjedLegacy.getTekst(),
                    beskjedLegacy.getLink(),
                    beskjedLegacy.getSikkerhetsnivaa(),
                    beskjedLegacy.getEksternVarsling(),
                    beskjedLegacy.getPrefererteKanaler()
    )

    private fun createNokkelIntern(nokkelLegacy: NokkelLegacy, beskjedLegacy: BeskjedLegacy) =
            NokkelIntern(
                    "1234",
                    nokkelLegacy.getEventId(),
                    beskjedLegacy.getGrupperingsId(),
                    beskjedLegacy.getFodselsnummer(),
                    "${nokkelLegacy.getSystembruker()}-namespace",
                    "${nokkelLegacy.getSystembruker()}-app",
                    nokkelLegacy.getSystembruker()
            )
}
