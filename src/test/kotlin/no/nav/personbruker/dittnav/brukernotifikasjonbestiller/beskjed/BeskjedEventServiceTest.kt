package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class BeskjedEventServiceTest {

    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, BeskjedIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val topic = "topic-beskjed-test"

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalBeskjedWithTooLongGrupperingsid = AvroBeskjedObjectMother.createBeskjedWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalBeskjedWithTooLongGrupperingsid, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedBeskjed = mockk<Beskjed>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedBeskjed, topic)
        val beskjedEventService = BeskjedEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            beskjedEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

}