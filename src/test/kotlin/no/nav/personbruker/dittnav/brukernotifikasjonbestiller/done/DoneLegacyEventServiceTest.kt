package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DuplicateCheckResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEventsLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSessionLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollectorLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.AvroOppgaveLegacyObjectMother
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DoneLegacyEventServiceTest {
    private val metricsCollector = mockk<MetricsCollectorLegacy>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSessionLegacy>(relaxed = true)
    private val topic = "topic-done-test"
    private val handleDuplicateEvents = mockk<HandleDuplicateDoneEventsLegacy>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<DoneIntern>>(relaxed = true)
    private val internalEvents = AvroDoneInternObjectMother.giveMeANumberOfInternalDoneEvents(2, "eventId", "systembruker", "fodselsnummer")
    private val transformer = mockk<DoneLegacyTransformer>()
    private val feilresponsTransformer = mockk<FeilresponsLegacyTransformer>()

    @AfterEach
    fun cleanUp() {
        clearMocks(transformer)
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalDone)
        val internalDone = createDoneIntern(externalDone)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        every { transformer.toDoneInternal(externalDone) } returns internalDone
        every { transformer.toNokkelInternal(externalNokkel, externalDone) } returns internalNokkel

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toDoneInternal(externalDone) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalDone) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val internalDone = createDoneIntern(externalDone)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNullNokkel, externalDone, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { transformer.toDoneInternal(externalDone) } returns internalDone

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toDoneInternal(externalDone) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), externalDone) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDoneWithTooLongGrupperingsid = AvroDoneLegacyObjectMother.createDoneLegacyWithGrupperingsId("G".repeat(101))

        val internalDone = createDoneIntern(externalDoneWithTooLongGrupperingsid)

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDoneWithTooLongGrupperingsid, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toDoneInternal(externalDoneWithTooLongGrupperingsid) } returns internalDone
        every { transformer.toNokkelInternal(externalNokkel, externalDoneWithTooLongGrupperingsid) } throws FieldValidationException("")
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }

        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
        verify(exactly = 0) { transformer.toDoneInternal(externalDoneWithTooLongGrupperingsid) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalDoneWithTooLongGrupperingsid) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalUnexpectedDone = mockk<Done>()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalUnexpectedDone, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toDoneInternal(externalUnexpectedDone) } throws Exception()
        every { transformer.toNokkelInternal(externalNokkel, externalUnexpectedDone) } throws Exception()
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }

        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }

        verify(exactly = 0) { transformer.toDoneInternal(externalUnexpectedDone) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalUnexpectedDone) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val internalNokkel = createNokkelIntern(externalNokkel, externalDone)
        val internalDone = createDoneIntern(externalDone)

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        every { transformer.toDoneInternal(externalDone) } returns internalDone
        every { transformer.toNokkelInternal(externalNokkel, externalDone) } returns internalNokkel

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 1) { transformer.toDoneInternal(externalDone) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalDone) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalOppgave = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalOppgave, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<Nokkel, Done>
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        every { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) } returns mockk()


        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
        verify(exactly = 0) { transformer.toDoneInternal(any()) }
        verify(exactly = 0) { transformer.toNokkelInternal(any(), any()) }
        verify(exactly = 1) { feilresponsTransformer.createFeilrespons(any(), any(), any(), any()) }
    }

    @Test
    fun `skal la ServiceUserMappingException boble oppover, og skal ikke skrive til noen topics eller basen`() {
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId("1")
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val externalEvents = ConsumerRecordsObjectMother.createLegacyConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneLegacyEventService(transformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit
        every { transformer.toNokkelInternal(externalNokkel, externalDone) } throws ServiceUserMappingException("")

        val slot = slot<suspend EventMetricsSessionLegacy.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                doneEventService.processEvents(externalEvents)
            }
        } `should throw` ServiceUserMappingException::class

        coVerify(exactly = 0) { metricsSession.countSuccessfulEventForProducer(any()) }
        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, DoneIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, DoneIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        verify(exactly = 0) { transformer.toDoneInternal(externalDone) }
        verify(exactly = 1) { transformer.toNokkelInternal(externalNokkel, externalDone) }
    }

    private fun createDoneIntern(beskjedLegacy: Done) = DoneIntern(beskjedLegacy.getTidspunkt())

    private fun createNokkelIntern(nokkelLegacy: Nokkel, doneLegacy: Done) =
            NokkelIntern(
                    "1234",
                    nokkelLegacy.getEventId(),
                    doneLegacy.getGrupperingsId(),
                    doneLegacy.getFodselsnummer(),
                    "${nokkelLegacy.getSystembruker()}-namespace",
                    "${nokkelLegacy.getSystembruker()}-app",
                    nokkelLegacy.getSystembruker()
            )
}
