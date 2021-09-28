package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

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

internal class StatusoppdateringLegacyTransformerTest {

    private val eventId = "1"

    private val mapper: ServiceUserMapper = mockk()

    private val transformer = StatusoppdateringLegacyTransformer(mapper)

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
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        val transformedNokkel = transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
        val transformedStatusoppdatering = transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)

        transformedNokkel.getFodselsnummer() `should be equal to` externalStatusoppdateringLegacy.getFodselsnummer()
        transformedNokkel.getSystembruker() `should be equal to` externalNokkelLegacy.getSystembruker()
        transformedNokkel.getEventId() `should be equal to` externalNokkelLegacy.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalStatusoppdateringLegacy.getGrupperingsId()
        transformedNokkel.getNamespace() `should be equal to` defaultNamespace
        transformedNokkel.getAppnavn() `should be equal to` defaultAppName

        transformedStatusoppdatering.getLink() `should be equal to` externalStatusoppdateringLegacy.getLink()
        transformedStatusoppdatering.getSikkerhetsnivaa() `should be equal to` externalStatusoppdateringLegacy.getSikkerhetsnivaa()
        transformedStatusoppdatering.getTidspunkt() `should be equal to` externalStatusoppdateringLegacy.getTidspunkt()
        transformedStatusoppdatering.getStatusGlobal() `should be equal to` externalStatusoppdateringLegacy.getStatusGlobal()
        transformedStatusoppdatering.getStatusIntern() `should be equal to` externalStatusoppdateringLegacy.getStatusIntern()
        transformedStatusoppdatering.getSakstema() `should be equal to` externalStatusoppdateringLegacy.getSakstema()
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }


    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(tooLongSystembruker)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(tooLongEventId)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithGrupperingsId(tooLongGrupperingsId)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithLink(tooLongLink)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithLink(invalidLink)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `should allow empty link`() {
        val emptyLink = ""
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithLink(emptyLink)
        val transformed = transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)

        externalStatusoppdateringLegacy.getLink() `should be equal to` transformed.getLink()
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow invalid statusGlobal`() {
        val invalidStatusGlobal = "invalidStatusGlobal"
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithStatusGlobal(invalidStatusGlobal)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "StatusGlobal"
    }

    @Test
    fun `should allow valid statusGlobal`() {
        val validStatusGlobal = "SENDT"
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithStatusGlobal(validStatusGlobal)

        transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
    }

    @Test
    fun `should allow valid statusGlobal field`() {
        val validStatusGlobal = "MOTTATT"
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithStatusGlobal(validStatusGlobal)

        transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
    }

    @Test
    fun `do not allow too long statusIntern`() {
        val tooLongStatusIntern = "S".repeat(101)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithStatusIntern(tooLongStatusIntern)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "statusIntern"
    }

    @Test
    fun `should allow statusIntern to be null`() {
        val validNullStatusIntern = null
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithStatusIntern(validNullStatusIntern)

        transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
    }

    @Test
    fun `do not allow too long sakstema`() {
        val tooLongSakstema = "S".repeat(51)
        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithSakstema(tooLongSakstema)

        invoking {
            runBlocking {
                transformer.toStatusoppdateringInternal(externalStatusoppdateringLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "sakstema"
    }

    @Test
    fun `should fetch event origin from service user map`() {
        val namespace = "thisNamespace"
        val appName = "thisApp"
        val systemBruker = "systembruker"

        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } returns NamespaceAppName(namespace, appName)

        val nokkelIntern = transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)

        nokkelIntern.getNamespace() `should be equal to` namespace
        nokkelIntern.getAppnavn() `should be equal to` appName
    }

    @Test
    fun `should bubble exception when service user mapping is missing`() {
        val systemBruker = "systembruker"

        val externalStatusoppdateringLegacy = AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } throws ServiceUserMappingException("")

        invoking {
            transformer.toNokkelInternal(externalNokkelLegacy, externalStatusoppdateringLegacy)
        } `should throw` ServiceUserMappingException::class
    }
}
