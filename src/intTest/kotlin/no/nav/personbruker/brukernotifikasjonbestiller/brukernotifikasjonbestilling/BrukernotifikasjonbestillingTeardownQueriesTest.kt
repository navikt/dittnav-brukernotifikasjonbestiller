package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.brukernotifikasjonbestiller.database.H2Database
import no.nav.personbruker.brukernotifikasjonbestiller.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.brukernotifikasjonbestiller.database.deleteAllBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.getAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class BrukernotifikasjonbestillingTeardownQueriesTest {

    private val database = H2Database()

    private val event1 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-001", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED)
    private val event2 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-002", systembruker = "systembruker-2", eventtype = Eventtype.BESKJED)
    private val event3 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-003", systembruker = "systembruker-3", eventtype = Eventtype.BESKJED)

    @Test
    fun `Skal slette alle rader i event-tabellen`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(event1, event2, event3))
            var result = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            result.size `should be equal to` 3
            database.deleteAllBrukernotifikasjonbestillinger()
            result = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            result.isEmpty() `should be equal to` true
        }
    }
}



