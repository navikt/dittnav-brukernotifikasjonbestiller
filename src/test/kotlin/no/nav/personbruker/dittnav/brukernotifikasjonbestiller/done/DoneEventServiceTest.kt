package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class DoneEventServiceTest {
    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, DoneIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val topic = "topic-done-test"
    private val handleEvents = mockk<HandleEvents>(relaxed = true)

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDone = AvroDoneObjectMother.createDone()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) } returns emptyList()
        coEvery { handleEvents.createFeilresponsEvents(any(), any()) } returns mutableListOf()
        coEvery { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) }
        coVerify(exactly = 1) { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalDone = AvroDoneObjectMother.createDone()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }
        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDoneWithTooLongGrupperingsid = AvroDoneObjectMother.createDoneWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDoneWithTooLongGrupperingsid, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedDone = mockk<Done>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) }
        coVerify(exactly = 0) { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) }
        coVerify(exactly = 0) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis det finnes duplikat`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDone = AvroDoneObjectMother.createDone()
        val duplicateEvents = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED))
        val problematicEvents = FeilresponsObjectMother.giveMeANumberOfFeilresponsEvents(1, "eventId", "systembruker", Eventtype.BESKJED)

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, handleEvents)

        coEvery { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) } returns duplicateEvents
        coEvery { handleEvents.createFeilresponsEvents(any(), any()) } returns problematicEvents
        coEvery { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { handleEvents.getDuplicateEvents(any<MutableMap<NokkelIntern, DoneIntern>>(), any()) }
        coVerify(exactly = 1) { handleEvents.sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, DoneIntern>>(), any(), any(), any()) }
        coVerify(exactly = 1) { handleEvents.createFeilresponsEvents(any(), any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }
}