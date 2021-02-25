package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class StatusoppdateringTransformerTest {

    val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val transformedNokkel = StatusoppdateringTransformer.toNokkelInternal(nokkelExternal, statusoppdateringExternal)
        val transformedStatusoppdatering = StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)

        transformedNokkel.getFodselsnummer() `should be equal to` statusoppdateringExternal.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` nokkelExternal.getEventId()

        transformedStatusoppdatering.getGrupperingsId() `should be equal to` statusoppdateringExternal.getGrupperingsId()
        transformedStatusoppdatering.getLink() `should be equal to` statusoppdateringExternal.getLink()
        transformedStatusoppdatering.getSikkerhetsnivaa() `should be equal to` statusoppdateringExternal.getSikkerhetsnivaa()
        transformedStatusoppdatering.getTidspunkt() `should be equal to` statusoppdateringExternal.getTidspunkt()
        transformedStatusoppdatering.getStatusGlobal() `should be equal to` statusoppdateringExternal.getStatusGlobal()
        transformedStatusoppdatering.getStatusIntern() `should be equal to` statusoppdateringExternal.getStatusIntern()
        transformedStatusoppdatering.getSakstema() `should be equal to` statusoppdateringExternal.getSakstema()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(nokkelExternal, statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(nokkelExternal, statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(nokkelExternal, statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventid`() {
        val tooLongEventId = "1".repeat(51)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(nokkelExternal, statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(tooLongLink)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(invalidLink)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(emptyLink)
        val transformed = StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)

        statusoppdateringExternal.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid statusGlobal`() {
        val invalidStatusGlobal = "invalidStatusGlobal"
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(invalidStatusGlobal)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow valid statusGlobal`() {
        val validStatusGlobal = "SENDT"
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(validStatusGlobal)

        StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
    }

    @Test
    fun `should allow valid statusGlobal field`() {
        val validStatusGlobal = "MOTTATT"
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(validStatusGlobal)

        StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
    }

    @Test
    fun `do not allow too long statusIntern`() {
        val tooLongStatusIntern = "S".repeat(101)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusIntern(tooLongStatusIntern)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow statusIntern to be null`() {
        val validNullStatusIntern = null
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusIntern(validNullStatusIntern)

        StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
    }

    @Test
    fun `do not allow too long sakstema`() {
        val tooLongSakstema = "S".repeat(51)
        val statusoppdateringExternal = AvroStatusoppdateringObjectMother.createStatusoppdateringWithSakstema(tooLongSakstema)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(statusoppdateringExternal)
            }
        } `should throw` FieldValidationException::class
    }
}