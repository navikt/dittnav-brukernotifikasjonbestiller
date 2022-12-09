package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BeskjedValideringTest {

    @Test
    fun `beskjed med gyldige felter er gyldig`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(300),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        ).isValid() shouldBe true
    }

    @Test
    fun `valgfrie felter kan være null`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                //sikkerhetsnivaa = null,
                link = null,
                prefererteKanaler = null,
                smsVarslingstekst = null,
                epostVarslingstekst = null,
                epostVarslingstittel = null
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
                TekstIsMax300Characters::class.java
            )
        }
    }

    @Test
    fun `tekst kan maks være 300 tegn`() {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(301)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
            TekstIsMax300Characters::class.java
        )
    }

    @Test
    fun `optional link må være gyldig lenke og maks 200 tegn`() {
        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                link = "https://" + "x".repeat(193)
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(
                LinkIsURLandMax200Characters::class.java
            )
        }

        BeskjedValidation(
            BeskjedTestData.beskjedInput(
                link = "ugyldig-link"
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(
                LinkIsURLandMax200Characters::class.java
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
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            SikkerhetsnivaaIs3Or4::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ABC", "SMS,ABC", ""])
    fun `optional prefererte kanaler må være SMS eller EPOST`(prefererteKanaler: String) {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                prefererteKanaler = prefererteKanaler.split(",")
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            PrefererteKanalerIsSMSorEpost::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 161])
    fun `optional smstekst kan ikke være tom, og maks 160 tegn`(length: Int) {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                smsVarslingstekst = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            SmstekstIsMax160Characters::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 4001])
    fun `optional eposttekst kan ikke være tom, og maks 4000 tegn`(length: Int) {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                epostVarslingstekst = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            EposttekstIsMax4000Characters::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 41])
    fun `optional eposttittel kan ikke være tom, og maks 40 tegn`(length: Int) {
        val validation = BeskjedValidation(
            BeskjedTestData.beskjedInput(
                epostVarslingstittel = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            EposttittelIsMax40Characters::class.java
        )
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}