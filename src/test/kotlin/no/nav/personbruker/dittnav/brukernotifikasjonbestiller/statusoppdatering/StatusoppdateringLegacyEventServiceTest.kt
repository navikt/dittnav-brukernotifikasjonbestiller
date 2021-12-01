package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DuplicateCheckResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneLegacyObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSessionLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollectorLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class StatusoppdateringLegacyEventServiceTest {
    private val metricsCollector = mockk<MetricsCollectorLegacy>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSessionLegacy>(relaxed = true)
    private val topic = "topic-statusoppdatering-test"
    private val transformer = mockk<StatusoppdateringLegacyTransformer>()
    private val feilresponsTransformer = mockk<FeilresponsLegacyTransformer>()
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<StatusoppdateringIntern>>(relaxed = true)
    private val internalEvents = AvroStatusoppdateringInternObjectMother.giveMeANumberOfInternalStatusoppdateringEvents(2, "systembruker", "eventId", "fodselsnummer")

    @AfterEach
    fun cleanUp() {
        clearMocks(transformer)
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalStatusoppdatering)
        val internalStatusoppdatering = createStatusoppdateringIntern(externalStatusoppdatering)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toStatusoppdateringInternal(externalStatusoppdatering) } returns internalStatusoppdatering
        every { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) } returns internalNokkel

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toStatusoppdateringInternal(externalStatusoppdatering) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalStatusoppdatering = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        val internalStatusoppdatering = createStatusoppdateringIntern(externalStatusoppdatering)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNullNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toStatusoppdateringInternal(externalStatusoppdatering) } returns internalStatusoppdatering

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toStatusoppdateringInternal(externalStatusoppdatering) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), externalStatusoppdatering) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalStatusoppdateringWithTooLongGrupperingsid = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithGrupperingsId("G".repeat(101))

        val internalStatusoppdatering = createStatusoppdateringIntern(externalStatusoppdateringWithTooLongGrupperingsid)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalStatusoppdateringWithTooLongGrupperingsid, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toStatusoppdateringInternal(externalStatusoppdateringWithTooLongGrupperingsid) } returns internalStatusoppdatering
        every { transformer.toNokkelInternal(externalNokkel, externalStatusoppdateringWithTooLongGrupperingsid) } throws FieldValidationException("")
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
        verify(exactly = 0) { transformer.toStatusoppdateringInternal(externalStatusoppdateringWithTooLongGrupperingsid) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalStatusoppdateringWithTooLongGrupperingsid) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalUnexpectedStatusoppdatering = mockk<Statusoppdatering>()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalUnexpectedStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toNokkelInternal(externalNokkel, externalUnexpectedStatusoppdatering) } throws Exception()
        every { transformer.toStatusoppdateringInternal(externalUnexpectedStatusoppdatering) } throws Exception()
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
        verify(exactly = 0) { transformer.toStatusoppdateringInternal(externalUnexpectedStatusoppdatering) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalUnexpectedStatusoppdatering) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalStatusoppdatering)
        val internalStatusoppdatering = createStatusoppdateringIntern(externalStatusoppdatering)

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toStatusoppdateringInternal(externalStatusoppdatering) } returns internalStatusoppdatering
        every { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) } returns internalNokkel

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toStatusoppdateringInternal(externalStatusoppdatering) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<Nokkel, Statusoppdatering>
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
        verify(exactly = 0) { transformer.toStatusoppdateringInternal(any()) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), any()) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal la ServiceUserMappingException boble oppover, og skal ikke skrive til noen topics eller basen`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) } throws ServiceUserMappingException("")

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                statusoppdateringEventService.processEvents(externalEvents)
            }
        } `should throw` ServiceUserMappingException::class

        coVerify(exactly = 0) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toStatusoppdateringInternal(externalStatusoppdatering) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalStatusoppdatering) }
    }

    private fun createStatusoppdateringIntern(statusoppdateringLegacy: Statusoppdatering) =
            StatusoppdateringIntern(
                    statusoppdateringLegacy.getTidspunkt(),
                    statusoppdateringLegacy.getLink(),
                    statusoppdateringLegacy.getSikkerhetsnivaa(),
                    statusoppdateringLegacy.getStatusGlobal(),
                    statusoppdateringLegacy.getStatusIntern(),
                    statusoppdateringLegacy.getSakstema()
            )

    private fun createNokkelIntern(nokkelLegacy: Nokkel, statusoppdateringLegacy: Statusoppdatering) =
            NokkelIntern(
                    "1234",
                    nokkelLegacy.getEventId(),
                    statusoppdateringLegacy.getGrupperingsId(),
                    statusoppdateringLegacy.getFodselsnummer(),
                    "${nokkelLegacy.getSystembruker()}-namespace",
                    "${nokkelLegacy.getSystembruker()}-app",
                    nokkelLegacy.getSystembruker()
            )
}
