package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.InternalEventsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test

internal class HandleEventsTest {

    private val fodselsnummer = "123"
    private val eventId = "eventId"
    private val systembruker = "systembruker"
    private val eventMetricsSession = mockk<EventMetricsSession>()
    private val brukernotifikasjonbestillingRepository = mockk<BrukernotifikasjonbestillingRepository>()

    @Test
    fun `Skal ikke inneholde duplikat i listen som sendes til kafka`() {
        val handleEvents = HandleEvents(brukernotifikasjonbestillingRepository)
        val duplicateEvent = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED))
        val successfullyValidatedEvents = InternalEventsObjectMother.giveMeANumberOfInternalEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size - duplicateEvent.size

        val eventsWithoutDuplicates = handleEvents.getRemainingValidatedEvents(successfullyValidatedEvents, duplicateEvent, Eventtype.BESKJED)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
    }

    @Test
    fun `Skal kun inneholde duplikat av onsket eventtype`() {
        val handleEvents = HandleEvents(brukernotifikasjonbestillingRepository)
        val dummyEvents = InternalEventsObjectMother.giveMeANumberOfInternalEvents(numberOfEvents = 1, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)

        val duplicatesWithBeskjedAndOppgaveEvents = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.OPPGAVE)
        )
        val expectedNumberOfEvents = duplicatesWithBeskjedAndOppgaveEvents.filter { event -> event.eventtype == Eventtype.BESKJED.toString() }.size

        coEvery { brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns duplicatesWithBeskjedAndOppgaveEvents

        runBlocking {
            val duplicates = handleEvents.getDuplicateEvents(dummyEvents, Eventtype.BESKJED)

            duplicates.size.`should be equal to`(expectedNumberOfEvents)

            duplicates.get(0).eventtype.`should be equal to`(Eventtype.BESKJED.toString())
            duplicates.get(0).eventId.`should be equal to`("$eventId-0")

            duplicates.get(1).eventtype.`should be equal to`(Eventtype.BESKJED.toString())
            duplicates.get(1).eventId.`should be equal to`("$eventId-0")
        }
    }

    @Test
    fun `Skal returnere en tom liste hvis ikke det finnes noen duplikat av onsket eventtype`() {
        val handleEvents = HandleEvents(brukernotifikasjonbestillingRepository)
        val dummyEvents = InternalEventsObjectMother.giveMeANumberOfInternalEvents(numberOfEvents = 1, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)

        val duplicatesWithOnlyBeskjedEvents = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED)
        )

        coEvery { brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(any<MutableMap<NokkelIntern, BeskjedIntern>>()) } returns duplicatesWithOnlyBeskjedEvents

        runBlocking {
            val oppgaveDuplicates = handleEvents.getDuplicateEvents(dummyEvents, Eventtype.OPPGAVE)

            oppgaveDuplicates.`should be equal to`(emptyList())
        }
    }

    @Test
    fun `Skal transformere duplikat av Brukernotifikasjonbestilling til feilrespons`() {
        val handleEvents = HandleEvents(brukernotifikasjonbestillingRepository)
        val duplicateEvents = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("$eventId-1", "$systembruker-1", Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("$eventId-2", "$systembruker-2", Eventtype.BESKJED)
        )
        coEvery { eventMetricsSession.countDuplicateEventForSystemUser(any()) } returns Unit

        val problematicEvents = handleEvents.createFeilresponsEvents(duplicateEvents, Eventtype.BESKJED)

        problematicEvents.size.`should be equal to`(duplicateEvents.size)

        problematicEvents.get(0).key.getEventId().`should be equal to`("$eventId-1")
        problematicEvents.get(0).key.getSystembruker().`should be equal to`("$systembruker-1")
        problematicEvents.get(0).value.getFeilmelding().`should contain`("duplikat")

        problematicEvents.get(1).key.getEventId().`should be equal to`("$eventId-2")
        problematicEvents.get(1).key.getSystembruker().`should be equal to`("$systembruker-2")
        problematicEvents.get(1).value.getFeilmelding().`should contain`("duplikat")
    }

}