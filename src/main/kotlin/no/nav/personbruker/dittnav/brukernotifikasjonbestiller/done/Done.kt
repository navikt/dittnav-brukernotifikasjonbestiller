package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import java.time.LocalDateTime

data class Done(
    val eventId: String,
    val forstBehandlet: LocalDateTime,
    val fodselsnummer: String
    )