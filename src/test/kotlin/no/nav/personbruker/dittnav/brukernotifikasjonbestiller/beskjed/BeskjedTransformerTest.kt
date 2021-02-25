package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class BeskjedTransformerTest {

    val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjed()

        val transformedNokkel = BeskjedTransformer.toNokkelInternal(nokkelExternal, beskjedExternal)
        val transformedBeskjed = BeskjedTransformer.toBeskjedInternal(beskjedExternal)

        transformedNokkel.getFodselsnummer() `should be equal to` beskjedExternal.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` nokkelExternal.getEventId()

        transformedBeskjed.getGrupperingsId() `should be equal to` beskjedExternal.getGrupperingsId()
        transformedBeskjed.getLink() `should be equal to` beskjedExternal.getLink()
        transformedBeskjed.getTekst() `should be equal to` beskjedExternal.getTekst()
        transformedBeskjed.getSikkerhetsnivaa() `should be equal to` beskjedExternal.getSikkerhetsnivaa()
        transformedBeskjed.getTidspunkt() `should be equal to` beskjedExternal.getTidspunkt()
        transformedBeskjed.getEksternVarsling() `should be equal to` beskjedExternal.getEksternVarsling()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(nokkelExternal, beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(nokkelExternal, beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val beskjedUtenSynligTilSatt = AvroBeskjedObjectMother.createBeskjedWithSynligFremTil(null)

        val transformed = BeskjedTransformer.toBeskjedInternal(beskjedUtenSynligTilSatt)

        transformed.getSynligFremTil().`should be null`()
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjed()

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(nokkelExternal, beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventid`() {
        val tooLongEventId = "1".repeat(51)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjed()

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(nokkelExternal, beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithText(textWithMaxAllowedLength)

        runBlocking {
            BeskjedTransformer.toBeskjedInternal(beskjedExternal)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithText(emptyText)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithText(tooLongText)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithLink(tooLongLink)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithLink(invalidLink)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithLink(emptyLink)
        val transformed = BeskjedTransformer.toBeskjedInternal(beskjedExternal)

        beskjedExternal.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val beskjedExternal = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(beskjedExternal)
            }
        } `should throw` FieldValidationException::class
    }

}