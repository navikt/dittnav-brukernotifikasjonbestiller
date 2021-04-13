package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class BeskjedEventServiceTest {

    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, BeskjedIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-beskjed-test"
    private val handleEvents = mockk<HandleDuplicateEvents>(relaxed = true)
    private val eventDispatcher = mockk<EventDispatcher>(relaxed = true)

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {

        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns emptyList()
        coEvery { handleEvents.createFeilresponsEvents(any()) } returns mutableListOf()
        coEvery { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) } returns emptyMap()
        coEvery { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) } returns Unit
        coEvery { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 1) { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }

        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any()) }
        coVerify(exactly = 0) { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjedWithTooLongGrupperingsid = AvroBeskjedObjectMother.createBeskjedWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjedWithTooLongGrupperingsid, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any()) }
        coVerify(exactly = 0) { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }

        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedBeskjed = mockk<Beskjed>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any()) }
        coVerify(exactly = 0) { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }

        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()
        val duplicateEvents = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED))
        val problematicEvents = FeilresponsObjectMother.giveMeANumberOfFeilresponsEvents(1, "eventId", "systembruker", Eventtype.BESKJED)

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents, eventDispatcher)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns duplicateEvents
        coEvery { handleEvents.createFeilresponsEvents(any()) } returns problematicEvents
        coEvery { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) } returns emptyMap()
        coEvery { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) } returns Unit
        coEvery { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 1) { handleEvents.createFeilresponsEvents(any()) }
        coVerify(exactly = 1) { handleEvents.getValidatedEventsWithoutDuplicates(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.sendEventsToInternalTopic(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 1) { eventDispatcher.persistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }
}