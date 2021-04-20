package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class StatusoppdateringEventServiceTest {
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val handleDuplicateEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val topic = "topic-statusoppdatering-test"
    private val eventDispatcher = mockk<EventDispatcher<StatusoppdateringIntern>>(relaxed = true)
    private val internalEvents = AvroStatusoppdateringInternObjectMother.giveMeANumberOfInternalStatusoppdateringEvents(2, "eventId", "systembruker", "fodselsummer")

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns emptyList()
        coEvery { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) } returns internalEvents
        coEvery { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEvents(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }

        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { eventDispatcher.dispatchProblematicEvents(any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdateringWithTooLongGrupperingsid = AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdateringWithTooLongGrupperingsid, topic)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }

        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedStatusoppdatering = mockk<Statusoppdatering>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 0) { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }

        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()
        val duplicateEvents = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdatering, topic)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, eventDispatcher)

        coEvery { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns duplicateEvents
        coEvery { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) } returns internalEvents
        coEvery { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) } returns Unit
        coEvery { eventDispatcher.dispatchProblematicEvents(any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateEvents(any()) }
        coVerify(exactly = 1) { handleDuplicateEvents.getDuplicateEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { handleDuplicateEvents.getValidatedEventsWithoutDuplicates(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchSuccessfullyValidatedEvents(any<MutableList<Pair<NokkelIntern, StatusoppdateringIntern>>>()) }
        coVerify(exactly = 1) { eventDispatcher.dispatchProblematicEvents(any()) }
    }
}