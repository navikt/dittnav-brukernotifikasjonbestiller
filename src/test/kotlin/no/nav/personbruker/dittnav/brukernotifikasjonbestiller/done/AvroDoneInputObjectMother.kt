package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.input.DoneInput
import java.time.Instant

object AvroDoneInputObjectMother {

    private val defaultTidspunkt = Instant.now().toEpochMilli()

    fun createDoneInput(): DoneInput {
        return createDoneInput(defaultTidspunkt)
    }

    private fun createDoneInput(tidspunkt: Long): DoneInput {
        return DoneInput(
            tidspunkt
        )
    }
}
