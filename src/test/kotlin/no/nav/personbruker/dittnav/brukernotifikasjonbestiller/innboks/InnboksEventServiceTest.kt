package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DuplicateCheckResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Test

internal class InnboksEventServiceTest {

    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-innboks-test"
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher<InnboksIntern>>(relaxed = true)
    private val internalEvents = AvroInnboksInternObjectMother.giveMeANumberOfInternalInnboksEvents(2, "systembruker", "eventId", "fodselsnummer")

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalInnboks, topic)
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) } returns DuplicateCheckResult(internalEvents, emptyList())
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalInnboks, topic)
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalInnboksWithTooLongGrupperingsid = AvroInnboksObjectMother.createInnboksWithGrupperingsid("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalInnboksWithTooLongGrupperingsid, topic)
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedInnboks = mockk<Innboks>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedInnboks, topic)
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        val validEvents = listOf(internalEvents[0])
        val duplicateEvents = listOf(internalEvents[1])

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalInnboks, topic)
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) } returns DuplicateCheckResult(validEvents, duplicateEvents)
        coEvery { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) } returns Unit
        coEvery { eventDispatcher.dispatchValidEventsOnly(any()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEventsOnly(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis er plassert event med feil type paa topic`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDone = AvroDoneObjectMother.createDone()

        val externalMalplacedEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDone, topic)

        val externalEvents = externalMalplacedEvents as ConsumerRecords<Nokkel, Innboks>
        val innboksEventService = InnboksEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            innboksEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.checkForDuplicateEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidAndProblematicEvents(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchValidEventsOnly(any<MutableList<Pair<NokkelIntern, InnboksIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEventsOnly(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }
}
