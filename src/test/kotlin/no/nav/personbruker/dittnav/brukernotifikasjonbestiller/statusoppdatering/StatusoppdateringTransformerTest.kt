package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class StatusoppdateringTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        val transformedNokkel = StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
        val transformedStatusoppdatering = StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)

        transformedNokkel.getFodselsnummer() `should be equal to` externalStatusoppdatering.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()

        transformedStatusoppdatering.getGrupperingsId() `should be equal to` externalStatusoppdatering.getGrupperingsId()
        transformedStatusoppdatering.getLink() `should be equal to` externalStatusoppdatering.getLink()
        transformedStatusoppdatering.getSikkerhetsnivaa() `should be equal to` externalStatusoppdatering.getSikkerhetsnivaa()
        transformedStatusoppdatering.getTidspunkt() `should be equal to` externalStatusoppdatering.getTidspunkt()
        transformedStatusoppdatering.getStatusGlobal() `should be equal to` externalStatusoppdatering.getStatusGlobal()
        transformedStatusoppdatering.getStatusIntern() `should be equal to` externalStatusoppdatering.getStatusIntern()
        transformedStatusoppdatering.getSakstema() `should be equal to` externalStatusoppdatering.getSakstema()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventid`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdatering()

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toNokkelInternal(externalNokkel, externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(tooLongLink)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(invalidLink)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithLink(emptyLink)
        val transformed = StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)

        externalStatusoppdatering.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid statusGlobal`() {
        val invalidStatusGlobal = "invalidStatusGlobal"
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(invalidStatusGlobal)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow valid statusGlobal`() {
        val validStatusGlobal = "SENDT"
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(validStatusGlobal)

        StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
    }

    @Test
    fun `should allow valid statusGlobal field`() {
        val validStatusGlobal = "MOTTATT"
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusGlobal(validStatusGlobal)

        StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
    }

    @Test
    fun `do not allow too long statusIntern`() {
        val tooLongStatusIntern = "S".repeat(101)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusIntern(tooLongStatusIntern)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow statusIntern to be null`() {
        val validNullStatusIntern = null
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithStatusIntern(validNullStatusIntern)

        StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
    }

    @Test
    fun `do not allow too long sakstema`() {
        val tooLongSakstema = "S".repeat(51)
        val externalStatusoppdatering = AvroStatusoppdateringObjectMother.createStatusoppdateringWithSakstema(tooLongSakstema)

        invoking {
            runBlocking {
                StatusoppdateringTransformer.toStatusoppdateringInternal(externalStatusoppdatering)
            }
        } `should throw` FieldValidationException::class
    }
}