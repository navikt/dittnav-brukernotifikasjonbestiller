package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DoneTransformerTest {

    private val eventId = "1"

    private val mapper: ServiceUserMapper = mockk()

    private val transformer = DoneLegacyTransformer(mapper)

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
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        val transformedNokkel = transformer.toNokkelInternal(externalNokkel, externalDone)
        val transformedDone = transformer.toDoneInternal(externalDone)

        transformedNokkel.getEventId() `should be equal to` externalNokkel.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalDone.getGrupperingsId()
        transformedNokkel.getFodselsnummer() `should be equal to` externalDone.getFodselsnummer()
        transformedNokkel.getNamespace() `should be equal to` defaultNamespace
        transformedNokkel.getAppnavn() `should be equal to` defaultAppName
        transformedNokkel.getSystembruker() `should be equal to` externalNokkel.getSystembruker()

        transformedDone.getTidspunkt() `should be equal to` externalDone.getTidspunkt()

    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacyWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacyWithFodselsnummer(tooLongFnr)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long systembruker`() {
        val tooLongSystembruker = "P".repeat(101)
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(tooLongSystembruker)
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `do not allow too long eventId`() {
        val tooLongEventId = "1".repeat(51)
        val externalNokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(tooLongEventId)
        val externalDone = AvroDoneLegacyObjectMother.createDoneLegacy()

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkel, externalDone)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalDoneLegacy = AvroDoneLegacyObjectMother.createDoneLegacyWithGrupperingsId(tooLongGrupperingsId)
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(eventId)

        invoking {
            runBlocking {
                transformer.toNokkelInternal(externalNokkelLegacy, externalDoneLegacy)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `should fetch event origin from service user map`() {
        val namespace = "thisNamespace"
        val appName = "thisApp"
        val systemBruker = "systembruker"

        val externalDoneLegacy = AvroDoneLegacyObjectMother.createDoneLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } returns NamespaceAppName(namespace, appName)

        val nokkelIntern = transformer.toNokkelInternal(externalNokkelLegacy, externalDoneLegacy)

        nokkelIntern.getNamespace() `should be equal to` namespace
        nokkelIntern.getAppnavn() `should be equal to` appName
    }

    @Test
    fun `should bubble exception when service user mapping is missing`() {
        val systemBruker = "systembruker"

        val externalDoneLegacy = AvroDoneLegacyObjectMother.createDoneLegacy()
        val externalNokkelLegacy = AvroNokkelLegacyObjectMother.createNokkelLegacyWithSystembruker(systemBruker)

        every { mapper.getNamespaceAppName(systemBruker) } throws ServiceUserMappingException("")

        invoking {
            transformer.toNokkelInternal(externalNokkelLegacy, externalDoneLegacy)
        } `should throw` ServiceUserMappingException::class
    }
}
