package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import de.huxhorn.sulky.ulid.ULID
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.CurrentTimeHelper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.`with message containing`
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

internal class StatusoppdateringInputTransformerTest {

    private val eventId = UUID.randomUUID().toString()

    private val epochTimeMillis = Instant.now().toEpochMilli()

    @BeforeEach
    fun setupMock() {
        mockkObject(CurrentTimeHelper)
    }

    @AfterEach
    fun clearMock() {
        unmockkObject(CurrentTimeHelper)
    }

    @Test
    fun `should transform from external to internal`() {
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        every { CurrentTimeHelper.nowInEpochMillis() } returns epochTimeMillis

        val (transformedNokkel, transformedStatusoppdatering) = StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)

        transformedNokkel.getFodselsnummer() `should be equal to` externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() `should be equal to` externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() `should be equal to` externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() `should be equal to` externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() `should be equal to` externalNokkelInput.getAppnavn()

        transformedStatusoppdatering.getLink() `should be equal to` externalStatusoppdateringInput.getLink()
        transformedStatusoppdatering.getSikkerhetsnivaa() `should be equal to` externalStatusoppdateringInput.getSikkerhetsnivaa()
        transformedStatusoppdatering.getTidspunkt() `should be equal to` externalStatusoppdateringInput.getTidspunkt()
        transformedStatusoppdatering.getBehandlet() `should be equal to` epochTimeMillis
        transformedStatusoppdatering.getStatusGlobal() `should be equal to` externalStatusoppdateringInput.getStatusGlobal()
        transformedStatusoppdatering.getStatusIntern() `should be equal to` externalStatusoppdateringInput.getStatusIntern()
        transformedStatusoppdatering.getSakstema() `should be equal to` externalStatusoppdateringInput.getSakstema()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        val (transformedNokkel, _) = StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)

        transformedNokkel.getEventId() `should be equal to` uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        val (transformedNokkel, _) = StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)

        transformedNokkel.getEventId() `should be equal to` ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "grupperingsId"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithLink(tooLongLink)

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithLink(invalidLink)

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "link"
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow invalid statusGlobal`() {
        val invalidStatusGlobal = "invalidStatusGlobal"
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusGlobal(invalidStatusGlobal)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "StatusGlobal"
    }

    @Test
    fun `should allow valid statusGlobal`() {
        val validStatusGlobal = "SENDT"
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusGlobal(validStatusGlobal)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
    }

    @Test
    fun `should allow valid statusGlobal field`() {
        val validStatusGlobal = "MOTTATT"
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusGlobal(validStatusGlobal)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
    }

    @Test
    fun `do not allow too long statusIntern`() {
        val tooLongStatusIntern = "S".repeat(101)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusIntern(tooLongStatusIntern)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "statusIntern"
    }

    @Test
    fun `should allow statusIntern to be null`() {
        val validNullStatusIntern = null
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusIntern(validNullStatusIntern)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
    }

    @Test
    fun `do not allow too long sakstema`() {
        val tooLongSakstema = "S".repeat(51)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithSakstema(tooLongSakstema)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        invoking {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        } `should throw` FieldValidationException::class `with message containing` "sakstema"
    }
}
