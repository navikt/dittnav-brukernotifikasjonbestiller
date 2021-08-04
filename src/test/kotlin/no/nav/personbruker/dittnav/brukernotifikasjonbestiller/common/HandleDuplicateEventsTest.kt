package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import io.mockk.mockk
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.junit.jupiter.api.Test

internal class HandleDuplicateEventsTest {

    private val fodselsnummer = "123"
    private val eventId = "eventId"
    private val systembruker = "systembruker"
    private val eventMetricsSession = mockk<EventMetricsSession>()
    private val brukernotifikasjonbestillingRepository = mockk<BrukernotifikasjonbestillingRepository>()

    @Test
    fun `Skal ikke inneholde duplikat i listen som returneres`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val duplicateEvent = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED))
        val successfullyValidatedEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size - duplicateEvent.size

        val eventsWithoutDuplicates = handleDuplicateEvents.getValidatedEventsWithoutDuplicates(successfullyValidatedEvents, duplicateEvent)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
    }

    @Test
    fun `Skal returnere hele listen med vellykket eventer hvis ikke det finnes duplikat`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val emptyListOfduplicateEvents = emptyList<Brukernotifikasjonbestilling>()
        val successfullyValidatedEvents = AvroBeskjedInternObjectMother.giveMeANumberOfInternalBeskjedEvents(numberOfEvents = 3, eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        val expectedEventSize = successfullyValidatedEvents.size

        val eventsWithoutDuplicates = handleDuplicateEvents.getValidatedEventsWithoutDuplicates(successfullyValidatedEvents, emptyListOfduplicateEvents)
        eventsWithoutDuplicates.size.`should be equal to`(expectedEventSize)
    }

    @Test
    fun `Skal returnere en liste uten duplikat selvom successfullyValidatedEvents inneholder flere duplikat med samme nokkel`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)

        val duplicateEvents = listOf(BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling(eventId = "$eventId-0", systembruker = "$systembruker-0", eventtype = Eventtype.BESKJED))

        val nokkelDuplicate = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-0", "$eventId-0", fodselsnummer)
        val nokkelValid = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-2", "$eventId-2", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val successfullyValidatedEvents =
                mutableListOf(Pair(nokkelDuplicate, beskjedIntern),
                        Pair(nokkelDuplicate, beskjedIntern),
                        Pair(nokkelValid, beskjedIntern))

        val expectedEvent = listOf(Pair(nokkelValid, beskjedIntern))

        val eventsWithoutDuplicates = handleDuplicateEvents.getValidatedEventsWithoutDuplicates(successfullyValidatedEvents, duplicateEvents)
        eventsWithoutDuplicates.size `should be equal to` expectedEvent.size
        eventsWithoutDuplicates `should contain all` expectedEvent
    }

    @Test
    fun `Skal returnere en liste uten duplikat naar successfullyValidatedEvents inneholder duplikat, men duplikat listen er tom, det vil si duplikatene i batch-en finnes ikke i basen og derfor er ikke et av de duplikat`() {
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)

        val emptyDuplicateList = emptyList<Brukernotifikasjonbestilling>()

        val nokkel_0 = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-0", "$eventId-0", fodselsnummer)
        val nokkel_1 = AvroNokkelInternObjectMother.createNokkelIntern("$systembruker-1", "$eventId-1", fodselsnummer)
        val beskjedIntern = AvroBeskjedInternObjectMother.createBeskjedInternWithGrupperingsId("123")

        val successfullyValidatedEventsWithDuplicates =
                mutableListOf(Pair(nokkel_0, beskjedIntern),
                        Pair(nokkel_0, beskjedIntern),
                        Pair(nokkel_1, beskjedIntern))

        val expectedEvents = listOf(Pair(nokkel_0, beskjedIntern), Pair(nokkel_1, beskjedIntern))

        val eventsWithoutDuplicates = handleDuplicateEvents.getValidatedEventsWithoutDuplicates(successfullyValidatedEventsWithDuplicates, emptyDuplicateList)
        eventsWithoutDuplicates.size `should be equal to` expectedEvents.size
        eventsWithoutDuplicates `should contain all` expectedEvents
    }

}