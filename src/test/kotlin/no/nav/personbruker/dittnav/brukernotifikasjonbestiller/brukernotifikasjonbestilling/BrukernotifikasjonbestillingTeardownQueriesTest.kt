package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.deleteAllBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.junit.jupiter.api.Test

class BrukernotifikasjonbestillingTeardownQueriesTest {

    private val database = LocalPostgresDatabase.cleanDb()

    private val event1 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-001", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED, fodselsnummer = "123")
    private val event2 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-002", systembruker = "systembruker-2", eventtype = Eventtype.BESKJED, fodselsnummer = "123")
    private val event3 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "B-test-003", systembruker = "systembruker-3", eventtype = Eventtype.BESKJED, fodselsnummer = "123")

    @Test
    fun `Skal slette alle rader i event-tabellen`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(event1, event2, event3))
            var result = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            result.size shouldBe 3
            database.deleteAllBrukernotifikasjonbestillinger()
            result = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            result.isEmpty() shouldBe true
        }
    }
}



