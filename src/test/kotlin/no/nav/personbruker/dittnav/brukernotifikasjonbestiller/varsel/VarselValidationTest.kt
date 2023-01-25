package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveTestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VarselValidationTest {

    @Test
    fun `beskjed med gyldige felter er gyldig`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(300),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        ).shouldBeValid()
    }

    @Test
    fun `oppgave med gyldige felter er gyldig`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            OppgaveTestData.oppgaveInput(
                tekst = "x".repeat(500),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        ).shouldBeValid()
    }

    @Test
    fun `innboks med gyldige felter er gyldig`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            AvroInnboksInputObjectMother.createInnboksInput(
                tekst = "x".repeat(500),
                link = "https://" + "x".repeat(192),
                epostVarslingstekst = "x".repeat(4000),
                smsVarslingstekst = "x".repeat(160),
                epostVarslingstittel = "x".repeat(40)
            )
        ).shouldBeValid()
    }

    @Test
    fun `valgfrie felter kan være null`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                //sikkerhetsnivaa = null,
                link = null,
                prefererteKanaler = null,
                smsVarslingstekst = null,
                epostVarslingstekst = null,
                epostVarslingstittel = null
            )
        ).shouldBeValid()
    }

    @Test
    fun `Finner alle feil i valideringen`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = null,
                sikkerhetsnivaa = 7,
                link = "nvsjhr"
            )
        ) shouldBeInvalidatedBy listOf(
            TekstValidator::class.java,
            LinkValidator::class.java,
            SikkerhetsnivaaValidator::class.java
        )
    }

    @Test
    fun `obligatoriske felter kan ikke være null`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = null
            )
        ) shouldBeInvalidatedBy TekstValidator::class.java
    }

    @Test
    fun `tekst kan maks være 300 tegn`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                tekst = "x".repeat(301)
            )
        ) shouldBeInvalidatedBy TekstValidator::class.java
    }

    @Test
    fun `optional link må være gyldig lenke og maks 200 tegn`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                link = "https://" + "x".repeat(193)
            )
        ) shouldBeInvalidatedBy LinkValidator::class.java

        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                link = "ugyldig-link"
            )
        ) shouldBeInvalidatedBy LinkValidator::class.java
    }

    @Test
    fun `link i beskjed kan være tom streng`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                link = ""
            )
        ).shouldBeValid()

        VarselValidation(
            NokkelTestData.nokkel(),
            OppgaveTestData.oppgaveInput(
                link = ""
            )
        ) shouldBeInvalidatedBy LinkValidator::class.java

        VarselValidation(
            NokkelTestData.nokkel(),
            AvroInnboksInputObjectMother.createInnboksInput(
                link = ""
            )
        ) shouldBeInvalidatedBy LinkValidator::class.java
    }

    @Test
    fun `sikkerhetsnivaa må være 3 eller 4`() {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                sikkerhetsnivaa = 5
            )
        ) shouldBeInvalidatedBy SikkerhetsnivaaValidator::class.java
    }

    @ParameterizedTest
    @ValueSource(strings = ["ABC", "SMS,ABC", ""])
    fun `optional prefererte kanaler må være SMS eller EPOST`(prefererteKanaler: String) {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                prefererteKanaler = prefererteKanaler.split(",")
            )
        ) shouldBeInvalidatedBy PrefererteKanalerValidator::class.java
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 161])
    fun `optional smstekst kan ikke være tom, og maks 160 tegn`(length: Int) {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                smsVarslingstekst = "x".repeat(length)
            )
        ) shouldBeInvalidatedBy SmstekstValidator::class.java
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 4001])
    fun `optional eposttekst kan ikke være tom, og maks 4000 tegn`(length: Int) {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                epostVarslingstekst = "x".repeat(length)
            )
        ) shouldBeInvalidatedBy EposttekstValidator::class.java
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 41])
    fun `optional eposttittel kan ikke være tom, og maks 40 tegn`(length: Int) {
        VarselValidation(
            NokkelTestData.nokkel(),
            BeskjedTestData.beskjedInput(
                epostVarslingstittel = "x".repeat(length)
            )
        ) shouldBeInvalidatedBy EposttittelValidator::class.java
    }
}

private infix fun VarselValidation.shouldBeInvalidatedBy(expectedValidator: Class<*>) =
    shouldBeInvalidatedBy(listOf(expectedValidator))

private infix fun VarselValidation.shouldBeInvalidatedBy(expectedValidators: List<Class<*>>) {
    isValid() shouldBe false
    failedValidators.map { it.javaClass } shouldContainExactlyInAnyOrder expectedValidators
}

private fun VarselValidation.shouldBeValid() {
    assert(isValid()) { "Validering feilet:" + failedValidators.map { it.description } }
}