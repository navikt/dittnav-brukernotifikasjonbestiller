package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class OppgaveTransformerTest {
    val eventId = "1"

    @Test
    fun `should transform from external to internal`() {
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgave()

        val transformedNokkel = OppgaveTransformer.toNokkelInternal(nokkelExternal, oppgaveExternal)
        val transformedOppgave = OppgaveTransformer.toOppgaveInternal(oppgaveExternal)

        transformedNokkel.getFodselsnummer() `should be equal to` oppgaveExternal.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` nokkelExternal.getEventId()

        transformedOppgave.getGrupperingsId() `should be equal to` oppgaveExternal.getGrupperingsId()
        transformedOppgave.getLink() `should be equal to` oppgaveExternal.getLink()
        transformedOppgave.getTekst() `should be equal to` oppgaveExternal.getTekst()
        transformedOppgave.getSikkerhetsnivaa() `should be equal to` oppgaveExternal.getSikkerhetsnivaa()
        transformedOppgave.getTidspunkt() `should be equal to` oppgaveExternal.getTidspunkt()
        transformedOppgave.getEksternVarsling() `should be equal to` oppgaveExternal.getEksternVarsling()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(nokkelExternal, oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(nokkelExternal, oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgave()

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(nokkelExternal, oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long eventid`() {
        val tooLongEventId = "1".repeat(51)
        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgave()

        invoking {
            runBlocking {
                OppgaveTransformer.toNokkelInternal(nokkelExternal, oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithGrupperingsId(tooLongGrupperingsId)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithText(textWithMaxAllowedLength)

        runBlocking {
            OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithText(emptyText)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithText(tooLongText)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithLink(tooLongLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithLink(invalidLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow empty link`() {
        val emptyLink = ""
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithLink(emptyLink)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val oppgaveExternal = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                OppgaveTransformer.toOppgaveInternal(oppgaveExternal)
            }
        } `should throw` FieldValidationException::class
    }

}