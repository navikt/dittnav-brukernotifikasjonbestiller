package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class StatusoppdateringEventServiceTest {

    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, StatusoppdateringIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdatering)
        val statusoppdateringEventService = StatusoppdateringEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalStatusoppdatering)
        val statusoppdateringEventService = StatusoppdateringEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalStatusoppdateringWithTooLongGrupperingsid = AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalStatusoppdateringWithTooLongGrupperingsid)
        val statusoppdateringEventService = StatusoppdateringEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedStatusoppdatering = mockk<Statusoppdatering>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedStatusoppdatering)
        val statusoppdateringEventService = StatusoppdateringEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            statusoppdateringEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

}