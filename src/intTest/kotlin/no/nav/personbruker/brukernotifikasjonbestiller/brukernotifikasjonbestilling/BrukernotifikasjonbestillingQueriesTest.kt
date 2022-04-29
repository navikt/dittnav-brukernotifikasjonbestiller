package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling.objectMother.giveMeANumberOfInternalEvents
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class brukernotifikasjonbestillingQueriesTest {

    private val database = LocalPostgresDatabase.cleanDb()

    private val brukernotifikasjonbestilling_1: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.BESKJED, fodselsnummer = "123")
    private val brukernotifikasjonbestilling_2: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-1", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED, fodselsnummer = "123")

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllBrukernotifikasjonbestilling()
            }
        }
    }

    @Test
    fun `Finner event med samme eventId, systembruker og eventtype`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling_1, brukernotifikasjonbestilling_2))
            val result = database.dbQuery { getEventsByIds(brukernotifikasjonbestilling_1.eventId, brukernotifikasjonbestilling_1.systembruker, brukernotifikasjonbestilling_1.eventtype) }
            result.size `should be equal to` 1
            result.first() `should be equal to` brukernotifikasjonbestilling_1
        }
    }

    @Test
    fun `Returnerer tomt resultat hvis event med eventId, systembruker og gitt eventtype ikke finnes`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling_1, brukernotifikasjonbestilling_2))
            val result = database.dbQuery { getEventsByIds("noMatch", "noMatch", Eventtype.BESKJED) }
            result `should be equal to` emptyList()
        }
    }

    @Test
    fun `Persister ikke entitet dersom rad med samme eventId, systembruker og eventtype finnes`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling_1, brukernotifikasjonbestilling_2))
            database.dbQuery {
                val numberOfEntities = getAllBrukernotifikasjonbestilling().size
                createBrukernotifikasjonbestilling(listOf(brukernotifikasjonbestilling_1, brukernotifikasjonbestilling_2))
                getAllBrukernotifikasjonbestilling().size `should be equal to` numberOfEntities
            }
        }
    }

    @Test
    fun `Skal opprette entitet dersom rad med samme eventId og systembruker finnes, men ikke samme eventtype`() {
        val brukernotifikasjonbestilling_oppgave: Brukernotifikasjonbestilling = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.OPPGAVE, fodselsnummer = "123")
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(brukernotifikasjonbestilling_1, brukernotifikasjonbestilling_2))
            database.dbQuery {
                val expectedNumberOfEntities = getAllBrukernotifikasjonbestilling().size + 1
                createBrukernotifikasjonbestilling(listOf(brukernotifikasjonbestilling_oppgave))
                getAllBrukernotifikasjonbestilling().size `should be equal to` expectedNumberOfEntities
            }
        }
    }

}
