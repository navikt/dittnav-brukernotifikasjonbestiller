package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test

internal class HandleDuplicateEventsTest {

    private val fodselsnummer = "123"
    private val eventId = "eventId"
    private val systembruker = "systembruker"
    private val brukernotifikasjonbestillingRepository = mockk<BrukernotifikasjonbestillingRepository>()

    @Test
    fun `Skal returnere hele listen med vellykket eventer hvis ikke det finnes duplikat`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val successfullyValidatedEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size

        coEvery {
            brukernotifikasjonbestillingRepository.fetchBrukernotifikasjonKeysThatMatchEventIds(any())
        } returns emptyList()

        val result = runBlocking {
            handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
        }

        result.validEvents.size `should be equal to` expectedEventSize
        result.duplicateEvents.size `should be equal to` 0
    }

    @Test
    fun `Skal filtere ut eventer som allerede finnes i basen`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)

        val nokkelDuplicate = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-0", "$eventId-0", fodselsnummer)
        val nokkelValid = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-2", "$eventId-2", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val duplicateInDb = BrukernotifikasjonKey(nokkelDuplicate.getEventId(), nokkelDuplicate.getSystembruker(), Eventtype.BESKJED)

        coEvery {
            brukernotifikasjonbestillingRepository.fetchBrukernotifikasjonKeysThatMatchEventIds(any())
        } returns listOf(duplicateInDb)

        val successfullyValidatedEvents = mutableListOf(
                Pair(nokkelDuplicate, beskjedIntern),
                Pair(nokkelValid, beskjedIntern)
        )


        val normalEvent = Pair(nokkelValid, beskjedIntern)
        val eventWithDuplicate = Pair(nokkelDuplicate, beskjedIntern)
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
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)

        val nokkelDuplicate = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-0", "$eventId-0", fodselsnummer)
        val nokkelValid = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-2", "$eventId-2", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        coEvery {
            brukernotifikasjonbestillingRepository.fetchBrukernotifikasjonKeysThatMatchEventIds(any())
        } returns emptyList()

        val successfullyValidatedEvents =
                mutableListOf(Pair(nokkelDuplicate, beskjedIntern),
                        Pair(nokkelDuplicate, beskjedIntern),
                        Pair(nokkelValid, beskjedIntern))


        val normalEvent = Pair(nokkelValid, beskjedIntern)
        val eventWithDuplicate = Pair(nokkelDuplicate, beskjedIntern)
        val expectedEvents = listOf(normalEvent, eventWithDuplicate)

        val result = runBlocking {
            handleDuplicateEvents.checkForDuplicateEvents(successfullyValidatedEvents)
        }

        result.validEvents.size `should be equal to` expectedEvents.size
        result.validEvents `should contain` normalEvent
        result.validEvents `should contain` eventWithDuplicate
        result.duplicateEvents `should contain` eventWithDuplicate
    }

}
