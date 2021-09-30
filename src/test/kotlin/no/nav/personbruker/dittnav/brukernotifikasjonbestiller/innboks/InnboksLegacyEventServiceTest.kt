package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.legacy.InnboksLegacy
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

internal class InnboksLegacyEventServiceTest {

    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-innboks-test"
    private val transformer = mockk<InnboksLegacyTransformer>()
    private val feilresponsTransformer = mockk<FeilresponsLegacyTransformer>()
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<InnboksIntern>>(relaxed = true)
    private val internalEvents = AvroInnboksInternObjectMother.giveMeANumberOfInternalInnboksEvents(2, "systembruker", "eventId", "fodselsnummer")

    @AfterEach
    fun cleanUp() {
        clearMocks(transformer)
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalInnboks = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalInnboks)
        val internalInnboks = createInnboksIntern(externalInnboks)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalInnboks, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toInnboksInternal(externalInnboks) } returns internalInnboks
        every { transformer.toNokkelInternal(externalNokkel, externalInnboks) } returns internalNokkel

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toInnboksInternal(externalInnboks) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalInnboks) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalInnboks = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        val internalInnboks = createInnboksIntern(externalInnboks)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNullNokkel, externalInnboks, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toInnboksInternal(externalInnboks) } returns internalInnboks

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toInnboksInternal(externalInnboks) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), externalInnboks) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalInnboksWithTooLongGrupperingsid = AvroInnboksLegacyObjectMother.createInnboksLegacyWithGrupperingsId("G".repeat(101))

        val internalInnboks = createInnboksIntern(externalInnboksWithTooLongGrupperingsid)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalInnboksWithTooLongGrupperingsid, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toInnboksInternal(externalInnboksWithTooLongGrupperingsid) } returns internalInnboks
        every { transformer.toNokkelInternal(externalNokkel, externalInnboksWithTooLongGrupperingsid) } throws FieldValidationException("")
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toInnboksInternal(externalInnboksWithTooLongGrupperingsid) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalInnboksWithTooLongGrupperingsid) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalUnexpectedInnboks = mockk<InnboksLegacy>()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalUnexpectedInnboks, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toNokkelInternal(externalNokkel, externalUnexpectedInnboks) } throws Exception()
        every { transformer.toInnboksInternal(externalUnexpectedInnboks) } throws Exception()
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toInnboksInternal(externalUnexpectedInnboks) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalUnexpectedInnboks) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalInnboks = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalInnboks)
        val internalInnboks = createInnboksIntern(externalInnboks)

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalInnboks, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toInnboksInternal(externalInnboks) } returns internalInnboks
        every { transformer.toNokkelInternal(externalNokkel, externalInnboks) } returns internalNokkel

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toInnboksInternal(externalInnboks) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalInnboks) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<NokkelLegacy, InnboksLegacy>
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        verify(exactly = 0) { transformer.toInnboksInternal(any()) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), any()) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal la ServiceUserMappingException boble oppover, og skal ikke skrive til noen topics eller basen`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalInnboks = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalInnboks, topic)
        val innboksEventService = InnboksLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toNokkelInternal(externalNokkel, externalInnboks) } throws ServiceUserMappingException("")

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                innboksEventService.processEvents(externalEvents)
            }
        } `should throw` ServiceUserMappingException::class

        coVerify(exactly = 0) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toInnboksInternal(externalInnboks) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalInnboks) }
    }

    private fun createInnboksIntern(innboksLegacy: InnboksLegacy) =
            InnboksIntern(
                    innboksLegacy.getTidspunkt(),
                    innboksLegacy.getTekst(),
                    innboksLegacy.getLink(),
                    innboksLegacy.getSikkerhetsnivaa(),
                    innboksLegacy.getEksternVarsling(),
                    innboksLegacy.getPrefererteKanaler()
            )

    private fun createNokkelIntern(nokkelLegacy: NokkelLegacy, innboksLegacy: InnboksLegacy) =
            NokkelIntern(
                    "1234",
                    nokkelLegacy.getEventId(),
                    innboksLegacy.getGrupperingsId(),
                    innboksLegacy.getFodselsnummer(),
                    "${nokkelLegacy.getSystembruker()}-namespace",
                    "${nokkelLegacy.getSystembruker()}-app",
                    nokkelLegacy.getSystembruker()
            )
}
