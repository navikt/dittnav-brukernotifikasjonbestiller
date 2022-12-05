package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class NokkelValidationTest {

    @Test
    fun `nøkkel kan ikke være null`() {
        val validation = NokkelValidation(null)
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            HasNokkel::class.java,
            FodselsnummerIs11Characters::class.java,
            HasNamespace::class.java,
            HasAppnavn::class.java,
            EventIdIsUUIDorULID::class.java
        )
    }

    @Test
    fun `nøkkel med gyldige felter er gyldig`() {
        val validation = NokkelValidation(NokkelTestData.nokkel())
        validation.isValid() shouldBe true
        validation.failedValidators.isEmpty() shouldBe true
    }

    @Test
    fun `eventid må være UUID eller ULID`() {
        NokkelValidation(
            NokkelTestData.nokkel(eventId = UUID.randomUUID().toString())
        ).isValid() shouldBe true

        NokkelValidation(
            NokkelTestData.nokkel(eventId = ULID().nextULID())
        ).isValid() shouldBe true

        NokkelValidation(
            NokkelTestData.nokkel(eventId = "ugyldig")
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(EventIdIsUUIDorULID::class.java)
        }

        NokkelValidation(
            NokkelTestData.nokkel(eventId = null)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(EventIdIsUUIDorULID::class.java)
        }
    }

    @Test
    fun `namespace kan ikke være null`() {
        NokkelValidation(
            NokkelTestData.nokkel(namespace = null)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(HasNamespace::class.java)
        }
    }

    @Test
    fun `appnavn kan ikke være null`() {
        NokkelValidation(
            NokkelTestData.nokkel(appnavn = null)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(HasAppnavn::class.java)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["1234567891", "123456789100"])
    fun `fodselsnummer må være 11 tegn`(fodselsnummer: String?) {
        NokkelValidation(
            NokkelTestData.nokkel(fodselsnummer = fodselsnummer)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(FodselsnummerIs11Characters::class.java)
        }
    }
}