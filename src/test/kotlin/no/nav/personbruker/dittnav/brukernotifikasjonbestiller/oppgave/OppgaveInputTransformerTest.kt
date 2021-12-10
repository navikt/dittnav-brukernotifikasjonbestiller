package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

    @Test
    fun `should transform from external to internal`() {
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        val (transformedNokkel, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getFodselsnummer() `should be equal to` externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() `should be equal to` externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() `should be equal to` externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() `should be equal to` externalNokkelInput.getAppnavn()

        transformedOppgave.getLink() `should be equal to` externalOppgaveInput.getLink()
        transformedOppgave.getTekst() `should be equal to` externalOppgaveInput.getTekst()
        transformedOppgave.getSikkerhetsnivaa() `should be equal to` externalOppgaveInput.getSikkerhetsnivaa()
        transformedOppgave.getTidspunkt() `should be equal to` externalOppgaveInput.getTidspunkt()
        transformedOppgave.getSynligFremTil() `should be equal to` externalOppgaveInput.getSynligFremTil()
        transformedOppgave.getEksternVarsling() `should be equal to` externalOppgaveInput.getEksternVarsling()
        transformedOppgave.getPrefererteKanaler() `should be equal to` externalOppgaveInput.getPrefererteKanaler()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        val (transformedNokkel, _) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getEventId() `should be equal to` uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        val (transformedNokkel, _) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getEventId() `should be equal to` ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)
        val oppgaveUtenSynligTilSatt = AvroOppgaveInputObjectMother.createOppgaveInputWithSynligFremTil(null)

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, oppgaveUtenSynligTilSatt)

        transformedOppgave.getSynligFremTil().`should be null`()
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithText(textWithMaxAllowedLength)

        runBlocking {
            OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithText(emptyText)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithText(tooLongText)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithLink(tooLongLink)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithLink(invalidLink)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        externalOppgaveInput.getPrefererteKanaler() `should be equal to` transformedOppgave.getPrefererteKanaler()
    }
}
