package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BrukernotifikasjonbestillingRepositoryTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)

    private val eventBeskjed_0 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.BESKJED, fodselsnummer = "0")

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllBrukernotifikasjonbestilling()
            }
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for vellykket persistering av Brukernotifikasjonbestillinger i batch`() {
        runBlocking {
            val toPersist = giveMeANumberOfInternalEvents(3, "eventId", "systembruker")
            val result = brukernotifikasjonbestillingRepository.persistInOneBatch(toPersist, Eventtype.BESKJED)
            result.getPersistedEntitites().size shouldBe toPersist.size
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for persistering i batch hvis noen Brukernotifikasjonbestillinger har unique key constraints`() {
        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))
            val mapToPersistWithOneDuplicateEvent = giveMeANumberOfInternalEvents(3, "eventId", "systembruker")
            val expectedPersistResult = mapToPersistWithOneDuplicateEvent.size - 1

            val result = brukernotifikasjonbestillingRepository.persistInOneBatch(mapToPersistWithOneDuplicateEvent, Eventtype.BESKJED)
            result.getPersistedEntitites().size shouldBe expectedPersistResult
            result.getConflictingEntities().size shouldBe 1
        }
    }

    @Test
    fun `Skal returnere en liste av duplikat naar successfullyValidatedEvents inneholder duplikat som finnes i basen`() {
        val fodselsnummer = "123"
        val nokkel_0 = AvroNokkelInternObjectMother.createNokkelIntern("123", "eventId-0", "1234",  fodselsnummer, "namespace", "app-0", "systembruker-0")
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val successfullyValidatedEvents =
                mutableListOf(Pair(nokkel_0, beskjedIntern))

        val expectedEvent = listOf(Pair(nokkel_0, beskjedIntern))

        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))

            val duplicates = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents).duplicateEvents
            duplicates.size shouldBe expectedEvent.size
            duplicates[0].first.getEventId() shouldBe expectedEvent[0].first.getEventId()
            duplicates[0].first.getSystembruker() shouldBe expectedEvent[0].first.getSystembruker()
        }
    }

    @Test
    fun `Skal returnere en tom liste hvis successfullyValidatedEvents ikke inneholder duplikat`() {
        val fodselsnummer = "123"
        val nokkel_1 = AvroNokkelInternObjectMother.createNokkelIntern("123", "eventId-1", "1234",  fodselsnummer, "namespace", "app-1", "systembruker-1")
        val nokkel_2 = AvroNokkelInternObjectMother.createNokkelIntern("123", "eventId-2", "1234",  fodselsnummer, "namespace", "app-2", "systembruker-2")
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val successfullyValidatedEvents =
                mutableListOf(
                        Pair(nokkel_1, beskjedIntern),
                        Pair(nokkel_2, beskjedIntern)
                )

        val expectedEvent = emptyList<Brukernotifikasjonbestilling>()

        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))

            val duplicates = handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents).duplicateEvents
            duplicates.size shouldBe expectedEvent.size
        }
    }
}