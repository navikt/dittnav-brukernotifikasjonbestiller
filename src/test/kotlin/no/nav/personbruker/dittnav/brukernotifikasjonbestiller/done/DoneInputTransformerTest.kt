package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

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

internal class DoneInputTransformerTest {

    private val eventId = "11112222-1234-1234-1234-1234567890ab"

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
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(eventId)

        every { CurrentTimeHelper.nowInEpochMillis() } returns epochTimeMillis

        val (transformedNokkel, transformedDone) = DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)

        transformedNokkel.getFodselsnummer() shouldBe externalNokkelInput.getFodselsnummer()
        transformedNokkel.getEventId() shouldBe externalNokkelInput.getEventId()
        transformedNokkel.getGrupperingsId() shouldBe externalNokkelInput.getGrupperingsId()
        transformedNokkel.getNamespace() shouldBe externalNokkelInput.getNamespace()
        transformedNokkel.getAppnavn() shouldBe externalNokkelInput.getAppnavn()

        transformedDone.getTidspunkt() shouldBe externalDoneInput.getTidspunkt()
        transformedDone.getBehandlet() shouldBe epochTimeMillis
    }

    @Test
    fun `should allow UUID as eventid`() {
        val uuidEventId = UUID.randomUUID().toString()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(uuidEventId)
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()

        val (transformedNokkel, _) = DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)

        transformedNokkel.getEventId() shouldBe uuidEventId
    }

    @Test
    fun `should allow ULID as eventid`() {
        val ulidEventId = ULID().nextULID()

        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ulidEventId)
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()

        val (transformedNokkel, _) = DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)

        transformedNokkel.getEventId() shouldBe ulidEventId
    }

    @Test
    fun `should allow eventId that is not ulid or uuid`() {
        val legacyEventId = "1234"
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventId(legacyEventId)
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()

        val (transformedNokkel, _) = DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)

        transformedNokkel.getEventId() shouldBe legacyEventId
    }

    @Test
    fun `do not allow empty fodselsnummer`() {
        val fodselsnummerEmpty = ""
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, fodselsnummerEmpty)
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)
            }
        }.message shouldContain "fodselsnummer"
    }

    @Test
    fun `do not allow too long fodselsnummer`() {
        val tooLongFnr = "1".repeat(12)
        val externalNokkelInput = AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndFnr(eventId, tooLongFnr)
        val externalDoneInput = AvroDoneInputObjectMother.createDoneInput()

        shouldThrow<FieldValidationException> {
            runBlocking {
                DoneInputTransformer.toInternal(externalNokkelInput, externalDoneInput)
            }
        }.message shouldContain "fodselsnummer"
    }
}
