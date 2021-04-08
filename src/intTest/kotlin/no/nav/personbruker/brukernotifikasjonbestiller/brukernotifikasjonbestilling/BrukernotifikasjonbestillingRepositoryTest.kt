package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling.objectMother.giveMeANumberOfInternalEvents
import no.nav.personbruker.brukernotifikasjonbestiller.database.H2Database
import no.nav.personbruker.brukernotifikasjonbestiller.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.deleteAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BrukernotifikasjonbestillingRepositoryTest {

    private val database = H2Database()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)

    private val eventBeskjed1 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.BESKJED)
    private val eventBeskjed2 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-1", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED)
    private val eventOppgave1 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.OPPGAVE)

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllBrukernotifikasjonbestilling()
            }
        }
    }

    @Test
    fun `Skal returnere korrekt antall eventer med samme eventId uavhengig av eventtype`() {
        runBlocking {
            val expectedEvents = listOf(eventBeskjed1, eventOppgave1)
            createBrukernotifikasjonbestillinger(database, listOf(eventBeskjed1, eventBeskjed2, eventOppgave1))
            val eventAlreadyPersisted = giveMeANumberOfInternalEvents(1, "eventId", "systembruker")

            val result = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(eventAlreadyPersisted)
            result.size.`should be equal to`(expectedEvents.size)
            result `should contain all` expectedEvents
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for vellykket persistering av Brukernotifikasjonbestillinger i batch`() {
        runBlocking {
            val toPersist = giveMeANumberOfInternalEvents(3, "eventId", "systembruker")
            val result = brukernotifikasjonbestillingRepository.persistInOneBatch(toPersist, Eventtype.BESKJED)
            result.getPersistedEntitites().size `should be equal to` toPersist.size
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for persistering i batch hvis noen Brukernotifikasjonbestillinger har unique key constraints`() {
        runBlocking {
            createBrukernotifikasjonbestillinger(database, listOf(eventBeskjed1))
            val mapToPersistWithOneDuplicateEvent = giveMeANumberOfInternalEvents(3, "eventId", "systembruker")
            val expectedPersistResult = mapToPersistWithOneDuplicateEvent.size - 1

            val result = brukernotifikasjonbestillingRepository.persistInOneBatch(mapToPersistWithOneDuplicateEvent, Eventtype.BESKJED)
            result.getPersistedEntitites().size `should be equal to` expectedPersistResult
            result.getConflictingEntities().size `should be equal to` 1
        }
    }

}
