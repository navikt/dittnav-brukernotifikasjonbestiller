package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.InternalEventsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test

internal class HandleDuplicateEventsKtTest {

    private val fodselsnummer = "123"
    private val eventId = "eventId"
    private val systembruker = "systembruker"
    private val eventMetricsSession = mockk<EventMetricsSession>()

    @Test
    fun `Skal ikke inneholde duplikat i listen som sendes til kafka`() {
        val duplicateEvent = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED))
        val successfullyValidatedEvents = InternalEventsObjectMother.giveMeANumberOfInternalEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size - duplicateEvent.size

        val eventsWithoutDuplicates = getRemainingValidatedEvents(successfullyValidatedEvents, duplicateEvent, Eventtype.BESKJED)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
    }

    @Test
    fun `Skal legge duplikat til i problematicEvents listen`() {
        val problematicEvents = FeilresponsObjectMother.giveMeANumberOfFeilresponsEvents(1, eventId, systembruker, Eventtype.BESKJED)
        val duplicateEvents = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("$eventId-1", "$systembruker-1", Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("$eventId-2", "$systembruker-2", Eventtype.BESKJED)
        )
        val expectedNumberOfEvents = problematicEvents.size + duplicateEvents.size
        coEvery { eventMetricsSession.countDuplicateEventForSystemUser(any()) } returns Unit

        addDuplicatesToProblematicEventsList(duplicateEvents, problematicEvents, eventMetricsSession)

        problematicEvents.size.`should be equal to`(expectedNumberOfEvents)

        problematicEvents.get(0).key.getEventId().`should be equal to`("$eventId-0")
        problematicEvents.get(0).key.getSystembruker().`should be equal to`("$systembruker-0")
        problematicEvents.get(0).value.getFeilmelding().`should contain`("feil")

        problematicEvents.get(1).key.getEventId().`should be equal to`("$eventId-1")
        problematicEvents.get(1).key.getSystembruker().`should be equal to`("$systembruker-1")
        problematicEvents.get(1).value.getFeilmelding().`should contain`("duplikat")

        problematicEvents.get(2).key.getEventId().`should be equal to`("$eventId-2")
        problematicEvents.get(2).key.getSystembruker().`should be equal to`("$systembruker-2")
        problematicEvents.get(2).value.getFeilmelding().`should contain`("duplikat")
    }

}