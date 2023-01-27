package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class brukernotifikasjonbestillingQueriesTest {

    private val database = LocalPostgresDatabase.cleanDb()

    private val brukernotifikasjonbestilling1: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.BESKJED, fodselsnummer = "123")
    private val brukernotifikasjonbestilling2: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-1", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED, fodselsnummer = "123")

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllBrukernotifikasjonbestilling()
            }
        }
    }

    @Test
    fun `Persister ikke entitet dersom rad med samme eventId, systembruker og eventtype finnes`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling1, brukernotifikasjonbestilling2))
            database.dbQuery {
                val numberOfEntities = getAllBrukernotifikasjonbestilling().size
                createBrukernotifikasjonbestilling(listOf(brukernotifikasjonbestilling1, brukernotifikasjonbestilling2))
                getAllBrukernotifikasjonbestilling().size shouldBe numberOfEntities
            }
        }
    }

    @Test
    fun `Skal opprette entitet dersom rad med samme eventId og systembruker finnes, men ikke samme eventtype`() {
        val brukernotifikasjonbestilling_oppgave: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.OPPGAVE, fodselsnummer = "123")
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling1, brukernotifikasjonbestilling2))
            database.dbQuery {
                val expectedNumberOfEntities = getAllBrukernotifikasjonbestilling().size + 1
                createBrukernotifikasjonbestilling(listOf(brukernotifikasjonbestilling_oppgave))
                getAllBrukernotifikasjonbestilling().size shouldBe expectedNumberOfEntities
            }
        }
    }

}
