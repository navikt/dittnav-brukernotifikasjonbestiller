package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.NokkelEventPairObjectMother.createANumberOfValidEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsObjectMother.createANumberOfProblematicEvents
import org.junit.jupiter.api.Test


internal class EventDispatcherTest {
    private val eventType: Eventtype = mockk()
    private val repository: BrukernotifikasjonbestillingRepository = mockk()
    private val internalEventProducer: Producer<NokkelIntern, String> = mockk()
    private val feilresponsEventProducer: Producer<NokkelFeilrespons, Feilrespons> = mockk()

    private val peristMock: ListPersistActionResult<Brukernotifikasjonbestilling> = mockk()

    private val validatedEvents = createANumberOfValidEvents(10)
    private val problematicEvents = createANumberOfProblematicEvents(3)

    private val dispatcher = EventDispatcher(eventType, repository, internalEventProducer, feilresponsEventProducer)

    @Test
    fun `Should commit for both producers if no problems occur`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { internalEventProducer.commitCurrentTransaction() } returns Unit
        every { feilresponsEventProducer.commitCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } returns peristMock

        runBlocking {
            dispatcher.dispatchValidAndProblematicEvents(validatedEvents, problematicEvents)
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.commitCurrentTransaction() }
        verify(exactly = 1) { feilresponsEventProducer.commitCurrentTransaction() }
    }

    @Test
    fun `Should abort for both producers if we encounter a problem with sending internal events`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        every { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { internalEventProducer.abortCurrentTransaction() } returns Unit
        every { feilresponsEventProducer.abortCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } returns peristMock

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                dispatcher.dispatchValidAndProblematicEvents(validatedEvents, problematicEvents)
            }
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 0) { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.abortCurrentTransaction() }
        verify(exactly = 1) { feilresponsEventProducer.abortCurrentTransaction() }
        verify(exactly = 0) { internalEventProducer.commitCurrentTransaction() }
        verify(exactly = 0) { feilresponsEventProducer.commitCurrentTransaction() }
    }

    @Test
    fun `Should abort for both producers if we encounter a problem with sending feilrespons-events`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        every { internalEventProducer.abortCurrentTransaction() } returns Unit
        every { feilresponsEventProducer.abortCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } returns peristMock

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                dispatcher.dispatchValidAndProblematicEvents(validatedEvents, problematicEvents)
            }
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.abortCurrentTransaction() }
        verify(exactly = 1) { feilresponsEventProducer.abortCurrentTransaction() }
        verify(exactly = 0) { internalEventProducer.commitCurrentTransaction() }
        verify(exactly = 0) { feilresponsEventProducer.commitCurrentTransaction() }
    }

    @Test
    fun `Should abort for both producers if we encounter a problem with persisting events to database`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { internalEventProducer.abortCurrentTransaction() } returns Unit
        every { feilresponsEventProducer.abortCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } throws RetriableDatabaseException("")

        shouldThrow<RetriableDatabaseException> {
            runBlocking {
                dispatcher.dispatchValidAndProblematicEvents(validatedEvents, problematicEvents)
            }
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { feilresponsEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.abortCurrentTransaction() }
        verify(exactly = 1) { feilresponsEventProducer.abortCurrentTransaction() }
        verify(exactly = 0) { internalEventProducer.commitCurrentTransaction() }
        verify(exactly = 0) { feilresponsEventProducer.commitCurrentTransaction() }
    }

    @Test
    fun `Should commit for valid events if no problems occur`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { internalEventProducer.commitCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } returns peristMock

        runBlocking {
            dispatcher.dispatchValidEventsOnly(validatedEvents)
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.commitCurrentTransaction() }
    }

    @Test
    fun `Should abort transaction if we encounter a problem with sending internal events`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        every { internalEventProducer.abortCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } returns peristMock

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                dispatcher.dispatchValidEventsOnly(validatedEvents)
            }
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.abortCurrentTransaction() }
        verify(exactly = 0) { internalEventProducer.commitCurrentTransaction() }
    }


    @Test
    fun `Should abort transaction if we encounter a problem with persisting events to database`() {
        every { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        every { internalEventProducer.abortCurrentTransaction() } returns Unit

        coEvery { repository.persistInOneBatch(validatedEvents, any()) } throws RetriableDatabaseException("")

        shouldThrow<RetriableDatabaseException> {
            runBlocking {
                dispatcher.dispatchValidEventsOnly(validatedEvents)
            }
        }

        verify(exactly = 1) { internalEventProducer.sendEventsAndLeaveTransactionOpen(any()) }
        verify(exactly = 1) { internalEventProducer.abortCurrentTransaction() }
        verify(exactly = 0) { internalEventProducer.commitCurrentTransaction() }
    }
}