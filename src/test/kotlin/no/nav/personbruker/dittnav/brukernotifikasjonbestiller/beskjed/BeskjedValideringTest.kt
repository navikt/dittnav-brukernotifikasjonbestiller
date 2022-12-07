package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeskjedValideringTest {

    @Test
    fun `beskjed med gyldige felter er gyldig`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(299)
            )
        ).isValid() shouldBe true
    }

    @Test
    fun `valgfrie felter kan være null`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                //sikkerhetsnivaa = null,
                link = null
            )
        ).isValid() shouldBe true
    }

    @Test
    fun `obligatoriske felter kan ikke være null`() {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = null
            )
        )
        validation.apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
                TekstIsUnder300Characters::class.java
            )
        }
    }

    @Test
    fun `tekst må være mindre enn 300 tegn`() {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(300)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            TekstIsUnder300Characters::class.java
        )
    }

    @Test
    fun `link må være gyldig linke og mindre enn 200 tegn`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                link = "https://" + "x".repeat(192)
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
                LinkIsURLUnder200Characters::class.java
            )
        }

        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                link = "ugyldig-link"
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
                LinkIsURLUnder200Characters::class.java
            )
        }

    }

    @Test
    fun `sikkerhetsnivaa må være 3 eller 4`() {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                sikkerhetsnivaa = 5
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            SikkerhetsnivaaIs3Or4::class.java
        )
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}