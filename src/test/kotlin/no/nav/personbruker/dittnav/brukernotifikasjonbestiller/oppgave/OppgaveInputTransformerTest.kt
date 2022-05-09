package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppgaveInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

    @Test
    fun `should transform from external to internal`() {
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        val (transformedNokkel, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getFodselsnummer() shouldBe externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() shouldBe externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() shouldBe externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() shouldBe externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() shouldBe externalNokkelInput.getAppnavn()

        transformedOppgave.getLink() shouldBe externalOppgaveInput.getLink()
        transformedOppgave.getTekst() shouldBe externalOppgaveInput.getTekst()
        transformedOppgave.getSikkerhetsnivaa() shouldBe externalOppgaveInput.getSikkerhetsnivaa()
        transformedOppgave.getTidspunkt() shouldBe externalOppgaveInput.getTidspunkt()
        transformedOppgave.getSynligFremTil() shouldBe externalOppgaveInput.getSynligFremTil()
        transformedOppgave.getEksternVarsling() shouldBe externalOppgaveInput.getEksternVarsling()
        transformedOppgave.getPrefererteKanaler() shouldBe externalOppgaveInput.getPrefererteKanaler()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        val (transformedNokkel, _) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getEventId() shouldBe uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        val (transformedNokkel, _) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedNokkel.getEventId() shouldBe ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)
        val oppgaveUtenSynligTilSatt = AvroOppgaveInputObjectMother.createOppgaveInputWithSynligFremTil(null)

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, oppgaveUtenSynligTilSatt)

        transformedOppgave.getSynligFremTil().shouldBeNull()
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "grupperingsId"
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

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithText(tooLongText)

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithLink(tooLongLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithLink(invalidLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        externalOppgaveInput.getPrefererteKanaler() shouldBe transformedOppgave.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getSmsVarslingstekst() shouldBe externalOppgaveInput.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getSmsVarslingstekst().shouldBeNull()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getEpostVarslingstekst() shouldBe externalOppgaveInput.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getEpostVarslingstekst().shouldBeNull()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(4_001)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstittel = "Hei ".repeat(10)
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getEpostVarslingstittel() shouldBe externalOppgaveInput.getEpostVarslingstittel()
    }

    @Test
    fun `should allow null epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstittel = null
        )

        val (_, transformedOppgave) = OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)

        transformedOppgave.getEpostVarslingstittel().shouldBeNull()
    }

    @Test
    fun `do not allow epostVarslingstittel if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = false,
            epostVarslingstittel = "<p>Hei!</p>"
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow too long email titel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstittel = "L".repeat(41)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow empty email tittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalOppgaveInput = AvroOppgaveInputObjectMother.createOppgaveInput(
            eksternVarsling = true,
            epostVarslingstittel = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                OppgaveInputTransformer.toInternal(externalNokkelInput, externalOppgaveInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }
}
