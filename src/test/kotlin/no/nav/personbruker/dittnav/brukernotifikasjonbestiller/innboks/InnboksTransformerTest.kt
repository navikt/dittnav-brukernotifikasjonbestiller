package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class InnboksTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        val transformedNokkel = InnboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
        val transformedInnboks = InnboksTransformer.toInnboksInternal(externalInnboks)

        transformedNokkel.getFodselsnummer() `should be equal to` externalInnboks.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()

        transformedInnboks.getGrupperingsId() `should be equal to` externalInnboks.getGrupperingsId()
        transformedInnboks.getLink() `should be equal to` externalInnboks.getLink()
        transformedInnboks.getTekst() `should be equal to` externalInnboks.getTekst()
        transformedInnboks.getSikkerhetsnivaa() `should be equal to` externalInnboks.getSikkerhetsnivaa()
        transformedInnboks.getTidspunkt() `should be equal to` externalInnboks.getTidspunkt()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                InnboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                InnboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        invoking {
            runBlocking {
                InnboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val externalInnboks = AvroInnboksObjectMother.createInnboks()

        invoking {
            runBlocking {
                InnboksTransformer.toNokkelInternal(externalNokkel, externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsid`() {
        val tooLongGrupperingsid = "G".repeat(101)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithGrupperingsid(tooLongGrupperingsid)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithText(textWithMaxAllowedLength)

        runBlocking {
            InnboksTransformer.toInnboksInternal(externalInnboks)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithText(emptyText)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithText(tooLongText)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithLink(tooLongLink)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithLink(invalidLink)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithLink(emptyLink)
        val transformed = InnboksTransformer.toInnboksInternal(externalInnboks)

        externalInnboks.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalInnboks = AvroInnboksObjectMother.createInnboksWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                InnboksTransformer.toInnboksInternal(externalInnboks)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }
}
