package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.EposttekstValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.EposttittelValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.LinkValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.PrefererteKanalerValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.SikkerhetsnivaaValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.SmstekstValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.TekstValidator
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.VarselValidation
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.AvroOppgaveInputObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VarselValidationTest {

    @Test
    fun `validere avro key og value`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(eventId = null), BeskjedTestData.beskjedInput()
        )

        validation.isValid() shouldBe false
    }


    @Test
    fun `beskjed med gyldige felter er gyldig`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(300),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        )
        assert(validation.isValid()) { "Validering feilet:" + validation.failedValidators.map { it.description } }
    }

    @Test
    fun `oppgave med gyldige felter er gyldig`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            AvroOppgaveInputObjectMother.createOppgaveInput(
                tekst = "x".repeat(500),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        )
        assert(validation.isValid()) { "Validering feilet:" + validation.failedValidators.map { it.description } }
    }

    @Test
    fun `innboks med gyldige felter er gyldig`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            AvroInnboksInputObjectMother.createInnboksInput(
                tekst = "x".repeat(500),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        )
        assert(validation.isValid()) { "Validering feilet:" + validation.failedValidators.map { it.description } }
    }

    @Test
    fun `valgfrie felter kan være null`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                //sikkerhetsnivaa = null,
                link = null,
                prefererteKanaler = null,
                smsVarslingstekst = null,
                epostVarslingstekst = null,
                epostVarslingstittel = null
            )
        )
        assert(validation.isValid()) { "Validering feilet:" + validation.failedValidators.map { it.description } }
    }

    @Test
    fun `obligatoriske felter kan ikke være null`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = null
            )
        )
        validation.apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder listOf(
                TekstValidator::class.java
            )
        }
    }

    @Test
    fun `tekst kan maks være 300 tegn`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(301)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            TekstValidator::class.java
        )
    }

    @Test
    fun `optional link må være gyldig lenke og maks 200 tegn`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                link = "https://" + "x".repeat(193)
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(
                LinkValidator::class.java
            )
        }

        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                link = "ugyldig-link"
            )
        ).apply {
            isValid() shouldBe false
            failedValidators.map { it.javaClass } shouldBe listOf(
                LinkValidator::class.java
            )
        }
    }

    @Test
    fun `sikkerhetsnivaa må være 3 eller 4`() {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                sikkerhetsnivaa = 5
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            SikkerhetsnivaaValidator::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ABC", "SMS,ABC", ""])
    fun `optional prefererte kanaler må være SMS eller EPOST`(prefererteKanaler: String) {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                prefererteKanaler = prefererteKanaler.split(",")
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            PrefererteKanalerValidator::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 161])
    fun `optional smstekst kan ikke være tom, og maks 160 tegn`(length: Int) {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                smsVarslingstekst = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            SmstekstValidator::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 4001])
    fun `optional eposttekst kan ikke være tom, og maks 4000 tegn`(length: Int) {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                epostVarslingstekst = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            EposttekstValidator::class.java
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 41])
    fun `optional eposttittel kan ikke være tom, og maks 40 tegn`(length: Int) {
        val validation = VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                epostVarslingstittel = "x".repeat(length)
            )
        )
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            EposttittelValidator::class.java
        )
    }
}