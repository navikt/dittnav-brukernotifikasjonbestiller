package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

internal class ValidateKtTest {

    private val currentDateTime = System.currentTimeMillis()

    @Test
    fun `Skal godkjenne validering av dateTime`() {
        validateDateTime(currentDateTime, "TIDSPUNKT") `should be equal to` currentDateTime
    }

    /*
    @Test
    fun `Skal kaste exception hvis dato ikke kan konverteres til LocalDateTime`() {
        invoking {
            validateDateTime(0, "TIDSPUNKT")
        } `should throw` FieldValidationException::class
    }

     */
}