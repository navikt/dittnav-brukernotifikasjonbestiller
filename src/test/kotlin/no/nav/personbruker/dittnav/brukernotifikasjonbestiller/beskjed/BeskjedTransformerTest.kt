package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class BeskjedTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        val transformedNokkel = BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
        val transformedBeskjed = BeskjedTransformer.toBeskjedInternal(externalBeskjed)

        transformedNokkel.getFodselsnummer() `should be equal to` externalBeskjed.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()

        transformedBeskjed.getGrupperingsId() `should be equal to` externalBeskjed.getGrupperingsId()
        transformedBeskjed.getLink() `should be equal to` externalBeskjed.getLink()
        transformedBeskjed.getTekst() `should be equal to` externalBeskjed.getTekst()
        transformedBeskjed.getSikkerhetsnivaa() `should be equal to` externalBeskjed.getSikkerhetsnivaa()
        transformedBeskjed.getTidspunkt() `should be equal to` externalBeskjed.getTidspunkt()
        transformedBeskjed.getEksternVarsling() `should be equal to` externalBeskjed.getEksternVarsling()
        transformedBeskjed.getPrefererteKanaler() `should be equal to` externalBeskjed.getPrefererteKanaler()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
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
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjed()

        invoking {
            runBlocking {
                BeskjedTransformer.toNokkelInternal(externalNokkel, externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithText(textWithMaxAllowedLength)

        runBlocking {
            BeskjedTransformer.toBeskjedInternal(externalBeskjed)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithText(emptyText)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithText(tooLongText)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithLink(tooLongLink)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithLink(invalidLink)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithLink(emptyLink)
        val transformed = BeskjedTransformer.toBeskjedInternal(externalBeskjed)

        externalBeskjed.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                BeskjedTransformer.toBeskjedInternal(externalBeskjed)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalBeskjed = AvroBeskjedObjectMother.createBeskjedWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val transformed = BeskjedTransformer.toBeskjedInternal(externalBeskjed)

        externalBeskjed.getPrefererteKanaler() `should be equal to` transformed.getPrefererteKanaler()
    }
}
