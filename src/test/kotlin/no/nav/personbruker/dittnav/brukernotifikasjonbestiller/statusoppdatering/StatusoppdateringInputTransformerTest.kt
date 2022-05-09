package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.CurrentTimeHelper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

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

        transformedNokkel.getFodselsnummer() shouldBe externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() shouldBe externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() shouldBe externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() shouldBe externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() shouldBe externalNokkelInput.getAppnavn()

        transformedStatusoppdatering.getLink() shouldBe externalStatusoppdateringInput.getLink()
        transformedStatusoppdatering.getSikkerhetsnivaa() shouldBe externalStatusoppdateringInput.getSikkerhetsnivaa()
        transformedStatusoppdatering.getTidspunkt() shouldBe externalStatusoppdateringInput.getTidspunkt()
        transformedStatusoppdatering.getBehandlet() shouldBe epochTimeMillis
        transformedStatusoppdatering.getStatusGlobal() shouldBe externalStatusoppdateringInput.getStatusGlobal()
        transformedStatusoppdatering.getStatusIntern() shouldBe externalStatusoppdateringInput.getStatusIntern()
        transformedStatusoppdatering.getSakstema() shouldBe externalStatusoppdateringInput.getSakstema()
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        val (transformedNokkel, _) = StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)

        transformedNokkel.getEventId() shouldBe uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        val (transformedNokkel, _) = StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)

        transformedNokkel.getEventId() shouldBe ulidEventId
    }

    @Test
    fun `should not allow eventId that is not ulid or uuid`() {
        val invalidEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(invalidEventId)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "eventId"
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long grupperingsId`() {
        val tooLongGrupperingsId = "G".repeat(101)
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId(eventId, tooLongGrupperingsId)

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "grupperingsId"
    }

    @Test
    fun `do not allow too long link`() {
        val tooLongLink = "http://" + "L".repeat(201)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithLink(tooLongLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `do not allow invalid link`() {
        val invalidLink = "invalidUrl"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithLink(invalidLink)

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "link"
    }

    @Test
    fun `do not allow invalid sikkerhetsnivaa`() {
        val invalidSikkerhetsnivaa = 2
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "Sikkerhetsnivaa"
    }

    @Test
    fun `do not allow invalid statusGlobal`() {
        val invalidStatusGlobal = "invalidStatusGlobal"
        val externalStatusoppdateringInput = AvroStatusoppdateringInputObjectMother.createStatusoppdateringInputWithStatusGlobal(invalidStatusGlobal)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "StatusGlobal"
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

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "statusIntern"
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

        shouldThrow<FieldValidationException> {
            runBlocking {
                StatusoppdateringInputTransformer.toInternal(externalNokkelInput, externalStatusoppdateringInput)
            }
        }.message shouldContain "sakstema"
    }
}
