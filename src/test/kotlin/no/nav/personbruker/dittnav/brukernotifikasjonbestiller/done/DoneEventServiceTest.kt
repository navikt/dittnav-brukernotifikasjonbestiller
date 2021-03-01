package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class DoneEventServiceTest {
    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, DoneIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val topic = "topic-done-test"

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDone = AvroDoneObjectMother.createDone()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalDone = AvroDoneObjectMother.createDone()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalDoneWithTooLongGrupperingsid = AvroDoneObjectMother.createDoneWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalDoneWithTooLongGrupperingsid, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedDone = mockk<Done>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedDone, topic)
        val doneEventService = DoneEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            doneEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }
}