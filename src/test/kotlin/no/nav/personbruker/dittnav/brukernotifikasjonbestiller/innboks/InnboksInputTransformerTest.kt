package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

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

internal class InnboksInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

    @Test
    fun `should transform from external to internal`() {
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        val (transformedNokkel, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getFodselsnummer() shouldBe externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() shouldBe externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() shouldBe externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() shouldBe externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() shouldBe externalNokkelInput.getAppnavn()

        transformedInnboks.getLink() shouldBe externalInnboksInput.getLink()
        transformedInnboks.getTekst() shouldBe externalInnboksInput.getTekst()
        transformedInnboks.getSikkerhetsnivaa() shouldBe externalInnboksInput.getSikkerhetsnivaa()
        transformedInnboks.getTidspunkt() shouldBe externalInnboksInput.getTidspunkt()
        transformedInnboks.getEksternVarsling() shouldBe externalInnboksInput.getEksternVarsling()
        transformedInnboks.getPrefererteKanaler() shouldBe externalInnboksInput.getPrefererteKanaler()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        val (transformedNokkel, _) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getEventId() shouldBe uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        val (transformedNokkel, _) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getEventId() shouldBe ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithText(textWithMaxAllowedLength)

        runBlocking {
            InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithText(emptyText)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithText(tooLongText)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(tooLongLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(invalidLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(emptyLink)
        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getLink() shouldBe transformedInnboks.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getPrefererteKanaler() shouldBe transformedInnboks.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getSmsVarslingstekst() shouldBe externalInnboksInput.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getSmsVarslingstekst().shouldBeNull()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getEpostVarslingstekst() shouldBe transformedInnboks.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstekst().shouldBeNull()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(4_001)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = "Hei ".repeat(10)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstittel() shouldBe externalInnboksInput.getEpostVarslingstittel()
    }

    @Test
    fun `should allow null epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstittel().shouldBeNull()
    }

    @Test
    fun `do not allow epostVarslingstittel if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            epostVarslingstittel = "<p>Hei!</p>"
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow too long email titel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = "L".repeat(41)
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow empty email tittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = " "
        )
        shouldThrow<FieldValidationException> {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        }.message shouldContain "epostVarslingstittel"
    }
}
