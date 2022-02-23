package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksLegacyObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OppgaveLegacyTransformerTest {

    private val eventId = "1"

    private val mapper: ServiceUserMapper = mockk()

    private val transformer = OppgaveLegacyTransformer(mapper)

    private val defaultNamespace = "namespace"
    private val defaultAppName = "appName"

    @BeforeEach
    fun initializeMock() {
        every { mapper.getNamespaceAppName(any()) } returns NamespaceAppName(defaultNamespace, defaultAppName)
    }

    @AfterEach
    fun cleanUpMock() {
        clearMocks(mapper)
    }

    @Test
    fun `should transform from external to internal`() {
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        val transformedNokkelLegacy = transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
        val transformedOppgaveLegacy = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformedNokkelLegacy.getFodselsnummer() `should be equal to` externalOppgaveLegacy.getFodselsnummer()
        transformedNokkelLegacy.getSystembruker() `should be equal to` externalNokkelLegacy.getSystembruker()
        transformedNokkelLegacy.getEventId() `should be equal to` externalNokkelLegacy.getEventId()
        transformedNokkelLegacy.getGrupperingsId() `should be equal to` externalOppgaveLegacy.getGrupperingsId()
        transformedNokkelLegacy.getNamespace() `should be equal to` defaultNamespace
        transformedNokkelLegacy.getAppnavn() `should be equal to` defaultAppName

        transformedOppgaveLegacy.getLink() `should be equal to` externalOppgaveLegacy.getLink()
        transformedOppgaveLegacy.getTekst() `should be equal to` externalOppgaveLegacy.getTekst()
        transformedOppgaveLegacy.getSikkerhetsnivaa() `should be equal to` externalOppgaveLegacy.getSikkerhetsnivaa()
        transformedOppgaveLegacy.getTidspunkt() `should be equal to` externalOppgaveLegacy.getTidspunkt()
        transformedOppgaveLegacy.getEksternVarsling() `should be equal to` externalOppgaveLegacy.getEksternVarsling()
        transformedOppgaveLegacy.getPrefererteKanaler() `should be equal to` externalOppgaveLegacy.getPrefererteKanaler()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(tooLongSystembruker)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(tooLongEventId)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithGrupperingsId(tooLongGrupperingsId)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithText(textWithMaxAllowedLength)

        runBlocking {
            transformer.toOppgaveInternal(externalOppgaveLegacy)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithText(emptyText)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithText(tooLongText)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithLink(tooLongLink)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithLink(invalidLink)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow empty link`() {
        val emptyLink = ""
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithLink(emptyLink)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val transformed = transformer.toOppgaveInternal(externalOppgaveLegacy)

        externalOppgaveLegacy.getPrefererteKanaler() `should be equal to` transformed.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val transformed = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformed.getSmsVarslingstekst() `should be equal to` externalOppgaveLegacy.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val transformed = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformed.getSmsVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val transformed = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformed.getEpostVarslingstekst() `should be equal to` externalOppgaveLegacy.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val transformed = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformed.getEpostVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(4_001)
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstittel`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstittel = "Hei ".repeat(10)
        )

        val transformedOppgave = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformedOppgave.getEpostVarslingstittel() `should be equal to` externalOppgaveLegacy.getEpostVarslingstittel()
    }

    @Test
    fun `should allow null epostVarslingstittel`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstittel = null
        )

        val transformedOppgave = transformer.toOppgaveInternal(externalOppgaveLegacy)

        transformedOppgave.getEpostVarslingstittel().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstittel if eksternVarsling is false`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = false,
            epostVarslingstittel = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow too long email titel`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstittel = "L".repeat(41)
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }

    @Test
    internal fun `should not allow empty email tittel`() {
        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy(
            eksternVarsling = true,
            epostVarslingstittel = " "
        )
        invoking {
            runBlocking {
                transformer.toOppgaveInternal(externalOppgaveLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstittel"
    }

    @Test
    fun `should fetch event origin from service user map`() {
        val namespace = "thisNamespace"
        val appName = "thisApp"
        val systemBruker = "systembruker"

        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } returns NamespaceAppName(namespace, appName)

        val nokkelIntern = transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)

        nokkelIntern.getNamespace() `should be equal to` namespace
        nokkelIntern.getAppnavn() `should be equal to` appName
    }

    @Test
    fun `should bubble exception when service user mapping is missing`() {
        val systemBruker = "systembruker"

        val externalOppgaveLegacy = AvroOppgaveLegacyObjectMother.createOppgaveLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } throws ServiceUserMappingException("")

        invoking {
            transformer.toNokkelInternal(externalNokkelLegacy, externalOppgaveLegacy)
        } `should throw` ServiceUserMappingException::class
    }
}
