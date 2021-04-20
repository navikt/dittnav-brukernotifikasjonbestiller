package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class OppgaveTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        val transformedNokkel = OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
        val transformedOppgave = OppgaveTransformer.toOppgaveInternal(externalOppgave)

        transformedNokkel.getFodselsnummer() `should be equal to` externalOppgave.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()

        transformedOppgave.getGrupperingsId() `should be equal to` externalOppgave.getGrupperingsId()
        transformedOppgave.getLink() `should be equal to` externalOppgave.getLink()
        transformedOppgave.getTekst() `should be equal to` externalOppgave.getTekst()
        transformedOppgave.getSikkerhetsnivaa() `should be equal to` externalOppgave.getSikkerhetsnivaa()
        transformedOppgave.getTidspunkt() `should be equal to` externalOppgave.getTidspunkt()
        transformedOppgave.getEksternVarsling() `should be equal to` externalOppgave.getEksternVarsling()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val externalOppgave = AvroOppgaveObjectMother.createOppgave()

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(externalNokkel, externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithText(textWithMaxAllowedLength)

        runBlocking {
            OppgaveTransformer.toOppgaveInternal(externalOppgave)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithText(emptyText)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithText(tooLongText)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithLink(tooLongLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithLink(invalidLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow empty link`() {
        val emptyLink = ""
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithLink(emptyLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalOppgave = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(externalOppgave)
            }
        } `should throw` FieldValidationException::class
    }

}