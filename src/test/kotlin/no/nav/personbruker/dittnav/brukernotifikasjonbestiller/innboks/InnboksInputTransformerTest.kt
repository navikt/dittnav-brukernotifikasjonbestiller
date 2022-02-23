package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

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

internal class InnboksInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

    @Test
    fun `should transform from external to internal`() {
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        val (transformedNokkel, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getFodselsnummer() `should be equal to` externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() `should be equal to` externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() `should be equal to` externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() `should be equal to` externalNokkelInput.getAppnavn()

        transformedInnboks.getLink() `should be equal to` externalInnboksInput.getLink()
        transformedInnboks.getTekst() `should be equal to` externalInnboksInput.getTekst()
        transformedInnboks.getSikkerhetsnivaa() `should be equal to` externalInnboksInput.getSikkerhetsnivaa()
        transformedInnboks.getTidspunkt() `should be equal to` externalInnboksInput.getTidspunkt()
        transformedInnboks.getEksternVarsling() `should be equal to` externalInnboksInput.getEksternVarsling()
        transformedInnboks.getPrefererteKanaler() `should be equal to` externalInnboksInput.getPrefererteKanaler()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        val (transformedNokkel, _) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getEventId() `should be equal to` uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        val (transformedNokkel, _) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedNokkel.getEventId() `should be equal to` ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
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

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithText(tooLongText)

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(tooLongLink)

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(invalidLink)

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithLink(emptyLink)
        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getLink() `should be equal to` transformedInnboks.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getPrefererteKanaler() `should be equal to` transformedInnboks.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getSmsVarslingstekst() `should be equal to` externalInnboksInput.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getSmsVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        externalInnboksInput.getEpostVarslingstekst() `should be equal to` transformedInnboks.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(4_001)
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = "Hei ".repeat(10)
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstittel() `should be equal to` externalInnboksInput.getEpostVarslingstittel()
    }

    @Test
    fun `should allow null epostVarslingstittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = null
        )

        val (_, transformedInnboks) = InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)

        transformedInnboks.getEpostVarslingstittel().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstittel if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = false,
            epostVarslingstittel = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow too long email titel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = "L".repeat(41)
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow empty email tittel`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalInnboksInput = AvroInnboksInputObjectMother.createInnboksInput(
            eksternVarsling = true,
            epostVarslingstittel = " "
        )
        invoking {
            runBlocking {
                InnboksInputTransformer.toInternal(externalNokkelInput, externalInnboksInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }
}
