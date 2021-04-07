package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.addDuplicatesToProblematicEventsList
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.getDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.sendRemainingValidatedEventsToInternalTopicAndPersistToDB
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BeskjedEventServiceTest {

    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, BeskjedIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val brukernotifikasjonbestillingRepository = mockk<BrukernotifikasjonbestillingRepository>(relaxed = true)
    private val topic = "topic-beskjed-test"

    @BeforeEach
    private fun resetMocks() {
        mockkStatic("no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEventsKt")
    }

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {

        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, brukernotifikasjonbestillingRepository)

        coEvery { getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) } returns emptyList()
        coEvery { addDuplicatesToProblematicEventsList(any(), any(), any()) } returns Unit
        coEvery { sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any(), any(), any(), any()) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = 1) { getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 1) { sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { addDuplicatesToProblematicEventsList(any(), any(), any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, brukernotifikasjonbestillingRepository)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }
        coVerify(exactly = 0) { getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { addDuplicatesToProblematicEventsList(any(), any(), any()) }
        coVerify(exactly = 0) { sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjedWithTooLongGrupperingsid = AvroBeskjedObjectMother.createBeskjedWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjedWithTooLongGrupperingsid, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, brukernotifikasjonbestillingRepository)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { addDuplicatesToProblematicEventsList(any(), any(), any()) }
        coVerify(exactly = 0) { sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedBeskjed = mockk<Beskjed>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer, metricsCollector, brukernotifikasjonbestillingRepository)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { getDuplicateEvents(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any()) }
        coVerify(exactly = 0) { addDuplicatesToProblematicEventsList(any(), any(), any()) }
        coVerify(exactly = 0) { sendRemainingValidatedEventsToInternalTopicAndPersistToDB(any<MutableMap<NokkelIntern, BeskjedIntern>>(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
    }

}