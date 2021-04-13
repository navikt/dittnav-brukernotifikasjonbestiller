package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
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
        val successfullyValidatedEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size - duplicateEvent.size

        val eventsWithoutDuplicates = handleEvents.getRemainingValidatedEvents(successfullyValidatedEvents, duplicateEvent, Eventtype.BESKJED)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
    }

    @Test
    fun `Skal returnere hele listen med vellykket eventer hvis ikke det finnes duplikat`() {
        val handleEvents = HandleEvents(brukernotifikasjonbestillingRepository)
        val emptyListOfduplicateEvents = emptyList<Brukernotifikasjonbestilling>()
        val successfullyValidatedEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size

        val eventsWithoutDuplicates = handleEvents.getRemainingValidatedEvents(successfullyValidatedEvents, emptyListOfduplicateEvents, Eventtype.BESKJED)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
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