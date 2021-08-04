package no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.brukernotifikasjonbestiller.brukernotifikasjonbestilling.objectMother.giveMeANumberOfInternalEvents
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.H2Database
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.createBrukernotifikasjonbestillinger
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.deleteAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BrukernotifikasjonbestillingRepositoryTest {

    private val database = H2Database()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)

    private val eventBeskjed_0 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.BESKJED)
    private val eventBeskjed_1 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-1", systembruker = "systembruker-1", eventtype = Eventtype.BESKJED)
    private val eventOppgave_0 = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "systembruker-0", eventtype = Eventtype.OPPGAVE)

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
            val expectedEvents = listOf(eventBeskjed_0, eventOppgave_0)
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0, eventBeskjed_1, eventOppgave_0))
            val eventsAlreadyPersisted = giveMeANumberOfInternalEvents(1, "eventId", "systembruker")

            val result = brukernotifikasjonbestillingRepository.fetchEventsThatMatchEventId(eventsAlreadyPersisted)
            result.size.`should be equal to`(expectedEvents.size)
            result `should contain all` expectedEvents
        }
    }

    @Test
    fun `Skal returnere korrekt antall duplikat innenfor en gitt eventtypen`() {
        runBlocking {
            val expectedEvents = listOf(eventBeskjed_0, eventBeskjed_1)
            val duplicateEvents = listOf(eventBeskjed_0, eventBeskjed_1, eventOppgave_0)
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0, eventBeskjed_1, eventOppgave_0))

            val duplicateBeskjeder = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(Eventtype.BESKJED, duplicateEvents)
            duplicateBeskjeder.size.`should be equal to`(expectedEvents.size)
            duplicateBeskjeder `should contain all` expectedEvents
        }
    }

    @Test
    fun `Skal returnere korrekt antall duplikat med samme eventId, systembruker og gitt eventtype`() {
        val eventBeskjedWithDifferentSystemuser = BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "eventId-0", systembruker = "dummy", eventtype = Eventtype.BESKJED)

        runBlocking {
            val expectedEvents = listOf(eventBeskjed_0, eventBeskjed_1)
            val duplicateEvents = listOf(eventBeskjed_0, eventBeskjed_1, eventOppgave_0)
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0, eventBeskjed_1, eventOppgave_0, eventBeskjedWithDifferentSystemuser))

            val duplicateBeskjeder = brukernotifikasjonbestillingRepository.fetchDuplicatesOfEventtype(Eventtype.BESKJED, duplicateEvents)
            duplicateBeskjeder.size.`should be equal to`(expectedEvents.size)
            duplicateBeskjeder `should contain all` expectedEvents
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
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))
            val mapToPersistWithOneDuplicateEvent = giveMeANumberOfInternalEvents(3, "eventId", "systembruker")
            val expectedPersistResult = mapToPersistWithOneDuplicateEvent.size - 1

            val result = brukernotifikasjonbestillingRepository.persistInOneBatch(mapToPersistWithOneDuplicateEvent, Eventtype.BESKJED)
            result.getPersistedEntitites().size `should be equal to` expectedPersistResult
            result.getConflictingEntities().size `should be equal to` 1
        }
    }

    @Test
    fun `Skal returnere en tom liste av duplikat naar successfullyValidatedEvents inneholder duplikat, men eventene finnes ikke i basen saa ett av eventene er ikke et duplikat`() {
        val fodselsnummer = "123"
        val nokkel_0 = AvroNokkelInternObjectMother.createNokkelIntern("systembruker-0", "eventId-0", fodselsnummer)
        val nokkel_3 = AvroNokkelInternObjectMother.createNokkelIntern("systembruker-3", "eventId-3", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val successfullyValidatedEvents =
                mutableListOf(
                        Pair(nokkel_0, beskjedIntern),
                        Pair(nokkel_0, beskjedIntern),
                        Pair(nokkel_3, beskjedIntern)
                )

        val expectedEvent = emptyList<Brukernotifikasjonbestilling>()

        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_1))

            val duplicates = handleDuplicateEvents.getDuplicateEvents(successfullyValidatedEvents)
            duplicates.size `should be equal to` expectedEvent.size
        }
    }

    @Test
    fun `Skal returnere en liste av duplikat naar successfullyValidatedEvents inneholder duplikat som finnes i basen`() {
        val fodselsnummer = "123"
        val nokkel_0 = AvroNokkelInternObjectMother.createNokkelIntern("systembruker-0", "eventId-0", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val successfullyValidatedEvents =
                mutableListOf(Pair(nokkel_0, beskjedIntern))

        val expectedEvent = listOf(Pair(nokkel_0, beskjedIntern))

        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))

            val duplicates = handleDuplicateEvents.getDuplicateEvents(successfullyValidatedEvents)
            duplicates.size `should be equal to` expectedEvent.size
            duplicates[0].eventId `should be equal to` expectedEvent[0].first.getEventId()
            duplicates[0].systembruker `should be equal to` expectedEvent[0].first.getSystembruker()
        }
    }

    @Test
    fun `Skal returnere en tom liste hvis successfullyValidatedEvents ikke inneholder duplikat`() {
        val fodselsnummer = "123"
        val nokkel_1 = AvroNokkelInternObjectMother.createNokkelIntern("systembruker-1", "eventId-1", fodselsnummer)
        val nokkel_2 = AvroNokkelInternObjectMother.createNokkelIntern("systembruker-2", "eventId-2", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val successfullyValidatedEvents =
                mutableListOf(
                        Pair(nokkel_1, beskjedIntern),
                        Pair(nokkel_2, beskjedIntern)
                )

        val expectedEvent = emptyList<Brukernotifikasjonbestilling>()

        runBlocking {
            database.createBrukernotifikasjonbestillinger(listOf(eventBeskjed_0))

            val duplicates = handleDuplicateEvents.getDuplicateEvents(successfullyValidatedEvents)
            duplicates.size `should be equal to` expectedEvent.size
        }
    }
}
