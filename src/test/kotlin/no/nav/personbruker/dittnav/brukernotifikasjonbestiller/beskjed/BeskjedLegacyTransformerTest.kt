package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

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

internal class BeskjedLegacyTransformerTest {

    private val eventId = "1"

    private val mapper: ServiceUserMapper = mockk()

    private val transformer = BeskjedLegacyTransformer(mapper)

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
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        val transformedNokkelLegacy = transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
        val transformedBeskjed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        transformedNokkelLegacy.getFodselsnummer() `should be equal to` externalBeskjedLegacy.getFodselsnummer()
        transformedNokkelLegacy.getSystembruker() `should be equal to` externalNokkelLegacy.getSystembruker()
        transformedNokkelLegacy.getEventId() `should be equal to` externalNokkelLegacy.getEventId()
        transformedNokkelLegacy.getGrupperingsId() `should be equal to` externalBeskjedLegacy.getGrupperingsId()
        transformedNokkelLegacy.getNamespace() `should be equal to` defaultNamespace
        transformedNokkelLegacy.getAppnavn() `should be equal to` defaultAppName

        transformedBeskjed.getLink() `should be equal to` externalBeskjedLegacy.getLink()
        transformedBeskjed.getTekst() `should be equal to` externalBeskjedLegacy.getTekst()
        transformedBeskjed.getSikkerhetsnivaa() `should be equal to` externalBeskjedLegacy.getSikkerhetsnivaa()
        transformedBeskjed.getTidspunkt() `should be equal to` externalBeskjedLegacy.getTidspunkt()
        transformedBeskjed.getEksternVarsling() `should be equal to` externalBeskjedLegacy.getEksternVarsling()
        transformedBeskjed.getPrefererteKanaler() `should be equal to` externalBeskjedLegacy.getPrefererteKanaler()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val beskjedUtenSynligTilSatt = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithSynligFremTil(null)

        val transformed = transformer.toBeskjedInternal(beskjedUtenSynligTilSatt)

        transformed.getSynligFremTil().`should be null`()
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(tooLongSystembruker)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(tooLongEventId)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithGrupperingsId(tooLongGrupperingsId)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(300)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithText(textWithMaxAllowedLength)

        runBlocking {
            transformer.toBeskjedInternal(externalBeskjedLegacy)
        }
    }

    @Test
    fun `do not allow empty tekst`() {
        val emptyText = ""
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithText(emptyText)

        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long tekst`() {
        val tooLongText = "T".repeat(501)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithText(tooLongText)

        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "tekst"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithLink(tooLongLink)

        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithLink(invalidLink)

        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithLink(emptyLink)
        val transformed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        externalBeskjedLegacy.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow prefererteKanaler if eksternVarsling is false`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = false, prefererteKanaler = listOf(PreferertKanal.SMS.toString()))
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `do not allow unknown preferert kanal`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = listOf("unknown"))
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "prefererteKanaler"
    }

    @Test
    fun `should allow empty prefererteKanaler`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithEksternVarslingAndPrefererteKanaler(eksternVarsling = true, prefererteKanaler = emptyList())
        val transformed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        externalBeskjedLegacy.getPrefererteKanaler() `should be equal to` transformed.getPrefererteKanaler()
    }

    @Test
    fun `should transform smsVarslingstekst`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(160)
        )

        val transformedBeskjed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        externalBeskjedLegacy.getSmsVarslingstekst() `should be equal to` transformedBeskjed.getSmsVarslingstekst()
    }

    @Test
    fun `should allow null smsVarslingstekst`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            smsVarslingstekst = null
        )

        val transformedBeskjed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        transformedBeskjed.getSmsVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow smsVarslingstekst if eksternVarsling is false`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = false,
            smsVarslingstekst = "L".repeat(160)
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow too long sms text`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            smsVarslingstekst = "L".repeat(161)
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    internal fun `should not allow empty sms text`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            smsVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "smsVarslingstekst"
    }

    @Test
    fun `should transform epostVarslingstekst`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "Hei ".repeat(20)
        )

        val transformedBeskjed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        externalBeskjedLegacy.getEpostVarslingstekst() `should be equal to` transformedBeskjed.getEpostVarslingstekst()
    }

    @Test
    fun `should allow null epostVarslingstekst`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            epostVarslingstekst = null
        )

        val transformedBeskjed = transformer.toBeskjedInternal(externalBeskjedLegacy)

        transformedBeskjed.getEpostVarslingstekst().`should be null`()
    }

    @Test
    fun `do not allow epostVarslingstekst if eksternVarsling is false`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = false,
            epostVarslingstekst = "<p>Hei!</p>"
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow too long email text`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            epostVarslingstekst = "L".repeat(10_001)
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    internal fun `should not allow empty email text`() {
        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(
            eksternVarsling = true,
            epostVarslingstekst = " "
        )
        invoking {
            runBlocking {
                transformer.toBeskjedInternal(externalBeskjedLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "epostVarslingstekst"
    }

    @Test
    fun `should fetch event origin from service user map`() {
        val namespace = "thisNamespace"
        val appName = "thisApp"
        val systemBruker = "systembruker"

        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } returns NamespaceAppName(namespace, appName)

        val nokkelIntern = transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)

        nokkelIntern.getNamespace() `should be equal to` namespace
        nokkelIntern.getAppnavn() `should be equal to` appName
    }

    @Test
    fun `should bubble exception when service user mapping is missing`() {
        val systemBruker = "systembruker"

        val externalBeskjedLegacy = AvroBeskjedLegacyObjectMother.createBeskjedLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } throws ServiceUserMappingException("")

        invoking {
            transformer.toNokkelInternal(externalNokkelLegacy, externalBeskjedLegacy)
        } `should throw` ServiceUserMappingException::class
    }
}
