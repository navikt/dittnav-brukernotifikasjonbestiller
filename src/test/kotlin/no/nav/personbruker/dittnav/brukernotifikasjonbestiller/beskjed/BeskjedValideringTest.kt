package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class BeskjedValideringTest {

    @Test
    fun `beskjed med gyldige felter er gyldig`() {
        val validation = BeskjedValidation(BeskjedTestData.beskjedInput(
            tekst = "x".repeat(199)
        ))
        validation.isValid() shouldBe true
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = [200])
    fun `tekst kan ikke være null`(length: Int?) {
        val tekst = length?.let { "x".repeat(length) }
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = tekst
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            TekstIsUnder200Characters::class.java
        )
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}