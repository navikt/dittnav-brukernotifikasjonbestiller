package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.util.*

internal class BeskjedInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

    @Test
    fun `should transform from external to internal`() {
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        val (transformedNokkel, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        transformedNokkel.getFodselsnummer() `should be equal to` externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() `should be equal to` externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() `should be equal to` externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() `should be equal to` externalNokkelInput.getAppnavn()

        transformedBeskjed.getLink() `should be equal to` externalBeskjedInput.getLink()
        transformedBeskjed.getTekst() `should be equal to` externalBeskjedInput.getTekst()
        transformedBeskjed.getSikkerhetsnivaa() `should be equal to` externalBeskjedInput.getSikkerhetsnivaa()
        transformedBeskjed.getTidspunkt() `should be equal to` externalBeskjedInput.getTidspunkt()
        transformedBeskjed.getSynligFremTil() `should be equal to` externalBeskjedInput.getSynligFremTil()
        transformedBeskjed.getEksternVarsling() `should be equal to` externalBeskjedInput.getEksternVarsling()
        transformedBeskjed.getPrefererteKanaler() `should be equal to` externalBeskjedInput.getPrefererteKanaler()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()

        val (transformedNokkel, _) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        transformedNokkel.getEventId() `should be equal to` uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()

        val (transformedNokkel, _) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        transformedNokkel.getEventId() `should be equal to` ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)
        val beskjedUtenSynligTilSatt = AvroBeskjedInputObjectMother.createBeskjedInputWithSynligFremTil(null)

        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, beskjedUtenSynligTilSatt)

        transformedBeskjed.getSynligFremTil().`should be null`()
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithText(textWithMaxAllowedLength)

        runBlocking {
            BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithText(emptyText)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithText(tooLongText)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithLink(tooLongLink)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithLink(invalidLink)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithLink(emptyLink)
        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        externalBeskjedInput.getLink() `should be equal to` transformedBeskjed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInputWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        externalBeskjedInput.getPrefererteKanaler() `should be equal to` transformedBeskjed.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        externalBeskjedInput.getSmsVarslingstekst() `should be equal to` transformedBeskjed.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        transformedBeskjed.getSmsVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        externalBeskjedInput.getEpostVarslingstekst() `should be equal to` transformedBeskjed.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val (_, transformedBeskjed) = BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)

        transformedBeskjed.getEpostVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(10_001)
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalBeskjedInput = AvroBeskjedInputObjectMother.createBeskjedInput(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        invoking {
            runBlocking {
                BeskjedInputTransformer.toInternal(externalNokkelInput, externalBeskjedInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }
}
