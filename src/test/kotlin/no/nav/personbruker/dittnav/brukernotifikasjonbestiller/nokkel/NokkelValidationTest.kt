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
            NamespaceIsUnder64Characters::class.java,
            AppnavnIsUnder100Characters::class.java,
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

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = [64])
    fun `namespace må være under 64 tegn`(length: Int?) {
        val namespace = length?.let { "x".repeat(it) }
        NokkelValidation(
            NokkelTestData.nokkel(namespace = namespace)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(NamespaceIsUnder64Characters::class.java)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = [101])
    fun `appnavn må være under 101 tegn`(length: Int?) {
        val appnavn = length?.let { "x".repeat(it) }
        NokkelValidation(
            NokkelTestData.nokkel(appnavn = appnavn)
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(AppnavnIsUnder100Characters::class.java)
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