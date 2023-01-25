package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class NokkelValidationTest {

    @Test
    fun `nøkkel kan ikke være null`() {
        val validation = VarselValidation(null, BeskjedTestData.beskjedInput())
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            HasNokkel::class.java
        )
    }

    @Test
    fun `nøkkel med gyldige felter er gyldig`() {
        val validation = VarselValidation(NokkelTestData.nokkel(), BeskjedTestData.beskjedInput())
        validation.isValid() shouldBe true
        validation.failedValidators.isEmpty() shouldBe true
    }

    @Test
    fun `eventid må være UUID eller ULID`() {
        VarselValidation(
            NokkelTestData.nokkel(eventId = UUID.randomUUID().toString()),
            BeskjedTestData.beskjedInput()
        ).isValid() shouldBe true

        VarselValidation(
            NokkelTestData.nokkel(eventId = ULID().nextULID()),
            BeskjedTestData.beskjedInput()
        ).isValid() shouldBe true

        VarselValidation(
            NokkelTestData.nokkel(eventId = "ugyldig"),
            BeskjedTestData.beskjedInput()
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(EventIdValidator::class.java)
        }

        VarselValidation(
            NokkelTestData.nokkel(eventId = null),
            BeskjedTestData.beskjedInput()
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(EventIdValidator::class.java)
        }
    }

    @Test
    fun `namespace kan ikke være null`() {
        VarselValidation(
            NokkelTestData.nokkel(namespace = null),
            BeskjedTestData.beskjedInput()
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(NamespaceValidator::class.java)
        }
    }

    @Test
    fun `appnavn kan ikke være null`() {
        VarselValidation(
            NokkelTestData.nokkel(appnavn = null),
            BeskjedTestData.beskjedInput()
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(AppnavnValidator::class.java)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["1234567891", "123456789100"])
    fun `fodselsnummer må være 11 tegn`(fodselsnummer: String?) {
        VarselValidation(
            NokkelTestData.nokkel(fodselsnummer = fodselsnummer),
            BeskjedTestData.beskjedInput()
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(FodselsnummerValidator::class.java)
        }
    }
}