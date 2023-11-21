package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.input.DoneInput
import java.time.Instant

object DoneTestData {

    fun doneInput(tidspunkt: Long = Instant.now().toEpochMilli()) =
        DoneInput(
            tidspunkt
        )
}
