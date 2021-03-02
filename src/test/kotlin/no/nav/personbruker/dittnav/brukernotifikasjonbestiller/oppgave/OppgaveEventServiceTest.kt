package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.junit.jupiter.api.Test

internal class OppgaveEventServiceTest {
    private val internalEventProducer = mockk<KafkaProducerWrapper<NokkelIntern, OppgaveIntern>>(relaxed = true)
    private val feilresponsEventProducer = mockk<KafkaProducerWrapper<NokkelFeilrespons, Feilrespons>>(relaxed = true)
    private val topic = "topic-oppgave-test"

    @Test
    fun `skal skrive til internal-topic hvis alt er ok`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 1) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal ikke skrive til topic hvis nokkel er null`() {
        val externalNullNokkel = null
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNullNokkel, externalOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 0) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis eventet har en valideringsfeil`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalOppgaveWithTooLongGrupperingsid = AvroOppgaveObjectMother.createOppgaveWithGrupperingsId("G".repeat(101))

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalOppgaveWithTooLongGrupperingsid, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }

    @Test
    fun `skal skrive til feilrespons-topic hvis vi faar en uventet feil under transformering`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId("1")
        val externalUnexpectedOppgave = mockk<Oppgave>()

        val externalEvents = ConsumerRecordsObjectMother.createConsumerRecords(externalNokkel, externalUnexpectedOppgave, topic)
        val oppgaveEventService = OppgaveEventService(internalEventProducer, feilresponsEventProducer)

        every { internalEventProducer.sendEvents(any()) } returns Unit
        every { feilresponsEventProducer.sendEvents(any()) } returns Unit

        runBlocking {
            oppgaveEventService.processEvents(externalEvents)
        }

        coVerify(exactly = 0) { internalEventProducer.sendEvents(any()) }
        coVerify(exactly = 1) { feilresponsEventProducer.sendEvents(any()) }
    }
}