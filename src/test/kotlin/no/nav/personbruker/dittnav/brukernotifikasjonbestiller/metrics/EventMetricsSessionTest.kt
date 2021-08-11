package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.NokkelEventPairObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

internal class EventMetricsSessionTest {

    @Test
    fun `Skal telle event hvis nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)

        session.countNokkelWasNull()

        session.getEventsSeen() `should be` 1
        session.getNokkelWasNull() `should be` 1
    }

    @Test
    fun `Skal telle event som feiler`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countFailedEventForSystemUser(systemUser)

        session.getEventsSeen() `should be` 1
        session.getEventsSeen(systemUser) `should be` 1
    }

    @Test
    fun `Skal telle vellykket event `() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countSuccessfulEventForSystemUser(systemUser)

        session.getEventsSeen() `should be` 1
        session.getEventsSeen(systemUser) `should be` 1
    }

    @Test
    fun `Skal telle rett antall totale events fra Kafka`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countNokkelWasNull()
        session.countFailedEventForSystemUser(systemUser)
        session.countSuccessfulEventForSystemUser(systemUser)

        session.getEventsSeen() `should be` 3
        session.getEventsSeen(systemUser) `should be` 2
        session.getNokkelWasNull() `should be` 1
        session.getEventsFailed() `should be` 1
        session.getEventsProcessed() `should be` 1
    }

    @Test
    fun `Skal telle duplikat per systembruker`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countDuplicateEventForSystemUser(systemUser)

        session.getDuplicateKeys().size `should be` 1
        session.getDuplicateKeys(systemUser) `should be` 1
    }

    @Test
    fun `Skal telle alle duplikat`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val duplicate_1 = NokkelEventPairObjectMother.createEventPair(eventId = "eventId-0", systembruker = "systembruker-0")
        val duplicate_2 = NokkelEventPairObjectMother.createEventPair(eventId = "eventId-1", systembruker = "systembruker-1")
        val duplicateEvents = listOf(duplicate_1, duplicate_2)

        session.countDuplicateEvents(duplicateEvents)

        session.getDuplicateKeys().size `should be equal to` duplicateEvents.size
        session.getDuplicateKeys("systembruker-0") `should be equal to` 1
        session.getDuplicateKeys("systembruker-1") `should be equal to` 1
    }
}
