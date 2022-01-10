package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnboksLegacyTransformerTest {

    private val eventId = "1"

    private val mapper: ServiceUserMapper = mockk()

    private val transformer = InnboksLegacyTransformer(mapper)

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
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        val transformedNokkelLegacy = transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
        val transformedInnboksLegacy = transformer.toInnboksInternal(externalInnboksLegacy)

        transformedNokkelLegacy.getFodselsnummer() `should be equal to` externalInnboksLegacy.getFodselsnummer()
        transformedNokkelLegacy.getSystembruker() `should be equal to` externalNokkelLegacy.getSystembruker()
        transformedNokkelLegacy.getEventId() `should be equal to` externalNokkelLegacy.getEventId()
        transformedNokkelLegacy.getGrupperingsId() `should be equal to` externalInnboksLegacy.getGrupperingsId()
        transformedNokkelLegacy.getNamespace() `should be equal to` defaultNamespace
        transformedNokkelLegacy.getAppnavn() `should be equal to` defaultAppName

        transformedInnboksLegacy.getLink() `should be equal to` externalInnboksLegacy.getLink()
        transformedInnboksLegacy.getTekst() `should be equal to` externalInnboksLegacy.getTekst()
        transformedInnboksLegacy.getSikkerhetsnivaa() `should be equal to` externalInnboksLegacy.getSikkerhetsnivaa()
        transformedInnboksLegacy.getTidspunkt() `should be equal to` externalInnboksLegacy.getTidspunkt()
        transformedInnboksLegacy.getEksternVarsling() `should be equal to` externalInnboksLegacy.getEksternVarsling()
        transformedInnboksLegacy.getPrefererteKanaler() `should be equal to` externalInnboksLegacy.getPrefererteKanaler()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(tooLongSystembruker)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(tooLongEventId)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithGrupperingsId(tooLongGrupperingsId)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithText(textWithMaxAllowedLength)

        runBlocking {
            transformer.toInnboksInternal(externalInnboksLegacy)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithText(emptyText)

        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithText(tooLongText)

        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithLink(tooLongLink)

        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithLink(invalidLink)

        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithLink(emptyLink)
        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        externalInnboksLegacy.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        externalInnboksLegacy.getPrefererteKanaler() `should be equal to` transformed.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        externalInnboksLegacy.getSmsVarslingstekst() `should be equal to` transformed.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        transformed.getSmsVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        externalInnboksLegacy.getEpostVarslingstekst() `should be equal to` transformed.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val transformed = transformer.toInnboksInternal(externalInnboksLegacy)

        transformed.getEpostVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(10_001)
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toInnboksInternal(externalInnboksLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    fun `should fetch event origin from service user map`() {
        val namespace = "thisNamespace"
        val appName = "thisApp"
        val systemBruker = "systembruker"

        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } returns NamespaceAppName(namespace, appName)

        val nokkelIntern = transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)

        nokkelIntern.getNamespace() `should be equal to` namespace
        nokkelIntern.getAppnavn() `should be equal to` appName
    }

    @Test
    fun `should bubble exception when service user mapping is missing`() {
        val systemBruker = "systembruker"

        val externalInnboksLegacy = AvroInnboksLegacyObjectMother.createInnboksLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } throws ServiceUserMappingException("")

        invoking {
            transformer.toNokkelInternal(externalNokkelLegacy, externalInnboksLegacy)
        } `should throw` ServiceUserMappingException::class
    }
}
