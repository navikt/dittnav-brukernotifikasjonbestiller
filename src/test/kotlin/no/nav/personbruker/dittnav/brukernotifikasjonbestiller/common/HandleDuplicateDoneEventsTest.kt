package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.AvroDoneInternObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test

internal class HandleDuplicateDoneEventsTest {
    private val fodselsnummer = "123"
    private val eventId = "eventId"
    private val appnavn = "appnavn"
    private val brukernotifikasjonbestillingRepository = mockk<BrukernotifikasjonbestillingRepository>()

    @Test
    fun `Skal returnere hele listen med vellykket eventer hvis ikke det finnes duplikat`() {
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)
        val successfullyValidatedEvents = AvroDoneInternObjectMother.giveMeANumberOfInternalDoneEvents(numberOfEvents = 3, eventId = eventId, systembruker = appnavn, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size

        coEvery {
            brukernotifikasjonbestillingRepository.fetchExistingEventIdsForDone(any())
        } returns emptyList()

        val result = runBlocking {
            handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
        }

        result.validEvents.size `should be equal to` expectedEventSize
        result.duplicateEvents.size `should be equal to` 0
    }

    @Test
    fun `Skal filtere ut eventer som allerede finnes i basen`() {
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)

        val nokkelDuplicate = createNokkelIntern("$appnavn-0", "$eventId-0", fodselsnummer)
        val nokkelValid = createNokkelIntern("$appnavn-2", "$eventId-2", fodselsnummer)
        val doneIntern = AvroDoneInternObjectMother.createDoneIntern()

        val duplicateInDb = nokkelDuplicate.getEventId()

        coEvery {
            brukernotifikasjonbestillingRepository.fetchExistingEventIdsForDone(any())
        } returns listOf(duplicateInDb)

        val successfullyValidatedEvents = mutableListOf(
                Pair(nokkelDuplicate, doneIntern),
                Pair(nokkelValid, doneIntern)
        )

        val normalEvent = Pair(nokkelValid, doneIntern)
        val eventWithDuplicate = Pair(nokkelDuplicate, doneIntern)
        val expectedEvents = listOf(normalEvent)

        val result = runBlocking {
            handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
        }

        result.validEvents.size `should be equal to` expectedEvents.size
        result.validEvents `should contain` normalEvent
        result.duplicateEvents `should contain` eventWithDuplicate
    }

    @Test
    fun `Skal plassere duplikater innen samme batch i validEvents og duplicateEvents dersom de ikke allerede fantes i basen`() {
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)

        val nokkelDuplicate = createNokkelIntern("$appnavn-0", "$eventId-0", fodselsnummer)
        val nokkelValid = createNokkelIntern("$appnavn-2", "$eventId-2", fodselsnummer)
        val doneIntern = AvroDoneInternObjectMother.createDoneIntern()

        coEvery {
            brukernotifikasjonbestillingRepository.fetchExistingEventIdsForDone(any())
        } returns emptyList()

        val successfullyValidatedEvents =
                mutableListOf(Pair(nokkelDuplicate, doneIntern),
                        Pair(nokkelDuplicate, doneIntern),
                        Pair(nokkelValid, doneIntern))


        val normalEvent = Pair(nokkelValid, doneIntern)
        val eventWithDuplicate = Pair(nokkelDuplicate, doneIntern)
        val expectedEvents = listOf(normalEvent, eventWithDuplicate)

        val result = runBlocking {
            handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
        }

        result.validEvents.size `should be equal to` expectedEvents.size
        result.validEvents `should contain` normalEvent
        result.validEvents `should contain` eventWithDuplicate
        result.duplicateEvents `should contain` eventWithDuplicate
    }

    fun createNokkelIntern(appnavn: String, eventId: String, fnr: String): NokkelIntern {
        return NokkelIntern(
                "12345",
                eventId,
                "123",
                fnr,
                "namespace",
                appnavn,
                "N/A"
        )
    }
}
